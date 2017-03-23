package org.eclipse.che.api.languageserver.shared.lsapi;

import org.eclipse.che.api.languageserver.shared.model.LanguageServerDescription;
import org.eclipse.che.dto.shared.DTO;

import java.util.List;

@DTO
public interface LanguageServerDescriptionDTO extends LanguageServerDescription {
    void setLanguageIds(List<String> ids);
    List<DocumentFilterDTO> getDocumentFilters();
    void setDocumentFilters(List<DocumentFilterDTO> filters);
}
 