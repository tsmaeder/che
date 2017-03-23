package org.eclipse.che.api.languageserver.shared.model;

import io.typefox.lsapi.InitializeResult;

public interface InitializedServer {
    LanguageServerDescription getDescription();
    InitializeResult getInitializeResult();
}
