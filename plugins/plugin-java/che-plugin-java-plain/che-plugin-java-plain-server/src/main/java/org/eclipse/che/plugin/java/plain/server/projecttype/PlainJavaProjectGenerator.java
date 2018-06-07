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

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.eclipse.che.api.fs.server.WsPathUtils.resolve;
import static org.eclipse.che.ide.ext.java.shared.Constants.JAVAC;
import static org.eclipse.che.ide.ext.java.shared.Constants.SOURCE_FOLDER;
import static org.eclipse.che.plugin.java.plain.shared.PlainJavaProjectConstants.DEFAULT_OUTPUT_FOLDER_VALUE;
import static org.eclipse.che.plugin.java.plain.shared.PlainJavaProjectConstants.DEFAULT_SOURCE_FOLDER_VALUE;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.fs.server.FsManager;
import org.eclipse.che.api.project.server.handlers.CreateProjectHandler;
import org.eclipse.che.api.project.server.type.AttributeValue;

/**
 * Generates new project which contains file with default content.
 *
 * @author Valeriy Svydenko
 */
public class PlainJavaProjectGenerator implements CreateProjectHandler {

  private static final String MAIN_CLASS_RESOURCE = "Main.java";
  public static final String PROJECT_FILE_RESOURCE = "project";
  private static final String CLASSPATH_FILE_RESOURCE = "classpath";

  static final String PROJECT_NAME_TEMPLATE = "project_name";
  private static final String SOURCE_FOLDER_TEMPLATE = "source_folder";

  private static final String CLASSPATH_FILE = ".classpath";
  static final String PROJECT_FILE = ".project";
  private static final String MAIN_CLASS_FILE = "Main.java";

  private final FsManager fsManager;
  private final FileCreator fileCreator;

  @Inject
  protected PlainJavaProjectGenerator(FsManager fsManager, FileCreator fileCreator) {
    this.fsManager = fsManager;
    this.fileCreator = fileCreator;
  }

  @Override
  public void onCreateProject(
      String projectWsPath, Map<String, AttributeValue> attributes, Map<String, String> options)
      throws ForbiddenException, ConflictException, ServerException, NotFoundException {

    List<String> sourceFolders;
    if (attributes.containsKey(SOURCE_FOLDER) && !attributes.get(SOURCE_FOLDER).isEmpty()) {
      sourceFolders = attributes.get(SOURCE_FOLDER).getList();
    } else {
      sourceFolders = singletonList(DEFAULT_SOURCE_FOLDER_VALUE);
    }

    fsManager.createDir(projectWsPath);

    String outputDirWsPath = resolve(projectWsPath, DEFAULT_OUTPUT_FOLDER_VALUE);
    fsManager.createDir(outputDirWsPath);

    String sourceDirWsPath = resolve(projectWsPath, sourceFolders.get(0));
    fsManager.createDir(sourceDirWsPath);

    String mainJavaWsPath = resolve(sourceDirWsPath, MAIN_CLASS_FILE);
    fileCreator.createFile(mainJavaWsPath, MAIN_CLASS_RESOURCE, Collections.emptyMap());

    // create .classpath
    String dotClasspathWsPath = resolve(projectWsPath, CLASSPATH_FILE);
    fileCreator.createFile(
        dotClasspathWsPath,
        CLASSPATH_FILE_RESOURCE,
        singletonMap(SOURCE_FOLDER_TEMPLATE, sourceFolders.get(0)));

    // create .project
    String dotProjectWsPath = resolve(projectWsPath, PROJECT_FILE);
    fileCreator.createFile(
        dotProjectWsPath,
        PROJECT_FILE_RESOURCE,
        singletonMap(PROJECT_NAME_TEMPLATE, projectWsPath.substring(1)));
  }

  @Override
  public String getProjectType() {
    return JAVAC;
  }
}
