/*******************************************************************************
 * Copyright (c) 2012-2017 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.languageserver.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.typefox.lsapi.CompletionItem;
import io.typefox.lsapi.CompletionList;
import io.typefox.lsapi.DocumentHighlight;
import io.typefox.lsapi.Hover;
import io.typefox.lsapi.Location;
import io.typefox.lsapi.SignatureHelp;
import io.typefox.lsapi.SymbolInformation;
import io.typefox.lsapi.TextDocumentSyncKind;
import io.typefox.lsapi.TextEdit;
import org.eclipse.che.api.languageserver.DtoConverter;
import org.eclipse.che.api.languageserver.exception.LanguageServerException;
import org.eclipse.che.api.languageserver.registry.LSOperation;
import org.eclipse.che.api.languageserver.registry.LanguageServerRegistry;
import org.eclipse.che.api.languageserver.registry.LanguageServerRegistryImpl;
import org.eclipse.che.api.languageserver.server.dto.DtoServerImpls.CompletionListDTOImpl;
import org.eclipse.che.api.languageserver.shared.lsapi.CompletionItemDTO;
import org.eclipse.che.api.languageserver.shared.lsapi.DidChangeTextDocumentParamsDTO;
import org.eclipse.che.api.languageserver.shared.lsapi.DidCloseTextDocumentParamsDTO;
import org.eclipse.che.api.languageserver.shared.lsapi.DidOpenTextDocumentParamsDTO;
import org.eclipse.che.api.languageserver.shared.lsapi.DidSaveTextDocumentParamsDTO;
import org.eclipse.che.api.languageserver.shared.lsapi.DocumentFormattingParamsDTO;
import org.eclipse.che.api.languageserver.shared.lsapi.DocumentOnTypeFormattingParamsDTO;
import org.eclipse.che.api.languageserver.shared.lsapi.DocumentRangeFormattingParamsDTO;
import org.eclipse.che.api.languageserver.shared.lsapi.DocumentSymbolParamsDTO;
import org.eclipse.che.api.languageserver.shared.lsapi.ReferenceParamsDTO;
import org.eclipse.che.api.languageserver.shared.lsapi.TextDocumentPositionParamsDTO;
import org.eclipse.che.api.languageserver.shared.model.impl.InitializedServerImpl;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * REST API for the textDocument/* services defined in
 * https://github.com/Microsoft/vscode-languageserver-protocol Dispatches onto
 * the {@link LanguageServerRegistryImpl}.
 */
@Singleton
@Path("languageserver/textDocument")
public class TextDocumentService {

    private static final int TIMEOUT = 5000;

    private static final String FILE_PROJECTS = "file:///projects";

    private final LanguageServerRegistry languageServerRegistry;

    @Inject
    public TextDocumentService(LanguageServerRegistry languageServerRegistry) {
        this.languageServerRegistry = languageServerRegistry;
    }

    static String prefixURI(String relativePath) {
        return FILE_PROJECTS + relativePath;
    }

    static String removePrefixUri(String uri) {
        if (uri.startsWith(FILE_PROJECTS)) {
            return uri.substring(FILE_PROJECTS.length());
        }
        return uri;
    }

