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
package org.eclipse.che.plugin.web.inject;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import org.eclipse.che.api.languageserver.launcher.LanguageServerLauncher;
import org.eclipse.che.api.languageserver.registry.LanguageRegistrar;
import org.eclipse.che.api.project.server.type.ProjectTypeDef;
import org.eclipse.che.inject.DynaModule;
import org.eclipse.che.plugin.web.typescript.TSLSLauncher;
import org.eclipse.che.plugin.web.typescript.TypeScriptLanguage;
import org.eclipse.che.plugin.web.typescript.TypeScriptProjectType;

/**
 * The module that contains configuration of the server side part of the Web plugin
 */
@DynaModule
public class WebModule extends AbstractModule {

    @Override
    protected void configure() {
        Multibinder<ProjectTypeDef> projectTypeMultibinder = Multibinder.newSetBinder(binder(), ProjectTypeDef.class);
        projectTypeMultibinder.addBinding().to(TypeScriptProjectType.class);

        Multibinder.newSetBinder(binder(), LanguageServerLauncher.class).addBinding().to(TSLSLauncher.class);
        Multibinder.newSetBinder(binder(), LanguageRegistrar.class).addBinding().to(TypeScriptLanguage.class);
   }
}
