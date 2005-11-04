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
package org.objectstyle.cayenne.access.trans;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.jdbc.ColumnDescriptor;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbJoin;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.DerivedDbEntity;
import org.objectstyle.cayenne.map.EntityInheritanceTree;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.query.PrefetchSelectQuery;
import org.objectstyle.cayenne.query.SelectQuery;

/**
 * A builder of JDBC PreparedStatements based on Cayenne SelectQueries. Translates
 * SelectQuery to parameterized SQL string and wraps it in a PreparedStatement.
 * SelectTranslator is stateful and thread-unsafe.
 * 
 * @author Andrei Adamchik
 */
public class SelectTranslator extends QueryAssembler {

    protected static final int[] UNSUPPORTED_DISTINCT_TYPES = new int[] {
            Types.BLOB, Types.CLOB, Types.LONGVARBINARY, Types.LONGVARCHAR
    };

    protected static boolean isUnsupportedForDistinct(int type) {
        for (int i = 0; i < UNSUPPORTED_DISTINCT_TYPES.length; i++) {
            if (UNSUPPORTED_DISTINCT_TYPES[i] == type) {
                return true;
            }
        }

        return false;
    }

    final Map aliasLookup = new HashMap();

    final List tableList = new ArrayList();
    final List aliasList = new ArrayList();
    final List dbRelList = new ArrayList();

    List resultColumns;
    List groupByList;

    int aliasCounter;

    boolean suppressingDistinct;

    /**
     * If set to <code>true</code>, indicates that distinct select query is required no
     * matter what the original query settings where. This flag can be set when joins are
     * created using "to-many" relationships.
     */
    boolean forcingDistinct;

    /**
     * Returns a list of DbAttributes representing columns in this query.
     * 
     * @deprecated since 1.2 - contents of this list are now ColumnDescriptors, not
     *             DbAttributes. Anyway, use getResultColumns() instead to reduce
     *             confusion.
     */
    protected List getColumns() {
        return resultColumns;
    }

