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

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import org.joda.time.DateTime;

import static org.mobicents.servlet.restcomm.dao.DaoUtils.*;
import org.mobicents.servlet.restcomm.dao.TranscriptionsDao;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.entities.Transcription;
import org.mobicents.servlet.restcomm.mappers.TranscriptionsMapper;
import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe
public final class MybatisTranscriptionsDao implements TranscriptionsDao {
    private static final String namespace = "org.mobicents.servlet.sip.restcomm.dao.TranscriptionsDao.";
    private final SqlSessionFactory sessions;

    public MybatisTranscriptionsDao(final SqlSessionFactory sessions) {
        super();
        this.sessions = sessions;
    }

    @Override
    public void addTranscription(final Transcription transcription) {
        final SqlSession session = sessions.openSession();
        try {
            TranscriptionsMapper mapper= session.getMapper(TranscriptionsMapper.class);
            mapper.addTranscription(toMap(transcription));
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public Transcription getTranscription(final Sid sid) {
        final SqlSession session = sessions.openSession();
        try {
            TranscriptionsMapper mapper= session.getMapper(TranscriptionsMapper.class);
            final Map<String, Object> result = mapper.getTranscription(sid.toString());
            if (result != null) {
                return toTranscription(result);
            } else {
                return null;
            }
        } finally {
            session.close();
        }
    }

    @Override
    public Transcription getTranscriptionByRecording(final Sid recordingSid) {
        final SqlSession session = sessions.openSession();
        try {
            TranscriptionsMapper mapper= session.getMapper(TranscriptionsMapper.class);
            final Map<String, Object> result = mapper.getTranscriptionByRecording(recordingSid.toString());
            if (result != null) {
                return toTranscription(result);
            } else {
                return null;
            }
        } finally {
            session.close();
        }
    }

    @Override
    public List<Transcription> getTranscriptions(final Sid accountSid) {
        final SqlSession session = sessions.openSession();
        try {
            TranscriptionsMapper mapper= session.getMapper(TranscriptionsMapper.class);
            final List<Map<String, Object>> results = mapper.getTranscriptions(accountSid.toString());
            final List<Transcription> transcriptions = new ArrayList<Transcription>();
            if (results != null && !results.isEmpty()) {
                for (final Map<String, Object> result : results) {
                    transcriptions.add(toTranscription(result));
                }
            }
            return transcriptions;
        } finally {
            session.close();
        }
    }

    @Override
    public void removeTranscription(final Sid sid) {
        final SqlSession session = sessions.openSession();
        try {
            TranscriptionsMapper mapper= session.getMapper(TranscriptionsMapper.class);
            mapper.removeTranscription(sid.toString());
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public void removeTranscriptions(final Sid accountSid) {
        final SqlSession session = sessions.openSession();
        try {
            TranscriptionsMapper mapper= session.getMapper(TranscriptionsMapper.class);
            mapper.removeTranscriptions(accountSid.toString());
            session.commit();
        } finally {
            session.close();
        }
    }


    @Override
    public void updateTranscription(final Transcription transcription) {
        final SqlSession session = sessions.openSession();
        try {
            TranscriptionsMapper mapper= session.getMapper(TranscriptionsMapper.class);
            mapper.updateTranscription(toMap(transcription));
            session.commit();
        } finally {
            session.close();
        }
    }

    private Map<String, Object> toMap(final Transcription transcription) {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("sid", writeSid(transcription.getSid()));
        map.put("date_created", writeDateTime(transcription.getDateCreated()));
        map.put("date_updated", writeDateTime(transcription.getDateUpdated()));
        map.put("account_sid", writeSid(transcription.getAccountSid()));
        map.put("status", transcription.getStatus().toString());
        map.put("recording_sid", writeSid(transcription.getRecordingSid()));
        map.put("duration", transcription.getDuration());
        map.put("transcription_text", transcription.getTranscriptionText());
        map.put("price", writeBigDecimal(transcription.getPrice()));
        map.put("price_unit", writeCurrency(transcription.getPriceUnit()));
        map.put("uri", writeUri(transcription.getUri()));
        return map;
    }

    private Transcription toTranscription(final Map<String, Object> map) {
        final Sid sid = readSid(map.get("sid"));
        final DateTime dateCreated = readDateTime(map.get("date_created"));
        final DateTime dateUpdated = readDateTime(map.get("date_updated"));
        final Sid accountSid = readSid(map.get("account_sid"));
        final String text = readString(map.get("status"));
        final Transcription.Status status = Transcription.Status.getStatusValue(text);
        final Sid recordingSid = readSid(map.get("recording_sid"));
        final Double duration = readDouble(map.get("duration"));
        final String transcriptionText = readString(map.get("transcription_text"));
        final BigDecimal price = readBigDecimal(map.get("price"));
        final Currency priceUnit = readCurrency(map.get("price_unit"));
        final URI uri = readUri(map.get("uri"));
        return new Transcription(sid, dateCreated, dateUpdated, accountSid, status, recordingSid, duration, transcriptionText,
                price, priceUnit, uri);
    }
}
