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
import com.google.inject.Singleton;
import io.typefox.lsapi.InitializeResult;
import io.typefox.lsapi.MessageType;
import io.typefox.lsapi.impl.ClientCapabilitiesImpl;
import io.typefox.lsapi.impl.InitializeParamsImpl;
import io.typefox.lsapi.impl.MessageParamsImpl;
import io.typefox.lsapi.services.LanguageServer;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.languageserver.launcher.LanguageServerLauncher;
import org.eclipse.che.api.languageserver.messager.PublishDiagnosticsParamsMessenger;
import org.eclipse.che.api.languageserver.messager.ShowMessageMessenger;
import org.eclipse.che.api.languageserver.shared.model.DocumentFilter;
import org.eclipse.che.api.languageserver.shared.model.LanguageDescription;
import org.eclipse.che.api.languageserver.shared.model.LanguageServerDescription;
import org.eclipse.che.api.languageserver.shared.model.impl.InitializedServerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// TODO: Thread safety for various collections
// TODO: document manager -> for full/incremental sync handling

@Singleton
public class LanguageServerRegistryImpl implements LanguageServerRegistry {

    private Set<LanguageServerLauncher> launchers;
    private Set<LanguageServerLauncher> launchedServers = new HashSet<>();

    private final static Logger LOG = LoggerFactory.getLogger(LanguageServerRegistryImpl.class);
    private static final int PROCESS_ID = getProcessId();
    private static final String CLIENT_NAME = "EclipseChe";

    private final List<ServerInitializerObserver> observers;
    private final PublishDiagnosticsParamsMessenger publishDiagnosticsParamsMessenger;
    private final ShowMessageMessenger showMessageMessenger;

    private final ConcurrentHashMap<String, LanguageServer> serversById;

    private List<LanguageDescription> languages;

    private ArrayList<InitializedServerImpl> initializedServers;

    @Inject
    public LanguageServerRegistryImpl(Set<LanguageServerLauncher> languageServerLaunchers, Set<LanguageRegistrar> registrars,
                    PublishDiagnosticsParamsMessenger publishDiagnosticsParamsMessenger, ShowMessageMessenger showMessageMessenger) {
        this.launchers = new HashSet<>(languageServerLaunchers);
        this.languages = new ArrayList<>();
        this.launchedServers = new HashSet<>();
        this.publishDiagnosticsParamsMessenger = publishDiagnosticsParamsMessenger;
        this.showMessageMessenger = showMessageMessenger;
        this.observers = new ArrayList<>();
        this.serversById = new ConcurrentHashMap<>();
        this.initializedServers = new ArrayList<>();
        registrars.forEach(r -> r.register(this));
    }

    public void launchServers(String fileUri) {
        LanguageDescription language = findLanguage(fileUri);
        for (LanguageServerLauncher launcher : launchers) {
            boolean shouldLaunch = false;
            synchronized (launchedServers) {
                shouldLaunch = !launchedServers.contains(launcher)
                                && matchScore(launcher.getDescription(), fileUri, language != null ? language.getLanguageId() : null) > 0;
                launchedServers.add(launcher);
            }
            if (shouldLaunch) {
                CompletableFuture.runAsync(() -> {
                    if (!launcher.isAbleToLaunch()) {
                        showMessageMessenger.onEvent(new MessageParamsImpl(MessageType.Error,
                                        "Failed to launch language server " + launcher.getDescription().getId()));
                        return;
                    }
                    LanguageServerDescription description = launcher.getDescription();
                    try {
                        String rootPath = "/projects";
                        LanguageServer server = launcher.launch(rootPath);
                        this.serversById.put(description.getId(), server);

                        registerCallbacks(server);
                        InitializeParamsImpl initializeParams = prepareInitializeParams(rootPath);
                        InitializeResult initializeResult = server.initialize(initializeParams).get();
                        onServerInitialized(server, initializeResult, description);
                        synchronized (initializedServers) {
                            initializedServers.add(new InitializedServerImpl(description, server, initializeResult));
                        }
                        LOG.info("Initialized Language Server {}", launcher.getDescription().getId());
                    } catch (ServerException | ExecutionException e) {
                        showMessageMessenger.onEvent(new MessageParamsImpl(MessageType.Error,
                                        "Failed to launch language server " + launcher.getDescription().getId()));
                        LOG.error("Failed to launch language server " + launcher.getDescription().getId(), e);
                    } catch (InterruptedException e) {
                        LOG.info("Thread interrupted: ", e);
                        Thread.currentThread().interrupt();
                    }

                });
            }
        }
    }

