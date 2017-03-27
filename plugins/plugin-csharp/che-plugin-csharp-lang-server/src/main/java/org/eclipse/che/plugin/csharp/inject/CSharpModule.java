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
package org.eclipse.che.plugin.csharp.inject;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import org.eclipse.che.api.languageserver.launcher.LanguageServerLauncher;
import org.eclipse.che.api.languageserver.registry.LanguageRegistrar;
import org.eclipse.che.api.project.server.handlers.ProjectHandler;
import org.eclipse.che.api.project.server.type.ProjectTypeDef;
import org.eclipse.che.inject.DynaModule;
import org.eclipse.che.plugin.csharp.languageserver.CSharpLanguage;
import org.eclipse.che.plugin.csharp.languageserver.CSharpLanguageServerLauncher;
import org.eclipse.che.plugin.csharp.projecttype.CSharpProjectType;
import org.eclipse.che.plugin.csharp.projecttype.CreateNetCoreProjectHandler;

/**
 * @author Anatolii Bazko
 */
@DynaModule
public class CSharpModule extends AbstractModule {
    
    @Override
    protected void configure() {
        Multibinder<ProjectTypeDef> projectTypeMultibinder = Multibinder.newSetBinder(binder(), ProjectTypeDef.class);
        projectTypeMultibinder.addBinding().to(CSharpProjectType.class);

        Multibinder<ProjectHandler> projectHandlersMultibinder = Multibinder.newSetBinder(binder(), ProjectHandler.class);
        projectHandlersMultibinder.addBinding().to(CreateNetCoreProjectHandler.class);

        Multibinder.newSetBinder(binder(), LanguageServerLauncher.class).addBinding().to(CSharpLanguageServerLauncher.class);
        Multibinder.newSetBinder(binder(), LanguageRegistrar.class).addBinding().to(CSharpLanguage.class);
    }
}
