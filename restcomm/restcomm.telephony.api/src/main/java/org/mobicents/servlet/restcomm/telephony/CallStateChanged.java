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

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Immutable
public final class CallStateChanged {
    public static enum State {
        QUEUED("queued"), RINGING("ringing"), CANCELED("canceled"), BUSY("busy"), NOT_FOUND("not-found"), FAILED("failed"), NO_ANSWER(
                "no-answer"), IN_PROGRESS("in-progress"), COMPLETED("completed");

        private final String text;

        private State(final String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    };

    private final State state;

    public CallStateChanged(final State state) {
        super();
        this.state = state;
    }

    public State state() {
        return state;
    }
}
