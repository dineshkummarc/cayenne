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
package org.objectstyle.cayenne.xml;

import java.io.FileReader;
import java.io.Reader;
import java.util.List;

import junit.framework.TestCase;

import org.jdom.Document;
import org.jdom.Element;

/**
 * @author Andrei Adamchik
 */
public class XMLDecoderTst extends TestCase {

    static final String XML_DATA_DIR = "src/tests/resources/test-resources/xmlcoding/";
    protected XMLDecoder decoder;

    public void setUp() {
        decoder = new XMLDecoder();
    }

    public void testParse() throws Exception {
        Reader xml = new FileReader(XML_DATA_DIR + "parser-test.xml");
        Document document = decoder.parse(xml);

        assertNotNull(document);

        Element root = document.getRootElement();
        assertEquals("root", root.getName());

        List children = root.getChildren("some-element");
        assertEquals(2, children.size());
    }

    public void testDecode() throws Exception {
        Reader xml = new FileReader(XML_DATA_DIR + "encoded-object.xml");
        Object object = decoder.decode(xml);

        assertTrue(object instanceof TestObject);
        TestObject test = (TestObject) object;
        assertEquals("n1", test.getName());
        assertEquals(5, test.getAge());
        assertEquals(true, test.isOpen());
    }

    /**
     * Test decoding with a mapping file.
     * 
     * @throws Exception
     */
    public void testDecodeMapping() throws Exception {
        Reader xml = new FileReader(XML_DATA_DIR + "simple-mapped.xml");
        Object object = decoder.decode(xml, XML_DATA_DIR + "simple-mapping.xml");

        assertTrue(object instanceof TestObject);
        TestObject test = (TestObject) object;
        assertEquals("George", test.getName());
        assertEquals(57, test.getAge());
        assertEquals(false, test.isOpen());
    }

    public void testDecodeCollection() throws Exception {
        Reader xml = new FileReader(XML_DATA_DIR + "encoded-simple-collection.xml");
        Object object = decoder.decode(xml);

        assertTrue(object instanceof TestObject);
        TestObject test = (TestObject) object;
        assertEquals(2, test.getChildren().size());
    }

    public void testDecodeComplexCollection() throws Exception {
        Reader xml = new FileReader(XML_DATA_DIR + "encoded-complex-collection.xml");
        List testObjects = (List) decoder.decode(xml);

        assertEquals(4, testObjects.size());

        List embeddedList = (List) testObjects.get(3);

        TestObject obj1 = new TestObject();
        obj1.setName("George");
        obj1.addChild("Bill");
        obj1.addChild("Sue");

        TestObject obj2 = new TestObject();
        obj2.setAge(31);
        obj2.setOpen(false);
        obj2.addChild("Harry");

        assertEquals(obj1, testObjects.get(0));
        assertEquals("Random String", testObjects.get(1));
        assertEquals(obj2, testObjects.get(2));

        assertEquals(4, embeddedList.size());
        assertEquals("Testing", embeddedList.get(0));
        assertEquals("embedded", embeddedList.get(1));
        assertEquals("list", embeddedList.get(2));
    }

    // TODO Test decode with mapping file & data context.

    // TODO Test decode with data context.

    public void testDecodePrimitives() throws Exception {
        Reader xml = new FileReader(XML_DATA_DIR + "encoded-object-primitives.xml");
        Object object = decoder.decode(xml);

        assertTrue(object instanceof TestObject);
        TestObject test = (TestObject) object;
        assertEquals("n1", test.getName());
        assertEquals(5, test.getAge());
        assertEquals(true, test.isOpen());
    }
}