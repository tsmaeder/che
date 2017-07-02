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
package org.eclipse.che.api.languageserver.registry;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.languageserver.exception.LanguageServerException;
import org.eclipse.che.api.languageserver.launcher.LanguageServerLauncher;
import org.eclipse.che.api.languageserver.shared.model.LanguageDescription;
import org.eclipse.che.api.project.server.FolderEntry;
import org.eclipse.che.api.project.server.ProjectManager;
import org.eclipse.che.api.project.server.VirtualFileEntry;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.services.LanguageServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Singleton
public class LanguageServerRegistryImpl implements LanguageServerRegistry {
    private final static Logger                LOG                 = LoggerFactory.getLogger(LanguageServerRegistryImpl.class);
    public final static String                 PROJECT_FOLDER_PATH = "file:///projects";
    private final List<LanguageDescription>    languages           = new ArrayList<>();
    private final List<LanguageServerLauncher> launchers           = new ArrayList<>();

    /**
     * Started {@link LanguageServer} by project.
     */
    private final Map<String, List<LanguageServerLauncher>>    launchedServers;
    private final Map<String, List<InitializedLanguageServer>> initializedServers;

    private final Provider<ProjectManager> projectManagerProvider;
    private final ServerInitializer        initializer;

    @Inject
    public LanguageServerRegistryImpl(Set<LanguageServerLauncher> languageServerLaunchers, Set<LanguageDescription> languages,
                                      Provider<ProjectManager> projectManagerProvider, ServerInitializer initializer) {
        this.languages.addAll(languages);
        this.launchers.addAll(languageServerLaunchers);
        this.projectManagerProvider = projectManagerProvider;
        this.initializer = initializer;
        this.launchedServers = new HashMap<>();
        this.initializedServers = new HashMap<>();
    }

    private LanguageDescription findLanguage(String path) {
        for (LanguageDescription language : languages) {
            if (matchesFilenames(language, path) || matchesExtensions(language, path)) {
                return language;
            }
        }
        return null;
    }

