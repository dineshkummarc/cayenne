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
package org.apache.cayenne.itest.jpa;

import java.util.Properties;

import org.apache.cayenne.itest.ItestDBUtils;

import junit.framework.TestCase;

/**
 * Abstract test case that bootstraps default JPA unit called "itest" and a schema script
 * called "schema-hsqldb.sql".
 * 
 * @author Andrus Adamchik
 */
public abstract class JpaTestCase extends TestCase {

    static {
        Properties properties = new Properties();
        ItestSetup.initInstance(properties);
    }

    protected ItestDBUtils getDbHelper() {
        return ItestSetup.getInstance().getDbHelper();
    }
}