    @POST
    @Path("completion")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public CompletionList completion(TextDocumentPositionParamsDTO textDocumentPositionParams) {
        textDocumentPositionParams.getTextDocument().setUri(prefixURI(textDocumentPositionParams.getTextDocument().getUri()));
        textDocumentPositionParams.setUri(prefixURI(textDocumentPositionParams.getUri()));
        ArrayList<CompletionItemDTO> items = new ArrayList<>();
        CompletionListDTOImpl result = new CompletionListDTOImpl();
        LSOperation<Collection<InitializedServerImpl>, CompletionList> op = new LSOperation<Collection<InitializedServerImpl>, CompletionList>() {

            @Override
            public boolean canDo(Collection<InitializedServerImpl> element) {
                return element.stream().anyMatch(server -> server.getInitializeResult().getCapabilities().getCompletionProvider() != null);
            }

            @Override
            public CompletableFuture<CompletionList> start(Collection<InitializedServerImpl> element) {
                return CompletableFuture.supplyAsync(() -> {
                    ArrayList<CompletionItemDTO> items = new ArrayList<>();
                    CompletionListDTOImpl list = new CompletionListDTOImpl();
                    LanguageServerRegistryImpl.doInParallel(element, new LSOperation<InitializedServerImpl, CompletionList>() {

                        @Override
                        public boolean canDo(InitializedServerImpl element) {
                            return element.getInitializeResult().getCapabilities().getCompletionProvider() != null;
                        }

                        @Override
                        public CompletableFuture<CompletionList> start(InitializedServerImpl element) {
                            return element.getServer().getTextDocumentService().completion(textDocumentPositionParams);
                        }

                        @Override
                        public boolean handleResult(InitializedServerImpl element, CompletionList result) {
                            list.setIncomplete(list.isIncomplete() || result.isIncomplete());
                            result.getItems().forEach(item -> {
                                CompletionItemDTO dto = DtoConverter.asDto(item);
                                dto.setServerId(element.getDescription().getId());
                                items.add(dto);
                            });
                            return !result.getItems().isEmpty();
                        }
                    }, 5000);
                    list.setItems(items);
                    return list;
                });

            }

            @Override
            public boolean handleResult(Collection<InitializedServerImpl> element, CompletionList list) {
                result.setIncomplete(list.isIncomplete());
                list.getItems().forEach(item -> items.add(DtoConverter.asDto(item)));
                return !result.getItems().isEmpty();
            }

        };
        LanguageServerRegistryImpl.doInSequence(languageServerRegistry.getApplicableLanguageServers(textDocumentPositionParams.getTextDocument().getUri()), op, 5000);
        return result;
    }

    @POST
    @Path("documentSymbol")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<? extends SymbolInformation> documentSymbol(DocumentSymbolParamsDTO documentSymbolParams)
                    throws ExecutionException, InterruptedException, LanguageServerException {
        documentSymbolParams.getTextDocument().setUri(prefixURI(documentSymbolParams.getTextDocument().getUri()));
        List<SymbolInformation> symbols = new ArrayList<>();

        LanguageServerRegistryImpl.doInParallel(flatten(languageServerRegistry.getApplicableLanguageServers(documentSymbolParams.getTextDocument().getUri())),
                        new LSOperation<InitializedServerImpl, List<? extends SymbolInformation>>() {

                            @Override
                            public boolean canDo(InitializedServerImpl element) {
                                return element.getInitializeResult().getCapabilities().isDocumentSymbolProvider() != null
                                                && element.getInitializeResult().getCapabilities().isDocumentSymbolProvider();
                            }

                            @Override
                            public CompletableFuture<List<? extends SymbolInformation>> start(InitializedServerImpl element) {
                                return element.getServer().getTextDocumentService().documentSymbol(documentSymbolParams);
                            }

                            @Override
                            public boolean handleResult(InitializedServerImpl element, List<? extends SymbolInformation> result) {
                                symbols.addAll(result);
                                return false;
                            }
                        }, TIMEOUT);
        return symbols;
    }

    private static <C> Collection<C> flatten(Collection<? extends Collection<C>> c) {
        List<C> result = new ArrayList<>();
        c.forEach(coll -> result.addAll(coll));
        return result;
    }

    @POST
    @Path("references")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<? extends Location> references(ReferenceParamsDTO params)
                    throws ExecutionException, InterruptedException, LanguageServerException {
        params.getTextDocument().setUri(prefixURI(params.getTextDocument().getUri()));
        List<Location> locations = new ArrayList<>();

        LanguageServerRegistryImpl.doInParallel(flatten(languageServerRegistry.getApplicableLanguageServers(params.getTextDocument().getUri())),
                        new LSOperation<InitializedServerImpl, List<? extends Location>>() {

                            @Override
                            public boolean canDo(InitializedServerImpl element) {
                                return element.getInitializeResult().getCapabilities().isReferencesProvider() != null
                                                && element.getInitializeResult().getCapabilities().isReferencesProvider();
                            }

                            @Override
                            public CompletableFuture<List<? extends Location>> start(InitializedServerImpl element) {
                                return element.getServer().getTextDocumentService().references(params);
                            }

                            @Override
                            public boolean handleResult(InitializedServerImpl element, List<? extends Location> result) {
                                locations.addAll(result);
                                return false;
                            }
                        }, TIMEOUT);
        return locations;
    }