    private LanguageDescription findLanguage(String fileUri) {
        synchronized (languages) {
            for (LanguageDescription language : languages) {
                for (String ext : language.getFileExtensions()) {
                    if (fileUri.endsWith("." + ext)) {
                        return language;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public List<LanguageDescription> getLanguages() {
        synchronized (languages) {
            return Collections.unmodifiableList(languages);
        }
    }

    private static int getProcessId() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        int prefixEnd = name.indexOf('@');
        if (prefixEnd != -1) {
            String prefix = name.substring(0, prefixEnd);
            try {
                return Integer.parseInt(prefix);
            } catch (NumberFormatException ignored) {
            }
        }

        LOG.error("Failed to recognize the pid of the process");
        return -1;
    }

    public void addObserver(ServerInitializerObserver observer) {
        synchronized (observers) {
            observers.add(observer);
        }
    }

    public void removeObserver(ServerInitializerObserver observer) {
        synchronized (observers) {
            observers.remove(observer);
        }
    }

    protected void onServerInitialized(LanguageServer server, InitializeResult initResult,
                    LanguageServerDescription languageServerDescription) {
        List<ServerInitializerObserver> list = null;
        synchronized (observers) {
            list = new ArrayList<>(observers);
        }
        list.forEach(observer -> observer.onServerInitialized(server, initResult, languageServerDescription));
    }

    private void registerCallbacks(LanguageServer server) {
        server.getTextDocumentService().onPublishDiagnostics(publishDiagnosticsParamsMessenger::onEvent);
        server.getWindowService().onLogMessage(messageParams -> LOG.error(messageParams.getType() + " " + messageParams.getMessage()));
        server.getWindowService().onShowMessage(showMessageMessenger::onEvent);
        server.onTelemetryEvent(o -> LOG.error(o.toString()));

        if (server instanceof ServerInitializerObserver) {
            addObserver((ServerInitializerObserver) server);
        }
    }

    private InitializeParamsImpl prepareInitializeParams(String projectPath) {
        InitializeParamsImpl initializeParams = new InitializeParamsImpl();
        initializeParams.setProcessId(PROCESS_ID);
        initializeParams.setRootPath(projectPath);
        initializeParams.setCapabilities(new ClientCapabilitiesImpl());
        initializeParams.setClientName(CLIENT_NAME);
        return initializeParams;
    }

    @PreDestroy
    protected void shutdown() {
        for (LanguageServer server : serversById.values()) {
            server.shutdown();
            server.exit();
        }
    }

    @Override
    public Collection<InitializedServerImpl> getInitializedServers() {
        synchronized (initializedServers) {
            return Collections.unmodifiableCollection(initializedServers);
        }
    }

    @Override
    public InitializedServerImpl getServer(String serverId) {
        synchronized (initializedServers) {
            for (InitializedServerImpl server : initializedServers) {
                if (server.getDescription().getId().equals(serverId)) {
                    return server;
                }
            }
        }
        return null;
    }

    @Override
    public void registerLanguage(LanguageDescription language) {
        synchronized (languages) {
            languages.add(language);
        }
    }

    public List<Collection<InitializedServerImpl>> getApplicableLanguageServers(String fileUri) {
        LanguageDescription language = findLanguage(fileUri);
        Map<Integer, List<InitializedServerImpl>> result = new HashMap<>();
        List<InitializedServerImpl> servers = null;
        synchronized (initializedServers) {
            servers = new ArrayList<>(initializedServers);
        }
        for (InitializedServerImpl server : servers) {
            int score = matchScore(server.getDescription(), fileUri, language.getLanguageId());
            if (score > 0) {
                List<InitializedServerImpl> list = result.get(score);
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
}
