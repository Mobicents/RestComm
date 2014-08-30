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
import org.mobicents.servlet.restcomm.dao.ShortCodesDao;
import org.mobicents.servlet.restcomm.entities.ShortCode;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe
public final class MybatisShortCodesDao implements ShortCodesDao {
    private static final String namespace = "org.mobicents.servlet.sip.restcomm.dao.ShortCodesDao.";
    private final SqlSessionFactory sessions;

    public MybatisShortCodesDao(final SqlSessionFactory sessions) {
        super();
        this.sessions = sessions;
    }

    @Override
    public void addShortCode(final ShortCode shortCode) {
        final SqlSession session = sessions.openSession();
        try {
            session.insert(namespace + "addShortCode", toMap(shortCode));
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public ShortCode getShortCode(final Sid sid) {
        final SqlSession session = sessions.openSession();
        try {
            final Map<String, Object> result = session.selectOne(namespace + "getShortCode", sid.toString());
            if (result != null) {
                return toShortCode(result);
            } else {
                return null;
            }
        } finally {
            session.close();
        }
    }

    @Override
    public List<ShortCode> getShortCodes(final Sid accountSid) {
        final SqlSession session = sessions.openSession();
        try {
            final List<Map<String, Object>> results = session.selectList(namespace + "getShortCodes", accountSid.toString());
            final List<ShortCode> shortCodes = new ArrayList<ShortCode>();
            if (results != null && !results.isEmpty()) {
                for (final Map<String, Object> result : results) {
                    shortCodes.add(toShortCode(result));
                }
            }
            return shortCodes;
        } finally {
            session.close();
        }
    }

    @Override
    public void removeShortCode(final Sid sid) {
        removeShortCodes(namespace + "removeShortCode", sid);
    }

    @Override
    public void removeShortCodes(final Sid accountSid) {
        removeShortCodes(namespace + "removeShortCodes", accountSid);
    }

    private void removeShortCodes(final String selector, final Sid sid) {
        final SqlSession session = sessions.openSession();
        try {
            session.delete(selector, sid.toString());
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public void updateShortCode(final ShortCode shortCode) {
        final SqlSession session = sessions.openSession();
        try {
            session.update(namespace + "updateShortCode", toMap(shortCode));
            session.commit();
        } finally {
            session.close();
        }
    }

    private Map<String, Object> toMap(final ShortCode shortCode) {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("sid", writeSid(shortCode.getSid()));
        map.put("date_created", writeDateTime(shortCode.getDateCreated()));
        map.put("date_updated", writeDateTime(shortCode.getDateUpdated()));
        map.put("friendly_name", shortCode.getFriendlyName());
        map.put("account_sid", writeSid(shortCode.getAccountSid()));
        map.put("short_code", shortCode.getShortCode());
        map.put("api_version", shortCode.getApiVersion());
        map.put("sms_url", writeUri(shortCode.getSmsUrl()));
        map.put("sms_method", shortCode.getSmsMethod());
        map.put("sms_fallback_url", writeUri(shortCode.getSmsFallbackUrl()));
        map.put("sms_fallback_method", shortCode.getSmsFallbackMethod());
        map.put("uri", writeUri(shortCode.getUri()));
        return map;
    }

    private ShortCode toShortCode(final Map<String, Object> map) {
        final Sid sid = readSid(map.get("sid"));
        final DateTime dateCreated = readDateTime(map.get("date_created"));
        final DateTime dateUpdated = readDateTime(map.get("date_updated"));
        final String friendlyName = readString(map.get("friendly_name"));
        final Sid accountSid = readSid(map.get("account_sid"));
        final Integer shortCode = readInteger(map.get("short_code"));
        final String apiVersion = readString(map.get("api_version"));
        final URI smsUrl = readUri(map.get("sms_url"));
        final String smsMethod = readString(map.get("sms_method"));
        final URI smsFallbackUrl = readUri(map.get("sms_fallback_url"));
        final String smsFallbackMethod = readString(map.get("sms_fallback_method"));
        final URI uri = readUri(map.get("uri"));
        return new ShortCode(sid, dateCreated, dateUpdated, friendlyName, accountSid, shortCode, apiVersion, smsUrl, smsMethod,
                smsFallbackUrl, smsFallbackMethod, uri);
    }
}
