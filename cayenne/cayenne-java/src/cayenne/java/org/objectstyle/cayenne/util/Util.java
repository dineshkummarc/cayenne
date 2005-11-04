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
package org.objectstyle.cayenne.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.objectstyle.cayenne.CayenneException;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Contains various unorganized static utility methods used across Cayenne.
 * 
 * @author Andrei Adamchik
 */
public class Util {

    /**
     * Reads file contents, returning it as a String, using System default line separator.
     */
    public static String stringFromFile(File file) throws IOException {
        return stringFromFile(file, System.getProperty("line.separator"));
    }

    /**
     * Reads file contents, returning it as a String, joining lines with provided
     * separator.
     */
    public static String stringFromFile(File file, String joinWith) throws IOException {
        StringBuffer buf = new StringBuffer();
        BufferedReader in = new BufferedReader(new FileReader(file));

        try {
            String line = null;
            while ((line = in.readLine()) != null) {
                buf.append(line).append(joinWith);
            }
        }
        finally {
            in.close();
        }
        return buf.toString();
    }

    /**
     * Copies file contents from source to destination. Makes up for the lack of file
     * copying utilities in Java
     */
    public static boolean copy(File source, File destination) {
        BufferedInputStream fin = null;
        BufferedOutputStream fout = null;
        try {
            int bufSize = 8 * 1024;
            fin = new BufferedInputStream(new FileInputStream(source), bufSize);
            fout = new BufferedOutputStream(new FileOutputStream(destination), bufSize);
            copyPipe(fin, fout, bufSize);
        }
        catch (IOException ioex) {
            return false;
        }
        catch (SecurityException sx) {
            return false;
        }
        finally {
            if (fin != null) {
                try {
                    fin.close();
                }
                catch (IOException cioex) {
                }
            }
            if (fout != null) {
                try {
                    fout.close();
                }
                catch (IOException cioex) {
                }
            }
        }
        return true;
    }

    /**
     * Save URL contents to a file.
     */
    public static boolean copy(URL from, File to) {
        BufferedInputStream urlin = null;
        BufferedOutputStream fout = null;
        try {
            int bufSize = 8 * 1024;
            urlin = new BufferedInputStream(
                    from.openConnection().getInputStream(),
                    bufSize);
            fout = new BufferedOutputStream(new FileOutputStream(to), bufSize);
            copyPipe(urlin, fout, bufSize);
        }
        catch (IOException ioex) {
            return false;
        }
        catch (SecurityException sx) {
            return false;
        }
        finally {
            if (urlin != null) {
                try {
                    urlin.close();
                }
                catch (IOException cioex) {
                }
            }
            if (fout != null) {
                try {
                    fout.close();
                }
                catch (IOException cioex) {
                }
            }
        }
        return true;
    }

    /**
     * Reads data from the input and writes it to the output, until the end of the input
     * stream.
     * 
     * @param in
     * @param out
     * @param bufSizeHint
     * @throws IOException
     */
    public static void copyPipe(InputStream in, OutputStream out, int bufSizeHint)
            throws IOException {
        int read = -1;
        byte[] buf = new byte[bufSizeHint];
        while ((read = in.read(buf, 0, bufSizeHint)) >= 0) {
            out.write(buf, 0, read);
        }
        out.flush();
    }

    /**
     * Deletes a file or directory, allowing recursive directory deletion. This is an
     * improved version of File.delete() method.
     */
    public static boolean delete(String filePath, boolean recursive) {
        File file = new File(filePath);
        if (!file.exists()) {
            return true;
        }

        if (!recursive || !file.isDirectory())
            return file.delete();

        String[] list = file.list();
        for (int i = 0; i < list.length; i++) {
            if (!delete(filePath + File.separator + list[i], true))
                return false;
        }

        return file.delete();
    }

    /**
     * Replaces all backslashes "\" with forward slashes "/". Convenience method to
     * convert path Strings to URI format.
     */
    public static String substBackslashes(String string) {
        return RegexUtil.substBackslashes(string);
    }

