package org.eclipse.che.plugin.php.languageserver;

import org.eclipse.che.api.languageserver.registry.LanguageRegistrar;
import org.eclipse.che.api.languageserver.registry.LanguageServerRegistry;
import org.eclipse.che.api.languageserver.shared.model.impl.LanguageDescriptionImpl;

import static java.util.Arrays.asList;

public class PhpLanguage implements LanguageRegistrar {
    public static final String   LANGUAGE_ID = "php";
    private static final String[] EXTENSIONS  = new String[] {"php"};
    private static final String[] MIME_TYPES  = new String[] {"text/x-php"};

    @Override
    public void register(LanguageServerRegistry registry) {
        LanguageDescriptionImpl description = new LanguageDescriptionImpl();
        description.setFileExtensions(asList(EXTENSIONS));
        description.setLanguageId(LANGUAGE_ID);
        description.setMimeTypes(asList(MIME_TYPES));
        registry.registerLanguage(description);
    }

}
