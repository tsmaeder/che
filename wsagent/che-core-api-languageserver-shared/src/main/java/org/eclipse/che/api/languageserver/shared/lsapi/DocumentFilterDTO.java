package org.eclipse.che.api.languageserver.shared.lsapi;

import org.eclipse.che.api.languageserver.shared.model.DocumentFilter;
import org.eclipse.che.dto.shared.DTO;

@DTO
public interface DocumentFilterDTO extends DocumentFilter {
    void setGlobPattern(String pattern);

    void setLanguageId(String languageId);

    void setScheme(String scheme);
}