    @POST
    @Path("definition")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<? extends Location> definition(TextDocumentPositionParamsDTO params)
                    throws ExecutionException, InterruptedException, LanguageServerException {
        params.getTextDocument().setUri(prefixURI(params.getTextDocument().getUri()));
        List<Location> locations = new ArrayList<>();

        LanguageServerRegistryImpl.doInParallel(flatten(languageServerRegistry.getApplicableLanguageServers(params.getTextDocument().getUri())),
                        new LSOperation<InitializedServerImpl, List<? extends Location>>() {

                            @Override
                            public boolean canDo(InitializedServerImpl element) {
                                return element.getInitializeResult().getCapabilities().isDefinitionProvider() != null
                                                && element.getInitializeResult().getCapabilities().isDefinitionProvider();
                            }

                            @Override
                            public CompletableFuture<List<? extends Location>> start(InitializedServerImpl element) {
                                return element.getServer().getTextDocumentService().definition(params);
                            }

                            @Override
                            public boolean handleResult(InitializedServerImpl element, List<? extends Location> result) {
                                locations.addAll(result);
                                return false;
                            }
                        }, TIMEOUT);
        return locations;
    }

    @POST
    @Path("completionItem/resolve")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public CompletionItem resolveCompletionItem(CompletionItemDTO unresolved)
                    throws InterruptedException, ExecutionException, LanguageServerException {
        InitializedServerImpl server = languageServerRegistry.getServer(unresolved.getServerId());
        if (server != null) {
            return server.getServer().getTextDocumentService().resolveCompletionItem(unresolved).get();
        } else {
            return unresolved;
        }
    }

    @POST
    @Path("hover")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Hover hover(TextDocumentPositionParamsDTO positionParams)
                    throws LanguageServerException, ExecutionException, InterruptedException {
        String originalUri = positionParams.getTextDocument().getUri();
        positionParams.getTextDocument().setUri(prefixURI(originalUri));
        positionParams.setUri(prefixURI(positionParams.getUri()));

        Hover[] res= new Hover[1];

        LanguageServerRegistryImpl.doInParallel(flatten(languageServerRegistry.getApplicableLanguageServers(originalUri)),
                        new LSOperation<InitializedServerImpl, Hover>() {

                            @Override
                            public boolean canDo(InitializedServerImpl element) {
                                return element.getInitializeResult().getCapabilities().isHoverProvider() != null
                                                && element.getInitializeResult().getCapabilities().isHoverProvider();
                            }

                            @Override
                            public CompletableFuture<Hover> start(InitializedServerImpl element) {
                                return element.getServer().getTextDocumentService().hover(positionParams);
                            }

                            @Override
                            public boolean handleResult(InitializedServerImpl element, Hover result) {
                                res[0]= result;
                                return result != null;
                            }
                        }, TIMEOUT);
        return res[0];
    }

    @POST
    @Path("signatureHelp")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public SignatureHelp signatureHelp(TextDocumentPositionParamsDTO positionParams)
                    throws LanguageServerException, ExecutionException, InterruptedException {
        String originalUri = positionParams.getTextDocument().getUri();
        positionParams.getTextDocument().setUri(prefixURI(originalUri));
        positionParams.setUri(prefixURI(positionParams.getUri()));
        SignatureHelp[] res = new SignatureHelp[1];

        LanguageServerRegistryImpl.doInSequence(flatten(languageServerRegistry.getApplicableLanguageServers(originalUri)),
                        new LSOperation<InitializedServerImpl, SignatureHelp>() {

                            @Override
                            public boolean canDo(InitializedServerImpl element) {
                                return element.getInitializeResult().getCapabilities().getSignatureHelpProvider() != null;
                            }

                            @Override
                            public CompletableFuture<SignatureHelp> start(InitializedServerImpl element) {
                                return element.getServer().getTextDocumentService().signatureHelp(positionParams);
                            }

                            @Override
                            public boolean handleResult(InitializedServerImpl element, SignatureHelp result) {
                                res[0] = result;
                                return result != null;
                            }
                        }, TIMEOUT);
        return res[0];
    }

    @POST
    @Path("formatting")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<? extends TextEdit> formatting(DocumentFormattingParamsDTO params)
                    throws InterruptedException, ExecutionException, LanguageServerException {
        String originalUri = params.getTextDocument().getUri();
        params.getTextDocument().setUri(prefixURI(originalUri));
        List<TextEdit> res = new ArrayList<>();

        LanguageServerRegistryImpl.doInSequence(flatten(languageServerRegistry.getApplicableLanguageServers(originalUri)),
                        new LSOperation<InitializedServerImpl, List<? extends TextEdit>>() {

                            @Override
                            public boolean canDo(InitializedServerImpl element) {
                                return element.getInitializeResult().getCapabilities().isDocumentFormattingProvider() != null
                                                && element.getInitializeResult().getCapabilities().isDocumentFormattingProvider();
                            }

                            @Override
                            public CompletableFuture<List<? extends TextEdit>> start(InitializedServerImpl element) {
                                return element.getServer().getTextDocumentService().formatting(params);
                            }

                            @Override
                            public boolean handleResult(InitializedServerImpl element, List<? extends TextEdit> result) {
                                res.addAll(result);
                                return result != null;
                            }
                        }, TIMEOUT);
        return res;
    }

