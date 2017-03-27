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
package org.eclipse.che.plugin.languageserver.ide.registry;

import com.google.gwt.regexp.shared.RegExp;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;
import io.typefox.lsapi.ServerCapabilities;
import org.eclipse.che.api.languageserver.shared.event.LanguageServerInitializeEventDto;
import org.eclipse.che.api.languageserver.shared.lsapi.InitializedServerDTO;
import org.eclipse.che.api.languageserver.shared.model.DocumentFilter;
import org.eclipse.che.api.languageserver.shared.model.LanguageDescription;
import org.eclipse.che.api.languageserver.shared.model.LanguageServerDescription;
import org.eclipse.che.ide.api.machine.events.WsAgentStateEvent;
import org.eclipse.che.ide.api.machine.events.WsAgentStateHandler;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.notification.StatusNotification;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.util.loging.Log;
import org.eclipse.che.ide.websocket.MessageBus;
import org.eclipse.che.ide.websocket.MessageBusProvider;
import org.eclipse.che.ide.websocket.WebSocketException;
import org.eclipse.che.ide.websocket.rest.SubscriptionHandler;
import org.eclipse.che.ide.websocket.rest.Unmarshallable;
import org.eclipse.che.plugin.languageserver.ide.service.LanguageServerRegistryServiceClient;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Anatoliy Bazko
 */
@Singleton
public class LanguageServerRegistry {
    private final EventBus eventBus;
    private final LanguageServerRegistryServiceClient client;
    private final List<InitializedServerDTO> initializedServers = new ArrayList<>();
    private final List<LanguageDescription> registeredLanguages = new ArrayList<>();

    @Inject
    public LanguageServerRegistry(EventBus eventBus, LanguageServerRegistryServiceClient client) {
        this.eventBus = eventBus;
        this.client = client;
    }

    public void getOrInitializeServer(String filePath) {
        client.initializeServer(filePath);
    }

    @Inject
    protected void registerAllServers() {
        eventBus.addHandler(WsAgentStateEvent.TYPE, new WsAgentStateHandler() {
            @Override
            public void onWsAgentStarted(WsAgentStateEvent event) {
                client.getSupportedLanguages().then(languages -> {
                    registeredLanguages.addAll(languages);
                });
                client.getInitializedServers().then((s) -> {
                    initializedServers.addAll(s);
                });
            }

            @Override
            public void onWsAgentStopped(WsAgentStateEvent event) {
            }
        });
    }

    @Inject
    protected void subscribeToInitializeEvent(final DtoUnmarshallerFactory unmarshallerFactory, final MessageBusProvider messageBusProvider,
                    final NotificationManager notificationManager, final EventBus eventBus) {
        eventBus.addHandler(WsAgentStateEvent.TYPE, new WsAgentStateHandler() {
            @Override
            public void onWsAgentStarted(WsAgentStateEvent event) {
                MessageBus messageBus = messageBusProvider.getMachineMessageBus();
                Unmarshallable<LanguageServerInitializeEventDto> unmarshaller = unmarshallerFactory
                                .newWSUnmarshaller(LanguageServerInitializeEventDto.class);

                try {
                    messageBus.subscribe("languageserver", new SubscriptionHandler<LanguageServerInitializeEventDto>(unmarshaller) {
                        @Override
                        protected void onMessageReceived(LanguageServerInitializeEventDto initializeEvent) {
                            initializedServers.add(initializeEvent.getServer());
                        }

                        @Override
                        protected void onErrorReceived(Throwable exception) {
                            notificationManager.notify(exception.getMessage(), StatusNotification.Status.FAIL,
                                            StatusNotification.DisplayMode.NOT_EMERGE_MODE);
                        }
                    });
                } catch (WebSocketException exception) {
                    Log.error(getClass(), exception);
                }
            }

            @Override
            public void onWsAgentStopped(WsAgentStateEvent event) {
            }
        });
    }

    public ServerCapabilities getCapabilities(String path) {
        LanguageDescription language = findLanguage(path);
        return initializedServers.stream()
                        .filter(server -> matchScore(server.getDescription(), path, language == null ? null : language.getLanguageId()) > 0)
                        .map(server -> (ServerCapabilities)server.getInitializeResult().getCapabilities()).reduce(null, (left, right) -> {
                            if (left == null) {
                                return right;
                            }
                            return new ServerCapabilitiesOverlay(left, right);
                        });
    }

    private LanguageDescription findLanguage(String fileUri) {
        for (LanguageDescription language : registeredLanguages) {
            for (String ext : language.getFileExtensions()) {
                if (fileUri.endsWith("." + ext)) {
                    return language;
                }
            }
        }
        return null;
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
                RegExp regex = RegExp.compile(pattern);
                if (regex.test(path)) {
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
        for (String id : desc.getLanguageIds()) {
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
