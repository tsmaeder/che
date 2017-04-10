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
package org.eclipse.che.plugin.languageserver.ide.navigation.symbol;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.typefox.lsapi.ServerCapabilities;
import org.eclipse.che.api.languageserver.shared.lsapi.DocumentSymbolParamsDTO;
import org.eclipse.che.api.languageserver.shared.lsapi.RangeDTO;
import org.eclipse.che.api.languageserver.shared.lsapi.SymbolInformationDTO;
import org.eclipse.che.api.languageserver.shared.lsapi.TextDocumentIdentifierDTO;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.promises.client.PromiseProvider;
import org.eclipse.che.ide.api.action.AbstractPerspectiveAction;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.editor.text.TextPosition;
import org.eclipse.che.ide.api.editor.text.TextRange;
import org.eclipse.che.ide.api.editor.texteditor.TextEditor;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.notification.StatusNotification;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.filters.FuzzyMatches;
import org.eclipse.che.ide.filters.Match;
import org.eclipse.che.plugin.languageserver.ide.LanguageServerLocalization;
import org.eclipse.che.plugin.languageserver.ide.quickopen.QuickOpenModel;
import org.eclipse.che.plugin.languageserver.ide.quickopen.QuickOpenPresenter;
import org.eclipse.che.plugin.languageserver.ide.registry.LanguageServerRegistry;
import org.eclipse.che.plugin.languageserver.ide.service.TextDocumentServiceClient;

import javax.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.eclipse.che.ide.workspace.perspectives.project.ProjectPerspective.PROJECT_PERSPECTIVE_ID;

/**
 * Action for 'Go to symbol' function
 *
 * @author Evgen Vidolob
 */
@Singleton
public class GoToSymbolAction extends AbstractPerspectiveAction implements QuickOpenPresenter.QuickOpenPresenterOpts {

    public static final String               SCOPE_PREFIX = ":";
    private final LanguageServerLocalization localization;
    private final TextDocumentServiceClient  client;
    private final LanguageServerRegistry registry;
    private final EditorAgent                editorAgent;
    private final DtoFactory                 dtoFactory;
    private final NotificationManager        notificationManager;
    private final FuzzyMatches               fuzzyMatches;
    private final SymbolKindHelper           symbolKindHelper;
    private QuickOpenPresenter               presenter;
    private List<SymbolInformationDTO>       cachedItems;
    private TextEditor                       activeEditor;
    private TextPosition                     cursorPosition;
    private PromiseProvider promiseProvider;

