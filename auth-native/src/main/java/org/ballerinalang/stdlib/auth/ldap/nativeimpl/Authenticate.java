/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.stdlib.auth.ldap.nativeimpl;

import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import org.ballerinalang.stdlib.auth.ldap.CommonLdapConfiguration;
import org.ballerinalang.stdlib.auth.ldap.ConnectionContext;
import org.ballerinalang.stdlib.auth.ldap.LdapConstants;
import org.ballerinalang.stdlib.auth.ldap.utils.LdapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.ldap.LdapContext;

/**
 * Authenticate a user with LDAP user store.
 *
 * @since 0.983.0
 */
public class Authenticate {

    private static final Logger LOG = LoggerFactory.getLogger(Authenticate.class);

    private Authenticate() {}

    public static Object authenticate(BMap<BString, Object> ldapConnection, BString userName, BString password) {
        if (userName == null || userName.getValue().isEmpty()) {
            return LdapUtils.createError("Username cannot be null or empty.");
        }

        byte[] credential = password.getValue().getBytes(StandardCharsets.UTF_8);
        ConnectionContext connectionSource =
                (ConnectionContext) ldapConnection.getNativeData(LdapConstants.LDAP_CONNECTION_SOURCE);
        DirContext ldapConnectionContext = (DirContext) ldapConnection.getNativeData(
                LdapConstants.LDAP_CONNECTION_CONTEXT);
        CommonLdapConfiguration ldapConfiguration = (CommonLdapConfiguration) ldapConnection.getNativeData(
                LdapConstants.LDAP_CONFIGURATION);
        LdapUtils.setServiceName((String) ldapConnection.getNativeData(LdapConstants.ENDPOINT_INSTANCE_ID));

        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Authenticating user '{}'", userName);
            }
            String name = LdapUtils.getNameInSpaceForUsernameFromLDAP(userName.getValue().trim(), ldapConfiguration,
                                                                      ldapConnectionContext);
            if (name == null || name.isEmpty()) {
                return LdapUtils.createError("Username cannot be found in the directory.");
            }
            LdapContext cxt = connectionSource.getContextWithCredentials(name, credential);
            if (LOG.isDebugEnabled()) {
                LOG.debug("User '{}' is authenticated", name);
            }
            LdapUtils.closeContext(cxt);
        } catch (NamingException e) {
            LOG.error("Failed to bind user '{}'", userName, e);
            return LdapUtils.createError(e.getMessage());
        } finally {
            LdapUtils.removeServiceName();
        }
        return null;
    }
}
