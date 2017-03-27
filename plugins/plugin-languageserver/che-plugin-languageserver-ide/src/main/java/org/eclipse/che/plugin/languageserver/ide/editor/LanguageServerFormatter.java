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
package org.eclipse.che.plugin.languageserver.ide.editor;

import com.google.inject.Inject;
import io.typefox.lsapi.ServerCapabilities;
import org.eclipse.che.api.languageserver.shared.lsapi.DocumentFormattingParamsDTO;
import org.eclipse.che.api.languageserver.shared.lsapi.DocumentOnTypeFormattingParamsDTO;
import org.eclipse.che.api.languageserver.shared.lsapi.DocumentRangeFormattingParamsDTO;
import org.eclipse.che.api.languageserver.shared.lsapi.FormattingOptionsDTO;
import org.eclipse.che.api.languageserver.shared.lsapi.PositionDTO;
import org.eclipse.che.api.languageserver.shared.lsapi.RangeDTO;
import org.eclipse.che.api.languageserver.shared.lsapi.TextDocumentIdentifierDTO;
import org.eclipse.che.api.languageserver.shared.lsapi.TextEditDTO;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.ide.api.editor.document.Document;
import org.eclipse.che.ide.api.editor.events.DocumentChangeEvent;
import org.eclipse.che.ide.api.editor.events.DocumentChangeHandler;
import org.eclipse.che.ide.api.editor.formatter.ContentFormatter;
import org.eclipse.che.ide.api.editor.text.TextPosition;
import org.eclipse.che.ide.api.editor.text.TextRange;
import org.eclipse.che.ide.api.editor.texteditor.HandlesUndoRedo;
import org.eclipse.che.ide.api.editor.texteditor.TextEditor;
import org.eclipse.che.ide.api.editor.texteditor.UndoableEditor;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.editor.preferences.editorproperties.EditorProperties;
import org.eclipse.che.ide.editor.preferences.editorproperties.EditorPropertiesManager;
import org.eclipse.che.ide.util.loging.Log;
import org.eclipse.che.plugin.languageserver.ide.registry.LanguageServerRegistry;
import org.eclipse.che.plugin.languageserver.ide.service.TextDocumentServiceClient;

import java.util.Collections;
import java.util.List;

/**
 * @author Evgen Vidolob
 */
public class LanguageServerFormatter implements ContentFormatter {

    private final TextDocumentServiceClient client;
    private final DtoFactory dtoFactory;
    private final NotificationManager manager;
    private final EditorPropertiesManager editorPropertiesManager;
    private TextEditor editor;
    private LanguageServerRegistry registry;

    @Inject
    public LanguageServerFormatter(TextDocumentServiceClient client, DtoFactory dtoFactory, NotificationManager manager,
                    EditorPropertiesManager editorPropertiesManager, LanguageServerRegistry registry) {
        this.client = client;
        this.dtoFactory = dtoFactory;
        this.manager = manager;
        this.editorPropertiesManager = editorPropertiesManager;
        this.registry = registry;
    }

    @Override
    public void format(Document document) {
        ServerCapabilities capabilities = registry.getCapabilities(document.getFile().getLocation().toString());
        TextRange selectedRange = document.getSelectedTextRange();
        if (selectedRange != null && !selectedRange.getFrom().equals(selectedRange.getTo())
                        && capabilities.isDocumentRangeFormattingProvider()) {
            // selection formatting
            formatRange(selectedRange, document);
        } else if (capabilities.isDocumentFormattingProvider()) {
            // full document formatting
            formatFullDocument(document);
        }

    }

