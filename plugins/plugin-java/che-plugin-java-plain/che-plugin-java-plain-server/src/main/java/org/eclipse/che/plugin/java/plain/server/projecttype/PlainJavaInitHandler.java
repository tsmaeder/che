/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.plugin.java.plain.server.projecttype;

import static java.util.Collections.singletonMap;
import static org.eclipse.che.api.fs.server.WsPathUtils.resolve;
import static org.eclipse.che.ide.ext.java.shared.Constants.JAVAC;

import com.google.inject.Inject;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.fs.server.FsManager;
import org.eclipse.che.api.project.server.handlers.ProjectInitHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Init handler for simple java project.
 *
 * @author Evgen Vidolob
 * @author Valeriy Svydenko
 */
public class PlainJavaInitHandler implements ProjectInitHandler {
  private FsManager fsManager;
  private FileCreator fileCreator;

  private static final Logger LOG = LoggerFactory.getLogger(PlainJavaInitHandler.class);

  @Inject
  public PlainJavaInitHandler(FsManager fsManager, FileCreator fileCreator) {
    this.fsManager = fsManager;
    this.fileCreator = fileCreator;
  }

  @Override
  public String getProjectType() {
    return JAVAC;
  }

  @Override
  public void onProjectInitialized(String projectFolder)
      throws ServerException, ForbiddenException, ConflictException, NotFoundException {
    String oldClasspathWsPath = projectFolder + "/.che/classpath";
    if (fsManager.exists(oldClasspathWsPath)) {
      try {
        fsManager.move(oldClasspathWsPath, projectFolder + "/.classpath");
        fileCreator.createFile(
            resolve(projectFolder, PlainJavaProjectGenerator.PROJECT_FILE),
            PlainJavaProjectGenerator.PROJECT_FILE_RESOURCE,
            singletonMap(
                PlainJavaProjectGenerator.PROJECT_NAME_TEMPLATE, projectFolder.substring(1)));
      } catch (ConflictException | NotFoundException | ServerException e) {
        LOG.error("Can't update project {}", projectFolder, e);
      }
    }
  }
}
