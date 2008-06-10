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
package org.apache.cayenne;

import java.util.List;

import org.apache.cayenne.access.ClientServerChannel;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.remote.ClientChannel;
import org.apache.cayenne.remote.service.LocalConnection;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.unit.AccessStack;
import org.apache.cayenne.unit.CayenneCase;
import org.apache.cayenne.unit.CayenneResources;
import org.apache.cayenne.unit.UnitLocalConnection;

public class CayenneContextEJBQLTest extends CayenneCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }

    @Override
    protected AccessStack buildAccessStack() {
        return CayenneResources.getResources().getAccessStack(MULTI_TIER_ACCESS_STACK);
    }

    public void testEJBQLSelect() throws Exception {
        createTestData("testEJBQLSelect");
        
        DataContext context = createDataContext();
        ClientServerChannel clientServerChannel = new ClientServerChannel(context);
        UnitLocalConnection connection = new UnitLocalConnection(
                clientServerChannel,
                LocalConnection.HESSIAN_SERIALIZATION);
        ClientChannel channel = new ClientChannel(connection);
        CayenneContext clientContext = new CayenneContext(channel);

        EJBQLQuery query = new EJBQLQuery("SELECT a FROM MtTable1 a");

        List<ClientMtTable1> results = clientContext.performQuery(query);
        assertEquals(2, results.size());
    }
}
