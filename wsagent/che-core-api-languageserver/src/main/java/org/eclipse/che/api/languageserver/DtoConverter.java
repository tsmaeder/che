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
package org.eclipse.che.api.languageserver;

import io.typefox.lsapi.CodeLensOptions;
import io.typefox.lsapi.CompletionItem;
import io.typefox.lsapi.CompletionOptions;
import io.typefox.lsapi.DocumentOnTypeFormattingOptions;
import io.typefox.lsapi.InitializeResult;
import io.typefox.lsapi.Position;
import io.typefox.lsapi.Range;
import io.typefox.lsapi.ServerCapabilities;
import io.typefox.lsapi.SignatureHelpOptions;
import io.typefox.lsapi.TextEdit;
import org.eclipse.che.api.languageserver.shared.lsapi.CodeLensOptionsDTO;
import org.eclipse.che.api.languageserver.shared.lsapi.CompletionItemDTO;
import org.eclipse.che.api.languageserver.shared.lsapi.CompletionOptionsDTO;
import org.eclipse.che.api.languageserver.shared.lsapi.DocumentFilterDTO;
import org.eclipse.che.api.languageserver.shared.lsapi.DocumentOnTypeFormattingOptionsDTO;
import org.eclipse.che.api.languageserver.shared.lsapi.InitializeResultDTO;
import org.eclipse.che.api.languageserver.shared.lsapi.InitializedServerDTO;
import org.eclipse.che.api.languageserver.shared.lsapi.LanguageDescriptionDTO;
import org.eclipse.che.api.languageserver.shared.lsapi.LanguageServerDescriptionDTO;
import org.eclipse.che.api.languageserver.shared.lsapi.PositionDTO;
import org.eclipse.che.api.languageserver.shared.lsapi.RangeDTO;
import org.eclipse.che.api.languageserver.shared.lsapi.ServerCapabilitiesDTO;
import org.eclipse.che.api.languageserver.shared.lsapi.SignatureHelpOptionsDTO;
import org.eclipse.che.api.languageserver.shared.lsapi.TextEditDTO;
import org.eclipse.che.api.languageserver.shared.model.DocumentFilter;
import org.eclipse.che.api.languageserver.shared.model.InitializedServer;
import org.eclipse.che.api.languageserver.shared.model.LanguageDescription;
import org.eclipse.che.api.languageserver.shared.model.LanguageServerDescription;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.che.dto.server.DtoFactory.newDto;

/**
 * @author Anatoliy Bazko
 */
public class DtoConverter {

    public static InitializeResultDTO asDto(InitializeResult initializeResult) {
        InitializeResultDTO initializeResultDTO = newDto(InitializeResultDTO.class);
        initializeResultDTO.setCapabilities(asDto(initializeResult.getCapabilities()));
        return initializeResultDTO;
    }
    
    public static InitializedServerDTO asDto(InitializedServer server) {
        InitializedServerDTO dto = newDto(InitializedServerDTO.class);
        dto.setDescription(asDto(server.getDescription()));
        dto.setInitializeResult(asDto(server.getInitializeResult()));
        return dto;
    }


    public static LanguageDescriptionDTO asDto(LanguageDescription languageDescription) {
        LanguageDescriptionDTO languageDescriptionDTO = newDto(LanguageDescriptionDTO.class);
        languageDescriptionDTO.setFileExtensions(languageDescription.getFileExtensions());
        languageDescriptionDTO.setHighlightingConfiguration(languageDescription.getHighlightingConfiguration());
        languageDescriptionDTO.setLanguageId(languageDescription.getLanguageId());
        languageDescriptionDTO.setMimeTypes(languageDescription.getMimeTypes());
        return languageDescriptionDTO;
    }

    public static ServerCapabilitiesDTO asDto(ServerCapabilities capabilities) {
        ServerCapabilitiesDTO serverCapabilitiesDTO = newDto(ServerCapabilitiesDTO.class);

        serverCapabilitiesDTO.setCodeActionProvider(capabilities.isCodeActionProvider());

        CodeLensOptions codeLensProvider = capabilities.getCodeLensProvider();
        serverCapabilitiesDTO.setCodeLensProvider(codeLensProvider == null
                                                  ? null
                                                  : asDto(codeLensProvider));

        CompletionOptions completionProvider = capabilities.getCompletionProvider();
        serverCapabilitiesDTO.setCompletionProvider(completionProvider == null
                                                    ? null
                                                    : asDto(completionProvider));

        serverCapabilitiesDTO.setDefinitionProvider(capabilities.isDefinitionProvider());
        serverCapabilitiesDTO.setDocumentFormattingProvider(capabilities.isDocumentFormattingProvider());

        DocumentOnTypeFormattingOptions documentOnTypeFormattingProvider = capabilities.getDocumentOnTypeFormattingProvider();
        serverCapabilitiesDTO.setDocumentOnTypeFormattingProvider(documentOnTypeFormattingProvider == null
                                                                  ? null
                                                                  : asDto(documentOnTypeFormattingProvider));

        serverCapabilitiesDTO.setDocumentHighlightProvider(capabilities.isDocumentHighlightProvider());
        serverCapabilitiesDTO.setDocumentRangeFormattingProvider(capabilities.isDocumentRangeFormattingProvider());
        serverCapabilitiesDTO.setDocumentSymbolProvider(capabilities.isDocumentSymbolProvider());
        serverCapabilitiesDTO.setHoverProvider(capabilities.isHoverProvider());
        serverCapabilitiesDTO.setReferencesProvider(capabilities.isReferencesProvider());
        serverCapabilitiesDTO.setRenameProvider(capabilities.isRenameProvider());

        SignatureHelpOptions signatureHelpProvider = capabilities.getSignatureHelpProvider();
        serverCapabilitiesDTO.setSignatureHelpProvider(signatureHelpProvider == null
                                                       ? null
                                                       : asDto(signatureHelpProvider));

        serverCapabilitiesDTO.setTextDocumentSync(capabilities.getTextDocumentSync());
        serverCapabilitiesDTO.setWorkspaceSymbolProvider(capabilities.isWorkspaceSymbolProvider());

        return serverCapabilitiesDTO;
    }

