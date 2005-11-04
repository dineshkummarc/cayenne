/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.map;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.objectstyle.cayenne.opp.hessian.HessianUtil;
import org.objectstyle.cayenne.project.NamedObjectFactory;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.util.Util;

/**
 * DataMap unit tests.
 */
public class DataMapTst extends TestCase {

    public void testSerializability() throws Exception {
        DataMap m1 = new DataMap("abc");
        DataMap d1 = (DataMap) Util.cloneViaSerialization(m1);
        assertEquals(m1.getName(), d1.getName());

        ObjEntity oe1 = new ObjEntity("oe1");
        m1.addObjEntity(oe1);

        DataMap d2 = (DataMap) Util.cloneViaSerialization(m1);
        assertNotNull(d2.getObjEntity(oe1.getName()));
    }

    public void testSerializabilityWithHessian() throws Exception {
        DataMap m1 = new DataMap("abc");
        DataMap d1 = (DataMap) HessianUtil.cloneViaHessianSerialization(m1);
        assertEquals(m1.getName(), d1.getName());

        ObjEntity oe1 = new ObjEntity("oe1");
        m1.addObjEntity(oe1);

        DataMap d2 = (DataMap) Util.cloneViaSerialization(m1);
        assertNotNull(d2.getObjEntity(oe1.getName()));
    }

    public void testInitWithProperties() {
        Map properties = new HashMap();
        properties.put(DataMap.CLIENT_SUPPORTED_PROPERTY, "true");
        properties.put(DataMap.DEFAULT_CLIENT_PACKAGE_PROPERTY, "aaaaa");

        DataMap map = new DataMap();
        map.initWithProperties(properties);

        assertTrue(map.isClientSupported());
        assertEquals("aaaaa", map.getDefaultClientPackage());

        // TODO: test other defaults
    }

    public void testDefaultSchema() {
        DataMap map = new DataMap();
        String tstSchema = "tst_schema";
        assertNull(map.getDefaultSchema());
        map.setDefaultSchema(tstSchema);
        assertEquals(tstSchema, map.getDefaultSchema());

        map.setDefaultSchema(null);
        assertNull(map.getDefaultSchema());
    }

    public void testDefaultClientPackage() {
        DataMap map = new DataMap();
        String tstPackage = "tst.pkg";
        assertNull(map.getDefaultClientPackage());
        map.setDefaultClientPackage(tstPackage);
        assertEquals(tstPackage, map.getDefaultClientPackage());

        map.setDefaultClientPackage(null);
        assertNull(map.getDefaultClientPackage());
    }

    public void testDefaultPackage() {
        DataMap map = new DataMap();
        String tstPackage = "tst.pkg";
        assertNull(map.getDefaultPackage());
        map.setDefaultPackage(tstPackage);
        assertEquals(tstPackage, map.getDefaultPackage());

        map.setDefaultPackage(null);
        assertNull(map.getDefaultPackage());
    }

    public void testDefaultSuperclass() {
        DataMap map = new DataMap();
        String tstSuperclass = "tst_superclass";
        assertNull(map.getDefaultSuperclass());
        map.setDefaultSuperclass(tstSuperclass);
        assertEquals(tstSuperclass, map.getDefaultSuperclass());

        map.setDefaultSuperclass(null);
        assertNull(map.getDefaultSuperclass());
    }

    public void testDefaultLockType() {
        DataMap map = new DataMap();
        assertEquals(ObjEntity.LOCK_TYPE_NONE, map.getDefaultLockType());
        map.setDefaultLockType(ObjEntity.LOCK_TYPE_OPTIMISTIC);
        assertEquals(ObjEntity.LOCK_TYPE_OPTIMISTIC, map.getDefaultLockType());

        map.setDefaultLockType(ObjEntity.LOCK_TYPE_NONE);
        assertEquals(ObjEntity.LOCK_TYPE_NONE, map.getDefaultLockType());
    }

    public void testName() {
        DataMap map = new DataMap();
        String tstName = "tst_name";
        map.setName(tstName);
        assertEquals(tstName, map.getName());
    }

    public void testLocation() {
        DataMap map = new DataMap();
        String tstName = "tst_name";
        assertNull(map.getLocation());
        map.setLocation(tstName);
        assertEquals(tstName, map.getLocation());
    }

    public void testAddObjEntity() {
        DataMap map = new DataMap();
        ObjEntity e = new ObjEntity("b");
        e.setClassName("b");
        map.addObjEntity(e);
        assertSame(e, map.getObjEntity(e.getName()));
        assertSame(map, e.getDataMap());
    }

    public void testAddEntityWithSameName() {
        DataMap map = new DataMap();

        // Give them different class-names... we are only testing for the same entity name
        // being a problem
        ObjEntity e1 = new ObjEntity("c");
        e1.setClassName("c1");
        ObjEntity e2 = new ObjEntity("c");
        e2.setClassName("c2");
        map.addObjEntity(e1);
        try {
            map.addObjEntity(e2);
            fail("Should not be able to add more than one entity with the same name");
        }
        catch (Exception e) {
        }
    }

    public void testRemoveThenAddNullClassName() {
        DataMap map = new DataMap();
        // It should be possible to cleanly remove and then add the same entity again.
        // Uncovered the need for this while testing modeller manually.

        ObjEntity e = new ObjEntity("f");
        map.addObjEntity(e);

        map.removeObjEntity(e.getName(), false);
        map.addObjEntity(e);
    }

