package org.eclipse.che.api.languageserver.shared.model.impl;

import org.eclipse.che.api.languageserver.shared.model.DocumentFilter;
import org.eclipse.che.api.languageserver.shared.model.LanguageServerDescription;

import java.util.List;

public class LanguageServerDescriptionImpl implements LanguageServerDescription {
    private final String id;
    private final List<String> languageIds;
    private final List<? extends DocumentFilter> documentFilters;

    public LanguageServerDescriptionImpl(String id, List<String> languageIds, List<? extends DocumentFilter> documentFilters) {
        this.id = id;
        this.languageIds = languageIds;
        this.documentFilters = documentFilters;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public List<String> getLanguageIds() {
        return languageIds;
    }

    @Override
    public List<? extends DocumentFilter> getDocumentFilters() {
        return documentFilters;
    }

}
