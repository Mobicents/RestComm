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
package org.mobicents.servlet.restcomm.dao.mybatis;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import org.joda.time.DateTime;

import static org.mobicents.servlet.restcomm.dao.DaoUtils.*;
import org.mobicents.servlet.restcomm.dao.NotificationsDao;
import org.mobicents.servlet.restcomm.entities.Notification;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.mappers.NotificationsMapper;
import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author zahid.med@gmail.com (Mohammed ZAHID)
 */
@ThreadSafe
public final class MybatisNotificationsDao implements NotificationsDao {
    private static final String namespace = "org.mobicents.servlet.sip.restcomm.dao.NotificationsDao.";
    private final SqlSessionFactory sessions;

    public MybatisNotificationsDao(final SqlSessionFactory sessions) {
        super();
        this.sessions = sessions;
    }

    @Override
    public void addNotification(final Notification notification) {
        final SqlSession session = sessions.openSession();
        try {
        	NotificationsMapper mapper=session.getMapper(NotificationsMapper.class);
        	mapper.addNotification(toMap(notification));
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public Notification getNotification(final Sid sid) {
        final SqlSession session = sessions.openSession();
        try {
        	NotificationsMapper mapper=session.getMapper(NotificationsMapper.class);
        	final Map<String, Object> result = mapper.getNotification(sid.toString());
            if (result != null) {
                return toNotification(result);
            } else {
                return null;
            }
        } finally {
            session.close();
        }
    }

    @Override
    public List<Notification> getNotifications(final Sid accountSid) {
    	 final SqlSession session = sessions.openSession();
         try {
        	 NotificationsMapper mapper=session.getMapper(NotificationsMapper.class);
             final List<Map<String, Object>> results = mapper.getNotifications(accountSid.toString());
             final List<Notification> notifications = new ArrayList<Notification>();
             if (results != null && !results.isEmpty()) {
                 for (final Map<String, Object> result : results) {
                     notifications.add(toNotification(result));
                 }
             }
             return notifications;
         } finally {
             session.close();
         }
    }

    @Override
    public List<Notification> getNotificationsByCall(final Sid callSid) {
        final SqlSession session = sessions.openSession();
        try {
       	    NotificationsMapper mapper=session.getMapper(NotificationsMapper.class);
            final List<Map<String, Object>> results = mapper.getNotificationsByCall(callSid.toString());
            final List<Notification> notifications = new ArrayList<Notification>();
            if (results != null && !results.isEmpty()) {
                for (final Map<String, Object> result : results) {
                    notifications.add(toNotification(result));
                }
            }
            return notifications;
        } finally {
            session.close();
        }
    }

    @Override
    public List<Notification> getNotificationsByLogLevel(final int logLevel) {
        final SqlSession session = sessions.openSession();
        try {
       	    NotificationsMapper mapper=session.getMapper(NotificationsMapper.class);
            final List<Map<String, Object>> results = mapper.getNotificationsByLogLevel(logLevel);
            final List<Notification> notifications = new ArrayList<Notification>();
            if (results != null && !results.isEmpty()) {
                for (final Map<String, Object> result : results) {
                    notifications.add(toNotification(result));
                }
            }
            return notifications;
        } finally {
            session.close();
        }
    }

    @Override
    public List<Notification> getNotificationsByMessageDate(final DateTime messageDate) {
        final Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("start_date", messageDate.toDate());
        parameters.put("end_date", messageDate.plusDays(1).toDate());
        final SqlSession session = sessions.openSession();
        try {
       	    NotificationsMapper mapper=session.getMapper(NotificationsMapper.class);
            final List<Map<String, Object>> results = mapper.getNotificationsByMessageDate(parameters);
            final List<Notification> notifications = new ArrayList<Notification>();
            if (results != null && !results.isEmpty()) {
                for (final Map<String, Object> result : results) {
                    notifications.add(toNotification(result));
                }
            }
            return notifications;
        } finally {
            session.close();
        }
    }

    @Override
    public void removeNotification(final Sid sid) {
        final SqlSession session = sessions.openSession();
        try {
        	NotificationsMapper mapper=session.getMapper(NotificationsMapper.class);
        	mapper.removeNotification(sid.toString());
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public void removeNotifications(final Sid accountSid) {
        final SqlSession session = sessions.openSession();
        try {
        	NotificationsMapper mapper=session.getMapper(NotificationsMapper.class);
        	mapper.removeNotifications(accountSid.toString());
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public void removeNotificationsByCall(final Sid callSid) {
        final SqlSession session = sessions.openSession();
        try {
        	NotificationsMapper mapper=session.getMapper(NotificationsMapper.class);
        	mapper.removeNotificationsByCall(callSid.toString());
            session.commit();
        } finally {
            session.close();
        }
    }

    private Map<String, Object> toMap(final Notification notification) {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("sid", writeSid(notification.getSid()));
        map.put("date_created", writeDateTime(notification.getDateCreated()));
        map.put("date_updated", writeDateTime(notification.getDateUpdated()));
        map.put("account_sid", writeSid(notification.getAccountSid()));
        map.put("call_sid", writeSid(notification.getCallSid()));
        map.put("api_version", notification.getApiVersion());
        map.put("log", notification.getLog());
        map.put("error_code", notification.getErrorCode());
        map.put("more_info", writeUri(notification.getMoreInfo()));
        map.put("message_text", notification.getMessageText());
        map.put("message_date", writeDateTime(notification.getMessageDate()));
        map.put("request_url", writeUri(notification.getRequestUrl()));
        map.put("request_method", notification.getRequestMethod());
        map.put("request_variables", notification.getRequestVariables());
        map.put("response_headers", notification.getResponseHeaders());
        map.put("response_body", notification.getResponseBody());
        map.put("uri", writeUri(notification.getUri()));
        return map;
    }

    private Notification toNotification(final Map<String, Object> map) {
        final Sid sid = readSid(map.get("sid"));
        final DateTime dateCreated = readDateTime(map.get("date_created"));
        final DateTime dateUpdated = readDateTime(map.get("date_updated"));
        final Sid accountSid = readSid(map.get("account_sid"));
        final Sid callSid = readSid(map.get("call_sid"));
        final String apiVersion = readString(map.get("api_version"));
        final Integer log = readInteger(map.get("log"));
        final Integer errorCode = readInteger(map.get("error_code"));
        final URI moreInfo = readUri(map.get("more_info"));
        final String messageText = readString(map.get("message_text"));
        final DateTime messageDate = readDateTime(map.get("message_date"));
        final URI requestUrl = readUri(map.get("request_url"));
        final String requestMethod = readString(map.get("request_method"));
        final String requestVariables = readString(map.get("request_variables"));
        final String responseHeaders = readString(map.get("response_headers"));
        final String responseBody = readString(map.get("response_body"));
        final URI uri = readUri(map.get("uri"));
        return new Notification(sid, dateCreated, dateUpdated, accountSid, callSid, apiVersion, log, errorCode, moreInfo,
                messageText, messageDate, requestUrl, requestMethod, requestVariables, responseHeaders, responseBody, uri);
    }
}