    /**
     * Returns query translated to SQL. This is a main work method of the
     * SelectTranslator.
     */
    public String createSqlString() throws Exception {
        forcingDistinct = false;

        // build column list
        this.resultColumns = buildResultColumns();

        QualifierTranslator tr = adapter.getQualifierTranslator(this);

        // build parent qualifier
        // Parent qualifier translation must PRECEED main qualifier
        // since it will be appended first and its parameters must
        // go first as well
        String parentQualifierStr = null;
        if (getSelectQuery().isQualifiedOnParent()) {
            tr.setTranslateParentQual(true);
            parentQualifierStr = tr.doTranslation();
        }

        // build main qualifier
        tr.setTranslateParentQual(false);
        String qualifierStr = tr.doTranslation();

        // build GROUP BY
        this.groupByList = buildGroupByList();

        // build ORDER BY
        OrderingTranslator orderingTranslator = new OrderingTranslator(this);
        String orderByStr = orderingTranslator.doTranslation();

        // assemble
        StringBuffer queryBuf = new StringBuffer();
        queryBuf.append("SELECT ");

        // check if DISTINCT is appropriate
        // side effect: "suppressingDistinct" flag may end up being flipped here
        if (forcingDistinct || getSelectQuery().isDistinct()) {

            suppressingDistinct = false;
            Iterator it = resultColumns.iterator();
            while (it.hasNext()) {
                ColumnDescriptor column = (ColumnDescriptor) it.next();
                if (isUnsupportedForDistinct(column.getJdbcType())) {
                    suppressingDistinct = true;
                    break;
                }
            }

            if (!suppressingDistinct) {
                queryBuf.append("DISTINCT ");
            }
        }

        // convert ColumnDescriptors to column names
        List selectColumnExpList = new ArrayList();

        Iterator it = resultColumns.iterator();
        while (it.hasNext()) {
            ColumnDescriptor column = (ColumnDescriptor) it.next();
            selectColumnExpList.add(column.getQualifiedColumnName());
        }

        // append any column expressions used in the order by if this query
        // uses the DISTINCT modifier
        if (forcingDistinct || getSelectQuery().isDistinct()) {
            List orderByColumnList = orderingTranslator.getOrderByColumnList();
            for (int i = 0; i < orderByColumnList.size(); i++) {
                // Convert to ColumnDescriptors??
                Object orderByColumnExp = orderByColumnList.get(i);
                if (!selectColumnExpList.contains(orderByColumnExp)) {
                    selectColumnExpList.add(orderByColumnExp);
                }
            }
        }

        // append columns (unroll the loop's first element)
        int columnCount = selectColumnExpList.size();
        queryBuf.append((String) selectColumnExpList.get(0));
        // assume there is at least 1 element
        for (int i = 1; i < columnCount; i++) {
            queryBuf.append(", ");
            queryBuf.append((String) selectColumnExpList.get(i));
        }

        // append from clause
        queryBuf.append(" FROM ");

        // append table list (unroll loop's 1st element)
        int tableCount = tableList.size();
        appendTable(queryBuf, 0); // assume there is at least 1 table
        for (int i = 1; i < tableCount; i++) {
            queryBuf.append(", ");
            appendTable(queryBuf, i);
        }

        // append db relationship joins if any
        boolean hasWhere = false;
        int dbRelCount = dbRelList.size();
        if (dbRelCount > 0) {
            hasWhere = true;
            queryBuf.append(" WHERE ");

            appendJoins(queryBuf, 0);
            for (int i = 1; i < dbRelCount; i++) {
                queryBuf.append(" AND ");
                appendJoins(queryBuf, i);
            }
        }

        // append parent qualifier if any
        if (parentQualifierStr != null) {
            if (hasWhere) {
                queryBuf.append(" AND (");
                queryBuf.append(parentQualifierStr);
                queryBuf.append(")");
            }
            else {
                hasWhere = true;
                queryBuf.append(" WHERE ");
                queryBuf.append(parentQualifierStr);
            }
        }

        // append group by
        boolean hasGroupBy = false;
        if (groupByList != null) {
            int groupByCount = groupByList.size();
            if (groupByCount > 0) {
                hasGroupBy = true;
                queryBuf.append(" GROUP BY ");
                appendGroupBy(queryBuf, 0);
                for (int i = 1; i < groupByCount; i++) {
                    queryBuf.append(", ");
                    appendGroupBy(queryBuf, i);
                }
            }
        }

        // append qualifier
        if (qualifierStr != null) {
            if (hasGroupBy) {
                queryBuf.append(" HAVING ");
                queryBuf.append(qualifierStr);
            }
            else {
                if (hasWhere) {
                    queryBuf.append(" AND (");
                    queryBuf.append(qualifierStr);
                    queryBuf.append(")");
                }
                else {
                    hasWhere = true;
                    queryBuf.append(" WHERE ");
                    queryBuf.append(qualifierStr);
                }
            }
        }

        // append prebuilt ordering
        if (orderByStr != null) {
            queryBuf.append(" ORDER BY ").append(orderByStr);
        }

        return queryBuf.toString();
    }

    /**
     * Returns a list of ColumnDescriptors for the query columns.
     * 
     * @since 1.2
     */
    public ColumnDescriptor[] getResultColumns() {
        if (resultColumns == null || resultColumns.isEmpty()) {
            return new ColumnDescriptor[0];
        }

        return (ColumnDescriptor[]) resultColumns
                .toArray(new ColumnDescriptor[resultColumns.size()]);
    }

    /**
     * Returns true if SelectTranslator determined that a query requiring DISTINCT can't
     * be run with DISTINCT keyword for internal reasons. If this method returns true,
     * DataNode may need to do in-memory distinct filtering.
     * 
     * @since 1.1
     */
    public boolean isSuppressingDistinct() {
        return suppressingDistinct;
    }

    private SelectQuery getSelectQuery() {
        return (SelectQuery) getQuery();
    }

    /**
     * Creates a list of columns used in the query's GROUP BY clause.
     */
    private List buildGroupByList() {
        DbEntity dbEntity = getRootDbEntity();
        return (dbEntity instanceof DerivedDbEntity) ? ((DerivedDbEntity) dbEntity)
                .getGroupByAttributes() : Collections.EMPTY_LIST;
    }

