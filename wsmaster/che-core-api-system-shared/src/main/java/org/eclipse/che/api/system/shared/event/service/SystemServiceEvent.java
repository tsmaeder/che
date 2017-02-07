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
package org.eclipse.che.api.system.shared.event.service;

import org.eclipse.che.api.system.shared.event.SystemEvent;

import java.util.Objects;

/**
 * The base class for system service events.
 *
 * @author Yevhenii Voevodin
 */
public abstract class SystemServiceEvent implements SystemEvent {

    protected final String serviceName;

    protected SystemServiceEvent(String serviceName) {
        this.serviceName = Objects.requireNonNull(serviceName, "Service name required");
    }

    public String getServiceName() {
        return serviceName;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SystemServiceEvent)) {
            return false;
        }
        final SystemServiceEvent that = (SystemServiceEvent)obj;
        return Objects.equals(getType(), that.getType())
               && Objects.equals(serviceName, that.serviceName);
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = 31 * hash + Objects.hashCode(getType());
        hash = 31 * hash + Objects.hashCode(serviceName);
        return hash;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{evenType='" + getType() + "', serviceName=" + serviceName + "'}";
    }
}
