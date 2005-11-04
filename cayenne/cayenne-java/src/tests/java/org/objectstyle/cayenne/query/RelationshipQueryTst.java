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
package org.objectstyle.cayenne.query;

import junit.framework.TestCase;

import org.objectstyle.cayenne.GlobalID;
import org.objectstyle.cayenne.MockDataObject;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.opp.hessian.HessianUtil;
import org.objectstyle.cayenne.util.Util;

/**
 * @author Andrus Adamchik
 */
public class RelationshipQueryTst extends TestCase {

    public void testConstructorObjectId() {

        ObjectId oid = new ObjectId(MockDataObject.class, "a", "b");
        RelationshipQuery query = new RelationshipQuery(oid, "relX");
        assertSame(oid, query.getObjectID());
        assertNull(query.getGlobalID());
        assertSame("relX", query.getRelationshipName());
    }

    public void testConstructorGlobalId() {

        GlobalID oid = new GlobalID("test", "a", "b");
        RelationshipQuery query = new RelationshipQuery(oid, "relX");
        assertSame(oid, query.getGlobalID());
        assertNull(query.getObjectID());
        assertSame("relX", query.getRelationshipName());
    }

    public void testSerializability() throws Exception {
        GlobalID oid = new GlobalID("test", "a", "b");
        RelationshipQuery query = new RelationshipQuery(oid, "relX");

        RelationshipQuery q1 = (RelationshipQuery) Util.cloneViaSerialization(query);
        assertNotNull(q1);
        assertEquals(oid, q1.getGlobalID());
        assertEquals("relX", q1.getRelationshipName());
    }

    public void testSerializabilityWithHessian() throws Exception {
        GlobalID oid = new GlobalID("test", "a", "b");
        RelationshipQuery query = new RelationshipQuery(oid, "relX");

        RelationshipQuery q1 = (RelationshipQuery) HessianUtil
                .cloneViaHessianSerialization(query);
        assertNotNull(q1);
        assertEquals(oid, q1.getGlobalID());
        assertEquals("relX", q1.getRelationshipName());
    }
}