    public static SignatureHelpOptionsDTO asDto(SignatureHelpOptions signatureHelpOptions) {
        SignatureHelpOptionsDTO signatureHelpOptionsDTO = newDto(SignatureHelpOptionsDTO.class);
        signatureHelpOptionsDTO.setTriggerCharacters(signatureHelpOptions.getTriggerCharacters());
        return signatureHelpOptionsDTO;
    }

    public static DocumentOnTypeFormattingOptionsDTO asDto(DocumentOnTypeFormattingOptions documentOnTypeFormattingOptions) {
        DocumentOnTypeFormattingOptionsDTO formattingOptionsDTO = newDto(DocumentOnTypeFormattingOptionsDTO.class);
        formattingOptionsDTO.setFirstTriggerCharacter(documentOnTypeFormattingOptions.getFirstTriggerCharacter());
        formattingOptionsDTO.setMoreTriggerCharacter(documentOnTypeFormattingOptions.getMoreTriggerCharacter());
        return formattingOptionsDTO;
    }

    public static CompletionOptionsDTO asDto(CompletionOptions completionOptions) {
        CompletionOptionsDTO completionOptionsDTO = newDto(CompletionOptionsDTO.class);
        completionOptionsDTO.setTriggerCharacters(completionOptions.getTriggerCharacters());
        completionOptionsDTO.setResolveProvider(completionOptions.getResolveProvider());
        return completionOptionsDTO;
    }

    public static CodeLensOptionsDTO asDto(CodeLensOptions codeLensOptions) {
        CodeLensOptionsDTO codeLensOptionsDTO = newDto(CodeLensOptionsDTO.class);
        codeLensOptionsDTO.setResolveProvider(codeLensOptions.getResolveProvider());
        return codeLensOptionsDTO;
    }
    
    public static DocumentFilterDTO asDto(DocumentFilter documentFilter) {
        DocumentFilterDTO documentFilterDTO = newDto(DocumentFilterDTO.class);
        documentFilterDTO.setPathRegex(documentFilter.getPathRegex());
        documentFilterDTO.setLanguageId(documentFilter.getLanguageId());
        documentFilterDTO.setScheme(documentFilter.getScheme());
        return documentFilterDTO;
    }

    public static LanguageServerDescriptionDTO asDto(LanguageServerDescription serverDescription) {
        LanguageServerDescriptionDTO serverDescriptionDTO = newDto(LanguageServerDescriptionDTO.class);
        List<DocumentFilterDTO> filters= new ArrayList<>();
        for (DocumentFilter filter : serverDescription.getDocumentFilters()) {
            filters.add(asDto(filter));
        }
        
        serverDescriptionDTO.setDocumentFilters(filters);
        serverDescriptionDTO.setLanguageIds(serverDescription.getLanguageIds());
        return serverDescriptionDTO;
    }
    
    public static CompletionItemDTO asDto(CompletionItem c) {
        CompletionItemDTO dto = newDto(CompletionItemDTO.class);
        dto.setData(c.getData());
        dto.setDetail(c.getDetail());
        dto.setDocumentation(c.getDocumentation());
        dto.setFilterText(c.getFilterText());
        dto.setInsertText(c.getInsertText());
        dto.setKind(c.getKind());
        dto.setLabel(c.getLabel());
        dto.setSortText(c.getSortText());
        dto.setTextEdit(asDto(c.getTextEdit()));
        return dto;
    }


    public static TextEditDTO asDto(TextEdit textEdit) {
        if (textEdit == null) {
            return null;
        }
        TextEditDTO dto = newDto(TextEditDTO.class);
        dto.setNewText(textEdit.getNewText());
        dto.setRange(asDto(textEdit.getRange()));
        return dto;
    }

    public static RangeDTO asDto(Range range) {
        RangeDTO dto = newDto(RangeDTO.class);
        dto.setStart(asDto(range.getStart()));
        dto.setEnd(asDto(range.getEnd()));
        return dto;
    }

    public static PositionDTO asDto(Position pos) {
        PositionDTO dto = newDto(PositionDTO.class);
        dto.setCharacter(pos.getCharacter());
        dto.setLine(pos.getLine());
        return dto;
    }

    private DtoConverter() { }
}
