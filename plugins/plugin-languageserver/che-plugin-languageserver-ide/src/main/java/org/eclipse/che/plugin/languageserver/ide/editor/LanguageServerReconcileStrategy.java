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
import org.eclipse.che.ide.api.editor.document.Document;
import org.eclipse.che.ide.api.editor.events.DocumentChangeEvent;
import org.eclipse.che.ide.api.editor.events.DocumentChangeHandler;
import org.eclipse.che.ide.api.editor.reconciler.DirtyRegion;
import org.eclipse.che.ide.api.editor.reconciler.ReconcilingStrategy;
import org.eclipse.che.ide.api.editor.text.Region;
import org.eclipse.che.plugin.languageserver.ide.editor.sync.TextDocumentSynchronizeFactory;
import org.eclipse.che.plugin.languageserver.ide.registry.LanguageServerRegistry;

/**
 * Responsible for document synchronization
 *
 * @author Evgen Vidolob
 */
public class LanguageServerReconcileStrategy implements ReconcilingStrategy {

    private int version = 0;
    private TextDocumentSynchronizeFactory synchronizeFactory;
    private LanguageServerRegistry registry;

    @Inject
    public LanguageServerReconcileStrategy(TextDocumentSynchronizeFactory synchronizeFactory, LanguageServerRegistry registry) {
        this.synchronizeFactory= synchronizeFactory;
        this.registry= registry;
    }

    @Override
    public void setDocument(Document document) {
        document.getDocumentHandle().getDocEventBus().addHandler(DocumentChangeEvent.TYPE, new DocumentChangeHandler() {
            @Override
            public void onDocumentChange(DocumentChangeEvent event) {
                ServerCapabilities capabilities = registry.getCapabilities(event.getDocument().getDocument().getFile());
                synchronizeFactory.getSynchronize(capabilities.getTextDocumentSync()).syncTextDocument(event, ++version);
            }
        });
    }

    @Override
    public void reconcile(DirtyRegion dirtyRegion, Region subRegion) {
        doReconcile();
    }

    public void doReconcile() {
        //TODO use DocumentHighlight to add additional highlight for file
    }

    @Override
    public void reconcile(Region partition) {
        doReconcile();
    }

    @Override
    public void closeReconciler() {

    }
}
