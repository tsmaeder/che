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
import org.eclipse.che.api.languageserver.shared.model.LanguageServerDescription;

/**
 * @author Anatoliy Bazko
 */
public interface ServerInitializerObserver {

    /**
     * Notifies observers when server is initialized and ready to use.
     *
     * @param server
     *      the {@link LanguageServer}
     * @param capabilities
     *      the supported capabilities by server
     * @param languageDescription
     * @param projectPath
     */
    void onServerInitialized(LanguageServer server,
                             InitializeResult initResult,
                             LanguageServerDescription languageServerDescription);
}