    List buildResultColumns() {

        // create alias for root table
        newAliasForTable(getRootDbEntity());

        List columns = new ArrayList();
        SelectQuery query = getSelectQuery();

        // for query with custom attributes use a different strategy
        if (query.isFetchingCustomAttributes()) {
            appendCustomColumns(columns, query);
        }
        else {
            appendQueryColumns(columns, query);
        }

        return columns;
    }

    /**
     * Appends columns needed for object SelectQuery to the provided columns list.
     */
    //  TODO: this whole method screams REFACTORING!!!
    List appendQueryColumns(List columns, SelectQuery query) {

        Set attributes = new HashSet();

        // fetched attributes include attributes that are either:
        // 
        //   * class properties
        //   * PK
        //   * FK used in relationships
        //   * GROUP BY
        //   * joined prefetch PK

        ObjEntity oe = getRootEntity();

        // null tree will indicate that we don't take inheritance into account
        EntityInheritanceTree tree = null;

        if (query.isResolvingInherited()) {
            tree = getRootInheritanceTree();
        }

        // ObjEntity attrs
        Iterator attrs = (tree != null) ? tree.allAttributes().iterator() : oe
                .getAttributes()
                .iterator();
        while (attrs.hasNext()) {
            ObjAttribute oa = (ObjAttribute) attrs.next();
            Iterator dbPathIterator = oa.getDbPathIterator();
            while (dbPathIterator.hasNext()) {
                Object pathPart = dbPathIterator.next();
                if (pathPart instanceof DbRelationship) {
                    DbRelationship rel = (DbRelationship) pathPart;
                    dbRelationshipAdded(rel);
                }
                else if (pathPart instanceof DbAttribute) {
                    DbAttribute dbAttr = (DbAttribute) pathPart;
                    if (dbAttr == null) {
                        throw new CayenneRuntimeException(
                                "ObjAttribute has no DbAttribute: " + oa.getName());
                    }

                    appendColumn(columns, oa, dbAttr, attributes, null);
                }
            }
        }

        // relationship keys
        Iterator rels = (tree != null) ? tree.allRelationships().iterator() : oe
                .getRelationships()
                .iterator();
        while (rels.hasNext()) {
            ObjRelationship rel = (ObjRelationship) rels.next();
            DbRelationship dbRel = (DbRelationship) rel.getDbRelationships().get(0);

            List joins = dbRel.getJoins();
            int len = joins.size();
            for (int i = 0; i < len; i++) {
                DbJoin join = (DbJoin) joins.get(i);
                DbAttribute src = join.getSource();
                appendColumn(columns, null, src, attributes, null);
            }
        }

        // add remaining needed attrs from DbEntity

        DbEntity table = getRootDbEntity();
        Iterator pk = table.getPrimaryKey().iterator();
        while (pk.hasNext()) {
            DbAttribute dba = (DbAttribute) pk.next();
            appendColumn(columns, null, dba, attributes, null);
        }

        // add FKs for Prefetch selects for to-many ObjRelationships with no reverse
        if (query instanceof PrefetchSelectQuery) {
            PrefetchSelectQuery pq = (PrefetchSelectQuery) query;
            ObjRelationship r = pq.getLastPrefetchHint();
            if ((r != null) && (r.getReverseRelationship() == null)) {

                // Prefetching a single step toMany relationship which
                // has no reverse obj relationship. Add the FK attributes
                // of the relationship (wouldn't otherwise be included)

                // TODO: seems like a hack for srcPkSnapshotWithTargetSnapshot benefit;
                // if we have a flat row with multiple joins, this will violate the naming
                // conventions.
                DbRelationship dbRel = (DbRelationship) r.getDbRelationships().get(0);

                List joins = dbRel.getJoins();
                for (int j = 0; j < joins.size(); j++) {
                    DbJoin join = (DbJoin) joins.get(j);
                    DbAttribute target = join.getTarget();
                    appendColumn(columns, null, target, attributes, null);
                }
            }
        }

        // handle joint prefetches
        if (!query.getJointPrefetches().isEmpty()) {
            Iterator jointPrefetches = query.getJointPrefetches().iterator();
            while (jointPrefetches.hasNext()) {
                String prefetch = (String) jointPrefetches.next();

                // for each prefetch add all joins plus columns from the target entity
                Expression prefetchExp = Expression.fromString(prefetch);
                Expression dbPrefetch = oe.translateToDbPath(prefetchExp);

                // find target entity
                Iterator it = table.resolvePathComponents(dbPrefetch);

                DbRelationship r = null;
                while (it.hasNext()) {
                    r = (DbRelationship) it.next();
                    dbRelationshipAdded(r);
                }

                if (r == null) {
                    throw new CayenneRuntimeException("Invalid joint prefetch '"
                            + prefetch
                            + "' for entity: "
                            + oe.getName());
                }

                // add columns from the target entity, skipping those that are an FK to
                // source entity

                Collection skipColumns = Collections.EMPTY_LIST;
                if (r.getSourceEntity() == table) {
                    skipColumns = new ArrayList(2);
                    Iterator joins = r.getJoins().iterator();
                    while (joins.hasNext()) {
                        DbJoin join = (DbJoin) joins.next();
                        if (attributes.contains(join.getSource())) {
                            skipColumns.add(join.getTarget());
                        }
                    }
                }

                // go via target OE to make sure that Java types are mapped correctly...
                ObjRelationship targetRel = (ObjRelationship) prefetchExp.evaluate(oe);
                Iterator targetObjAttrs = targetRel
                        .getTargetEntity()
                        .getAttributes()
                        .iterator();
                while (targetObjAttrs.hasNext()) {
                    ObjAttribute oa = (ObjAttribute) targetObjAttrs.next();
                    Iterator dbPathIterator = oa.getDbPathIterator();
                    while (dbPathIterator.hasNext()) {
                        Object pathPart = dbPathIterator.next();
                        if (pathPart instanceof DbRelationship) {
                            DbRelationship rel = (DbRelationship) pathPart;
                            dbRelationshipAdded(rel);
                        }
                        else if (pathPart instanceof DbAttribute) {
                            DbAttribute attribute = (DbAttribute) pathPart;
                            if (attribute == null) {
                                throw new CayenneRuntimeException(
                                        "ObjAttribute has no DbAttribute: "
                                                + oa.getName());
                            }

                            if (!skipColumns.contains(attribute)) {
                                appendColumn(columns,
                                        oa,
                                        attribute,
                                        attributes,
                                        dbPrefetch);
                            }
                        }
                    }
                }

                // append remaining target attributes such as keys
                Iterator targetAttributes = r
                        .getTargetEntity()
                        .getAttributes()
                        .iterator();
                while (targetAttributes.hasNext()) {
                    DbAttribute attribute = (DbAttribute) targetAttributes.next();
                    if (!skipColumns.contains(attribute)) {
                        appendColumn(columns, null, attribute, attributes, dbPrefetch);
                    }
                }
            }
        }

        return columns;
    }