    /**
     * Looks up and returns the root cause of an exception. If none is found, returns
     * supplied Throwable object unchanged. If root is found, recursively "unwraps" it,
     * and returns the result to the user.
     */
    public static Throwable unwindException(Throwable th) {
        if (th instanceof CayenneException) {
            CayenneException e = (CayenneException) th;
            if (e.getCause() != null && e.getCause() != e) {
                return unwindException(e.getCause());
            }
        }
        else if (th instanceof CayenneRuntimeException) {
            CayenneRuntimeException e = (CayenneRuntimeException) th;
            if (e.getCause() != null && e.getCause() != e) {
                return unwindException(e.getCause());
            }
        }
        else if (th instanceof InvocationTargetException) {
            InvocationTargetException e = (InvocationTargetException) th;
            if (e.getTargetException() != null) {
                return unwindException(e.getTargetException());
            }
        }
        else if (th instanceof SAXException) {
            SAXException sax = (SAXException) th;
            if (sax.getException() != null) {
                return unwindException(sax.getException());
            }
        }
        else if (th instanceof SQLException) {
            SQLException sql = (SQLException) th;
            if (sql.getNextException() != null) {
                return unwindException(sql.getNextException());
            }
        }

        return th;
    }

    /**
     * Compares two objects similar to "Object.equals(Object)". Unlike Object.equals(..),
     * this method doesn't throw an exception if any of the two objects is null.
     */
    public static boolean nullSafeEquals(Object obj1, Object obj2) {
        if (obj1 == null && obj2 == null) {
            return true;
        }
        else if (obj1 != null) {
            // Arrays must be handled differently since equals() only does
            // an "==" for an array and ignores equivalence. If an array, use
            // the Jakarta Commons Language component EqualsBuilder to determine
            // the types contained in the array and do individual comparisons.
            if (obj1.getClass().isArray()) {
                EqualsBuilder builder = new EqualsBuilder();
                builder.append(obj1, obj2);
                return builder.isEquals();
            }
            else { // It is NOT an array, so use regular equals()
                return obj1.equals(obj2);
            }
        }
        else {
            return false;
        }
    }

    /**
     * Compares two objects similar to "Comparable.compareTo(Object)". Unlike
     * Comparable.compareTo(..), this method doesn't throw an exception if any of the two
     * objects is null.
     * 
     * @since 1.1
     */
    public static int nullSafeCompare(boolean nullsFirst, Comparable o1, Object o2) {
        if (o1 == null && o2 == null) {
            return 0;
        }
        else if (o1 == null) {
            return nullsFirst ? -1 : 1;
        }
        else if (o2 == null) {
            return nullsFirst ? 1 : -1;
        }
        else {
            return o1.compareTo(o2);
        }
    }

    /**
     * Returns true, if the String is null or an empty string.
     */
    public static boolean isEmptyString(String string) {
        return string == null || string.length() == 0;
    }

