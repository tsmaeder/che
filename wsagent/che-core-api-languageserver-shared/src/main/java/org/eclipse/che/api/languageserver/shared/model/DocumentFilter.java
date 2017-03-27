package org.eclipse.che.api.languageserver.shared.model;

public interface DocumentFilter {
    String getPathRegex();
    String getLanguageId();
    String getScheme();
}
