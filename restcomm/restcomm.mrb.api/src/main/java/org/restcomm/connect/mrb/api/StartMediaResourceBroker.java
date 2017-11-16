/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */
package org.restcomm.connect.mrb.api;

import akka.actor.ActorRef;
import org.apache.commons.configuration.Configuration;
import org.restcomm.connect.commons.annotations.concurrency.Immutable;
import org.restcomm.connect.dao.DaoManager;

/**
 * @author maria.farooq@telestax.com (Maria Farooq)
 */
@Immutable
public final class StartMediaResourceBroker {
    private final Configuration configuration;
    private final DaoManager storage;
    private final ClassLoader loader;
    private final ActorRef monitoringService;

    public StartMediaResourceBroker(final Configuration configuration, final DaoManager storage, final ClassLoader loader, final ActorRef monitoringService) {
        super();
        this.configuration = configuration;
        this.storage = storage;
        this.loader = loader;
        this.monitoringService = monitoringService;
    }

    public Configuration configuration(){
        return this.configuration;
    }

    public DaoManager storage(){
        return this.storage;
    }

    public ClassLoader loader(){
        return this.loader;
    }

    public ActorRef getMonitoringService () {
        return monitoringService;
    }
}
