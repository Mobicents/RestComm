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
package org.mobicents.servlet.restcomm.sms;

import java.util.concurrent.ConcurrentHashMap;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Immutable
public final class SmsSessionRequest {
    private final String from;
    private final String to;
    private final String body;
    private final ConcurrentHashMap<String, String> customHeaders;

    //TODO need to check which is using the SmsSessionRequest and modify accordingly to include or not the custom headers
    public SmsSessionRequest(final String from, final String to, final String body, final ConcurrentHashMap<String, String> customHeaders) {
        super();
        this.from = from;
        this.to = to;
        this.body = body;
        this.customHeaders = customHeaders;
    }

    public String from() {
        return from;
    }

    public String to() {
        return to;
    }

    public String body() {
        return body;
    }

    public ConcurrentHashMap<String, String> headers() {
        return customHeaders;
    }
}
