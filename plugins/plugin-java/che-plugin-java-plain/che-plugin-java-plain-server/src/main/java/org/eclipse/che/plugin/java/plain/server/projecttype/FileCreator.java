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

import static org.eclipse.che.commons.lang.Deserializer.resolveVariables;

import com.google.inject.Inject;
import java.io.InputStream;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.fs.server.FsManager;

public class FileCreator {
  private FsManager fsManager;

  @Inject
  protected FileCreator(FsManager fsManager) {
    this.fsManager = fsManager;
  }

  void createFile(String fileWsPath, String resourceName, Map<String, String> parameters)
      throws ConflictException, NotFoundException, ServerException {
    String template = getResource(resourceName);
    String content = resolveVariables(template, parameters);
    fsManager.createFile(fileWsPath, content);
  }

  private String getResource(String resourceName) throws ServerException {
    try (InputStream resourceAsStream =
        getClass().getClassLoader().getResourceAsStream(resourceName)) {
      return IOUtils.toString(resourceAsStream);
    } catch (Exception e) {
      throw new ServerException(e.getMessage());
    }
  }
}
