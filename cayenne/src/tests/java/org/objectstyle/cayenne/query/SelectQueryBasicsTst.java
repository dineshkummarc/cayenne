/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002 The ObjectStyle Group 
 * and individual authors of the software.  All rights reserved.
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
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:  
 *       "This product includes software developed by the 
 *        ObjectStyle Group (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "ObjectStyle Group" and "Cayenne" 
 *    must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written 
 *    permission, please contact andrus@objectstyle.org.
 *
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    nor may "ObjectStyle" appear in their names without prior written
 *    permission of the ObjectStyle Group.
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
 * individuals on behalf of the ObjectStyle Group.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 *
 */

package org.objectstyle.cayenne.query;

import org.objectstyle.cayenne.CayenneTestCase;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;

public class SelectQueryBasicsTst extends CayenneTestCase {
    protected SelectQuery q;

    public SelectQueryBasicsTst(String name) {
        super(name);
    }

    public void setUp() throws java.lang.Exception {
        q = new SelectQuery();
    }

    public void testPageSize() throws java.lang.Exception {
        q.setPageSize(10);
        assertEquals(10, q.getPageSize());
    }

    public void testFetchLimit1() throws Exception {
        q.setFetchLimit(5);
        assertEquals(5, q.getFetchLimit());
    }

    public void testAddOrdering1() throws Exception {
        Ordering ord = new Ordering();
        q.addOrdering(ord);
        assertEquals(1, q.getOrderingList().size());
        assertSame(ord, q.getOrderingList().get(0));
    }

    public void testAddPrefetching() throws Exception {
        String path = "a.b.c";
        q.addPrefetch(path);
        assertEquals(1, q.getPrefetchList().size());
        assertSame(path, q.getPrefetchList().get(0));
    }

    public void testAddOrdering2() throws Exception {
        String path = "a.b.c";
        q.addOrdering(path, Ordering.DESC);
        assertEquals(1, q.getOrderingList().size());

        Ordering ord = (Ordering) q.getOrderingList().get(0);
        assertEquals(path, ord.getSortSpec().getOperand(0));
        assertEquals(Ordering.DESC, ord.isAscending());
    }

    public void testDistinct() throws Exception {
        assertTrue(!q.isDistinct());
        q.setDistinct(true);
        assertTrue(q.isDistinct());
    }

    public void testFetchingDataRows1() {
        assertTrue(!q.isFetchingDataRows());
        q.setFetchingDataRows(true);
        assertTrue(q.isFetchingDataRows());
    }

    public void testFetchingDataRows2() {
        assertTrue(!q.isFetchingDataRows());
        q.addCustDbAttribute("ARTIST_ID");
        assertTrue(q.isFetchingDataRows());

        // this shouldn't have any effect, since custom attributes are fetched
        q.setFetchingDataRows(false);
        assertTrue(q.isFetchingDataRows());
    }

    public void testQueryAttributes() throws Exception {
        assertEquals(0, q.getCustDbAttributes().size());

        q.addCustDbAttribute("ARTIST_ID");
        assertEquals(1, q.getCustDbAttributes().size());
        assertEquals("ARTIST_ID", q.getCustDbAttributes().get(0));
    }

    public void testUsingRootEntityAttributes() throws Exception {
        assertTrue(!q.isFetchingCustAttributes());

        q.addCustDbAttribute("ARTIST_ID");
        assertTrue(q.isFetchingCustAttributes());
    }

    public void testSetParentQualifier() throws Exception {
        assertNull(q.getParentQualifier());

        Expression qual = ExpressionFactory.expressionOfType(Expression.AND);
        q.setParentQualifier(qual);
        assertNotNull(q.getParentQualifier());
        assertSame(qual, q.getParentQualifier());
    }

    public void testAndParentQualifier() throws Exception {
        assertNull(q.getParentQualifier());

        Expression e1 = ExpressionFactory.expressionOfType(Expression.EQUAL_TO);
        q.andParentQualifier(e1);
        assertSame(e1, q.getParentQualifier());

        Expression e2 = ExpressionFactory.expressionOfType(Expression.NOT_EQUAL_TO);
        q.andParentQualifier(e2);
        assertEquals(Expression.AND, q.getParentQualifier().getType());
    }

    public void testOrParentQualifier() throws Exception {
        assertNull(q.getParentQualifier());

        Expression e1 = ExpressionFactory.expressionOfType(Expression.EQUAL_TO);
        q.orParentQualifier(e1);
        assertSame(e1, q.getParentQualifier());

        Expression e2 = ExpressionFactory.expressionOfType(Expression.NOT_EQUAL_TO);
        q.orParentQualifier(e2);
        assertEquals(Expression.OR, q.getParentQualifier().getType());
    }

    public void testParentObjEntityName() throws Exception {
        assertNull(q.getParentObjEntityName());

        q.setParentObjEntityName("SomeEntity");
        assertSame("SomeEntity", q.getParentObjEntityName());
    }
}
