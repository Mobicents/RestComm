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
package org.restcomm.connect.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.xstream.XStream;
import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.restcomm.connect.commons.annotations.concurrency.ThreadSafe;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.dao.UsageDao;
import org.restcomm.connect.dao.entities.RestCommResponse;
import org.restcomm.connect.dao.entities.Usage;
import org.restcomm.connect.dao.entities.UsageList;
import org.restcomm.connect.http.converter.RestCommResponseConverter;
import org.restcomm.connect.http.converter.UsageConverter;
import org.restcomm.connect.http.converter.UsageListConverter;
import org.restcomm.connect.identity.AuthType;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.regex.Pattern;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;

/**
 * @author charles.roufay@telestax.com (Charles Roufay)
 * @author brainslog@gmail.com (Alexandre Mendonca)
 */
@ThreadSafe
public abstract class UsageEndpoint extends SecuredEndpoint {
  @Context
  protected ServletContext context;
  protected Configuration configuration;
  protected UsageDao dao;
  protected Gson gson;
  protected XStream xstream;

  public UsageEndpoint() {
    super();
  }

  public static final Pattern relativePattern = Pattern.compile("(\\+|\\-)(\\d+)day[s]");

  @PostConstruct
  public void init() {
    final DaoManager storage = (DaoManager) context.getAttribute(DaoManager.class.getName());
    configuration = (Configuration) context.getAttribute(Configuration.class.getName());
    configuration = configuration.subset("runtime-settings");
    super.init(configuration);
    dao = storage.getUsageDao();
    final UsageConverter converter = new UsageConverter(configuration);
    final GsonBuilder builder = new GsonBuilder();
    builder.registerTypeAdapter(Usage.class, converter);
    builder.setPrettyPrinting();
    gson = builder.create();
    xstream = new XStream();
    xstream.alias("RestcommResponse", RestCommResponse.class);
    xstream.registerConverter(converter);
    xstream.registerConverter(new UsageListConverter(configuration));
    xstream.registerConverter(new RestCommResponseConverter(configuration));
  }

  protected Response getUsage(final String accountSid, final String subresource, UriInfo info, final MediaType responseType) {
    secure(accountsDao.getAccount(accountSid), "RestComm:Read:Usage", AuthType.AuthToken);

    String categoryStr = info.getQueryParameters().getFirst("Category");
    String startDateStr = info.getQueryParameters().getFirst("StartDate");
    String endDateStr = info.getQueryParameters().getFirst("EndDate");

    Usage.Category category = categoryStr != null ? Usage.Category.valueOf(categoryStr) : null;
    DateTime startDate = new DateTime(0).withTimeAtStartOfDay();
    if (startDateStr != null) {
      try {
        startDate = DateTimeFormat.forPattern("yyyyy-MM-dd").parseDateTime(startDateStr);
      }
      catch (IllegalArgumentException iae) {
        // TODO: Support relative
      }
    }
    DateTime endDate = new DateTime();
    if (endDateStr != null) {
      try {
        endDate = DateTimeFormat.forPattern("yyyyy-MM-dd").parseDateTime(endDateStr);
      }
      catch (IllegalArgumentException iae) {
        // TODO: Support relative
      }
    }

    final List<Usage> usage;
    if (subresource.toLowerCase().equals("daily")) {
      usage = dao.getUsageDaily(new Sid(accountSid), category, startDate, endDate);
    }
    else if (subresource.toLowerCase().equals("monthly")) {
      usage = dao.getUsageMonthly(new Sid(accountSid), category, startDate, endDate);
    }
    else if (subresource.toLowerCase().equals("yearly")) {
      usage = dao.getUsageYearly(new Sid(accountSid), category, startDate, endDate);
    }
    else if (subresource.toLowerCase().equals("alltime")) {
      usage = dao.getUsageAllTime(new Sid(accountSid), category, startDate, endDate);
    } else if (subresource.toLowerCase().equals("today")) {
      usage = dao.getUsageAllTime(new Sid(accountSid), category, DateTime.now(), DateTime.now());
    }
    else if (subresource.toLowerCase().equals("yesterday")) {
      usage = dao.getUsageAllTime(new Sid(accountSid), category, DateTime.now().minusDays(1), DateTime.now().minusDays(1));
    }
    else if (subresource.toLowerCase().equals("thismonth")) {
      usage = dao.getUsageAllTime(new Sid(accountSid), category, DateTime.now().dayOfMonth().withMinimumValue(), DateTime.now().dayOfMonth().withMaximumValue());
    }
    else if (subresource.toLowerCase().equals("lastmonth")) {
      usage = dao.getUsageAllTime(new Sid(accountSid), category, DateTime.now().minusMonths(1).dayOfMonth().withMinimumValue(), DateTime.now().minusMonths(1).dayOfMonth().withMaximumValue());
    }
    else {
      usage = dao.getUsageAllTime(new Sid(accountSid), category, startDate, endDate);
    }

    if (usage == null) {
      return status(NOT_FOUND).build();
    } else {
      if (APPLICATION_JSON_TYPE == responseType) {
        return ok(gson.toJson(usage), APPLICATION_JSON).build();
      } else if (APPLICATION_XML_TYPE == responseType) {
        final RestCommResponse response = new RestCommResponse(new UsageList(usage));
        return ok(xstream.toXML(response), APPLICATION_XML).build();
      } else {
        return null;
      }
    }
  }
}
