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

import org.eclipse.che.api.languageserver.shared.model.LanguageDescription;
import org.eclipse.che.api.languageserver.shared.model.LanguageServerDescription;
import org.eclipse.che.api.languageserver.shared.model.impl.InitializedServerImpl;

import java.util.Collection;
import java.util.List;

/**
 * @author Anatoliy Bazko
 * @author Thomas Mäder
 */
public interface LanguageServerRegistry {
    /**
     * Returns all registered languages.
     */
    Collection<LanguageDescription> getLanguages();
    void registerLanguage(LanguageDescription language);
    
    Collection<LanguageServerDescription> getRegisteredServers();

    Collection<InitializedServerImpl> getInitializedServers();
    
    void launchServers(String fileUri);
    
    List<Collection<InitializedServerImpl>> getApplicableLanguageServers(String fileUri);
    
    InitializedServerImpl getServer(String serverId);
}
