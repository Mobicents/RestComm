/*
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
package org.mobicents.servlet.restcomm.telephony;

import org.joda.time.DateTime;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;
import org.mobicents.servlet.restcomm.entities.Sid;

import javax.servlet.sip.SipServletResponse;

import static org.mobicents.servlet.restcomm.telephony.CallStateChanged.*;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author jean.deruelle@telestax.com
 */
@Immutable
public final class CallInfo {
    private final Sid sid;
    private final State state;
    private final CreateCall.Type type;
    private final String direction;
    private final DateTime dateCreated;
    private final String forwardedFrom;
    private final String fromName;
    private final String from;
    private final String to;
    private final SipServletResponse lastResponse;

    public CallInfo(final Sid sid, final State state, final CreateCall.Type type, final String direction,
            final DateTime dateCreated, final String forwardedFrom, final String fromName, final String from, final String to,
            final SipServletResponse lastResponse) {
        super();
        this.sid = sid;
        this.state = state;
        this.direction = direction;
        this.dateCreated = dateCreated;
        this.forwardedFrom = forwardedFrom;
        this.fromName = fromName;
        this.from = from;
        this.to = to;
        this.lastResponse = lastResponse;
        this.type = type;
    }

    public DateTime dateCreated() {
        return dateCreated;
    }

    public String direction() {
        return direction;
    }

    public CreateCall.Type type() {
        return type;
    }

    public String forwardedFrom() {
        return forwardedFrom;
    }

    public String fromName() {
        return fromName;
    }

    public String from() {
        return from;
    }

    public Sid sid() {
        return sid;
    }

    public State state() {
        return state;
    }

    public String to() {
        return to;
    }

    public SipServletResponse lastResponse() {
        return lastResponse;
    }
}
