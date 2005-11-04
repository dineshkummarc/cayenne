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
package org.objectstyle.cayenne.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.Factory;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;

/**
 * Batched INSERT query. Allows inserting multiple object snapshots (DataRows) for a given
 * DbEntity in a single query. InsertBatchQuery normally is not used directly. Rather
 * DataContext creates one internally when committing DataObjects.
 * 
 * @author Andriy Shapochka
 */
public class InsertBatchQuery extends BatchQuery {

    /**
     * @since 1.2
     */
    protected List objectIds;

    protected List objectSnapshots;
    protected List dbAttributes;

    /**
     * @since 1.2
     */
    protected int batchIndex;

    /**
     * Creates new InsertBatchQuery for a given DbEntity and estimated capacity.
     */
    public InsertBatchQuery(DbEntity entity, int batchCapacity) {
        super(entity);

        this.objectSnapshots = new ArrayList(batchCapacity);
        this.objectIds = new ArrayList(batchCapacity);
        this.dbAttributes = new ArrayList(getDbEntity().getAttributes());
        this.batchIndex = -1;
    }

    /**
     * Resets InsertBatchQuery so that a following call to "next" would start from the
     * beginning of the batch.
     */
    public void reset() {
        batchIndex = -1;
    }

    public boolean next() {
        batchIndex++;
        return size() > batchIndex;
    }

    public Object getValue(int dbAttributeIndex) {
        DbAttribute attribute = (DbAttribute) dbAttributes.get(dbAttributeIndex);
        Map currentSnapshot = (Map) objectSnapshots.get(batchIndex);
        Object value = currentSnapshot.get(attribute.getName());

        // if a value is a Factory, resolve it here...
        // slight chance that a normal value will implement Factory interface???
        if (value instanceof Factory) {
            value = ((Factory) value).create();

            // update replacement id
            if (attribute.isPrimaryKey()) {
                // sanity check
                if (value == null) {
                    String name = attribute.getEntity() != null ? attribute
                            .getEntity()
                            .getName() : "<null>";
                    throw new CayenneRuntimeException("Failed to generate PK: "
                            + name
                            + "."
                            + attribute.getName());
                }

                ObjectId id = getObjectId();
                if (id != null) {
                    // always override with fresh value as this is what's in the DB
                    id.getReplacementIdMap().put(attribute.getName(), value);
                }
            }

            // update snapshot
            currentSnapshot.put(attribute.getName(), value);
        }

        return value;
    }

    /**
     * Adds a snapshot to batch. A shortcut for "add(snapshot, null)".
     */
    public void add(Map snapshot) {
        add(snapshot, null);
    }

    /**
     * Adds a snapshot to batch. Optionally stores the object id for the snapshot. Note
     * that snapshot can hold either the real values or the instances of
     * org.apache.commons.collections.Factory that will be resolved to the actual value on
     * the spot, thus allowing deferred propagated keys resolution.
     * 
     * @since 1.2
     */
    public void add(Map snapshot, ObjectId id) {
        objectSnapshots.add(snapshot);
        objectIds.add(id);
    }

    public int size() {
        return objectSnapshots.size();
    }

    public List getDbAttributes() {
        return dbAttributes;
    }

    /**
     * Returns an ObjectId associated with the current batch iteration.
     * 
     * @since 1.2
     */
    public ObjectId getObjectId() {
        return (ObjectId) objectIds.get(batchIndex);
    }
}