    /**
     * Creates Serializable object copy using serialization/deserialization.
     */
    public static Object cloneViaSerialization(Serializable obj) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bytes);
        out.writeObject(obj);
        out.close();

        byte[] data = bytes.toByteArray();

        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data));
        Object objCopy = in.readObject();
        in.close();
        return objCopy;
    }

    /**
     * Creates an XMLReader with default feature set. Note that all Cayenne internal XML
     * parsers should probably use XMLReader obtained via this method for consistency
     * sake, and can customize feature sets as needed.
     */
    public static XMLReader createXmlReader() throws SAXException,
            ParserConfigurationException {
        SAXParserFactory spf = SAXParserFactory.newInstance();

        // Create a JAXP SAXParser
        SAXParser saxParser = spf.newSAXParser();

        // Get the encapsulated SAX XMLReader
        XMLReader reader = saxParser.getXMLReader();

        // set default features
        reader.setFeature("http://xml.org/sax/features/namespaces", true);

        return reader;
    }

    /**
     * Returns package name for the Java class as a path separated with forward slash
     * ("/"). Method is used to lookup resources that are located in package
     * subdirectories. For example, a String "a/b/c" will be returned for class name
     * "a.b.c.ClassName".
     */
    public static String getPackagePath(String className) {
        return RegexUtil.getPackagePath(className);
    }

    /**
     * Creates a mutable map out of two arrays with keys and values.
     * 
     * @since 1.2
     */
    public static Map toMap(Object[] keys, Object[] values) {
        int keysSize = (keys != null) ? keys.length : 0;
        int valuesSize = (values != null) ? values.length : 0;

        if (keysSize == 0 && valuesSize == 0) {
            // return mutable map
            return new HashMap();
        }

        if (keysSize != valuesSize) {
            throw new IllegalArgumentException(
                    "The number of keys doesn't match the number of values.");
        }

        Map map = new HashMap();
        for (int i = 0; i < keysSize; i++) {
            map.put(keys[i], values[i]);
        }

        return map;
    }

    /**
     * Extracts extension from the file name. Dot is not included in the returned string.
     */
    public static String extractFileExtension(String fileName) {
        int dotInd = fileName.lastIndexOf('.');

        // if dot is in the first position,
        // we are dealing with a hidden file rather than an extension
        return (dotInd > 0 && dotInd < fileName.length()) ? fileName
                .substring(dotInd + 1) : null;
    }

    /**
     * Strips extension from the file name.
     */
    public static String stripFileExtension(String fileName) {
        int dotInd = fileName.lastIndexOf('.');

        // if dot is in the first position,
        // we are dealing with a hidden file rather than an extension
        return (dotInd > 0) ? fileName.substring(0, dotInd) : fileName;
    }

    /**
     * Strips "\n", "\r\n", "\r" from the argument string.
     * 
     * @since 1.2
     */
    public static String stripLineBreaks(String string, String replaceWith) {
        if (isEmptyString(string)) {
            return string;
        }

        int len = string.length();
        StringBuffer buffer = new StringBuffer(len);
        for (int i = 0; i < len; i++) {
            char c = string.charAt(i);

            // skip \n, \r, \r\n
            switch (c) {
                case '\n':
                case '\r': // do lookahead
                    if (i + 1 < len && string.charAt(i + 1) == '\n') {
                        i++;
                    }

                    buffer.append(replaceWith);
                    break;
                default:
                    buffer.append(c);
            }
        }

        return buffer.toString();
    }

    /**
     * Encodes a string so that it can be used as an attribute value in an XML document.
     * Will do conversion of the greater/less signs, quotes and ampersands.
     */
    public static String encodeXmlAttribute(String str) {
        if (str == null)
            return null;

        int len = str.length();
        if (len == 0)
            return str;

        StringBuffer encoded = new StringBuffer();
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
            if (c == '<')
                encoded.append("&lt;");
            else if (c == '\"')
                encoded.append("&quot;");
            else if (c == '>')
                encoded.append("&gt;");
            else if (c == '\'')
                encoded.append("&apos;");
            else if (c == '&')
                encoded.append("&amp;");
            else
                encoded.append(c);
        }

        return encoded.toString();
    }

    /**
     * Trims long strings substituting middle part with "...".
     * 
     * @param str String to trim.
     * @param maxLength maximum allowable length. Must be at least 5, or an
     *            IllegalArgumentException is thrown.
     * @return String
     */
    public static String prettyTrim(String str, int maxLength) {
        if (maxLength < 5) {
            throw new IllegalArgumentException(
                    "Algorithm for 'prettyTrim' works only with length >= 5. "
                            + "Supplied length is "
                            + maxLength);
        }

        if (str == null || str.length() <= maxLength) {
            return str;
        }

        // find a section to cut off
        int len = maxLength - 3;
        int startLen = len / 2;
        int endLen = len - startLen;

        return str.substring(0, startLen) + "..." + str.substring(str.length() - endLen);
    }

    /**
     * Returns a sorted iterator from an unsorted one. Use this method as a last resort,
     * since it is much less efficient then just sorting a collection that backs the
     * original iterator.
     */
    public static Iterator sortedIterator(Iterator it, Comparator comparator) {
        List list = new ArrayList();
        while (it.hasNext()) {
            list.add(it.next());
        }

        Collections.sort(list, comparator);
        return list.iterator();
    }

    /**
     * Builds a hashCode of Collection.
     */
    public static int hashCode(Collection c) {
        HashCodeBuilder builder = new HashCodeBuilder();
        for (Iterator i = c.iterator(); i.hasNext();)
            builder.append(i.next());
        return builder.toHashCode();
    }

    /**
     * Converts a SQL-style pattern to a valid Perl regular expression. E.g.:
     * <p>
     * <code>"billing_%"</code> will become <code>/^billing_.*$/</code>
     * <p>
     * <code>"user?"</code> will become <code>/^user.?$/</code>
     * 
     * @deprecated since 1.2 use "sqlPatternToPattern".
     */
    public static String sqlPatternToRegex(String pattern, boolean ignoreCase) {
        return RegexUtil.sqlPatternToRegex(pattern);
    }
    
    /**
     * @since 1.2
     */
    public static Pattern sqlPatternToPattern(String pattern, boolean ignoreCase) {
        String preprocessed = RegexUtil.sqlPatternToRegex(pattern);
        
        int flag = (ignoreCase) ? Pattern.CASE_INSENSITIVE : 0;
        return Pattern.compile(preprocessed, flag);
    }

    /**
     * Returns true if a Member is accessible via reflection under normal Java access
     * controls.
     * 
     * @since 1.2
     */
    public static boolean isAccessible(Member member) {
        return Modifier.isPublic(member.getModifiers())
                && Modifier.isPublic(member.getDeclaringClass().getModifiers());
    }

    /**
     * Creates a Java class, handling regular class names as well as single-dimensional
     * arrays and primitive types.
     * 
     * @since 1.2
     */
    public static Class getJavaClass(String className)
            throws ClassNotFoundException {

        // is there a better way to get array class from string name?

        if (className == null) {
            throw new ClassNotFoundException("Null class name");
        }
        
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        if (classLoader == null) {
            classLoader = Util.class.getClassLoader();
        }

        // use custom logic on failure only, assuming primitives and arrays are not that
        // common
        try {
            return Class.forName(className, true, classLoader);
        }
        catch (ClassNotFoundException e) {
            if (!className.endsWith("[]")) {
                if ("byte".equals(className)) {
                    return Byte.TYPE;
                }
                else if ("int".equals(className)) {
                    return Integer.TYPE;
                }
                else if ("short".equals(className)) {
                    return Short.TYPE;
                }
                else if ("char".equals(className)) {
                    return Character.TYPE;
                }
                else if ("double".equals(className)) {
                    return Double.TYPE;
                }
                else if ("float".equals(className)) {
                    return Float.TYPE;
                }
                else if ("boolean".equals(className)) {
                    return Boolean.TYPE;
                }

                throw e;
            }

            if (className.length() < 3) {
                throw new IllegalArgumentException("Invalid class name: " + className);
            }

            // TODO: support for multi-dim arrays
            className = className.substring(0, className.length() - 2);

            if ("byte".equals(className)) {
                return byte[].class;
            }
            else if ("int".equals(className)) {
                return int[].class;
            }
            else if ("short".equals(className)) {
                return short[].class;
            }
            else if ("char".equals(className)) {
                return char[].class;
            }
            else if ("double".equals(className)) {
                return double[].class;
            }
            else if ("float".equals(className)) {
                return float[].class;
            }
            else if ("boolean".equals(className)) {
                return boolean[].class;
            }

            return Class.forName("[L" + className + ";", true, classLoader);
        }
    }
}