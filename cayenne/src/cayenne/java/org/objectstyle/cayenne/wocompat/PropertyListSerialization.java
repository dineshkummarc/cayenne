/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002-2003 The ObjectStyle Group 
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
package org.objectstyle.cayenne.wocompat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.wocompat.parser.Parser;

/**
 * A <b>PropertyListSerialization</b> is a utility class 
 * that reads and stores files in NeXT/Apple
 * property list format. Unlike corresponding WebObjects 
 * class, <code>PropertyListSerialization</code> uses standard
 * Java collections (lists and maps) to store property lists.
 * 
 * @author Andrei Adamchik
 */
public class PropertyListSerialization {
	/**
	 * Reads a property list file. Returns a property list object, that is
	 * normally a java.util.List or a java.util.Map, but can also be a String
	 * or a Number. 
	 */
	public static Object propertyListFromFile(File f) throws FileNotFoundException {
		if (!f.isFile()) {
			throw new FileNotFoundException("No such file: " + f);
		}

		return new Parser(f).propertyList();
	}

	/**
	 * Reads a property list data from InputStream. Returns a property list o
	 * bject, that is normally a java.util.List or a java.util.Map, 
	 * but can also be a String or a Number. 
	 */
	public static Object propertyListFromStream(InputStream in) {
		return new Parser(in).propertyList();
	}

	/**
	 * Saves property list to file.
	 */
	public static void propertyListToFile(File f, Object plist) {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(f));
			try {
				writeObject("", out, plist);
			} finally {
				out.close();
			}
		} catch (IOException ioex) {
			throw new CayenneRuntimeException("Error saving plist.", ioex);
		}
	}

    /**
     * Internal method to recursively write a property list object.
     */
	protected static void writeObject(String offset, Writer out, Object plist)
		throws IOException {
		if (plist == null) {
			return;
		}

		if (plist instanceof List) {
			List list = (List) plist;

			out.write('\n');
			out.write(offset);

			if (list.size() == 0) {
				out.write("()");
				return;
			}

			out.write("(\n");

			String childOffset = offset + "   ";
			Iterator it = list.iterator();
			boolean appended = false;
			while (it.hasNext()) {
				// Java collections can contain nulls, skip them
				Object obj = it.next();
				if (obj != null) {
					if (appended) {
						out.write(", \n");
					}

					out.write(childOffset);
					writeObject(childOffset, out, obj);
					appended = true;
				}
			}

			out.write('\n');
			out.write(offset);
			out.write(')');
		} else if (plist instanceof Map) {
			Map map = (Map) plist;
			out.write('\n');
			out.write(offset);

			if (map.size() == 0) {
				out.write("{}");
				return;
			}

			out.write("{");

			String childOffset = offset + "    ";

			Iterator it = map.keySet().iterator();
			while (it.hasNext()) {
				// Java collections can contain nulls, skip them
				Object key = it.next();
				if (key == null) {
					continue;
				}
				Object obj = map.get(key);
				if (obj == null) {
					continue;
				}
				out.write('\n');
				out.write(childOffset);
				out.write(quoteString(key.toString()));
				out.write(" = ");
				writeObject(childOffset, out, obj);
				out.write(';');
			}

			out.write('\n');
			out.write(offset);
			out.write('}');
		} else if (plist instanceof String) {
			out.write(quoteString(plist.toString()));
		} else if (plist instanceof Number) {
			out.write(plist.toString());
		} else {
			throw new CayenneRuntimeException(
				"Unsupported class for property list serialization: "
					+ plist.getClass().getName());
		}
	}

	/** 
	 * Escapes all doublequotes and backslashes.
	 */
	protected static String escapeString(String str) {
		char[] chars = str.toCharArray();
		int len = chars.length;
		StringBuffer buf = new StringBuffer(len + 3);

		for (int i = 0; i < len; i++) {
			if (chars[i] == '\"' || chars[i] == '\\') {
				buf.append('\\');
			}			
			buf.append(chars[i]);
		}

		return buf.toString();
	}

    /**
     * Returns a quoted Stirng, with all the escapes preprocessed.
     * May return an unquoted String if it contains on special
     * characters, such as spaces, doublequotes and braces.
     */
	protected static String quoteString(String str) {
		boolean shouldQuote = false;
		
		// scan string for special chars, 
        // if we have them, string must be quoted
		String special = " \\\"{}();,-\'";
		char[] chars = str.toCharArray();
		int len = chars.length;
		for (int i = 0; i < len; i++) {
			if (special.indexOf(chars[i]) >= 0) {
				shouldQuote = true;
				break;
			}
		}

		str = escapeString(str);
		return (shouldQuote) ? '\"' + str + '\"' : str;
	}
}
