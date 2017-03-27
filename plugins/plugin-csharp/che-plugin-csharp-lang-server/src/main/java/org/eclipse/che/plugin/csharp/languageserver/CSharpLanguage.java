package org.eclipse.che.plugin.csharp.languageserver;

import org.eclipse.che.api.languageserver.registry.LanguageRegistrar;
import org.eclipse.che.api.languageserver.registry.LanguageServerRegistry;
import org.eclipse.che.api.languageserver.shared.model.impl.LanguageDescriptionImpl;

import java.util.Arrays;

import static java.util.Arrays.asList;

public class CSharpLanguage implements LanguageRegistrar {
    public static final String LANGUAGE_ID = "csharp";
    static final String[] EXTENSIONS = new String[] { "cs", "csx" };
    static final String[] MIME_TYPES = new String[] { "text/x-csharp" };

    @Override
    public void register(LanguageServerRegistry registry) {
        LanguageDescriptionImpl description = new LanguageDescriptionImpl();
        description.setFileExtensions(asList(EXTENSIONS));
        description.setLanguageId(LANGUAGE_ID);
        description.setMimeTypes(Arrays.asList(MIME_TYPES));
        registry.registerLanguage(description);
    }

}