    @Override
    public void install(TextEditor editor) {
        this.editor = editor;
        editor.getDocument().getDocumentHandle().getDocEventBus().addHandler(DocumentChangeEvent.TYPE, new DocumentChangeHandler() {
            @Override
            public void onDocumentChange(DocumentChangeEvent event) {
                ServerCapabilities capabilities = registry
                                .getCapabilities(event.getDocument().getDocument().getFile().getLocation().toString());
                if (capabilities.getDocumentOnTypeFormattingProvider() != null
                                && capabilities.getDocumentOnTypeFormattingProvider().getFirstTriggerCharacter() != null) {
                    if (capabilities.getDocumentOnTypeFormattingProvider().getFirstTriggerCharacter().equals(event.getText())) {
                        Document document = event.getDocument().getDocument();

                        DocumentOnTypeFormattingParamsDTO params = dtoFactory.createDto(DocumentOnTypeFormattingParamsDTO.class);
                        TextDocumentIdentifierDTO identifier = dtoFactory.createDto(TextDocumentIdentifierDTO.class);
                        identifier.setUri(document.getFile().getLocation().toString());
                        params.setTextDocument(identifier);
                        params.setOptions(getFormattingOptions());
                        params.setCh(event.getText());

                        TextPosition position = document.getPositionFromIndex(event.getOffset());

                        PositionDTO start = dtoFactory.createDto(PositionDTO.class);
                        start.setLine(position.getLine());
                        start.setCharacter(position.getCharacter());
                        params.setPosition(start);

                        Promise<List<TextEditDTO>> promise = client.onTypeFormatting(params);
                        handleFormatting(promise, document);

                    }
                }
            }
        });
    }

    private void formatFullDocument(Document document) {
        DocumentFormattingParamsDTO params = dtoFactory.createDto(DocumentFormattingParamsDTO.class);

        TextDocumentIdentifierDTO identifier = dtoFactory.createDto(TextDocumentIdentifierDTO.class);
        identifier.setUri(document.getFile().getLocation().toString());

        params.setTextDocument(identifier);
        params.setOptions(getFormattingOptions());
        Promise<List<TextEditDTO>> promise = client.formatting(params);
        handleFormatting(promise, document);
    }

    private void handleFormatting(Promise<List<TextEditDTO>> promise, final Document document) {
        promise.then(new Operation<List<TextEditDTO>>() {
            @Override
            public void apply(List<TextEditDTO> arg) throws OperationException {
                applyEdits(arg, document);
            }
        }).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {
                manager.notify(arg.getMessage());
            }
        });
    }

    private void applyEdits(List<TextEditDTO> edits, Document document) {
        HandlesUndoRedo undoRedo = null;

        if (editor instanceof UndoableEditor) {
            undoRedo = ((UndoableEditor) editor).getUndoRedo();
        }
        try {
            if (undoRedo != null) {
                undoRedo.beginCompoundChange();
            }

            // #2437: apply the text edits from last to first to avoid messing
            // up the document
            Collections.reverse(edits);
            for (TextEditDTO change : edits) {
                RangeDTO range = change.getRange();
                document.replace(range.getStart().getLine(), range.getStart().getCharacter(), range.getEnd().getLine(),
                                range.getEnd().getCharacter(), change.getNewText());
            }
        } catch (final Exception e) {
            Log.error(getClass(), e);
        } finally {
            if (undoRedo != null) {
                undoRedo.endCompoundChange();
            }
        }
    }

    private FormattingOptionsDTO getFormattingOptions() {
        FormattingOptionsDTO options = dtoFactory.createDto(FormattingOptionsDTO.class);
        options.setInsertSpaces(Boolean.parseBoolean(getEditorProperty(EditorProperties.EXPAND_TAB)));
        options.setTabSize(Integer.parseInt(getEditorProperty(EditorProperties.TAB_SIZE)));
        return options;
    }

    private String getEditorProperty(EditorProperties property) {
        return editorPropertiesManager.getEditorProperties().get(property.toString()).toString();
    }

    private void formatRange(TextRange selectedRange, Document document) {
        DocumentRangeFormattingParamsDTO params = dtoFactory.createDto(DocumentRangeFormattingParamsDTO.class);

        TextDocumentIdentifierDTO identifier = dtoFactory.createDto(TextDocumentIdentifierDTO.class);
        identifier.setUri(document.getFile().getLocation().toString());

        params.setTextDocument(identifier);
        params.setOptions(getFormattingOptions());

        RangeDTO range = dtoFactory.createDto(RangeDTO.class);
        PositionDTO start = dtoFactory.createDto(PositionDTO.class);
        PositionDTO end = dtoFactory.createDto(PositionDTO.class);

        start.setLine(selectedRange.getFrom().getLine());
        start.setCharacter(selectedRange.getFrom().getCharacter());

        end.setLine(selectedRange.getTo().getLine());
        end.setCharacter(selectedRange.getTo().getCharacter());

        range.setStart(start);
        range.setEnd(end);

        params.setRange(range);

        Promise<List<TextEditDTO>> promise = client.rangeFormatting(params);
        handleFormatting(promise, document);
    }

}
