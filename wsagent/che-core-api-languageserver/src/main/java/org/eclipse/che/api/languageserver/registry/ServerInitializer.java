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

import io.typefox.lsapi.InitializeResult;
import io.typefox.lsapi.services.LanguageServer;
import org.eclipse.che.api.languageserver.exception.LanguageServerException;
import org.eclipse.che.api.languageserver.launcher.LanguageServerLauncher;
import org.eclipse.che.api.languageserver.shared.model.LanguageServerDescription;
import org.eclipse.che.commons.lang.Pair;

import java.util.Collection;

/**
 * Is responsible to start new {@link LanguageServer}.
 *
 * @author Anatoliy Bazko
 */
public interface ServerInitializer extends ServerInitializerObservable {
    /**
     * Initialize new {@link LanguageServer} with given project path.
     */
    void initialize(LanguageServerLauncher launcher) throws LanguageServerException;

    /**
     * Returns initialized servers.
     */
    Collection<Pair<LanguageServerDescription, InitializeResult>> getInitializedServers();
}
