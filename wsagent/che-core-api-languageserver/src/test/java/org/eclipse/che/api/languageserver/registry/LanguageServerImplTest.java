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
package org.eclipse.che.api.languageserver.registry;

import io.typefox.lsapi.InitializeParams;
import io.typefox.lsapi.InitializeResult;
import io.typefox.lsapi.services.LanguageServer;
import io.typefox.lsapi.services.TextDocumentService;
import io.typefox.lsapi.services.WindowService;
import org.eclipse.che.api.languageserver.launcher.LanguageServerLauncher;
import org.eclipse.che.api.languageserver.messager.PublishDiagnosticsParamsMessenger;
import org.eclipse.che.api.languageserver.messager.ShowMessageMessenger;
import org.eclipse.che.api.languageserver.shared.model.LanguageServerDescription;
import org.eclipse.che.api.languageserver.shared.model.impl.LanguageDescriptionImpl;
import org.eclipse.che.api.languageserver.shared.model.impl.LanguageServerDescriptionImpl;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Anatoliy Bazko
 */
@Listeners(MockitoTestNGListener.class)
public class LanguageServerImplTest {

    @Mock
    private ServerInitializerObserver           observer;
    @Mock
    private PublishDiagnosticsParamsMessenger   publishDiagnosticsParamsMessenger;
    @Mock
    private ShowMessageMessenger                showMessageParamsMessenger;
    @Mock
    private LanguageServerLauncher              launcher;
    @Mock
    private LanguageServer                      server;

    private LanguageServerRegistryImpl registry; 

    @BeforeMethod
    public void setUp() throws Exception {
        registry = spy(new LanguageServerRegistryImpl(Collections.singleton(launcher), Collections.emptySet(), publishDiagnosticsParamsMessenger, showMessageParamsMessenger));
        LanguageDescriptionImpl ld = new LanguageDescriptionImpl();
        ld.setLanguageId("languageId");
        ld.setFileExtensions(Arrays.asList("foo"));
        registry.registerLanguage(ld);
    } 
 
    @Test
    public void initializerShouldNotifyObservers() throws Exception {
         
        when(server.initialize(any(InitializeParams.class))).thenReturn(CompletableFuture.completedFuture(mock(InitializeResult.class)));
        when(server.getTextDocumentService()).thenReturn(mock(TextDocumentService.class));
        when(server.getWindowService()).thenReturn(mock(WindowService.class));
        
        LanguageServerDescriptionImpl lsd = new LanguageServerDescriptionImpl("serverId", Arrays.asList("languageId"), Collections.emptyList());
        when(launcher.getDescription()).thenReturn(lsd);
        when(launcher.isAbleToLaunch()).thenReturn(true);
        when(launcher.launch(anyString())).thenReturn(server); 

        registry.addObserver(observer);
        registry.launchServers("/path.foo");

        verify(observer, Mockito.timeout(5000).times(1)).onServerInitialized(eq(server), any(InitializeResult.class), eq(lsd));
        
        registry.launchServers("/path.foo");
        Thread.sleep(100);
        verify(observer, Mockito.timeout(100).times(1)).onServerInitialized(any(LanguageServer.class), any(InitializeResult.class), any(LanguageServerDescription.class));;
        registry.launchServers("/path.bar");
        // verify it's only called once
        verify(observer, Mockito.timeout(100).times(1)).onServerInitialized(any(LanguageServer.class), any(InitializeResult.class), any(LanguageServerDescription.class));;

    }
}