    public void testRemoveObjEntity() {
        // make sure deleting an ObjEntity & other entity's relationships to it
        // works & does not cause a ConcurrentModificationException

        ObjEntity e1 = new ObjEntity("1");
        ObjEntity e2 = new ObjEntity("2");

        ObjRelationship r1 = new ObjRelationship("r1");
        r1.setTargetEntityName("2");

        ObjRelationship r2 = new ObjRelationship("r2");
        r2.setTargetEntityName("1");

        ObjRelationship r3 = new ObjRelationship("r3");
        r1.setTargetEntityName("2");

        ObjRelationship r4 = new ObjRelationship("r4");
        r4.setTargetEntityName("1");

        e1.addRelationship(r1);
        e1.addRelationship(r2);
        e2.addRelationship(r3);
        e2.addRelationship(r4);

        DataMap map = new DataMap();
        map.addObjEntity(e1);
        map.addObjEntity(e2);

        map.removeObjEntity("1", true);
        assertNull(map.getObjEntity("1"));
        assertEquals(1, e2.getRelationships().size());

        map.removeObjEntity("2", true);
        assertNull(map.getObjEntity("2"));
    }

    public void testMultipleNullClassNames() {
        // Now possible to have more than one objEntity with a null class name.
        // This test proves it

        ObjEntity e1 = new ObjEntity("g");
        ObjEntity e2 = new ObjEntity("h");

        DataMap map = new DataMap();
        map.addObjEntity(e1);
        map.addObjEntity(e2);
    }

    public void testRemoveThenAddRealClassName() {
        ObjEntity e = new ObjEntity("f");
        e.setClassName("f");

        DataMap map = new DataMap();
        map.addObjEntity(e);

        map.removeObjEntity(e.getName(), false);
        map.addObjEntity(e);
    }

    public void testAddDbEntity() {
        DbEntity e = new DbEntity("b");

        DataMap map = new DataMap();
        map.addDbEntity(e);
        assertSame(e, map.getDbEntity(e.getName()));
        assertSame(map, e.getDataMap());
    }

    public void testAddQuery() {
        Query q = new SelectQuery();
        q.setName("a");

        DataMap map = new DataMap();
        map.addQuery(q);
        assertSame(q, map.getQuery("a"));
    }

    public void testRemoveQuery() {
        Query q = new SelectQuery();
        q.setName("a");

        DataMap map = new DataMap();
        map.addQuery(q);
        assertSame(q, map.getQuery("a"));
        map.removeQuery("a");
        assertNull(map.getQuery("a"));
    }

    public void testGetQueryMap() {
        Query q = new SelectQuery();
        q.setName("a");

        DataMap map = new DataMap();
        map.addQuery(q);
        Map queries = map.getQueryMap();
        assertEquals(1, queries.size());
        assertSame(q, queries.get("a"));
    }

    // make sure deleting a DbEntity & other entity's relationships to it
    // works & does not cause a ConcurrentModificationException
    public void testRemoveDbEntity() {

        DataMap map = new DataMap();

        // create a twisty maze of intermingled relationships.
        DbEntity e1 = (DbEntity) NamedObjectFactory.createObject(DbEntity.class, map);
        e1.setName("e1");

        DbEntity e2 = (DbEntity) NamedObjectFactory.createObject(DbEntity.class, map);
        e2.setName("e2");

        DbRelationship r1 = (DbRelationship) NamedObjectFactory.createObject(
                DbRelationship.class,
                e1);
        r1.setName("r1");
        r1.setTargetEntity(e2);

        DbRelationship r2 = (DbRelationship) NamedObjectFactory.createObject(
                DbRelationship.class,
                e2);
        r2.setName("r2");
        r2.setTargetEntity(e1);

        DbRelationship r3 = (DbRelationship) NamedObjectFactory.createObject(
                DbRelationship.class,
                e1);
        r3.setName("r3");
        r3.setTargetEntity(e2);

        e1.addRelationship(r1);
        e1.addRelationship(r2);
        e1.addRelationship(r3);

        e2.addRelationship(r1);
        e2.addRelationship(r2);
        e2.addRelationship(r3);

        map.addDbEntity(e1);
        map.addDbEntity(e2);

        // now actually test something
        map.removeDbEntity(e1.getName(), true);
        assertNull(map.getDbEntity(e1.getName()));
        map.removeDbEntity(e2.getName(), true);
        assertNull(map.getDbEntity(e2.getName()));
    }

    public void testChildProcedures() throws Exception {
        DataMap map = new DataMap();
        checkProcedures(map, new String[0]);

        map.addProcedure(new Procedure("proc1"));
        checkProcedures(map, new String[] {
            "proc1"
        });

        map.addProcedure(new Procedure("proc2"));
        checkProcedures(map, new String[] {
                "proc1", "proc2"
        });

        map.removeProcedure("proc2");
        checkProcedures(map, new String[] {
            "proc1"
        });
    }

    protected void checkProcedures(DataMap map, String[] expectedNames) throws Exception {
        int len = expectedNames.length;
        Map proceduresMap = map.getProcedureMap();
        Collection proceduresCollection = map.getProcedures();

        assertNotNull(proceduresMap);
        assertEquals(len, proceduresMap.size());
        assertNotNull(proceduresCollection);
        assertEquals(len, proceduresCollection.size());

        for (int i = 0; i < len; i++) {
            Procedure proc = map.getProcedure(expectedNames[i]);
            assertNotNull(proc);
            assertEquals(expectedNames[i], proc.getName());
        }
    }

}