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
package org.eclipse.che.api.languageserver.shared.model.impl;

import io.typefox.lsapi.InitializeResult;
import io.typefox.lsapi.ServerCapabilities;

import org.eclipse.che.api.languageserver.shared.model.LanguageDescription;

import java.util.List;

import static java.util.Collections.singletonList;

/**
 * @author Anatoliy Bazko
 */
public class InitializeResultImpl implements InitializeResult {

    private final ServerCapabilities  serverCapabilities;
    private final LanguageDescription languageDescription;

    public InitializeResultImpl(ServerCapabilities serverCapabilities, LanguageDescription languageDescription) {
        this.serverCapabilities = serverCapabilities;
        this.languageDescription = languageDescription;
    }

    @Override
    public ServerCapabilities getCapabilities() {
        return serverCapabilities;
    }

    public List<? extends LanguageDescription> getSupportedLanguages() {
        return singletonList(languageDescription);
    }
}
