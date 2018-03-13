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
package org.restcomm.connect.core.service.profile;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.core.service.api.CoreService;
import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.dao.entities.Account;
import org.restcomm.connect.dao.entities.Profile;
import org.restcomm.connect.dao.entities.ProfileAssociation;

public class ProfileService implements CoreService {
    static String DEFAULT_PROFILE_SID = Profile.DEFAULT_PROFILE_SID;
    private static Logger logger = Logger.getLogger(ProfileService.class);

    private final DaoManager daoManager;

    public ProfileService(DaoManager daoManager){
        super();
        this.daoManager = daoManager;
    }

    /**
     * @param accountSid
     * @return will return associated profile of provided accountSid
     */
    public Profile retrieveProfileByAccountSid(String accountSid) {
            Profile profile = null;
            Sid currentAccount = new Sid(accountSid);
            Account lastAccount = null;

            //try to find profile in account hierarchy
            do {
                profile = retrieveProfileForTarget(currentAccount.toString());
                if (profile == null) {
                    lastAccount = daoManager.getAccountsDao().getAccount(currentAccount);
                    if (lastAccount != null) {
                        currentAccount = lastAccount.getParentSid();
                    } else {
                        throw new RuntimeException("account not found!!!");
                    }
                }
            } while(profile == null && currentAccount != null);

            //if profile is not found in account hierarchy,try org
            if (profile == null && lastAccount != null) {
                Sid organizationSid = lastAccount.getOrganizationSid();
                profile = retrieveProfileForTarget(organizationSid.toString());
            }

            //finally try with default profile
            if (profile == null) {
                try {
                    profile = daoManager.getProfilesDao().getProfile(DEFAULT_PROFILE_SID);
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Returning profile:" + profile);
            }

            return profile;
        }

        /**
         * @param targetSid
         * @return will return associated profile of provided target (account or organization)
         */
        public Profile retrieveProfileForTarget(String targetSid) {
            ProfileAssociation assoc = daoManager.getProfileAssociationsDao().getProfileAssociationByTargetSid(targetSid);
            Profile profile = null;
            if (assoc != null) {
                try {
                    profile = daoManager.getProfilesDao().getProfile(assoc.getProfileSid().toString());
                } catch (SQLException ex) {
                    throw new RuntimeException("problem retrieving profile", ex);
                }
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Returning profile:" + profile);
            }
            return profile;
        }
}
