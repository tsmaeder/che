package org.eclipse.che.api.languageserver.shared.model;

public interface DocumentFilter {
    String getGlobPattern();
    String getLanguageId();
    String getScheme();
}
