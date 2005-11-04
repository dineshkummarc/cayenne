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

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;

import org.apache.commons.collections.FastTreeMap;

/**
 * A <code>CayenneMap</code> is a specialized double-linked sorted map class. Attempts
 * to add objects using an already existing keys will result in IllegalArgumentExceptions.
 * Any added entries that implement CayenneMapEntry interface will have their parent set
 * to the parent of this map.
 * <p>
 * CayenneMap is not subclassed directly, but is rather used as an instance variable
 * within another class. Enclosing instance would set itself as a parent of this map.
 * </p>
 * 
 * @author Andrei Adamchik
 */
// WARNING: CayenneMap is not serializable via Hessian serialization mechanism used by
// CayenneConnector implementation.
// TODO: deprecate this ugly map. it is only used in Configuration
public class CayenneMap extends FastTreeMap {

    protected Object parent;

    /**
     * Constructor for CayenneMap.
     */
    public CayenneMap(Object parent) {
        this.parent = parent;
    }

    /**
     * Constructor for CayenneMap.
     * 
     * @param c
     */
    public CayenneMap(Object parent, Comparator c) {
        super(c);
        this.parent = parent;
    }

    /**
     * Constructor for CayenneMap.
     * 
     * @param m
     */
    public CayenneMap(Object parent, Map m) {
        // !IMPORTANT - set parent before populating the map
        this.parent = parent;
        putAll(m);
    }

    /**
     * Constructor for CayenneMap.
     * 
     * @param m
     */
    public CayenneMap(Object parent, SortedMap m) {
        // !IMPORTANT - set parent before populating the map
        this.parent = parent;
        putAll(m);
    }

    /**
     * Maps specified key-value pair. If value is a CayenneMapEntry, sets its parent to
     * this map.
     * 
     * @see java.util.Map#put(Object, Object)
     */
    public Object put(Object key, Object value) {

        if (containsKey(key) && get(key) != value) {
            // build descriptive failure message
            StringBuffer message = new StringBuffer();
            message.append("Attempt to insert duplicate key. [key '");
            message.append(key);
            message.append("'");

            if (parent instanceof CayenneMapEntry) {
                message
                        .append(", parent '")
                        .append(((CayenneMapEntry) parent).getName())
                        .append("'");
            }

            if (value instanceof CayenneMapEntry) {
                message
                        .append(", child '")
                        .append(((CayenneMapEntry) value).getName())
                        .append("'");
            }
            message.append("]");

            throw new IllegalArgumentException(message.toString());
        }

        if (value instanceof CayenneMapEntry) {
            ((CayenneMapEntry) value).setParent(parent);
        }

        super.put(key, value);
        return null;
    }

    /**
     * @see java.util.Map#putAll(Map)
     */
    public void putAll(Map t) {
        Iterator it = t.entrySet().iterator();
        while (it.hasNext()) {
        	Map.Entry entry = (Map.Entry) it.next();
            put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Returns the parent.
     * 
     * @return Object
     */
    public Object getParent() {
        return parent;
    }

    public void setParent(Object mapParent) {
        this.parent = mapParent;
    }
}