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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.QueryRouter;
import org.objectstyle.cayenne.query.SQLAction;
import org.objectstyle.cayenne.query.SQLActionVisitor;
import org.objectstyle.cayenne.query.SQLTemplate;

/**
 * Query utilities used in test cases.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
// TODO: move this to Cayenne distro at some point
public class ObjectContextQueryUtils {

    /**
     * Returns a count of DataObjects matching a qualifier.
     */
    public static int countObjects(
            ObjectContext context,
            Class persistentClass,
            Expression qualifier) {

        // TODO: implement
        return -1;
    }

    /**
     * Counts related objects in the database.
     */
    public static int relatedCount(Persistent object, String relationshipName) {
        // TODO: implement
        return -1;
    }

    /**
     * @param maxKeyPath a path to related entity attribute that is used for "max"
     *            aggregation.
     */
    public static int relatedMax(Persistent object, String maxKeyPath) {

        // TODO: implement
        return -1;
    }

    public static int relatedMin(Persistent object, String minKeyPath) {

        // TODO: implement
        return -1;
    }

    public static int relatedSum(Persistent object, String sumKeyPath) {

        // TODO: implement
        return -1;
    }

    public static int relatedAverage(Persistent object, String avgKeyPath) {

        // TODO: implement
        return -1;
    }

    /**
     * Executes a selecting query that is expected to return a single DataRow or a single
     * DataObject. If no results are found, null is returned to the caller, if result
     * consists of more than one object, CayenneRuntimeException is thrown.
     */
    public static Object singleObjectOrDataRow(ObjectContext context, Query query) {
        List results = context.performQuery(query);
        if (results.isEmpty()) {
            return null;
        }

        if (results.size() == 1) {
            return results.get(0);
        }

        throw new CayenneRuntimeException("Query "
                + query
                + " expected to return only one object, instead got "
                + results.size());
    }

    // TODO: andrus, 09/25/2005 - uncomment and fix once ObjectContext.getChannel() works
    // /**
    // * Returns a list of data rows for a given SQL string. As a DataContext can
    // * potentially map to more than one database, a second argument (dataMapName) is
    // used
    // * to determine which database to use. SQL string can use velocity syntax understood
    // * by SQLTemplate.
    // */
    // public static List dataRowsWithSQL(
    // HierarchicalObjectContext context,
    // String dataMapName,
    // String sql) {
    // return dataRowsWithSQL(context, dataMapName, sql, null, null);
    // }
    //
    // /**
    // * Same as {@link #dataRowsWithSQL(DataContext, String, String)}, however allows
    // * template parameters.
    // */
    // public static List dataRowsWithSQL(
    // HierarchicalObjectContext context,
    // String dataMapName,
    // String sql,
    // String[] parameterNames,
    // Object[] parameterValues) {
    //
    // Map parameters = (parameterNames != null)
    // ? toMap(parameterNames, parameterValues)
    // : Collections.EMPTY_MAP;
    //
    // Query query = new SQLTemplateSelectWrapper(dataMapName, sql, parameters, true);
    // QueryResult result = new QueryResult();
    // context.getParentContext().performQuery(
    // query,
    // result,
    // Transaction.internalTransaction(null));
    // return result.getFirstRows(query);
    // }

    /**
     * Returns a list of DataObjects for a given SQL string. SQL string can use velocity
     * syntax understood by SQLTemplate.
     */
    public static List dataObjectsWithSQL(
            ObjectContext context,
            Class persistentClass,
            String sql) {
        return dataObjectsWithSQL(context, persistentClass, sql, null, null);
    }

    /**
     * Same as {@link #dataObjectsWithSQL(ObjectContext, Class, String)}, however allows
     * template parameters.
     */
    public static List dataObjectsWithSQL(
            ObjectContext context,
            Class persistentClass,
            String sql,
            String[] parameterNames,
            Object[] parameterValues) {

        SQLTemplate query = new SQLTemplate(persistentClass, sql, true);

        if (parameterNames != null) {
            query.setParameters(toMap(parameterNames, parameterValues));
        }
        query.setFetchingDataRows(false);
        return context.performQuery(query);
    }

    private static Map toMap(String[] keys, Object[] values) {

        if (keys.length != values.length) {
            throw new CayenneRuntimeException(
                    "keys and values arrays have different sizes.");
        }

        Map map = new HashMap(keys.length, 1.0f);

        for (int i = 0; i < keys.length; i++) {
            map.put(keys[i], values[i]);
        }

        return map;
    }

    /**
     * Not for instantiation.
     */
    private ObjectContextQueryUtils() {
        // noop
    }

    /**
     * A SQLTemplate decorator that allows DataMap name as root.
     */
    static class SQLTemplateSelectWrapper implements Query {

        String dataMapName;
        String sql;
        Map parameters;
        boolean dataRows;

        SQLTemplateSelectWrapper(String dataMapName, String sql, Map parameters,
                boolean dataRows) {
            this.dataMapName = dataMapName;
            this.sql = sql;
            this.parameters = parameters;
            this.dataRows = dataRows;
        }

        public Query resolve(EntityResolver resolver) {
            return this;
        }

        public void route(QueryRouter router, EntityResolver resolver) {

            // build a substitute query and route it to appropriate node
            DataMap rootMap = resolver.getDataMap(dataMapName);
            SQLTemplate substituteQuery = new SQLTemplate(rootMap, sql, true);
            substituteQuery.setParameters(parameters);
            substituteQuery.setFetchingDataRows(dataRows);

            substituteQuery.route(router, resolver);
        }

        public SQLAction createSQLAction(SQLActionVisitor visitor) {
            throw new CayenneRuntimeException(
                    "Not intended for execution. Should've been replaced by a SQLTemaplte during routing.");
        }

        /**
         * @deprecated since 1.2
         */
        public Level getLoggingLevel() {
            throw new CayenneRuntimeException("getLoggingLevel is not implemented");
        }

        public String getName() {
            throw new CayenneRuntimeException("getName is not implemented");
        }

        public Object getRoot() {
            throw new CayenneRuntimeException("getRoot is obsolete and is not supported");
        }

        /**
         * @deprecated since 1.2
         */
        public void setLoggingLevel(Level level) {
            throw new CayenneRuntimeException("setLoggingLevel is not implemented");
        }

        public void setName(String name) {
            throw new CayenneRuntimeException("setName is not implemented");
        }

        public void setRoot(Object root) {
            throw new CayenneRuntimeException("setRoot is obsolete and is not supported");
        }
    }
}
