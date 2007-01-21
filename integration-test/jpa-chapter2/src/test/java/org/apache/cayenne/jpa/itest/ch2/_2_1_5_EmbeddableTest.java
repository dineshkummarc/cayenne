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
package org.apache.cayenne.jpa.itest.ch2;

import org.apache.cayenne.itest.jpa.EntityManagerCase;
import org.apache.cayenne.jpa.itest.ch2.entity.Embeddable1;
import org.apache.cayenne.jpa.itest.ch2.entity.EmbeddedEntity;

public class _2_1_5_EmbeddableTest extends EntityManagerCase {

    public void testEmbeddable() throws Exception {
        getDbHelper().deleteAll("EmbeddedEntity");
        
        EmbeddedEntity o1 = new EmbeddedEntity();
        Embeddable1 o2 = new Embeddable1();
        o2.setProperty1("p1");
        o1.setEmbeddable(o2);

        getEntityManager().persist(o1);
        getEntityManager().getTransaction().commit();

        // TODO: andrus 8/10/2006 - fails
        // assertEquals("p1", ItestDBUtils.getSingleValue("EmbeddedEntity", "property1"));
    }
}
