/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.access.types;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Handles <code>java.lang.Short</code> type mapping. Can be configured to recast
 * java.lang.Short to java.lang.Integer when binding values to PreparedStatement. This is
 * a workaround for bugs in certain drivers. Drivers that are proven to have issues with
 * short values are Sybase and Oracle (Mac OS X only).
 * 
 * @author Andrus Adamchik
 * @since 1.0.2
 */
public class ShortType extends AbstractType {

    protected boolean widenShorts;

    public ShortType(boolean widenShorts) {
        this.widenShorts = widenShorts;
    }

    public String getClassName() {
        return Short.class.getName();
    }

    public Object materializeObject(ResultSet rs, int index, int type) throws Exception {
        short s = rs.getShort(index);
        return (rs.wasNull()) ? null : new Short(s);
    }

    public Object materializeObject(CallableStatement st, int index, int type)
            throws Exception {
        short s = st.getShort(index);
        return (st.wasNull()) ? null : new Short(s);
    }

    public void setJdbcObject(
            PreparedStatement st,
            Object val,
            int pos,
            int type,
            int precision) throws Exception {

        if (widenShorts && (val instanceof Short)) {
            val = new Integer(((Short) val).intValue());
        }

        super.setJdbcObject(st, val, pos, type, precision);
    }
}