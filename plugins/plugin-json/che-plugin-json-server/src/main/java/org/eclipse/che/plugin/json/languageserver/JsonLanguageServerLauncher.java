/*******************************************************************************
 * Copyright (c) 2012-2017 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.plugin.json.languageserver;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.typefox.lsapi.services.json.JsonBasedLanguageServer;
import org.eclipse.che.api.languageserver.exception.LanguageServerException;
import org.eclipse.che.api.languageserver.launcher.LanguageServerLauncherTemplate;
import org.eclipse.che.api.languageserver.registry.LanguageServerRegistry;
import org.eclipse.che.api.languageserver.shared.model.LanguageServerDescription;
import org.eclipse.che.api.languageserver.shared.model.impl.DocumentFilterImpl;
import org.eclipse.che.api.languageserver.shared.model.impl.LanguageDescriptionImpl;
import org.eclipse.che.api.languageserver.shared.model.impl.LanguageServerDescriptionImpl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static java.util.Arrays.asList;

/**
 * @author Evgen Vidolob
 * @author Anatolii Bazko
 */
@Singleton
public class JsonLanguageServerLauncher extends LanguageServerLauncherTemplate {

    private static final String   LANGUAGE_ID = "json";
    private static final String[] EXTENSIONS  = new String[] {"json", "bowerrc", "jshintrc", "jscsrc", "eslintrc", "babelrc"};
    private static final String[] MIME_TYPES  = new String[] {"application/json"};
    private static final String   GLOB  = "*.{json,bowerrc,jshintrc,jscsrc,eslintrc,babelrc}";
    private static final LanguageServerDescription DESCRIPTION = createServerDescription();

    private final Path launchScript;

    @Inject
    public JsonLanguageServerLauncher(LanguageServerRegistry registry) {
        launchScript = Paths.get(System.getenv("HOME"), "che/ls-json/launch.sh");
        LanguageDescriptionImpl description = new LanguageDescriptionImpl();
        description.setFileExtensions(asList(EXTENSIONS));
        description.setLanguageId(LANGUAGE_ID);
        description.setMimeTypes(asList(MIME_TYPES));
        registry.registerLanguage(description);
    }


    @Override
    public boolean isAbleToLaunch() {
        return Files.exists(launchScript);
    }

    protected JsonBasedLanguageServer connectToLanguageServer(Process languageServerProcess) {
        JsonBasedLanguageServer languageServer = new JsonLanguageServer();
        languageServer.connect(languageServerProcess.getInputStream(), languageServerProcess.getOutputStream());
        return languageServer;
    }

    protected Process startLanguageServerProcess(String projectPath) throws LanguageServerException {
        ProcessBuilder processBuilder = new ProcessBuilder(launchScript.toString());
        processBuilder.redirectInput(ProcessBuilder.Redirect.PIPE);
        processBuilder.redirectOutput(ProcessBuilder.Redirect.PIPE);
        try {
            return processBuilder.start();
        } catch (IOException e) {
            throw new LanguageServerException("Can't start JSON language server", e);
        }
    }
    
    @Override
    public LanguageServerDescription getDescription() {
        return DESCRIPTION;
    }

    private static LanguageServerDescription createServerDescription() {
        LanguageServerDescriptionImpl description = new LanguageServerDescriptionImpl("org.eclipse.che.plugin.csharp.languageserver", null,
                        Arrays.asList(new DocumentFilterImpl(LANGUAGE_ID, GLOB, null)));
        return description;
    }
}
