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

package org.apache.cayenne.query;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.cayenne.access.OperationObserver;

/**
 * Defines API for query execution strategy over JDBC connection (as in Strategy Pattern).
 * SQLAction unified interface allows a high degree of customization for JDBC
 * interactions.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public interface SQLAction {

    /**
     * Executes a query using a strategy defined by the implementation.
     */
    void performAction(Connection connection, OperationObserver observer)
            throws SQLException, Exception;
}