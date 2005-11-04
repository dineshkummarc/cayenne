/*
 * ==================================================================== The ObjectStyle
 * Group Software License, version 1.1 ObjectStyle Group - http://objectstyle.org/
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors of the
 * software. All rights reserved. Redistribution and use in source and binary forms, with
 * or without modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer. 2. Redistributions in binary form must
 * reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 * 3. The end-user documentation included with the redistribution, if any, must include
 * the following acknowlegement: "This product includes software developed by independent
 * contributors and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 * Alternately, this acknowlegement may appear in the software itself, if and wherever
 * such third-party acknowlegements normally appear. 4. The names "ObjectStyle Group" and
 * "Cayenne" must not be used to endorse or promote products derived from this software
 * without prior written permission. For written permission, email "andrus at objectstyle
 * dot org". 5. Products derived from this software may not be called "ObjectStyle" or
 * "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their names without prior
 * written permission. THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE OBJECTSTYLE
 * GROUP OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ==================================================================== This software
 * consists of voluntary contributions made by many individuals and hosted on ObjectStyle
 * Group web site. For more information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.access;

import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.map.DeleteRule;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.testdo.relationship.DeleteRuleFlatA;
import org.objectstyle.cayenne.testdo.relationship.DeleteRuleFlatB;
import org.objectstyle.cayenne.testdo.relationship.DeleteRuleTest1;
import org.objectstyle.cayenne.testdo.relationship.DeleteRuleTest2;
import org.objectstyle.cayenne.testdo.relationship.DeleteRuleTest3;
import org.objectstyle.cayenne.unit.RelationshipTestCase;

public class DeleteRulesTst extends RelationshipTestCase {

    private DataContext context;

    protected void setUp() throws Exception {
        super.setUp();

        deleteTestData();
        context = createDataContext();
    }

    public void testDenyToOne() {
        //DeleteRuleTest1 test2
        DeleteRuleTest1 test1 = (DeleteRuleTest1) context
                .createAndRegisterNewObject("DeleteRuleTest1");
        DeleteRuleTest2 test2 = (DeleteRuleTest2) context
                .createAndRegisterNewObject("DeleteRuleTest2");
        test1.setTest2(test2);
        context.commitChanges();

        try {
            context.deleteObject(test1);
            fail("Should have thrown an exception");
        }
        catch (Exception e) {
            //GOOD!
        }
        context.commitChanges();

    }

    public void testNoActionToOne() {
        DeleteRuleTest2 test2 = (DeleteRuleTest2) context
                .createAndRegisterNewObject("DeleteRuleTest2");
        DeleteRuleTest3 test3 = (DeleteRuleTest3) context
                .createAndRegisterNewObject("DeleteRuleTest3");
        test3.setToDeleteRuleTest2(test2);
        context.commitChanges();

        // must go on without exceptions...
        context.deleteObject(test3);
        context.commitChanges();
    }

    public void testNoActionToMany() {
        DeleteRuleTest2 test2 = (DeleteRuleTest2) context
                .createAndRegisterNewObject("DeleteRuleTest2");
        DeleteRuleTest3 test3 = (DeleteRuleTest3) context
                .createAndRegisterNewObject("DeleteRuleTest3");
        test3.setToDeleteRuleTest2(test2);
        context.commitChanges();

        // must go on without exceptions...
        context.deleteObject(test2);

        // don't commit, since this will cause a constraint exception
    }

    public void testNoActionFlattened() {
        // temporarily set delete rule to NOACTION...
        int oldRule = changeDeleteRule(DeleteRule.NO_ACTION);

        try {
            DeleteRuleFlatA a = (DeleteRuleFlatA) context
                    .createAndRegisterNewObject(DeleteRuleFlatA.class);
            DeleteRuleFlatB b = (DeleteRuleFlatB) context
                    .createAndRegisterNewObject(DeleteRuleFlatB.class);

            a.addToFlatB(b);
            context.commitChanges();

            // must go on without exceptions...
            context.deleteObject(a);

            // assert that join is deleted
            assertJoinDeleted(a, b);
            assertEquals(PersistenceState.DELETED, a.getPersistenceState());
            assertEquals(PersistenceState.COMMITTED, b.getPersistenceState());
            assertTrue(b.getUntitledRel().contains(a));
            context.commitChanges();
            
        }
        finally {
            changeDeleteRule(oldRule);
        }
    }

    public void testNoActionFlattenedNoReverse() {
        // temporarily set delete rule to NOACTION...
        int oldRule = changeDeleteRule(DeleteRule.NO_ACTION);
        ObjRelationship reverse = unsetReverse();

        try {
            DeleteRuleFlatA a = (DeleteRuleFlatA) context
                    .createAndRegisterNewObject(DeleteRuleFlatA.class);
            DeleteRuleFlatB b = (DeleteRuleFlatB) context
                    .createAndRegisterNewObject(DeleteRuleFlatB.class);

            a.addToFlatB(b);
            context.commitChanges();

            // must go on without exceptions...
            context.deleteObject(a);

            // assert that join is deleted
            assertJoinDeleted(a, b);
            assertEquals(PersistenceState.DELETED, a.getPersistenceState());
            assertEquals(PersistenceState.COMMITTED, b.getPersistenceState());
            context.commitChanges();
        }
        finally {
            changeDeleteRule(oldRule);
            restoreReverse(reverse);
        }
    }

    public void testCascadeFlattened() {
        // temporarily set delete rule to CASCADE...
        int oldRule = changeDeleteRule(DeleteRule.CASCADE);

        try {
            DeleteRuleFlatA a = (DeleteRuleFlatA) context
                    .createAndRegisterNewObject(DeleteRuleFlatA.class);
            DeleteRuleFlatB b = (DeleteRuleFlatB) context
                    .createAndRegisterNewObject(DeleteRuleFlatB.class);
            a.addToFlatB(b);
            context.commitChanges();

            // must go on without exceptions...
            context.deleteObject(a);

            // assert that join is deleted
            assertJoinDeleted(a, b);
            context.commitChanges();
            
            assertEquals(PersistenceState.TRANSIENT, a.getPersistenceState());
            assertEquals(PersistenceState.TRANSIENT, b.getPersistenceState());
        }
        finally {
            changeDeleteRule(oldRule);
        }
    }
    
    public void testCascadeFlattenedNoReverse() {
        // temporarily set delete rule to CASCADE...
        int oldRule = changeDeleteRule(DeleteRule.CASCADE);
        ObjRelationship reverse = unsetReverse();

        try {
            DeleteRuleFlatA a = (DeleteRuleFlatA) context
                    .createAndRegisterNewObject(DeleteRuleFlatA.class);
            DeleteRuleFlatB b = (DeleteRuleFlatB) context
                    .createAndRegisterNewObject(DeleteRuleFlatB.class);
            a.addToFlatB(b);
            context.commitChanges();

            // must go on without exceptions...
            context.deleteObject(a);

            // assert that join is deleted
            assertJoinDeleted(a, b);
            context.commitChanges();
            assertEquals(PersistenceState.TRANSIENT, a.getPersistenceState());
            assertEquals(PersistenceState.TRANSIENT, b.getPersistenceState());
        }
        finally {
            changeDeleteRule(oldRule);
            restoreReverse(reverse);
        }
    }

    public void testNullifyFlattened() {
        // temporarily set delete rule to NULLIFY...
        int oldRule = changeDeleteRule(DeleteRule.NULLIFY);

        try {
            DeleteRuleFlatA a = (DeleteRuleFlatA) context
                    .createAndRegisterNewObject(DeleteRuleFlatA.class);
            DeleteRuleFlatB b = (DeleteRuleFlatB) context
                    .createAndRegisterNewObject(DeleteRuleFlatB.class);
            a.addToFlatB(b);
            context.commitChanges();

            // must go on without exceptions...
            context.deleteObject(a);

            // assert that join is deleted
            assertJoinDeleted(a, b);
            assertEquals(PersistenceState.DELETED, a.getPersistenceState());
            assertEquals(PersistenceState.MODIFIED, b.getPersistenceState());
            assertFalse(b.getUntitledRel().contains(a));
            context.commitChanges();
        }
        finally {
            changeDeleteRule(oldRule);
        }
    }
    
    public void testNullifyFlattenedNoReverse() {
        // temporarily set delete rule to NULLIFY...
        int oldRule = changeDeleteRule(DeleteRule.NULLIFY);
        ObjRelationship reverse = unsetReverse();

        try {
            DeleteRuleFlatA a = (DeleteRuleFlatA) context
                    .createAndRegisterNewObject(DeleteRuleFlatA.class);
            DeleteRuleFlatB b = (DeleteRuleFlatB) context
                    .createAndRegisterNewObject(DeleteRuleFlatB.class);
            a.addToFlatB(b);
            context.commitChanges();

            // must go on without exceptions...
            context.deleteObject(a);

            // assert that join is deleted
            assertJoinDeleted(a, b);
            assertEquals(PersistenceState.DELETED, a.getPersistenceState());
            assertEquals(PersistenceState.COMMITTED, b.getPersistenceState());
            context.commitChanges();
        }
        finally {
            changeDeleteRule(oldRule);
            restoreReverse(reverse);
        }
    }

    public void testDenyFlattened() {
        // temporarily set delete rule to DENY...
        int oldRule = changeDeleteRule(DeleteRule.DENY);

        try {
            DeleteRuleFlatA a = (DeleteRuleFlatA) context
                    .createAndRegisterNewObject(DeleteRuleFlatA.class);
            DeleteRuleFlatB b = (DeleteRuleFlatB) context
                    .createAndRegisterNewObject(DeleteRuleFlatB.class);
            a.addToFlatB(b);
            context.commitChanges();

            try {
                context.deleteObject(a);
                fail("Must have thrown a deny exception..");
            }
            catch (DeleteDenyException ex) {
                // expected... but check further
                assertJoinNotDeleted(a, b);
            }
        }
        finally {
            changeDeleteRule(oldRule);
        }
    }
    
    public void testDenyFlattenedNoReverse() {
        // temporarily set delete rule to DENY...
        int oldRule = changeDeleteRule(DeleteRule.DENY);
        ObjRelationship reverse = unsetReverse();

        try {
            DeleteRuleFlatA a = (DeleteRuleFlatA) context
                    .createAndRegisterNewObject(DeleteRuleFlatA.class);
            DeleteRuleFlatB b = (DeleteRuleFlatB) context
                    .createAndRegisterNewObject(DeleteRuleFlatB.class);
            a.addToFlatB(b);
            context.commitChanges();

            try {
                context.deleteObject(a);
                fail("Must have thrown a deny exception..");
            }
            catch (DeleteDenyException ex) {
                // expected... but check further
                assertJoinNotDeleted(a, b);
            }
        }
        finally {
            changeDeleteRule(oldRule);
            restoreReverse(reverse);
        }
    }

    private int changeDeleteRule(int deleteRule) {
        ObjEntity entity = context.getEntityResolver().lookupObjEntity(
                DeleteRuleFlatA.class);

        ObjRelationship relationship = (ObjRelationship) entity
                .getRelationship(DeleteRuleFlatA.FLAT_B_PROPERTY);
        int oldRule = relationship.getDeleteRule();
        relationship.setDeleteRule(deleteRule);
        return oldRule;
    }

    private ObjRelationship unsetReverse() {
        ObjEntity entity = context.getEntityResolver().lookupObjEntity(
                DeleteRuleFlatA.class);

        ObjRelationship relationship = (ObjRelationship) entity
                .getRelationship(DeleteRuleFlatA.FLAT_B_PROPERTY);
        ObjRelationship reverse = relationship.getReverseRelationship();

        if (reverse != null) {
            reverse.getSourceEntity().removeRelationship(reverse.getName());
        }

        return reverse;
    }

    private void restoreReverse(ObjRelationship reverse) {
        ObjEntity entity = context.getEntityResolver().lookupObjEntity(
                DeleteRuleFlatA.class);

        ObjRelationship relationship = (ObjRelationship) entity
                .getRelationship(DeleteRuleFlatA.FLAT_B_PROPERTY);
        relationship.getTargetEntity().addRelationship(reverse);
    }

    private void assertJoinDeleted(DeleteRuleFlatA a, DeleteRuleFlatB b) {
        ObjEntity entity = context.getEntityResolver().lookupObjEntity(
                DeleteRuleFlatA.class);
        ObjRelationship relationship = (ObjRelationship) entity
                .getRelationship(DeleteRuleFlatA.FLAT_B_PROPERTY);

        FlattenedRelationshipUpdate info = new FlattenedRelationshipUpdate(a, b, relationship);
        assertTrue("Join was not deleted for flattened relationship", context
                .getObjectStore()
                .getFlattenedDeletes()
                .contains(info));
    }

    private void assertJoinNotDeleted(DeleteRuleFlatA a, DeleteRuleFlatB b) {
        ObjEntity entity = context.getEntityResolver().lookupObjEntity(
                DeleteRuleFlatA.class);
        ObjRelationship relationship = (ObjRelationship) entity
                .getRelationship(DeleteRuleFlatA.FLAT_B_PROPERTY);

        FlattenedRelationshipUpdate info = new FlattenedRelationshipUpdate(a, b, relationship);
        assertFalse("Join was deleted for flattened relationship", context
                .getObjectStore()
                .getFlattenedDeletes()
                .contains(info));
    }
}