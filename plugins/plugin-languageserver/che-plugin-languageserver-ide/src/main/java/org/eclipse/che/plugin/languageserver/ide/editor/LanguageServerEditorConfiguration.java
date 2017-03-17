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
import com.google.inject.Provider;
import io.typefox.lsapi.ServerCapabilities;
import org.eclipse.che.ide.api.editor.annotation.AnnotationModel;
import org.eclipse.che.ide.api.editor.codeassist.CodeAssistProcessor;
import org.eclipse.che.ide.api.editor.editorconfig.DefaultTextEditorConfiguration;
import org.eclipse.che.ide.api.editor.formatter.ContentFormatter;
import org.eclipse.che.ide.api.editor.partition.DocumentPartitioner;
import org.eclipse.che.ide.api.editor.partition.DocumentPositionMap;
import org.eclipse.che.ide.api.editor.reconciler.Reconciler;
import org.eclipse.che.ide.api.editor.reconciler.ReconcilerWithAutoSave;
import org.eclipse.che.ide.api.editor.signature.SignatureHelpProvider;
import org.eclipse.che.plugin.languageserver.ide.editor.signature.LanguageServerSignatureHelpFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Configure editor with LS support
 */
public class LanguageServerEditorConfiguration extends DefaultTextEditorConfiguration {

    public static final int INITIAL_DOCUMENT_VERSION = 0;

    private final AnnotationModel annotationModel;
    private final ReconcilerWithAutoSave reconciler;
    private final LanguageServerCodeassistProcessorFactory codeAssistProcessorFactory;
    private final SignatureHelpProvider signatureHelpProvider;
    private LanguageServerFormatter formatter;

    @Inject
    public LanguageServerEditorConfiguration(LanguageServerCodeassistProcessorFactory codeAssistProcessor,
                    Provider<DocumentPositionMap> docPositionMapProvider, LanguageServerAnnotationModelFactory annotationModelFactory,
                    LanguageServerReconcileStrategyFactory reconcileStrategyProviderFactory,
                    LanguageServerFormatterFactory formatterFactory, LanguageServerSignatureHelpFactory signatureHelpFactory) {
        codeAssistProcessorFactory = codeAssistProcessor;
        this.formatter = formatterFactory.create();
        this.annotationModel = annotationModelFactory.get(docPositionMapProvider.get());

        this.reconciler = new ReconcilerWithAutoSave(DocumentPartitioner.DEFAULT_CONTENT_TYPE, getPartitioner());
        reconciler.addReconcilingStrategy(DocumentPartitioner.DEFAULT_CONTENT_TYPE,
                        reconcileStrategyProviderFactory.build());
        signatureHelpProvider = signatureHelpFactory.create();
    }

    @Override
    public Map<String, CodeAssistProcessor> getContentAssistantProcessors() {
            Map<String, CodeAssistProcessor> map = new HashMap<>();
            map.put(DocumentPartitioner.DEFAULT_CONTENT_TYPE, codeAssistProcessorFactory.create());
            return map;
    }

    @Override
    public AnnotationModel getAnnotationModel() {
        return annotationModel;
    }

    @Override
    public Reconciler getReconciler() {
        return reconciler;
    }

    @Override
    public ContentFormatter getContentFormatter() {
        return formatter;
    }

    @Override
    public SignatureHelpProvider getSignatureHelpProvider() {
        return signatureHelpProvider;
    }
}
