package org.eclipse.che.plugin.languageserver.ide.registry;

import io.typefox.lsapi.CodeLensOptions;
import io.typefox.lsapi.CompletionOptions;
import io.typefox.lsapi.DocumentOnTypeFormattingOptions;
import io.typefox.lsapi.ServerCapabilities;
import io.typefox.lsapi.SignatureHelpOptions;
import io.typefox.lsapi.TextDocumentSyncKind;
import org.eclipse.che.api.languageserver.shared.dto.DtoClientImpls.CodeLensOptionsDTOImpl;
import org.eclipse.che.api.languageserver.shared.dto.DtoClientImpls.CompletionOptionsDTOImpl;
import org.eclipse.che.api.languageserver.shared.dto.DtoClientImpls.DocumentOnTypeFormattingOptionsDTOImpl;
import org.eclipse.che.api.languageserver.shared.dto.DtoClientImpls.SignatureHelpOptionsDTOImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServerCapabilitiesOverlay implements ServerCapabilities {
    private ServerCapabilities left;
    private ServerCapabilities right;

    public ServerCapabilitiesOverlay(ServerCapabilities left, ServerCapabilities right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public CodeLensOptions getCodeLensProvider() {
        CodeLensOptions leftOptions = left.getCodeLensProvider();
        CodeLensOptions rightOptions = right.getCodeLensProvider();
        if (leftOptions == null && rightOptions == null) {
            return null;
        }
        CodeLensOptionsDTOImpl result = CodeLensOptionsDTOImpl.make();
        if (leftOptions != null && leftOptions.getResolveProvider() || rightOptions != null && leftOptions.getResolveProvider()) {
            result.setResolveProvider(true);
        } 
        return result;
    }

    @Override
    public CompletionOptions getCompletionProvider() {
        CompletionOptions leftOptions = left.getCompletionProvider();
        CompletionOptions rightOptions = right.getCompletionProvider();
        if (leftOptions == null && rightOptions == null) {
            return null;
        }

        CompletionOptionsDTOImpl result = CompletionOptionsDTOImpl.make();
        List<String> triggerChars = new ArrayList<>();

        if (leftOptions != null) {
            triggerChars.addAll(listish(leftOptions.getTriggerCharacters()));
        }
        if (rightOptions != null) {
            triggerChars.addAll(listish(rightOptions.getTriggerCharacters()));
        }
        result.setTriggerCharacters(triggerChars);
        return result;
    }

    @Override
    public DocumentOnTypeFormattingOptions getDocumentOnTypeFormattingProvider() {
        DocumentOnTypeFormattingOptions leftOptions = left.getDocumentOnTypeFormattingProvider();
        DocumentOnTypeFormattingOptions rightOptions = right.getDocumentOnTypeFormattingProvider();
        if (leftOptions == null && rightOptions == null) {
            return null;
        }

        DocumentOnTypeFormattingOptionsDTOImpl result = DocumentOnTypeFormattingOptionsDTOImpl.make();
        List<String> triggerChars = new ArrayList<>();

        if (leftOptions != null) {
            result.setFirstTriggerCharacter(leftOptions.getFirstTriggerCharacter());
            triggerChars.addAll(listish(leftOptions.getMoreTriggerCharacter()));
        }
        if (rightOptions != null) {
            triggerChars.addAll(listish(rightOptions.getMoreTriggerCharacter()));
        }
        result.setMoreTriggerCharacter(triggerChars);
        return result;
    }

    @Override
    public SignatureHelpOptions getSignatureHelpProvider() {
        SignatureHelpOptions leftOptions = left.getSignatureHelpProvider();
        SignatureHelpOptions rightOptions = right.getSignatureHelpProvider();
        if (leftOptions == null && rightOptions == null) {
            return null;
        }
        SignatureHelpOptionsDTOImpl result = SignatureHelpOptionsDTOImpl.make();

        List<String> triggerChars = new ArrayList<>();

        if (leftOptions != null) {
            triggerChars.addAll(listish(leftOptions.getTriggerCharacters()));
        }
        if (rightOptions != null) {
            triggerChars.addAll(listish(rightOptions.getTriggerCharacters()));
        }
        result.setTriggerCharacters(triggerChars);
        return result;
    }

    @Override
    public TextDocumentSyncKind getTextDocumentSync() {
        // TODO: wait for back end document manager
        return TextDocumentSyncKind.Full;
    }

    @Override
    public Boolean isCodeActionProvider() {
        return truish(left.isCodeActionProvider()) || truish(right.isCodeActionProvider());
    }

    @Override
    public Boolean isDefinitionProvider() {
        return truish(left.isDefinitionProvider()) || truish(right.isDefinitionProvider());
    }

    @Override
    public Boolean isDocumentFormattingProvider() {
        return truish(left.isDocumentFormattingProvider()) || truish(right.isDocumentFormattingProvider());
    }

    @Override
    public Boolean isDocumentHighlightProvider() {
        return truish(left.isDocumentHighlightProvider()) || truish(right.isDocumentHighlightProvider());
    }

    @Override
    public Boolean isDocumentRangeFormattingProvider() {
        return truish(left.isDocumentRangeFormattingProvider()) || truish(right.isDocumentRangeFormattingProvider());
    }

    @Override
    public Boolean isDocumentSymbolProvider() {
        return truish(left.isDocumentSymbolProvider()) || truish(right.isDocumentSymbolProvider());
    }

    @Override
    public Boolean isHoverProvider() {
        return truish(left.isHoverProvider()) || truish(right.isHoverProvider());
    }

    @Override
    public Boolean isReferencesProvider() {
        return truish(left.isReferencesProvider()) || truish(right.isReferencesProvider());
    }

    @Override
    public Boolean isRenameProvider() {
        return truish(left.isRenameProvider()) || truish(right.isRenameProvider());
    }

    @Override
    public Boolean isWorkspaceSymbolProvider() {
        return truish(left.isWorkspaceSymbolProvider()) || truish(right.isWorkspaceSymbolProvider());
    }

    private boolean truish(Boolean b) {
        return b != null && b;
    }

    private <T> List<T> listish(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }

}
