/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2013, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.restcomm.connect.mscontrol.api.messages;

import org.restcomm.connect.commons.annotations.concurrency.Immutable;
import org.restcomm.connect.commons.dao.Sid;

import jain.protocol.ip.mgcp.message.parms.ConnectionMode;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * @author Maria Farooq (maria.farooq@telestax.com)
 *
 */
@Immutable
public final class JoinConference {

    private final Object endpoint;
    private final ConnectionMode connectionMode;
    private final Sid conferenceSid;
    private final boolean startConferenceOnEnter;
    private final boolean endConferenceOnExit;
    private final boolean beep;

    public JoinConference(final Object endpoint, final ConnectionMode connectionMode, final Sid conferenceSid, final boolean startConferenceOnEnter, final boolean endConferenceOnExit, final boolean beep) {
        this.endpoint = endpoint;
        this.connectionMode = connectionMode;
        this.conferenceSid = conferenceSid;
        this.startConferenceOnEnter = startConferenceOnEnter;
        this.endConferenceOnExit = endConferenceOnExit;
        this.beep = beep;
    }

    public Object getEndpoint() {
        return endpoint;
    }

    public ConnectionMode getConnectionMode() {
        return connectionMode;
    }

    public Sid conferenceSid() {
        return conferenceSid;
    }

    public boolean startConferenceOnEnter() {
        return startConferenceOnEnter;
    }

    public boolean endConferenceOnExit() {
        return endConferenceOnExit;
    }

    public boolean beep() {
        return beep;
    }

}
