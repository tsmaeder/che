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
package org.eclipse.che.plugin.languageserver.ide.hover;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.typefox.lsapi.MarkedString;
import io.typefox.lsapi.ServerCapabilities;
import org.eclipse.che.api.languageserver.shared.lsapi.HoverDTO;
import org.eclipse.che.api.languageserver.shared.lsapi.MarkedStringDTO;
import org.eclipse.che.api.languageserver.shared.lsapi.TextDocumentPositionParamsDTO;
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseProvider;
import org.eclipse.che.api.promises.client.js.JsPromise;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.editor.document.Document;
import org.eclipse.che.ide.api.editor.texteditor.TextEditor;
import org.eclipse.che.ide.editor.orion.client.OrionHoverHandler;
import org.eclipse.che.ide.editor.orion.client.jso.OrionHoverContextOverlay;
import org.eclipse.che.ide.editor.orion.client.jso.OrionHoverOverlay;
import org.eclipse.che.ide.util.StringUtils;
import org.eclipse.che.plugin.languageserver.ide.registry.LanguageServerRegistry;
import org.eclipse.che.plugin.languageserver.ide.service.TextDocumentServiceClient;
import org.eclipse.che.plugin.languageserver.ide.util.DtoBuildHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides hover LS functionality for Orion editor.
 *
 * @author Evgen Vidolob
 */
@Singleton
public class HoverProvider implements OrionHoverHandler {

    private final EditorAgent               editorAgent;
    private final TextDocumentServiceClient client;
    private final LanguageServerRegistry    registry;
    private final DtoBuildHelper            helper;
    private final PromiseProvider           promiseProvider;
    
    @Inject
    public HoverProvider(EditorAgent editorAgent, TextDocumentServiceClient client, LanguageServerRegistry registry,
                         DtoBuildHelper helper, PromiseProvider promiseProvider) {
        this.editorAgent = editorAgent;
        this.client = client;
        this.registry = registry;
        this.helper = helper;
        this.promiseProvider= promiseProvider;
    }

    @Override
    public JsPromise<OrionHoverOverlay> computeHover(OrionHoverContextOverlay context) {
        EditorPartPresenter activeEditor = editorAgent.getActiveEditor();
        if (activeEditor == null || !(activeEditor instanceof TextEditor)) {
            return null;
        }

        TextEditor editor = ((TextEditor) activeEditor);
        Document document = editor.getDocument();
        ServerCapabilities capabilities = registry.getCapabilities(document.getFile());
        if (capabilities.isHoverProvider() != null && capabilities.isHoverProvider()) {

            TextDocumentPositionParamsDTO paramsDTO = helper.createTDPP(document, context.getOffset());

            Promise<HoverDTO> promise = client.hover(paramsDTO);
            Promise<OrionHoverOverlay> then = promise.then(new Function<HoverDTO, OrionHoverOverlay>() {
                @Override
                public OrionHoverOverlay apply(HoverDTO arg) throws FunctionException {
                    OrionHoverOverlay hover = OrionHoverOverlay.create();
                    hover.setType("markdown");
                    String content = renderContent(arg);
                    // do not show hover with only white spaces
                    if (StringUtils.isNullOrWhitespace(content)) {
                        return null;
                    }
                    hover.setContent(content);

                    return hover;
                }

                private String renderContent(HoverDTO hover) {
                    List<String> contents = new ArrayList<String>();
                    for (MarkedStringDTO dto : hover.getContents()) {
                        String lang = dto.getLanguage();
                        if (lang == null || MarkedString.PLAIN_STRING.equals(lang)) {
                            // plain markdown text
                            contents.add(dto.getValue());
                        } else {
                            // markdown code block
                            contents.add("```" + lang + "\n" + dto.getValue() + "\n```");
                        }
                    }
                    return Joiner.on("\n\n").join(contents);
                }
            });
            return (JsPromise<OrionHoverOverlay>) then;
        }

        return (JsPromise<OrionHoverOverlay>) promiseProvider.resolve((OrionHoverOverlay)null);
    }
}
