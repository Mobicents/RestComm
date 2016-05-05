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
package org.mobicents.servlet.restcomm.http.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.utils.URIBuilder;
import org.mobicents.servlet.restcomm.configuration.RestcommConfiguration;
import org.mobicents.servlet.restcomm.http.CustomHttpClientBuilder;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLStreamException;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class Downloader extends UntypedActor {

    // Logger.
    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    public Downloader() {
        super();
    }

    public HttpResponseDescriptor fetch(final HttpRequestDescriptor descriptor) throws IllegalArgumentException, IOException,
            URISyntaxException, XMLStreamException {
        int code = -1;
        HttpRequest request = null;
        HttpResponse response = null;
        HttpRequestDescriptor temp = descriptor;
        do {
            final HttpClient client = CustomHttpClientBuilder.build(RestcommConfiguration.getInstance().getMain());
            client.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);
//            client.getParams().setParameter("http.protocol.content-charset", "UTF-8");
            request = request(temp);
//            request.setHeader(CoreProtocolPNames.HTTP_CONTENT_CHARSET, Consts.UTF_8.name());
            response = client.execute((HttpUriRequest) request);
            code = response.getStatusLine().getStatusCode();
            if (isRedirect(code)) {
                final Header header = response.getFirstHeader(HttpHeaders.LOCATION);
                if (header != null) {
                    final String location = header.getValue();
                    final URI uri = URI.create(location);
                    temp = new HttpRequestDescriptor(uri, temp.getMethod(), temp.getParameters());
                    continue;
                } else {
                    break;
                }
            }
        } while (isRedirect(code));
        if (isHttpError(code)) {
            String requestUrl = request.getRequestLine().getUri();
            String errorReason = response.getStatusLine().getReasonPhrase();
            String httpErrorMessage = String.format(
                    "Error while fetching http resource: %s \n Http error code: %d \n Http error message: %s", requestUrl,
                    code, errorReason);
            logger.warning(httpErrorMessage);
        }
        return  validateXML(response(request, response));
    }

    private boolean isRedirect(final int code) {
        return HttpStatus.SC_MOVED_PERMANENTLY == code || HttpStatus.SC_MOVED_TEMPORARILY == code
                || HttpStatus.SC_SEE_OTHER == code || HttpStatus.SC_TEMPORARY_REDIRECT == code;
    }

    private boolean isHttpError(final int code) {
        return (code >= 400);
    }

    private HttpResponseDescriptor validateXML (final HttpResponseDescriptor descriptor) throws XMLStreamException {
        if (descriptor.getContentLength() > 0) {
            try {
                // parse an XML document into a DOM tree
                String xml = descriptor.getContentAsString().trim().replaceAll("&([^;]+(?!(?:\\w|;)))", "&amp;$1");
                DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                parser.parse(new InputSource(new ByteArrayInputStream(xml.getBytes("utf-8"))));
                return descriptor;
            } catch (final Exception e) {
                throw new XMLStreamException("Error parsing the RCML:" + e);
            }
        }
        return descriptor;
    }

    @Override
    public void onReceive(final Object message) throws Exception {
        final Class<?> klass = message.getClass();
        final ActorRef self = self();
        final ActorRef sender = sender();
        if (HttpRequestDescriptor.class.equals(klass)) {
            final HttpRequestDescriptor request = (HttpRequestDescriptor) message;
            logger.debug("New HttpRequestDescriptor, method: "+request.getMethod()+" URI: "+request.getUri()+" parameters: "+request.getParametersAsString());
            DownloaderResponse response = null;
            try {
                response = new DownloaderResponse(fetch(request));
            } catch (final Exception exception) {
                logger.error("Exception while trying to download RCML, exception: "+exception);
                response = new DownloaderResponse(exception, "Exception while trying to download RCML");
            }
            if (sender != null) {
                sender.tell(response, self);
            }
        }
    }

    public HttpUriRequest request(final HttpRequestDescriptor descriptor) throws IllegalArgumentException, URISyntaxException,
            UnsupportedEncodingException {
        final URI uri = descriptor.getUri();
        final String method = descriptor.getMethod();
        if ("GET".equalsIgnoreCase(method)) {
            final String query = descriptor.getParametersAsString();
            URI result = null;
            if (query != null && !query.isEmpty()) {
                result = new URIBuilder()
                .setScheme(uri.getScheme())
                .setHost(uri.getHost())
                .setPort(uri.getPort())
                .setPath(uri.getPath())
                .setQuery(query)
                .build();
            } else {
                result = uri;
            }
            return new HttpGet(result);
        } else if ("POST".equalsIgnoreCase(method)) {
            final List<NameValuePair> parameters = descriptor.getParameters();
            final HttpPost post = new HttpPost(uri);
            post.setEntity(new UrlEncodedFormEntity(parameters, "UTF-8"));
            return post;
        } else {
            throw new IllegalArgumentException(method + " is not a supported downloader method.");
        }
    }

    private HttpResponseDescriptor response(final HttpRequest request, final HttpResponse response) throws IOException {
        final HttpResponseDescriptor.Builder builder = HttpResponseDescriptor.builder();
        final URI uri = URI.create(request.getRequestLine().getUri());
        builder.setURI(uri);
        builder.setStatusCode(response.getStatusLine().getStatusCode());
        builder.setStatusDescription(response.getStatusLine().getReasonPhrase());
        builder.setHeaders(response.getAllHeaders());
        final HttpEntity entity = response.getEntity();
        if (entity != null) {
            final Header contentEncoding = entity.getContentEncoding();
            if (contentEncoding != null) {
                builder.setContentEncoding(contentEncoding.getValue());
            }
            final Header contentType = entity.getContentType();
            if (contentType != null) {
                builder.setContentType(contentType.getValue());
            }
            builder.setContent(entity.getContent());
            builder.setContentLength(entity.getContentLength());
            builder.setIsChunked(entity.isChunked());
        }
        return builder.build();
    }

    @Override
    public void postStop() {
        logger.debug("Downloader at post stop");
        super.postStop();
    }
}
