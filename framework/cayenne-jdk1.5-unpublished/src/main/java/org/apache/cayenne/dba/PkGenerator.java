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


package org.apache.cayenne.dba;

import java.util.List;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.map.DbEntity;

/**
 * Defines methods to support automatic primary key generation.
 * 
 * @author Andrus Adamchik
 */
public interface PkGenerator {

    /**
     * Generates necessary database objects to provide automatic primary key support.
     * 
     * @param node node that provides access to a DataSource.
     * @param dbEntities a list of entities that require primary key autogeneration
     *            support
     */
    void createAutoPk(DataNode node, List dbEntities) throws Exception;

    /**
     * Returns a list of SQL strings needed to generates database objects to provide
     * automatic primary support for the list of entities. No actual database operations
     * are performed.
     */
    List createAutoPkStatements(List dbEntities);

    /**
     * Drops any common database objects associated with automatic primary key generation
     * process. This may be lookup tables, special stored procedures or sequences.
     * 
     * @param node node that provides access to a DataSource.
     * @param dbEntities a list of entities whose primary key autogeneration support
     *            should be dropped.
     */
    void dropAutoPk(DataNode node, List dbEntities) throws Exception;

    /**
     * Returns SQL string needed to drop database objects associated with automatic
     * primary key generation. No actual database operations are performed.
     */
    List dropAutoPkStatements(List dbEntities);

    /**
     * Generates new (unique and non-repeating) primary key for specified DbEntity.
     * 
     * @param ent DbEntity for which automatic PK is generated.
     */
    Object generatePkForDbEntity(DataNode dataNode, DbEntity ent) throws Exception;

    /**
     * Resets any cached primary keys forcing generator to go to the database next time id
     * generation is requested. May not be applicable for all generator implementations.
     */
    void reset();
}