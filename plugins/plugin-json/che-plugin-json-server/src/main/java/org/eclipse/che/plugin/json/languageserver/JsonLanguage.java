package org.eclipse.che.plugin.json.languageserver;

import org.eclipse.che.api.languageserver.registry.LanguageRegistrar;
import org.eclipse.che.api.languageserver.registry.LanguageServerRegistry;
import org.eclipse.che.api.languageserver.shared.model.impl.LanguageDescriptionImpl;

import static java.util.Arrays.asList;

public class JsonLanguage implements LanguageRegistrar {
    public static final String   LANGUAGE_ID = "json";
    private static final String[] EXTENSIONS  = new String[] {"json", "bowerrc", "jshintrc", "jscsrc", "eslintrc", "babelrc"};
    private static final String[] MIME_TYPES  = new String[] {"application/json"};

    @Override
    public void register(LanguageServerRegistry registry) {
        LanguageDescriptionImpl description = new LanguageDescriptionImpl();
        description.setFileExtensions(asList(EXTENSIONS));
        description.setLanguageId(LANGUAGE_ID);
        description.setMimeTypes(asList(MIME_TYPES));
        registry.registerLanguage(description);
    }

}
