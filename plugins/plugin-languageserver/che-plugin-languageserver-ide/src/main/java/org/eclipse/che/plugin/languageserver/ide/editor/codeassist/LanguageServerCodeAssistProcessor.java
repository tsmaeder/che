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
package org.eclipse.che.plugin.languageserver.ide.editor.codeassist;

import com.google.inject.Inject;
import io.typefox.lsapi.ServerCapabilities;
import org.eclipse.che.api.languageserver.shared.lsapi.CompletionItemDTO;
import org.eclipse.che.api.languageserver.shared.lsapi.CompletionListDTO;
import org.eclipse.che.api.languageserver.shared.lsapi.TextDocumentIdentifierDTO;
import org.eclipse.che.api.languageserver.shared.lsapi.TextDocumentPositionParamsDTO;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.ide.api.editor.codeassist.CodeAssistCallback;
import org.eclipse.che.ide.api.editor.codeassist.CodeAssistProcessor;
import org.eclipse.che.ide.api.editor.codeassist.CompletionProposal;
import org.eclipse.che.ide.api.editor.texteditor.TextEditor;
import org.eclipse.che.ide.filters.FuzzyMatches;
import org.eclipse.che.ide.filters.Match;
import org.eclipse.che.plugin.languageserver.ide.LanguageServerResources;
import org.eclipse.che.plugin.languageserver.ide.registry.LanguageServerRegistry;
import org.eclipse.che.plugin.languageserver.ide.service.TextDocumentServiceClient;
import org.eclipse.che.plugin.languageserver.ide.util.DtoBuildHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Implement code assist with LS
 */
public class LanguageServerCodeAssistProcessor implements CodeAssistProcessor {

    private final DtoBuildHelper            dtoBuildHelper;
    private final LanguageServerResources   resources;
    private final CompletionImageProvider   imageProvider;
    private final TextDocumentServiceClient documentServiceClient;
    private final FuzzyMatches              fuzzyMatches;
    private String                          lastErrorMessage;
    private final LatestCompletionResult    latestCompletionResult;
    private LanguageServerRegistry          registry;

    @Inject
    public LanguageServerCodeAssistProcessor(TextDocumentServiceClient documentServiceClient, LanguageServerRegistry registry,
                                             DtoBuildHelper dtoBuildHelper, LanguageServerResources resources,
                                             CompletionImageProvider imageProvider, FuzzyMatches fuzzyMatches) {
        this.documentServiceClient = documentServiceClient;
        this.registry = registry;
        this.dtoBuildHelper = dtoBuildHelper;
        this.resources = resources;
        this.imageProvider = imageProvider;
        this.fuzzyMatches = fuzzyMatches;
        this.latestCompletionResult = new LatestCompletionResult();
    }

    @Override
    public void computeCompletionProposals(TextEditor editor, final int offset, final boolean triggered,
                                           final CodeAssistCallback callback) {
        this.lastErrorMessage = null;

        ServerCapabilities capabilities = registry.getCapabilities(editor.getDocument().getFile());
        if (capabilities.getCompletionProvider() != null) {
            TextDocumentPositionParamsDTO documentPosition = dtoBuildHelper.createTDPP(editor.getDocument(), offset);
            final TextDocumentIdentifierDTO documentId = documentPosition.getTextDocument();
            String currentLine = editor.getDocument().getLineContent(documentPosition.getPosition().getLine());
            final String currentWord = getCurrentWord(currentLine, documentPosition.getPosition().getCharacter());

            boolean canResolve = capabilities.getCompletionProvider().getResolveProvider() != null
                            && capabilities.getCompletionProvider().getResolveProvider();

            if (!triggered && latestCompletionResult.isGoodFor(documentId, offset, currentWord)) {
                // no need to send new completion request
                computeProposals(currentWord, offset - latestCompletionResult.getOffset(), callback, canResolve);
            } else {
                documentServiceClient.completion(documentPosition).then(new Operation<CompletionListDTO>() {
                    @Override
                    public void apply(CompletionListDTO list) throws OperationException {
                        latestCompletionResult.update(documentId, offset, currentWord, list);
                        computeProposals(currentWord, 0, callback, canResolve);
                    }
                }).catchError(new Operation<PromiseError>() {
                    @Override
                    public void apply(PromiseError error) throws OperationException {
                        lastErrorMessage = error.getMessage();
                    }
                });
            }
        } else {
            callback.proposalComputed(Collections.emptyList());
        }
    }

    @Override
    public String getErrorMessage() {
        return lastErrorMessage;
    }

    private String getCurrentWord(String text, int offset) {
        int i = offset - 1;
        while (i >= 0 && isWordChar(text.charAt(i))) {
            i--;
        }
        return text.substring(i + 1, offset);
    }

    private boolean isWordChar(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c >= '\u007f' && c <= '\u00ff' || c == '$'
                        || c == '_' || c == '-';
    }

    private List<Match> filter(String word, CompletionItemDTO item) {
        return filter(word, item.getLabel(), item.getFilterText());
    }

    private List<Match> filter(String word, String label, String filterText) {
        if (filterText == null || filterText.isEmpty()) {
            filterText = label;
        }

        // check if the word matches the filterText
        if (fuzzyMatches.fuzzyMatch(word, filterText) != null) {
            // return the highlights based on the label
            List<Match> highlights = fuzzyMatches.fuzzyMatch(word, label);
            // return empty list of highlights if nothing matches the label
            return (highlights == null) ? new ArrayList<Match>() : highlights;
        }

        return null;
    }

    private void computeProposals(String currentWord, int offset, CodeAssistCallback callback, boolean canResolve) {

        List<CompletionProposal> proposals = newArrayList();
        for (CompletionItemDTO item : latestCompletionResult.getCompletionList().getItems()) {
            List<Match> highlights = filter(currentWord, item);
            if (highlights != null) {
                proposals.add(new CompletionItemBasedCompletionProposal(item, documentServiceClient, latestCompletionResult.getDocumentId(),
                                resources, imageProvider.getIcon(item.getKind()), canResolve, highlights, offset));
            }
        }
        callback.proposalComputed(proposals);
    }

}
