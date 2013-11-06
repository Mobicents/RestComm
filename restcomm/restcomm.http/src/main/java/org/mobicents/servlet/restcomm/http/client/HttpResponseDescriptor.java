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
package org.mobicents.servlet.restcomm.http.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.apache.http.Header;

import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.restcomm.util.HttpUtils;
import org.mobicents.servlet.restcomm.util.StringUtils;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe
public final class HttpResponseDescriptor {
    private final URI uri;
    private final int statusCode;
    private final String statusDescription;
    private final InputStream content;
    private final long contentLength;
    private final String contentEncoding;
    private final String contentType;
    private final boolean isChunked;
    private final Header[] headers;

    private volatile String buffer;

    private HttpResponseDescriptor(final URI uri, final int statusCode, final String statusDescription,
            final InputStream content, final long contentLength, final String contentEncoding, final String contentType,
            final boolean isChunked, final Header[] headers) {
        super();
        this.uri = uri;
        this.statusCode = statusCode;
        this.statusDescription = statusDescription;
        this.content = content;
        this.contentLength = contentLength;
        this.contentEncoding = contentEncoding;
        this.contentType = contentType;
        this.isChunked = isChunked;
        this.headers = headers;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusDescription() {
        return statusDescription;
    }

    public InputStream getContent() {
        return content;
    }

    public String getContentAsString() throws IOException {
        if (buffer != null) {
            return buffer;
        } else {
            synchronized (this) {
                if (buffer == null) {
                    buffer = StringUtils.toString(content);
                }
            }
            return buffer;
        }
    }

    public long getContentLength() {
        return contentLength;
    }

    public String getContentEncoding() {
        return contentEncoding;
    }

    public String getContentType() {
        return contentType;
    }

    public boolean isChunked() {
        return isChunked;
    }

    public Header[] getHeaders() {
        return headers;
    }

    public String getHeadersAsString() {
        return HttpUtils.toString(headers);
    }

    public URI getURI() {
        return uri;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private URI uri;
        private int statusCode;
        private String statusDescription;
        private InputStream content;
        private long contentLength;
        private String contentEncoding;
        private String contentType;
        private boolean isChunked;
        private Header[] headers;

        private Builder() {
            super();
        }

        public HttpResponseDescriptor build() {
            return new HttpResponseDescriptor(uri, statusCode, statusDescription, content, contentLength, contentEncoding,
                    contentType, isChunked, headers);
        }

        public void setStatusCode(final int statusCode) {
            this.statusCode = statusCode;
        }

        public void setStatusDescription(final String statusDescription) {
            this.statusDescription = statusDescription;
        }

        public void setContent(final InputStream content) {
            this.content = content;
        }

        public void setContentLength(final long contentLength) {
            this.contentLength = contentLength;
        }

        public void setContentEncoding(final String contentEncoding) {
            this.contentEncoding = contentEncoding;
        }

        public void setContentType(final String contentType) {
            this.contentType = contentType;
        }

        public void setIsChunked(final boolean isChunked) {
            this.isChunked = isChunked;
        }

        public void setHeaders(final Header[] headers) {
            this.headers = headers;
        }

        public void setURI(final URI uri) {
            this.uri = uri;
        }
    }
}
