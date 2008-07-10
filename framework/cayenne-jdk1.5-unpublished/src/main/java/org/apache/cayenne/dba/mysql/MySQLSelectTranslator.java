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

package org.apache.cayenne.dba.mysql;

import org.apache.cayenne.access.trans.SelectTranslator;
import org.apache.cayenne.query.QueryMetadata;

/**
 * @since 1.2
 * @author Andrus Adamchik
 */
public class MySQLSelectTranslator extends SelectTranslator {

    @Override
    public String createSqlString() throws Exception {
        String sql = super.createSqlString();
        QueryMetadata metadata = getQuery().getMetaData(getEntityResolver());
        
        // limit results
        int offset = metadata.getFetchOffset();
        int limit = metadata.getFetchLimit();
        
        if (offset > 0 || limit > 0) {
            sql += " LIMIT ";
            if (limit == 0) {
                limit = Integer.MAX_VALUE;
            }
            sql += limit + " OFFSET " +  offset; 
        }
        return sql;
    }
}
