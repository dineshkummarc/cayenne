/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
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
package org.objectstyle.cayenne.access;

import java.util.Iterator;
import java.util.List;

import org.objectstyle.art.Artist;
import org.objectstyle.art.Painting;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.unit.CayenneTestCase;

/**
 * Tests joint prefetch handling by Cayenne access stack.
 * 
 * @author Andrei Adamchik
 */
public class JointPrefetchTst extends CayenneTestCase {

    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }

    public void testJointPrefetchDataRows() throws Exception {
        createTestData("testJointPrefetch1");

        // query with to-many joint prefetches
        SelectQuery q = new SelectQuery(Painting.class);
        q.addOrdering("db:PAINTING_ID", true);
        q.setFetchingDataRows(true);
        q.addJointPrefetch(Painting.TO_ARTIST_PROPERTY);

        DataContext context = createDataContext();

        List rows = context.performQuery(q);
        assertEquals(3, rows.size());

        // row should contain columns from both entities minus those duplicated in a
        // join...
        int rowWidth = getDbEntity("ARTIST").getAttributes().size()
                + getDbEntity("PAINTING").getAttributes().size()
                - 1;
        Iterator it = rows.iterator();
        while (it.hasNext()) {
            DataRow row = (DataRow) it.next();
            assertEquals(rowWidth, row.size());
            
            // assert columns presence
            assertTrue(row + "", row.containsKey("PAINTING_ID"));
            assertTrue(row + "", row.containsKey("ARTIST_ID"));
            assertTrue(row + "", row.containsKey("GALLERY_ID"));
            assertTrue(row + "", row.containsKey("PAINTING_TITLE"));
            assertTrue(row + "", row.containsKey("ESTIMATED_PRICE"));
            assertTrue(row + "", row.containsKey("toArtist.ARTIST_NAME"));
            assertTrue(row + "", row.containsKey("toArtist.DATE_OF_BIRTH"));
        }
    }

    public void testJointPrefetchToOne() throws Exception {
        createTestData("testJointPrefetch1");

        // query with to-many joint prefetches
        SelectQuery q = new SelectQuery(Painting.class);
        q.addOrdering("db:PAINTING_ID", true);
        q.addJointPrefetch(Painting.TO_ARTIST_PROPERTY);

        DataContext context = createDataContext();

        List objects = context.performQuery(q);
        assertEquals(3, objects.size());

        Iterator it = objects.iterator();
        while (it.hasNext()) {
            Painting p = (Painting) it.next();
            Artist target = p.getToArtist();
            assertNotNull(target);
            assertEquals(PersistenceState.COMMITTED, target.getPersistenceState());
        }
    }

}