    /**
     * Appends custom columns from SelectQuery to the provided list.
     */
    List appendCustomColumns(List columns, SelectQuery query) {

        List customAttributes = query.getCustomDbAttributes();
        DbEntity table = getRootDbEntity();
        int len = customAttributes.size();

        for (int i = 0; i < len; i++) {
            DbAttribute attribute = (DbAttribute) table
                    .getAttribute((String) customAttributes.get(i));
            if (attribute == null) {
                throw new CayenneRuntimeException("Attribute does not exist: "
                        + customAttributes.get(i));
            }

            String alias = aliasForTable((DbEntity) attribute.getEntity());
            columns.add(new ColumnDescriptor(attribute, alias));
        }

        return columns;
    }

    private void appendColumn(
            List columns,
            ObjAttribute objAttribute,
            DbAttribute attribute,
            Set skipSet,
            Expression dbPath) {

        if (skipSet.add(attribute)) {
            String alias = aliasForTable((DbEntity) attribute.getEntity());
            ColumnDescriptor column = (objAttribute != null) ? new ColumnDescriptor(
                    objAttribute,
                    attribute,
                    alias) : new ColumnDescriptor(attribute, alias);

            // used for joint prefetches
            if (dbPath != null) {
                String path = dbPath.toString().substring("db:".length());
                column.setLabel(path + '.' + attribute.getName());
            }

            columns.add(column);
        }
    }

