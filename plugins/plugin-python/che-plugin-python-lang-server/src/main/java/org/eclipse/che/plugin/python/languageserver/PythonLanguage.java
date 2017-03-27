package org.eclipse.che.plugin.python.languageserver;

import org.eclipse.che.api.languageserver.registry.LanguageRegistrar;
import org.eclipse.che.api.languageserver.registry.LanguageServerRegistry;
import org.eclipse.che.api.languageserver.shared.model.impl.LanguageDescriptionImpl;
import org.eclipse.che.plugin.python.shared.ProjectAttributes;

import static java.util.Arrays.asList;

public class PythonLanguage implements LanguageRegistrar {
    private static final String[] EXTENSIONS  = new String[] {ProjectAttributes.PYTHON_EXT};
    private static final String[] MIME_TYPES  = new String[] {"text/x-python"};

    @Override
    public void register(LanguageServerRegistry registry) {
        LanguageDescriptionImpl description = new LanguageDescriptionImpl();
        description.setFileExtensions(asList(EXTENSIONS));
        description.setLanguageId(ProjectAttributes.PYTHON_ID);
        description.setMimeTypes(asList(MIME_TYPES));
        registry.registerLanguage(description);
    }

}
