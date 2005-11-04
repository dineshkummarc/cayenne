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
package org.objectstyle.cayenne.wocompat.parser;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.unit.CayenneTestCase;

/**
 * @author Andrei Adamchik
 */
public class PropertyListParserTst extends CayenneTestCase {

    private static Parser parser(String plistText) {
        return new Parser(new StringReader(plistText));
    }

    public void testListPlist() throws Exception {
        List list = new ArrayList();
        list.add("str");
        list.add(new Integer(5));

        Object plist = parser("(str, 5)").object();
        assertTrue(list.equals(plist));
    }

    public void testMapPlist() throws Exception {
        Map map = new HashMap();
        map.put("key1", "val");
        map.put("key2", new Integer(5));

        Object plist = parser("{key1 = val; key2 = 5}").object();
        assertTrue(map.equals(plist));
    }

    public void testStringWithQuotes() throws Exception {
        List list = new ArrayList();
        list.add("s\"tr");
        list.add(new Integer(5));

        Object plist = parser("(\"s\\\"tr\", 5)").object();
        assertTrue(list.equals(plist));
    }

    public void testNestedPlist() throws Exception {
        Map map = new HashMap();
        map.put("key1", "val");
        map.put("key2", new Integer(5));

        List list = new ArrayList();
        list.add("str");
        list.add(new Integer(5));
        map.put("key3", list);

        assertEquals(map, parser("{key1 = val; key2 = 5; key3 = (str, 5)}").object());
    }

    public void testStringWithSpaces() throws Exception {
        List list = new ArrayList();
        list.add("s tr");
        list.add(new Integer(5));

        Object plist = parser("(\"s tr\", 5)").object();
        assertTrue(list.equals(plist));
    }

    public void testStringWithBraces() throws Exception {
        List list = new ArrayList();
        list.add("s{t)r");
        list.add(new Integer(5));

        assertEquals(list, parser("(\"s{t)r\", 5)").object());
    }

    public void testStringWithSlashes() throws Exception {
        List list = new ArrayList();
        list.add("s/t\\r");
        list.add(new Integer(5));

        assertEquals(list, parser("(\"s/t\\\\r\", 5)").object());
    }

    public void testMapWithLastSemicolon() throws Exception {
        Map map = new HashMap();
        map.put("key1", "val");
        map.put("key2", new Integer(5));

        // last semicolon is optional
        assertEquals(map, parser("{key1 = val; key2 = 5; }").object());
        assertEquals(map, parser("{key1 = val; key2 = 5 }").object());
    }

    public void testEmptyMap() throws Exception {
        assertEquals(Collections.EMPTY_MAP, parser("{}").object());
    }

    public void testEmptyList() throws Exception {
        assertEquals(Collections.EMPTY_LIST, parser("()").object());
    }

    public void testOutsideComments() throws Exception {
        List list = Collections.singletonList("str");
        assertEquals(list, parser("// comment\n ( str)").object());
    }

    public void testInsideComments() throws Exception {
        List list = Collections.singletonList("str");
        assertEquals(list, parser("(\n // comment\n str )").object());
    }

    public void testInsideKVComments() throws Exception {
        Map map = Collections.singletonMap("str", new Integer(5));
        assertEquals(map, parser("{\n str = // comment\n 5; }").object());
    }

    public void testTrailingComments() throws Exception {
        List list = Collections.singletonList("str");
        assertEquals(list, parser("(// comment\n str)").object());
    }

    public void testDoubleslashInsideLiteral() throws Exception {
        List list = Collections.singletonList("s//tr");
        assertEquals(list, parser("( \"s//tr\" )").object());
    }

    public void testWindowsComments() throws Exception {
        List list = Collections.singletonList("str");
        assertEquals(list, parser("// comment\r\n ( str)").object());
    }

    public void testMacComments() throws Exception {
        List list = Collections.singletonList("str");
        assertEquals(list, parser("// comment\r ( str)").object());
    }

    public void testUNIXComments() throws Exception {
        List list = Collections.singletonList("str");
        assertEquals(list, parser("// comment\n ( str)").object());
    }
}