    @POST
    @Path("rangeFormatting")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<? extends TextEdit> rangeFormatting(DocumentRangeFormattingParamsDTO params)
                    throws InterruptedException, ExecutionException, LanguageServerException {
        params.getTextDocument().setUri(prefixURI(params.getTextDocument().getUri()));
        List<TextEdit> res = new ArrayList<>();

        LanguageServerRegistryImpl.doInSequence(flatten(languageServerRegistry.getApplicableLanguageServers(params.getTextDocument().getUri())),
                        new LSOperation<InitializedServerImpl, List<? extends TextEdit>>() {

                            @Override
                            public boolean canDo(InitializedServerImpl element) {
                                return element.getInitializeResult().getCapabilities().isDocumentRangeFormattingProvider() != null
                                                && element.getInitializeResult().getCapabilities().isDocumentRangeFormattingProvider();
                            }

                            @Override
                            public CompletableFuture<List<? extends TextEdit>> start(InitializedServerImpl element) {
                                return element.getServer().getTextDocumentService().rangeFormatting(params);
                            }

                            @Override
                            public boolean handleResult(InitializedServerImpl element, List<? extends TextEdit> result) {
                                res.addAll(result);
                                return result != null;
                            }
                        }, TIMEOUT);
        return res;
    }

    @POST
    @Path("onTypeFormatting")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<? extends TextEdit> onTypeFormatting(DocumentOnTypeFormattingParamsDTO params)
                    throws InterruptedException, ExecutionException, LanguageServerException {
        params.getTextDocument().setUri(prefixURI(params.getTextDocument().getUri()));
        List<TextEdit> res = new ArrayList<>();
        LanguageServerRegistryImpl.doInSequence(flatten(languageServerRegistry.getApplicableLanguageServers(params.getTextDocument().getUri())),
                        new LSOperation<InitializedServerImpl, List<? extends TextEdit>>() {

                            @Override
                            public boolean canDo(InitializedServerImpl element) {
                                return element.getInitializeResult().getCapabilities().getDocumentOnTypeFormattingProvider() != null;
                            }

                            @Override
                            public CompletableFuture<List<? extends TextEdit>> start(InitializedServerImpl element) {
                                return element.getServer().getTextDocumentService().onTypeFormatting(params);
                            }

                            @Override
                            public boolean handleResult(InitializedServerImpl element, List<? extends TextEdit> result) {
                                res.addAll(result);
                                return result != null;
                            }
                        }, TIMEOUT);
        return res;
    }

    @POST
    @Path("didChange")
    @Consumes(MediaType.APPLICATION_JSON)
    public void didChange(DidChangeTextDocumentParamsDTO change) throws LanguageServerException {
        change.getTextDocument().setUri(prefixURI(change.getTextDocument().getUri()));
        change.setUri(prefixURI(change.getUri()));
        LanguageServerRegistryImpl.doInSequence(flatten(languageServerRegistry.getApplicableLanguageServers(change.getTextDocument().getUri())),
                        new LSOperation<InitializedServerImpl, Void>() {

                            @Override
                            public boolean canDo(InitializedServerImpl element) {
                                return element.getInitializeResult().getCapabilities().getTextDocumentSync() != null && element
                                                .getInitializeResult().getCapabilities().getTextDocumentSync() != TextDocumentSyncKind.None;
                            }

                            @Override
                            public CompletableFuture<Void> start(InitializedServerImpl element) {
                                // TODO: need to handle full/incremental changes
                                // here: will only get incremental from front
                                // end.
                                element.getServer().getTextDocumentService().didChange(change);
                                return CompletableFuture.completedFuture(null);
                            }

                            @Override
                            public boolean handleResult(InitializedServerImpl element, Void result) {
                                return false;
                            }
                        }, TIMEOUT);
    }

