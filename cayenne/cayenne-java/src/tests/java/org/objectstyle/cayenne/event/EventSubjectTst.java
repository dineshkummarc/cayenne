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

package org.objectstyle.cayenne.event;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.objectstyle.cayenne.opp.hessian.HessianUtil;
import org.objectstyle.cayenne.util.Util;

public class EventSubjectTst extends TestCase {

    public void testIllegalArguments() {
        try {
            EventSubject.getSubject(null, "Subject");
            Assert.fail();
        }
        catch (IllegalArgumentException ex) {
            // OK
        }

        try {
            EventSubject.getSubject(Object.class, null);
            Assert.fail();
        }
        catch (IllegalArgumentException ex) {
            // OK
        }

        try {
            EventSubject.getSubject(Object.class, "");
            Assert.fail();
        }
        catch (IllegalArgumentException ex) {
            // OK
        }
    }

    public void testEqualityOfClonedSubjects() throws Exception {
        EventSubject s1 = EventSubject.getSubject(EventSubjectTst.class, "MySubject");
        EventSubject s2 = (EventSubject) Util.cloneViaSerialization(s1);

        assertNotSame(s1, s2);
        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());
    }

    public void testEqualityOfClonedSubjectsHessian() throws Exception {
        EventSubject s1 = EventSubject.getSubject(EventSubjectTst.class, "MySubject");
        EventSubject s2 = (EventSubject) HessianUtil
                .cloneViaHessianSerialization(s1);

        assertNotSame(s1, s2);
        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());
    }

    public void testIdenticalSubject() {
        EventSubject s1 = EventSubject.getSubject(EventSubjectTst.class, "MySubject");
        EventSubject s2 = EventSubject.getSubject(EventSubjectTst.class, "MySubject");
        Assert.assertSame(s1, s2);
    }

    public void testEqualityOfIdenticalSubjects() {
        EventSubject s1 = EventSubject.getSubject(EventSubjectTst.class, "MySubject");
        EventSubject s2 = EventSubject.getSubject(EventSubjectTst.class, "MySubject");
        Assert.assertEquals(s1, s2);
    }

    public void testEqualityOfSubjectsByDifferentOwner() {
        EventSubject s1 = EventSubject.getSubject(EventSubject.class, "MySubject");
        EventSubject s2 = EventSubject.getSubject(EventSubjectTst.class, "MySubject");
        Assert.assertFalse(s1.equals(s2));
    }

    public void testEqualityOfSubjectsByDifferentTopic() {
        EventSubject s1 = EventSubject.getSubject(EventSubjectTst.class, "Subject1");
        EventSubject s2 = EventSubject.getSubject(EventSubjectTst.class, "Subject2");
        Assert.assertFalse(s1.equals(s2));
    }

    public void testSubjectEqualsNull() {
        EventSubject s1 = EventSubject.getSubject(EventSubjectTst.class, "MySubject");
        Assert.assertFalse(s1.equals(null));
    }

    // TODO: (Andrus) This test can not be run reliably and in fact consistently
    // fails in some environments, since forcing GC at a certain time is not
    // guaranteed.
    /*
     * public void testSubjectGC() { EventSubject s =
     * EventSubject.getSubject(EventSubjectTst.class, "GCSubject"); long hash1 =
     * s.hashCode(); // try to make the subject go away s = null; System.gc();
     * System.gc(); s = EventSubject.getSubject(EventSubjectTst.class, "GCSubject"); long
     * hash2 = s.hashCode(); Assert.assertTrue(hash1 != hash2); }
     */

}