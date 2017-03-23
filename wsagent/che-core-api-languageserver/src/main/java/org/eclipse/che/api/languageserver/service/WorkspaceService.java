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
package org.eclipse.che.api.languageserver.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.typefox.lsapi.Location;
import io.typefox.lsapi.SymbolInformation;
import io.typefox.lsapi.impl.LocationImpl;
import org.eclipse.che.api.languageserver.exception.LanguageServerException;
import org.eclipse.che.api.languageserver.registry.LSOperation;
import org.eclipse.che.api.languageserver.registry.LanguageServerRegistry;
import org.eclipse.che.api.languageserver.registry.LanguageServerRegistryImpl;
import org.eclipse.che.api.languageserver.shared.lsapi.WorkspaceSymbolParamsDTO;
import org.eclipse.che.api.languageserver.shared.model.impl.InitializedServerImpl;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * REST API for the workspace/* services defined in
 * https://github.com/Microsoft/vscode-languageserver-protocol Dispatches onto
 * the {@link LanguageServerRegistryImpl}.
 *
 * @author Evgen Vidolob
 */
@Singleton
@Path("languageserver/workspace")
public class WorkspaceService {
    private LanguageServerRegistry registry;

    @Inject
    public WorkspaceService(LanguageServerRegistry registry) {
        this.registry = registry;
    }

    @POST
    @Path("symbol")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<? extends SymbolInformation> documentSymbol(WorkspaceSymbolParamsDTO workspaceSymbolParams)
                    throws ExecutionException, InterruptedException, LanguageServerException {

        Collection<InitializedServerImpl> initializedServers = registry.getInitializedServers();
        List<SymbolInformation> result = new ArrayList<>();

        LanguageServerRegistryImpl.doInParallel(initializedServers, new LSOperation<InitializedServerImpl, List<? extends SymbolInformation>>() {

            @Override
            public boolean canDo(InitializedServerImpl element) {
                return element.getInitializeResult().getCapabilities().isWorkspaceSymbolProvider();
            }

            @Override
            public CompletableFuture<List<? extends SymbolInformation>> start(InitializedServerImpl element) {
                return element.getServer().getWorkspaceService().symbol(workspaceSymbolParams);
            }

            @Override
            public boolean handleResult(InitializedServerImpl element, List<? extends SymbolInformation> infos) {
                infos.forEach(o -> {
                    Location location = o.getLocation();
                    if (location instanceof LocationImpl) {
                        ((LocationImpl) location).setUri(TextDocumentService.removePrefixUri(location.getUri()));
                    }
                });            
                result.addAll(infos);
                return !infos.isEmpty();
            }
        }, 50000);

        return result;
    }
}
