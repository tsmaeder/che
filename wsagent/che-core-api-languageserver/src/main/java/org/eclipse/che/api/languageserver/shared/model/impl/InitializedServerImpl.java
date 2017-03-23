package org.eclipse.che.api.languageserver.shared.model.impl;

import io.typefox.lsapi.InitializeResult;
import io.typefox.lsapi.services.LanguageServer;
import org.eclipse.che.api.languageserver.shared.model.InitializedServer;
import org.eclipse.che.api.languageserver.shared.model.LanguageServerDescription;

public class InitializedServerImpl implements InitializedServer {

    private LanguageServerDescription description;
    private InitializeResult initializeResult;
    private LanguageServer server;

    public InitializedServerImpl(LanguageServerDescription description, LanguageServer server, InitializeResult initializeResult) {
        this.description = description;
        this.server = server;
        this.initializeResult = initializeResult;
    }

    @Override
    public LanguageServerDescription getDescription() {
        return description;
    }

    @Override
    public InitializeResult getInitializeResult() {
        return initializeResult;
    }
    
    public LanguageServer getServer() {
        return server;
    }
    
}
