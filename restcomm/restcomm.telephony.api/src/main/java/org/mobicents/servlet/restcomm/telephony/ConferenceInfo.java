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
package org.mobicents.servlet.restcomm.telephony;

import akka.actor.ActorRef;

import java.util.List;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;
import org.mobicents.servlet.restcomm.entities.Sid;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Immutable
public final class ConferenceInfo {

    private final Sid conferenceSid;
    private final List<ActorRef> participants;
    private final ConferenceStateChanged.State state;
    private final String name;

    public ConferenceInfo(final Sid conferenceSid, final List<ActorRef> participants, final ConferenceStateChanged.State state, final String name) {
        super();
        this.conferenceSid = conferenceSid;
        this.participants = participants;
        this.state = state;
        this.name = name;
    }

    public List<ActorRef> participants() {
        return participants;
    }

    public ConferenceStateChanged.State state() {
        return state;
    }

    public String name() {
        return name;
    }

    public Sid conferenceSid() {
        return conferenceSid;
    }

}
