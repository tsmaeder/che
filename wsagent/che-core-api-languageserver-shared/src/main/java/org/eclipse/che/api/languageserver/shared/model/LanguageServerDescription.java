package org.eclipse.che.api.languageserver.shared.model;

import java.util.List;

public interface LanguageServerDescription {
    String getId();
    List<String> getLanguageIds();
    List<? extends DocumentFilter> getDocumentFilters();
}
