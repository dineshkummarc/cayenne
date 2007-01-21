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


package org.apache.cayenne.unit;

import java.sql.Connection;
import java.util.Collection;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DataMap;

/**
 * @author Andrus Adamchik
 */
public class DB2StackAdapter extends AccessStackAdapter {

    public DB2StackAdapter(DbAdapter adapter) {
        super(adapter);
    }
    
    public void willDropTables(Connection conn, DataMap map, Collection tablesToDrop) throws Exception {
        // avoid dropping constraints...  
    }

    public boolean supportsBinaryPK() {
        return false;
    }

    public boolean supportsLobs() {
        return true;
    }

    public boolean supportsStoredProcedures() {
        return false;
    }

  /*  public void createdTables(Connection con, DataMap map) throws Exception {
        executeDDL(con, super.ddlFile("db2", "create-update-sp.sql"));
        executeDDL(con, super.ddlFile("db2", "create-out-sp.sql"));
        executeDDL(con, super.ddlFile("db2", "create-select-sp.sql"));
    }

    public void willDropTables(Connection con, DataMap map) throws Exception {
        // still have to figure out how to safely drop procedures

        try {
            executeDDL(con, super.ddlFile("db2", "drop-select-sp.sql"));
        }
        catch (SQLException ex) {
            logObj.info("Can't drop procedure, ignoring.", ex);
        }

        try {
            executeDDL(con, super.ddlFile("db2", "drop-update-sp.sql"));
        }
        catch (SQLException ex) {
            logObj.info("Can't drop procedure, ignoring.", ex);
        }

        try {
            executeDDL(con, super.ddlFile("db2", "drop-out-sp.sql"));
        }
        catch (SQLException ex) {
            logObj.info("Can't drop procedure, ignoring.", ex);
        }
    } */
}