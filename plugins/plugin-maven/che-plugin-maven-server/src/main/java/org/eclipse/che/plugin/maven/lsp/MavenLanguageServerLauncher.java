package org.eclipse.che.plugin.maven.lsp;

import java.util.Collections;

import com.google.inject.Inject;
import org.eclipse.che.api.languageserver.exception.LanguageServerException;
import org.eclipse.che.api.languageserver.launcher.LanguageServerLauncher;
import org.eclipse.che.api.languageserver.registry.LanguageServerDescription;
import org.eclipse.che.plugin.maven.server.core.reconcile.PomReconciler;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;

public class MavenLanguageServerLauncher implements LanguageServerLauncher {
    private PomReconciler reconciler;

    @Inject
    public MavenLanguageServerLauncher(PomReconciler reconciler) {
        this.reconciler= reconciler;
    }

    public LanguageServer launch(String projectPath, LanguageClient client) throws LanguageServerException {
        return new MavenLanguageServer(new MavenTextDocumentService(client, reconciler));
    }

    public boolean isAbleToLaunch() {
        return true;
    }

    @Override
    public LanguageServerDescription getDescription() {
        return new LanguageServerDescription("org.eclipse.che.plugin.maven", Collections.singletonList("pom"), Collections.emptyList());
    }
}
