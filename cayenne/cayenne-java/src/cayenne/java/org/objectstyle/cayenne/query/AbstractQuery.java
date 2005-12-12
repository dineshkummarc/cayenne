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

import java.io.IOException;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.Procedure;

/**
 * Superclass of Cayenne queries.
 * 
 * @author Andrei Adamchik
 */
public abstract class AbstractQuery implements Query {

    /**
     * The root object this query. May be an entity name, Java class, ObjEntity or
     * DbEntity, depending on the specific query and how it was constructed.
     */
    protected Object root;
    protected String name;

    private void readObject(java.io.ObjectInputStream in) throws IOException,
            ClassNotFoundException {

        // addressing the fact that logLevel is not serializable

        in.defaultReadObject();
    }

    /**
     * Returns a symbolic name of the query.
     * 
     * @since 1.1
     */
    public String getName() {
        return name;
    }

    /**
     * Sets a symbolic name of the query.
     * 
     * @since 1.1
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the root of this query
     * 
     * @deprecated since 1.2, as corresponding interface method is also deprecated.
     */
    public Object getRoot() {
        return root;
    }

    /**
     * Returns a "root" ivar value.
     * 
     * @since 1.2
     */
    public Object getRoot(EntityResolver resolver) {
        return root;
    }

    /**
     * Sets the root of the query
     * 
     * @param value The new root
     * @throws IllegalArgumentException if value is not a String, ObjEntity, DbEntity,
     *             Procedure, DataMap, Class or null.
     */
    public void setRoot(Object value) {
        if (value == null) {
            this.root = null;
        }

        // sanity check
        if (!((value instanceof String)
                || (value instanceof ObjEntity)
                || (value instanceof DbEntity)
                || (value instanceof Class)
                || (value instanceof Procedure) || (value instanceof DataMap))) {

            String rootClass = (value != null) ? value.getClass().getName() : "null";

            throw new IllegalArgumentException(
                    getClass().getName()
                            + ": \"setRoot(..)\" takes a DataMap, String, ObjEntity, DbEntity, Procedure, "
                            + "or Class. It was passed a "
                            + rootClass);
        }

        this.root = value;
    }

    public String toString() {
        return new ToStringBuilder(this)
                .append("root", root)
                .append("name", getName())
                .toString();
    }

    /**
     * @since 1.2
     */
    public abstract SQLAction createSQLAction(SQLActionVisitor visitor);

    /**
     * Implements default routing mechanism relying on the EntityResolver to find DataMap
     * based on the query root. This mechanism should be sufficient for most queries that
     * "know" their root.
     * 
     * @since 1.2
     */
    public void route(QueryRouter router, EntityResolver resolver, Query substitutedQuery) {
        DataMap map = resolver.lookupDataMap(this);

        if (map == null) {
            throw new CayenneRuntimeException("No DataMap found, can't route query "
                    + this);
        }

        router.route(router.engineForDataMap(map), this, substitutedQuery);
    }
}