    private boolean matchesExtensions(LanguageDescription language, String path) {
        if (language.getFileExtensions() != null) {
            for (String extension : language.getFileExtensions()) {
                if (path.endsWith(extension)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matchesFilenames(LanguageDescription language, String path) {
        if (language.getFileNames() != null) {
            for (String name : language.getFileNames()) {
                if (name.equals(new File(path).getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public ServerCapabilities getCapabilities(String fileUri) throws LanguageServerException {
        return getApplicableLanguageServers(fileUri).stream().flatMap(Collection::stream)
                        .map(s -> s.getInitializeResult().getCapabilities())
                        .reduce(new ServerCapabilities(), (left, right) -> new ServerCapabilitiesOverlay(left, right).compute());
    }

    public ServerCapabilities initialize(String fileUri) throws LanguageServerException {
        String projectPath = extractProjectPath(fileUri);
        if (projectPath == null) {
            return null;
        }
        List<LanguageServerLauncher> launchers = findLaunchers(projectPath, fileUri);
        // launchers is the set of things we need to have initialized

        for (LanguageServerLauncher launcher : new ArrayList<>(launchers)) {
            synchronized (initializedServers) {
                List<LanguageServerLauncher> servers = launchedServers.get(projectPath);

                if (servers == null) {
                    servers = new ArrayList<>();
                    launchedServers.put(projectPath, servers);
                }
                List<LanguageServerLauncher> servers2 = servers;
                if (!servers2.contains(launcher)) {
                    servers2.add(launcher);
                    initializer.initialize(launcher, projectPath).thenAccept(pair -> {
                        synchronized (initializedServers) {
                            List<InitializedLanguageServer> initialized = initializedServers.get(projectPath);
                            if (initialized == null) {
                                initialized = new ArrayList<>();
                                initializedServers.put(projectPath, initialized);
                            }
                            initialized.add(new InitializedLanguageServer(pair.first, pair.second, launcher));
                            launchers.remove(launcher);
                            initializedServers.notifyAll();
                        }
                    }).exceptionally(t -> {
                        LOG.error("Error launching language server " + launcher, t);
                        synchronized (initializedServers) {
                            launchers.remove(launcher);
                            servers2.remove(launcher);
                            initializedServers.notifyAll();
                        }
                        return null;
                    });
                }
            }
        }

        // now wait for all launchers to arrive at initialized
        // eventually, all launchers will either fail or succeed, regardless of
        // which request thread started them. Thus the loop below will
        // end.
        synchronized (initializedServers) {
            List<InitializedLanguageServer> initForProject = initializedServers.get(projectPath);
            if (initForProject != null) {
                for (InitializedLanguageServer initialized : initForProject) {
                    launchers.remove(initialized.getLauncher());
                }
            }
            while (!launchers.isEmpty()) {
                try {
                    initializedServers.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
        return getCapabilities(fileUri);
    }

    private List<LanguageServerLauncher> findLaunchers(String projectPath, String fileUri) {
        LanguageDescription language = findLanguage(fileUri);
        if (language == null) {
            return Collections.emptyList();
        }
        List<LanguageServerLauncher> result = new ArrayList<>();
        for (LanguageServerLauncher launcher : launchers) {
            int score = matchScore(launcher.getDescription(), fileUri, language.getLanguageId());
            if (score > 0) {
                result.add(launcher);
            }
        }
        return result;
    }

    @Override
    public List<LanguageDescription> getSupportedLanguages() {
        return Collections.unmodifiableList(languages);
    }

    protected String extractProjectPath(String filePath) throws LanguageServerException {
        FolderEntry root;
        try {
            root = projectManagerProvider.get().getProjectsRoot();
        } catch (ServerException e) {
            throw new LanguageServerException("Project not found for " + filePath, e);
        }

        if (!filePath.startsWith(PROJECT_FOLDER_PATH)) {
            throw new LanguageServerException("Project not found for " + filePath);
        }

        VirtualFileEntry fileEntry;
        try {
            fileEntry = root.getChild(filePath.substring(PROJECT_FOLDER_PATH.length() + 1));
        } catch (ServerException e) {
            throw new LanguageServerException("Project not found for " + filePath, e);
        }

        if (fileEntry == null) {
            throw new LanguageServerException("Project not found for " + filePath);
        }

        return PROJECT_FOLDER_PATH + fileEntry.getProject();
    }

    public List<Collection<InitializedLanguageServer>> getApplicableLanguageServers(String fileUri) throws LanguageServerException {
        String projectPath = extractProjectPath(fileUri);
        LanguageDescription language = findLanguage(fileUri);
        if (projectPath == null || language == null) {
            return Collections.emptyList();
        }

        Map<Integer, List<InitializedLanguageServer>> result = new HashMap<>();

        List<InitializedLanguageServer> servers = null;
        synchronized (initializedServers) {
            List<InitializedLanguageServer> list = initializedServers.get(projectPath);
            if (list == null) {
                return Collections.emptyList();
            }
            servers = new ArrayList<InitializedLanguageServer>(list);
        }
        for (InitializedLanguageServer server : servers) {
            int score = matchScore(server.getLauncher().getDescription(), fileUri, language.getLanguageId());
            if (score > 0) {
                List<InitializedLanguageServer> list = result.get(score);
                if (list == null) {
                    list = new ArrayList<>();
                    result.put(score, list);
                }
                list.add(server);
            }
        }
        return result.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(entry -> entry.getValue()).collect(Collectors.toList());
    }

    public static <C, R> void doInParallel(Collection<C> collection, LSOperation<C, R> op, long timeoutMillis) {
        Object lock = new Object();
        List<CompletableFuture<?>> pendingResponses = new ArrayList<>();

        for (C element : collection) {
            if (op.canDo(element)) {
                CompletableFuture<R> future = op.start(element);
                synchronized (lock) {
                    pendingResponses.add(future);
                    lock.notifyAll();
                }
                future.thenAccept(result -> {
                    synchronized (lock) {
                        if (!future.isCancelled()) {
                            op.handleResult(element, result);
                            pendingResponses.remove(future);
                            lock.notifyAll();
                        }
                    }
                }).exceptionally((t) -> {
                    LOG.info("Exception occurred in request", t);
                    synchronized (lock) {
                        pendingResponses.remove(future);
                        lock.notifyAll();
                    }
                    return null;
                });
            }
        }

        long endTime = System.currentTimeMillis() + 5000;

        try {
            synchronized (lock) {
                while (System.currentTimeMillis() < endTime && pendingResponses.size() > 0) {
                    lock.wait(endTime - System.currentTimeMillis());
                }
            }
        } catch (InterruptedException e) {
            LOG.info("Thread interrupted", e);
            Thread.currentThread().interrupt();
        }
        synchronized (lock) {
            for (CompletableFuture<?> pending : pendingResponses) {
                pending.cancel(true);
            }
            lock.notifyAll();
        }
    }

    public static <C, R> void doInSequence(Collection<C> collection, LSOperation<C, R> op, long timeoutMillis) {
        long endTime = System.currentTimeMillis() + timeoutMillis;
        for (C element : collection) {
            if (op.canDo(element)) {
                CompletableFuture<R> future = op.start(element);
                try {
                    R result = future.get(Math.max(endTime - timeoutMillis, 1), TimeUnit.MILLISECONDS);
                    if (op.handleResult(element, result)) {
                        return;
                    }
                } catch (InterruptedException e) {
                    LOG.info("Thread interrupted", e);
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    LOG.info("Exception occurred in op", e);
                } catch (TimeoutException e) {
                    future.cancel(true);
                }
            }
        }
    }

    private int matchScore(LanguageServerDescription desc, String path, String languageId) {
        int match = matchLanguageId(desc, languageId);
        if (match == 10) {
            return 10;
        }

        for (DocumentFilter filter : desc.getDocumentFilters()) {
            if (filter.getLanguageId() != null && filter.getLanguageId().length() > 0) {
                match = Math.max(match, matchLanguageId(filter.getLanguageId(), languageId));
                if (match == 10) {
                    return 10;
                }
            }
            if (filter.getScheme() != null && path.startsWith(filter.getScheme() + ":")) {
                return 10;
            }
            String pattern = filter.getPathRegex();
            if (pattern != null) {
                if (pattern.equals(path)) {
                    return 10;
                }
                Pattern regex = Pattern.compile(pattern);
                if (regex.matcher(path).matches()) {
                    match = Math.max(match, 5);
                }
            }
        }
        return match;
    }

    private int matchLanguageId(String id, String languageId) {
        if (id.equals(languageId)) {
            return 10;
        } else if ("*".equals(id)) {
            return 5;
        }
        return 0;
    }

    private int matchLanguageId(LanguageServerDescription desc, String languageId) {
        int match = 0;
        List<String> languageIds = desc.getLanguageIds();
        if (languageIds == null) {
            return 0;
        }
        for (String id : languageIds) {
            if (id.equals(languageId)) {
                match = 10;
                break;
            } else if ("*".equals(id)) {
                match = 5;
            }
        }
        return match;
    }

    @PreDestroy
    protected void shutdown() {
        List<LanguageServer> allServers;
        synchronized (initializedServers) {
            allServers = initializedServers.values().stream().flatMap(l -> l.stream()).map(s -> s.getServer()).collect(Collectors.toList());
        }
        for (LanguageServer server : allServers) {
            server.shutdown();
            server.exit();
        }
    }

}
