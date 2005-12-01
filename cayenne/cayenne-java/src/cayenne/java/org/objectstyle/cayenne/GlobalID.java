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
package org.objectstyle.cayenne;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.objectstyle.cayenne.util.IDUtil;
import org.objectstyle.cayenne.util.Util;

/**
 * A portable global identifier for persistent objects. GlobalID can be temporary (used
 * for transient or new uncommitted objects) or permanent (used for objects that have been
 * already stored in DB). A temporary GlobalID stores object entity name and a
 * pseudo-unique binary key; permanent id stores a map of values from an external
 * persistent store (aka "primary key").
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class GlobalID implements Serializable {

    protected String entityName;
    protected Map idMap;

    protected byte[] key;

    protected Map replacementIdMap;

    // hash code is transient to make sure id is portable across VM
    transient int hashCode;

    // exists for deserialization with Hessian and similar
    private GlobalID() {
    }

    /**
     * Creates a TEMPORARY GlobalID. Assignes a generated unique key.
     */
    public GlobalID(String entityName) {
        this.entityName = entityName;
        this.key = IDUtil.pseudoUniqueByteSequence16();
    }

    /**
     * Creates a TEMPORARY id with a specified entity name and a binary key. It is a
     * caller responsibility to provide a globally unique binary key.
     */
    public GlobalID(String entityName, byte[] key) {
        this.entityName = entityName;
        this.key = key;
    }

    /**
     * Creates a portable permanent GlobalID.
     */
    public GlobalID(String entityName, String key, Object value) {
        this.entityName = entityName;

        // don't use Collections.singletonMap() as we need a mutable single level map
        // internally (for things like Hessian serialization).
        this.idMap = new HashMap(1);
        idMap.put(key, value);
    }

    /**
     * Creates a portable permanent GlobalID.
     */
    public GlobalID(String entityName, Map idMap) {
        this.entityName = entityName;

        // we have to create a copy of the map, otherwise we may run into serialization
        // problems with hessian
        this.idMap = idMap != null && !idMap.isEmpty() ? new HashMap(idMap) : null;
    }

    public boolean isTemporary() {
        return key != null;
    }

    public String getEntityName() {
        return entityName;
    }

    public byte[] getKey() {
        return key;
    }

    /**
     * Returns an unmodifiable Map of id keys. For temporary ids returns an empty map.
     */
    public Map getIdMap() {
        if (isTemporary()) {
            return (replacementIdMap == null) ? Collections.EMPTY_MAP : Collections
                    .unmodifiableMap(replacementIdMap);
        }

        return idMap != null ? Collections.unmodifiableMap(idMap) : Collections.EMPTY_MAP;
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof GlobalID)) {
            return false;
        }

        GlobalID id = (GlobalID) object;

        if (isTemporary()) {
            return new EqualsBuilder().append(entityName, id.entityName).append(
                    key,
                    id.key).isEquals();
        }

        if (!Util.nullSafeEquals(entityName, id.entityName)) {
            return false;
        }

        if (id.idMap == null && idMap == null) {
            return true;
        }

        if (id.idMap == null || idMap == null) {
            return false;
        }

        if (id.idMap.size() != idMap.size()) {
            return false;
        }

        EqualsBuilder builder = new EqualsBuilder();
        Iterator entries = idMap.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry entry = (Map.Entry) entries.next();

            Object key = entry.getKey();
            Object value = entry.getValue();
            if (value == null) {
                if (id.idMap.get(key) != null || !id.idMap.containsKey(key)) {
                    return false;
                }
            }
            else {
                // takes care of comparing primitive arrays, such as byte[]
                builder.append(value, id.idMap.get(key));
                if (!builder.isEquals()) {
                    return false;
                }
            }
        }

        return true;
    }

    public int hashCode() {

        if (this.hashCode == 0) {

            HashCodeBuilder builder = new HashCodeBuilder(3, 5);
            builder.append(entityName.hashCode());

            if (key != null) {
                builder.append(key);
            }
            else if (idMap != null) {
                int len = idMap.size();

                // handle cheap and most common case - single key
                if (len == 1) {
                    Iterator entries = idMap.entrySet().iterator();
                    Map.Entry entry = (Map.Entry) entries.next();
                    builder.append(entry.getKey()).append(entry.getValue());
                }
                // handle multiple keys - must sort the keys to use with HashCodeBuilder
                else {
                    Object[] keys = idMap.keySet().toArray();
                    Arrays.sort(keys);

                    for (int i = 0; i < len; i++) {
                        // HashCodeBuilder will take care of processing object if it
                        // happens to be a primitive array such as byte[]

                        // also we don't have to append the key hashcode, its index will
                        // work
                        builder.append(i).append(idMap.get(keys[i]));
                    }
                }
            }

            this.hashCode = builder.toHashCode();
        }

        return hashCode;
    }

    /**
     * Returns a non-null mutable map that can be used to append replacement id values.
     * This allows to incrementally build a replacement GlobalID.
     */
    public Map getReplacementIdMap() {
        if (replacementIdMap == null) {
            replacementIdMap = new HashMap();
        }

        return replacementIdMap;
    }

    /**
     * Creates and returns a replacement ObjectId. No validation of ID is done.
     */
    public GlobalID createReplacementId() {
        return new GlobalID(getEntityName(), replacementIdMap);
    }

    /**
     * Returns true if there is full or partial replacement id attached to this id. This
     * method is preferrable to "!getReplacementIdMap().isEmpty()" as it avoids unneeded
     * replacement id map creation.
     */
    public boolean isReplacementIdAttached() {
        return replacementIdMap != null && !replacementIdMap.isEmpty();
    }

    /**
     * A standard toString method used for debugging.
     */
    public String toString() {

        ToStringBuilder builder = new ToStringBuilder(
                this,
                ToStringStyle.SHORT_PREFIX_STYLE);

        builder.append("entityName", entityName);
        builder.append("temporary", isTemporary());

        if (isTemporary()) {
            builder.append("key", key);
        }
        else if (idMap != null) {
            Iterator it = idMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                builder.append(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return builder.toString();
    }
}
