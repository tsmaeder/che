package org.eclipse.che.plugin.web.typescript;

import org.eclipse.che.api.languageserver.registry.LanguageRegistrar;
import org.eclipse.che.api.languageserver.registry.LanguageServerRegistry;
import org.eclipse.che.api.languageserver.shared.model.impl.LanguageDescriptionImpl;
import org.eclipse.che.plugin.web.shared.Constants;

import static java.util.Arrays.asList;

public class TypeScriptLanguage implements LanguageRegistrar {
    private static final String[] EXTENSIONS  = new String[] {Constants.TS_EXT};
    private static final String[] MIME_TYPES  = new String[] {Constants.TS_MIME_TYPE};

    @Override
    public void register(LanguageServerRegistry registry) {
        LanguageDescriptionImpl description = new LanguageDescriptionImpl();
        description.setFileExtensions(asList(EXTENSIONS));
        description.setLanguageId(Constants.TS_LANG);
        description.setMimeTypes(asList(MIME_TYPES));
        description.setHighlightingConfiguration("[\n" +
                        "  {\"include\":\"orion.js\"},\n" +
                        "  {\"match\":\"\\\\b(?:constructor|declare|module)\\\\b\",\"name\" :\"keyword.operator.typescript\"},\n" +
                        "  {\"match\":\"\\\\b(?:any|boolean|number|string)\\\\b\",\"name\" : \"storage.type.typescript\"}\n" +
                        "]");
        registry.registerLanguage(description);
    }

}