    @POST
    @Path("didOpen")
    @Consumes(MediaType.APPLICATION_JSON)
    public void didOpen(DidOpenTextDocumentParamsDTO openEvent) throws LanguageServerException {
        openEvent.getTextDocument().setUri(prefixURI(openEvent.getTextDocument().getUri()));
        openEvent.setUri(prefixURI(openEvent.getUri()));
        LanguageServerRegistryImpl.doInSequence(flatten(languageServerRegistry.getApplicableLanguageServers(openEvent.getTextDocument().getUri())),
                        new LSOperation<InitializedServerImpl, Void>() {

                            @Override
                            public boolean canDo(InitializedServerImpl element) {
                                return true;
                            }

                            @Override
                            public CompletableFuture<Void> start(InitializedServerImpl element) {
                                element.getServer().getTextDocumentService().didOpen(openEvent);
                                return CompletableFuture.completedFuture(null);
                            }

                            @Override
                            public boolean handleResult(InitializedServerImpl element, Void result) {
                                return false;
                            }
                        }, TIMEOUT);
    }

    @POST
    @Path("didClose")
    @Consumes(MediaType.APPLICATION_JSON)
    public void didClose(DidCloseTextDocumentParamsDTO closeEvent) throws LanguageServerException {
        closeEvent.getTextDocument().setUri(prefixURI(closeEvent.getTextDocument().getUri()));
        LanguageServerRegistryImpl.doInSequence(
                        flatten(languageServerRegistry.getApplicableLanguageServers(closeEvent.getTextDocument().getUri())),
                        new LSOperation<InitializedServerImpl, Void>() {

                            @Override
                            public boolean canDo(InitializedServerImpl element) {
                                return true;
                            }

                            @Override
                            public CompletableFuture<Void> start(InitializedServerImpl element) {
                                element.getServer().getTextDocumentService().didClose(closeEvent);
                                return CompletableFuture.completedFuture(null);
                            }

                            @Override
                            public boolean handleResult(InitializedServerImpl element, Void result) {
                                return false;
                            }
                        }, TIMEOUT);
    }

    @POST
    @Path("didSave")
    @Consumes(MediaType.APPLICATION_JSON)
    public void didSave(DidSaveTextDocumentParamsDTO saveEvent) throws LanguageServerException {
        saveEvent.getTextDocument().setUri(prefixURI(saveEvent.getTextDocument().getUri()));
        LanguageServerRegistryImpl.doInSequence(
                        flatten(languageServerRegistry.getApplicableLanguageServers(saveEvent.getTextDocument().getUri())),
                        new LSOperation<InitializedServerImpl, Void>() {

                            @Override
                            public boolean canDo(InitializedServerImpl element) {
                                return true;
                            }

                            @Override
                            public CompletableFuture<Void> start(InitializedServerImpl element) {
                                element.getServer().getTextDocumentService().didSave(saveEvent);
                                return CompletableFuture.completedFuture(null);
                            }

                            @Override
                            public boolean handleResult(InitializedServerImpl element, Void result) {
                                return false;
                            }
                        }, TIMEOUT);
    }

    @POST
    @Path("documentHighlight")
    @Consumes(MediaType.APPLICATION_JSON)
    public List<DocumentHighlight> documentHighlight(TextDocumentPositionParamsDTO positionParams)
                    throws LanguageServerException, InterruptedException, ExecutionException {
        positionParams.getTextDocument().setUri(prefixURI(positionParams.getTextDocument().getUri()));
        List<DocumentHighlight> res = new ArrayList<>();

        LanguageServerRegistryImpl.doInSequence(
                        flatten(languageServerRegistry.getApplicableLanguageServers(positionParams.getTextDocument().getUri())),
                        new LSOperation<InitializedServerImpl, DocumentHighlight>() {

                            @Override
                            public boolean canDo(InitializedServerImpl element) {
                                return element.getInitializeResult().getCapabilities().isDocumentHighlightProvider() != null
                                                && element.getInitializeResult().getCapabilities().isDocumentHighlightProvider();
                            }

                            @Override
                            public CompletableFuture<DocumentHighlight> start(InitializedServerImpl element) {
                                return element.getServer().getTextDocumentService().documentHighlight(positionParams);
                            }

                            @Override
                            public boolean handleResult(InitializedServerImpl element, DocumentHighlight result) {
                                res.add(result);
                                return result != null;
                            }
                        }, TIMEOUT);
        return res;
    }

}
