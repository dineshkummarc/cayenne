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
package org.objectstyle.cayenne.project.validator;

import java.util.Arrays;
import java.util.Collection;
import java.util.StringTokenizer;

/**
 * Defines a set of rules for validating java and db mapping identifiers.
 * 
 * @author Andrei Adamchik
 * @since 1.1
 */
public class MappingNamesHelper {

    // TODO: used by StringUtils and ClassGenerationInfo... need to refactor..
    static final Collection RESERVED_JAVA_KEYWORDS = Arrays.asList(new Object[] {
            "abstract", "assert", "default", "if", "private", "this", "boolean", "do",
            "implements", "protected", "throw", "break", "double", "import", "public",
            "throws", "byte", "else", "instanceof", "return", "transient", "case",
            "extends", "int", "short", "try", "catch", "final", "interface", "static",
            "void", "char", "finally", "long", "strictfp", "volatile", "class", "float",
            "native", "super", "while", "const", "for", "new", "switch", "continue",
            "goto", "package", "synchronized"
    });

    public boolean isReservedJavaKeyword(String word)
    {
        return RESERVED_JAVA_KEYWORDS.contains(word);
    }
    
    // a property is considered invalid if there is a getter or a setter for it in
    // java.lang.Object or CayenneDataObject
    static final Collection INVALID_JAVA_PROPERTIES = Arrays.asList(new Object[] {
            "class", "committedSnapshot", "currentSnapshot", "dataContext", "objectId",
            "persistenceState", "snapshotVersion"
    });

    static final MappingNamesHelper sharedInstance = new MappingNamesHelper();

    /**
     * Returns shared instance of the validator.
     */
    public static MappingNamesHelper getInstance() {
        return sharedInstance;
    }

    /**
     * This is more of a sanity check than a real validation. As different DBs allow
     * different chars in identifiers, here we simply check for dots.
     */
    String invalidCharsInDbPathComponent(String dbPathComponent) {
        return (dbPathComponent.indexOf('.') >= 0) ? "." : null;
    }

    /**
     * Scans a name of ObjAttribute or ObjRelationship for invalid characters.
     */
    String invalidCharsInObjPathComponent(String objPathComponent) {
        String invalidChars = validateJavaIdentifier(objPathComponent, "");
        return (invalidChars.length() > 0) ? invalidChars : null;
    }

    String invalidCharsInJavaClassName(String javaClassName) {
        if (javaClassName == null) {
            return null;
        }

        String invalidChars = "";

        StringTokenizer toks = new StringTokenizer(javaClassName, ".");
        while (toks.hasMoreTokens()) {
            invalidChars = validateJavaIdentifier(toks.nextToken(), invalidChars);
        }

        return (invalidChars.length() > 0) ? invalidChars : null;
    }

    boolean invalidDataObjectClass(String dataObjectClassFQN) {
        if (dataObjectClassFQN == null) {
            return true;
        }

        StringTokenizer toks = new StringTokenizer(dataObjectClassFQN, ".");
        while (toks.hasMoreTokens()) {
            if (RESERVED_JAVA_KEYWORDS.contains(toks.nextToken())) {
                return true;
            }
        }

        return false;
    }

    private String validateJavaIdentifier(String id, String invalidChars) {
        // TODO: Java spec seems to allow "$" char in identifiers... Cayenne expressions do
        // not, so we should probably check for this char presence...

        int len = (id != null) ? id.length() : 0;
        if (len == 0) {
            return invalidChars;
        }

        if (!Character.isJavaIdentifierStart(id.charAt(0))) {
            if (invalidChars.indexOf(id.charAt(0)) < 0) {
                invalidChars = invalidChars + id.charAt(0);
            }
        }

        for (int i = 1; i < len; i++) {

            if (!Character.isJavaIdentifierPart(id.charAt(i))) {
                if (invalidChars.indexOf(id.charAt(i)) < 0) {
                    invalidChars = invalidChars + id.charAt(i);
                }
            }
        }

        return invalidChars;
    }

    /**
     * Returns whether a given String is a valid DataObject property. A property is
     * considered invalid if there is a getter or a setter for it in java.lang.Object or
     * CayenneDataObject.
     */
    boolean invalidDataObjectProperty(String dataObjectProperty) {
        return dataObjectProperty == null
                || INVALID_JAVA_PROPERTIES.contains(dataObjectProperty);
    }
}