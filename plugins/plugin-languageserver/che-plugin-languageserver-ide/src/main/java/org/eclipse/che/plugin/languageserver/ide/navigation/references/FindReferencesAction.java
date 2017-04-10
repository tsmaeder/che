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
package org.eclipse.che.plugin.languageserver.ide.navigation.references;

import com.google.inject.Inject;
import io.typefox.lsapi.ServerCapabilities;
import org.eclipse.che.api.languageserver.shared.lsapi.LocationDTO;
import org.eclipse.che.api.languageserver.shared.lsapi.PositionDTO;
import org.eclipse.che.api.languageserver.shared.lsapi.ReferenceContextDTO;
import org.eclipse.che.api.languageserver.shared.lsapi.ReferenceParamsDTO;
import org.eclipse.che.api.languageserver.shared.lsapi.TextDocumentIdentifierDTO;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.ide.api.action.AbstractPerspectiveAction;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.editor.texteditor.TextEditor;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.plugin.languageserver.ide.location.OpenLocationPresenter;
import org.eclipse.che.plugin.languageserver.ide.location.OpenLocationPresenterFactory;
import org.eclipse.che.plugin.languageserver.ide.registry.LanguageServerRegistry;
import org.eclipse.che.plugin.languageserver.ide.service.TextDocumentServiceClient;

import javax.validation.constraints.NotNull;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.eclipse.che.ide.workspace.perspectives.project.ProjectPerspective.PROJECT_PERSPECTIVE_ID;

/**
 * @author Evgen Vidolob
 */
public class FindReferencesAction extends AbstractPerspectiveAction {

    private final EditorAgent               editorAgent;
    private final TextDocumentServiceClient client;
    private LanguageServerRegistry          registry;
    private final DtoFactory                dtoFactory;
    private final OpenLocationPresenter     presenter;

    @Inject
    public FindReferencesAction(EditorAgent editorAgent, OpenLocationPresenterFactory presenterFactory, TextDocumentServiceClient client,
                                LanguageServerRegistry registry, DtoFactory dtoFactory) {
        super(singletonList(PROJECT_PERSPECTIVE_ID), "Find References", "Find References", null, null);
        this.editorAgent = editorAgent;
        this.client = client;
        this.registry = registry;
        this.dtoFactory = dtoFactory;
        presenter = presenterFactory.create("Find References");
    }

    @Override
    public void updateInPerspective(@NotNull ActionEvent event) {
        EditorPartPresenter activeEditor = editorAgent.getActiveEditor();
        if (activeEditor instanceof TextEditor) {
            // TODO: we can only ask the server here whether the action should
            // be enabled.

            TextEditor editor = (TextEditor) activeEditor;
            ServerCapabilities capabilities = registry.getCapabilities(editor.getDocument().getFile().getLocation().toString());
            event.getPresentation()
                            .setEnabledAndVisible(capabilities.isReferencesProvider() != null && capabilities.isReferencesProvider());
            return;
        }
        event.getPresentation().setEnabledAndVisible(false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        EditorPartPresenter activeEditor = editorAgent.getActiveEditor();

        // TODO replace this
        if (!(activeEditor instanceof TextEditor)) {
            return;
        }
        TextEditor textEditor = ((TextEditor) activeEditor);
        String path = activeEditor.getEditorInput().getFile().getLocation().toString();
        ReferenceParamsDTO paramsDTO = dtoFactory.createDto(ReferenceParamsDTO.class);

        PositionDTO positionDTO = dtoFactory.createDto(PositionDTO.class);
        positionDTO.setLine(textEditor.getCursorPosition().getLine());
        positionDTO.setCharacter(textEditor.getCursorPosition().getCharacter());

        TextDocumentIdentifierDTO identifierDTO = dtoFactory.createDto(TextDocumentIdentifierDTO.class);
        identifierDTO.setUri(path);

        ReferenceContextDTO contextDTO = dtoFactory.createDto(ReferenceContextDTO.class);
        contextDTO.setIncludeDeclaration(true);

        paramsDTO.setUri(path);
        paramsDTO.setPosition(positionDTO);
        paramsDTO.setTextDocument(identifierDTO);
        paramsDTO.setContext(contextDTO);
        Promise<List<LocationDTO>> promise = client.references(paramsDTO);
        presenter.openLocation(promise);
    }
}
