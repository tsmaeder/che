package org.eclipse.che.api.languageserver.shared.model.impl;

import org.eclipse.che.api.languageserver.shared.model.DocumentFilter;

public class DocumentFilterImpl implements DocumentFilter {

    private final String globPattern;
    private final String languageId;
    private final String scheme;

    public DocumentFilterImpl(String languageId, String globPattern, String scheme) {
        this.globPattern = globPattern;
        this.languageId = languageId;
        this.scheme = scheme;
    }
    
    @Override
    public String getLanguageId() {
        return languageId;
    }

    @Override
    public String getPathRegex() {
        return globPattern;
    }

    @Override
    public String getScheme() {
        return scheme;
    }

}