    private void appendGroupBy(StringBuffer queryBuf, int index) {
        DbAttribute column = (DbAttribute) groupByList.get(index);
        String alias = aliasForTable((DbEntity) column.getEntity());
        queryBuf.append(column.getAliasedName(alias));
    }

    private void appendTable(StringBuffer queryBuf, int index) {
        DbEntity ent = (DbEntity) tableList.get(index);
        queryBuf.append(ent.getFullyQualifiedName());
        //The alias should be the alias from the same index in aliasList, not that
        // returned by aliasForTable.
        queryBuf.append(' ').append((String) aliasList.get(index));
    }

    private void appendJoins(StringBuffer queryBuf, int index) {
        DbRelationship rel = (DbRelationship) dbRelList.get(index);
        String srcAlias = aliasForTable((DbEntity) rel.getSourceEntity());
        String targetAlias = (String) aliasLookup.get(rel);

        boolean andFlag = false;

        List joins = rel.getJoins();
        int len = joins.size();
        for (int i = 0; i < len; i++) {
            DbJoin join = (DbJoin) joins.get(i);

            if (andFlag) {
                queryBuf.append(" AND ");
            }
            else {
                andFlag = true;
            }

            queryBuf
                    .append(srcAlias)
                    .append('.')
                    .append(join.getSourceName())
                    .append(" = ")
                    .append(targetAlias)
                    .append('.')
                    .append(join.getTargetName());
        }
    }

    /**
     * Stores a new relationship in an internal list. Later it will be used to create
     * joins to relationship destination table.
     */
    public void dbRelationshipAdded(DbRelationship rel) {
        if (rel.isToMany()) {
            forcingDistinct = true;
        }

        String existAlias = (String) aliasLookup.get(rel);

        if (existAlias == null) {
            dbRelList.add(rel);

            // add alias for the destination table of the relationship
            String newAlias = newAliasForTable((DbEntity) rel.getTargetEntity());
            aliasLookup.put(rel, newAlias);
        }
    }

    /**
     * Sets up and returns a new alias for a speciafied table.
     */
    protected String newAliasForTable(DbEntity ent) {
        if (ent instanceof DerivedDbEntity) {
            ent = ((DerivedDbEntity) ent).getParentEntity();
        }

        String newAlias = "t" + aliasCounter++;
        tableList.add(ent);
        aliasList.add(newAlias);
        return newAlias;
    }

    public String aliasForTable(DbEntity ent, DbRelationship rel) {
        return (String) aliasLookup.get(rel);
    }

    /**
     * Overrides superclass implementation. Will return an alias that should be used for a
     * specified DbEntity in the query (or null if this DbEntity is not included in the
     * FROM clause).
     */
    public String aliasForTable(DbEntity ent) {
        if (ent instanceof DerivedDbEntity) {
            ent = ((DerivedDbEntity) ent).getParentEntity();
        }

        int entIndex = tableList.indexOf(ent);
        if (entIndex >= 0) {
            return (String) aliasList.get(entIndex);
        }
        else {
            StringBuffer msg = new StringBuffer();
            msg.append("Alias not found, DbEntity: '").append(ent != null
                    ? ent.getName()
                    : "<null entity>").append("'\nExisting aliases:");

            int len = aliasList.size();
            for (int i = 0; i < len; i++) {
                String dbeName = (tableList.get(i) != null) ? ((DbEntity) tableList
                        .get(i)).getName() : "<null entity>";
                msg.append("\n").append(aliasList.get(i)).append(" => ").append(dbeName);
            }

            throw new CayenneRuntimeException(msg.toString());
        }
    }

    /**
     * Always returns true.
     */
    public boolean supportsTableAliases() {
        return true;
    }
}