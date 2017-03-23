package org.eclipse.che.api.languageserver.shared.model;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.List;

public interface LanguageServerDescription {
    String getId();
    List<String> getLanguageIds();
    List<? extends DocumentFilter> getDocumentFilters();
    
    default int matchScore(String path, String languageId) {
        int match = matchLanguageId(languageId);
        if (match == 10) {
            return 10;
        }
        FileSystem fs = FileSystems.getDefault();
        
        for (DocumentFilter filter : getDocumentFilters()) {
            if (filter.getLanguageId() != null && filter.getLanguageId().length() > 0) {
                match= Math.max(match, matchLanguageId(filter.getLanguageId()));
                if (match == 10) {
                    return 10;
                }
            }
            if (filter.getScheme() != null && path.startsWith(filter.getScheme()+":")) {
                return 10;
            }
            String pattern = filter.getGlobPattern();
            if (pattern != null) {
                if (pattern.equals(path)) {
                    return 10;
                }
                PathMatcher pathMatcher = fs.getPathMatcher("glob:"+pattern);
                if (pathMatcher.matches(fs.getPath(path))) {
                    match= Math.max(match, 5);
                }
            }
        }
        return match;
    }
    default int matchLanguageId(String languageId) {
        int match= 0;
        for (String id : getLanguageIds()) {
            if (id.equals(languageId)) {
                match= 10;
                break;
            } else if ("*".equals(languageId)) {
                match= 5;
            }
        }
        return match;
    }
}