    @Inject
    public GoToSymbolAction(QuickOpenPresenter presenter, LanguageServerLocalization localization, TextDocumentServiceClient client, LanguageServerRegistry registry,
                            EditorAgent editorAgent, DtoFactory dtoFactory, NotificationManager notificationManager,
                            FuzzyMatches fuzzyMatches, SymbolKindHelper symbolKindHelper, PromiseProvider promiseProvider) {
        super(singletonList(PROJECT_PERSPECTIVE_ID), localization.goToSymbolActionDescription(), localization.goToSymbolActionTitle(), null,
                        null);
        this.presenter = presenter;
        this.localization = localization;
        this.client = client;
        this.registry= registry;
        this.editorAgent = editorAgent;
        this.dtoFactory = dtoFactory;
        this.notificationManager = notificationManager;
        this.fuzzyMatches = fuzzyMatches;
        this.symbolKindHelper = symbolKindHelper;
        this.promiseProvider= promiseProvider;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        DocumentSymbolParamsDTO paramsDTO = dtoFactory.createDto(DocumentSymbolParamsDTO.class);
        TextDocumentIdentifierDTO identifierDTO = dtoFactory.createDto(TextDocumentIdentifierDTO.class);
        identifierDTO.setUri(editorAgent.getActiveEditor().getEditorInput().getFile().getLocation().toString());
        paramsDTO.setTextDocument(identifierDTO);
        activeEditor = (TextEditor) editorAgent.getActiveEditor();
        cursorPosition = activeEditor.getDocument().getCursorPosition();
        client.documentSymbol(paramsDTO).then(new Operation<List<SymbolInformationDTO>>() {

            @Override
            public void apply(List<SymbolInformationDTO> arg) throws OperationException {

                cachedItems = arg;
                presenter.run(GoToSymbolAction.this);
            }
        }).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {
                notificationManager.notify("Can't fetch document symbols.", arg.getMessage(), StatusNotification.Status.FAIL,
                                           StatusNotification.DisplayMode.FLOAT_MODE);
            }
        });
    }

    @Override
    public void updateInPerspective(@NotNull ActionEvent event) {
        EditorPartPresenter activeEditor = editorAgent.getActiveEditor();
        if (activeEditor instanceof TextEditor) {
            // TODO: we can only ask the server here whether the action
            // should
            // be enabled.

            TextEditor editor = (TextEditor) activeEditor;
            ServerCapabilities capabilities = registry.getCapabilities(editor.getDocument().getFile());
            event.getPresentation().setEnabledAndVisible(capabilities.isDocumentSymbolProvider() != null
                            && capabilities.isDocumentSymbolProvider());
            return;
        }

        event.getPresentation().setEnabledAndVisible(false);
    }

    @Override
    public Promise<QuickOpenModel> getModel(String value) {
        return promiseProvider.resolve(new QuickOpenModel(toQuickOpenEntries(cachedItems, value)));
    }

    private List<SymbolEntry> toQuickOpenEntries(List<SymbolInformationDTO> items, final String value) {
        List<SymbolEntry> result = new ArrayList<>();
        String normalValue = value;
        if (value.startsWith(SCOPE_PREFIX)) {
            normalValue = normalValue.substring(SCOPE_PREFIX.length());
        }

        for (SymbolInformationDTO item : items) {
            String label = item.getName().trim();

            List<Match> highlights = fuzzyMatches.fuzzyMatch(normalValue, label);
            if (highlights != null) {
                String description = null;
                if (item.getContainerName() != null) {
                    description = item.getContainerName();
                }

                RangeDTO range = item.getLocation().getRange();
                TextRange textRange = new TextRange(new TextPosition(range.getStart().getLine(), range.getStart().getCharacter()),
                                new TextPosition(range.getEnd().getLine(), range.getEnd().getCharacter()));
                // TODO add icons
                result.add(new SymbolEntry(label, symbolKindHelper.from(item.getKind()), description, textRange,
                                (TextEditor) editorAgent.getActiveEditor(), highlights, symbolKindHelper.getIcon(item.getKind())));
            }
        }

        if (!value.isEmpty()) {
            if (value.startsWith(SCOPE_PREFIX)) {
                Collections.sort(result, new Comparator<SymbolEntry>() {
                    @Override
                    public int compare(SymbolEntry o1, SymbolEntry o2) {
                        return sortScoped(value.toLowerCase(), o1, o2);
                    }
                });
            } else {
                Collections.sort(result, new Comparator<SymbolEntry>() {

                    @Override
                    public int compare(SymbolEntry o1, SymbolEntry o2) {
                        return sortNormal(value.toLowerCase(), o1, o2);
                    }
                });
            }
        }

        if (!result.isEmpty() && value.startsWith(SCOPE_PREFIX)) {

            String currentType = null;
            SymbolEntry currentEntry = null;
            int counter = 0;
            for (int i = 0; i < result.size(); i++) {
                SymbolEntry res = result.get(i);
                if (!res.getType().equals(currentType)) {
                    if (currentEntry != null) {
                        currentEntry.setGroupLabel(typeToLabel(currentType, counter));
                    }

                    currentType = res.getType();
                    currentEntry = res;
                    counter = 1;

                    res.setWithBorder(i > 0);
                } else {
                    counter++;
                }
            }

            if (currentEntry != null) {
                currentEntry.setGroupLabel(typeToLabel(currentType, counter));
            }

        } else if (!result.isEmpty()) {
            result.get(0).setGroupLabel(localization.goToSymbolSymbols(result.size()));
        }

        return result;
    }

    private String typeToLabel(String type, int counter) {
        switch (type) {
        case "module":
            return localization.modulesType(counter);
        case "class":
            return localization.classType(counter);
        case "interface":
            return localization.interfaceType(counter);
        case "method":
            return localization.methodType(counter);
        case "function":
            return localization.functionType(counter);
        case "property":
            return localization.propertyType(counter);
        case "variable":
        case "var":
            return localization.variableType(counter);
        case "constructor":
            return localization.constructorType(counter);

        }

        return type;
    }

    private int sortScoped(String value, SymbolEntry a, SymbolEntry b) {
        value = value.substring(SCOPE_PREFIX.length());

        String aType = a.getType().toLowerCase();
        String bType = b.getType().toLowerCase();

        int r = aType.compareTo(bType);
        if (r != 0) {
            return r;
        }

        if (!value.isEmpty()) {
            String aName = a.getLabel().toLowerCase();
            String bName = b.getLabel().toLowerCase();

            r = aName.compareTo(bName);
            if (r != 0) {
                return r;
            }
        }

        TextRange aRange = a.getRange();
        TextRange bRange = b.getRange();
        return aRange.getFrom().getLine() - bRange.getFrom().getLine();
    }

    private int sortNormal(String value, SymbolEntry a, SymbolEntry b) {
        String aName = a.getLabel().toLowerCase();
        String bName = b.getLabel().toLowerCase();

        int r = aName.compareTo(bName);
        if (r != 0) {
            return r;
        }

        TextRange aRange = a.getRange();
        TextRange bRange = b.getRange();
        return aRange.getFrom().getLine() - bRange.getFrom().getLine();
    }

    @Override
    public void onClose(boolean canceled) {
        if (canceled) {
            activeEditor.getDocument().setCursorPosition(cursorPosition);
            activeEditor.setFocus();
        }
        cachedItems = null;
        activeEditor = null;
    }
}
