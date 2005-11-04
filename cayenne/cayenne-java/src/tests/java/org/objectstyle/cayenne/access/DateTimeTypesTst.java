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

package org.objectstyle.cayenne.access;

import java.util.Calendar;
import java.util.Date;

import org.objectstyle.art.DateTest;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.unit.CayenneTestCase;

/**
 * Tests Date handling in Cayenne.
 * 
 * @author Andrei Adamchik
 */
public class DateTimeTypesTst extends CayenneTestCase {

    protected DataContext context;

    protected void setUp() throws Exception {
        super.setUp();

        deleteTestData();
        context = createDataContext();
    }

    public void testDate() throws Exception {
        DateTest test = (DateTest) context.createAndRegisterNewObject("DateTest");

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2002, 1, 1);
        Date nowDate = cal.getTime();
        test.setDateColumn(nowDate);
        context.commitChanges();

        SelectQuery q = new SelectQuery(DateTest.class);
        DateTest testRead = (DateTest) context.performQuery(q).get(0);
        assertNotNull(testRead.getDateColumn());
        assertEquals(nowDate, testRead.getDateColumn());
    }

    public void testTime() throws Exception {
        DateTest test = (DateTest) context.createAndRegisterNewObject("DateTest");

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(1970, 0, 1, 1, 20, 30);
        Date nowTime = cal.getTime();
        test.setTimeColumn(nowTime);
        context.commitChanges();

        SelectQuery q = new SelectQuery(DateTest.class);
        DateTest testRead = (DateTest) context.performQuery(q).get(0);
        assertNotNull(testRead.getTimeColumn());

        // OpenBase fails to store seconds for the time
        // do an approximate match rounding to minutes

        assertTrue(
            Math.abs(nowTime.getTime() - testRead.getTimeColumn().getTime()) < 1000 * 60);
    }

    public void testTimestamp() throws Exception {
        DateTest test = (DateTest) context.createAndRegisterNewObject("DateTest");

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2003, 1, 1, 1, 20, 30);

        // most databases fail millisecond accuracy
        // cal.set(Calendar.MILLISECOND, 55);

        Date now = cal.getTime();
        test.setTimestampColumn(now);
        context.commitChanges();

        SelectQuery q = new SelectQuery(DateTest.class);
        DateTest testRead = (DateTest) context.performQuery(q).get(0);
        assertNotNull(testRead.getTimestampColumn());
        assertEquals(now, testRead.getTimestampColumn());
    }
}