package org.eclipse.che.api.languageserver.shared.lsapi;

import org.eclipse.che.api.languageserver.shared.model.InitializedServer;
import org.eclipse.che.dto.shared.DTO;

@DTO
public interface InitializedServerDTO extends InitializedServer {
    @Override
    LanguageServerDescriptionDTO getDescription();
    void setDescription(LanguageServerDescriptionDTO description);
    @Override
    InitializeResultDTO getInitializeResult();
    void setInitializeResult(InitializeResultDTO result);
}
