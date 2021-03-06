/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.access.jdbc;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.ejbql.EJBQLBaseVisitor;
import org.apache.cayenne.ejbql.EJBQLException;
import org.apache.cayenne.ejbql.EJBQLExpression;
import org.apache.cayenne.ejbql.EJBQLParserFactory;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Entity;

/**
 * Handles appending joins to the content buffer at a marked position.
 * 
 * @since 3.0
 */
public class EJBQLJoinAppender {

    protected EJBQLTranslationContext context;
    private Map<String, String> reusableJoins;

    static String makeJoinTailMarker(String id) {
        return "FROM_TAIL" + id;
    }

    public EJBQLJoinAppender(EJBQLTranslationContext context) {
        this.context = context;
    }

    /**
     * Registers a "reusable" join, returning a preexisting ID if the join is already
     * registered. Reusable joins are the implicit inner joins that are added as a result
     * of processing of path expressions in SELECT or WHERE clauses. Note that if an
     * implicit INNER join overlaps with an explicit INNER join, both joins are added to
     * the query.
     */
    public String registerReusableJoin(
            String sourceIdPath,
            String relationship,
            String targetId) {

        if (reusableJoins == null) {
            reusableJoins = new HashMap<String, String>();
        }

        String key = sourceIdPath + ":" + relationship;

        String oldId = reusableJoins.put(key, targetId);
        if (oldId != null) {
            // revert back to old id
            reusableJoins.put(key, oldId);
            return oldId;
        }

        return null;
    }

    public void appendInnerJoin(String marker, EJBQLTableId lhsId, EJBQLTableId rhsId) {
        appendJoin(marker, lhsId, rhsId, "INNER JOIN");
    }

    public void appendOuterJoin(String marker, EJBQLTableId lhsId, EJBQLTableId rhsId) {
        appendJoin(marker, lhsId, rhsId, "LEFT OUTER JOIN");
    }

    protected void appendJoin(
            String marker,
            EJBQLTableId lhsId,
            EJBQLTableId rhsId,
            String semantics) {

        List<DbRelationship> joinRelationships = context.getIncomingRelationships(rhsId);
        if (joinRelationships.isEmpty()) {
            throw new EJBQLException("No join configured for id " + rhsId);
        }

        // TODO: andrus, 4/8/2007 - support for flattened relationships
        DbRelationship incomingDB = joinRelationships.get(0);

        // TODO: andrus, 1/6/2008 - move reusable join check here...

        Entity sourceEntity = incomingDB.getSourceEntity();
        String tableName;

        if (sourceEntity instanceof DbEntity) {
           tableName = ((DbEntity) sourceEntity).getFullyQualifiedName();
        } else {
           tableName = sourceEntity.getName();
        }

        String sourceAlias = context.getTableAlias(lhsId.getEntityId(), tableName);

        if (marker != null) {
            context.pushMarker(marker, false);
        }

        try {

            context.append(" ").append(semantics);
            String targetAlias = appendTable(rhsId);
            context.append(" ON (");

            Iterator<DbJoin> it = incomingDB.getJoins().iterator();
            if (it.hasNext()) {
                DbJoin dbJoin = it.next();
                context
                        .append(sourceAlias)
                        .append('.')
                        .append(dbJoin.getSourceName())
                        .append(" = ")
                        .append(targetAlias)
                        .append('.')
                        .append(dbJoin.getTargetName());
            }

            while (it.hasNext()) {
                context.append(", ");
                DbJoin dbJoin = it.next();
                context
                        .append(sourceAlias)
                        .append('.')
                        .append(dbJoin.getSourceName())
                        .append(" = ")
                        .append(targetAlias)
                        .append('.')
                        .append(dbJoin.getTargetName());
            }

            context.append(")");
        }
        finally {
            if (marker != null) {
                context.popMarker();
            }
        }
    }

    public String appendTable(EJBQLTableId id) {

        String tableName = id.getDbEntity(context).getFullyQualifiedName();
        String alias;

        if (context.isUsingAliases()) {
            // TODO: andrus 1/5/2007 - if the same table is joined more than once, this
            // will create an incorrect alias.
            alias = context.getTableAlias(id.getEntityId(), tableName);

            // not using "AS" to separate table name and alias name - OpenBase doesn't
            // support
            // "AS", and the rest of the databases do not care
            context.append(' ').append(tableName).append(' ').append(alias);
        }
        else {
            context.append(' ').append(tableName);
            alias = tableName;
        }

        // append inheritance qualifier...
        if (id.isPrimaryTable()) {

            Expression qualifier = context
                    .getEntityDescriptor(id.getEntityId())
                    .getEntityQualifier();

            if (qualifier != null) {

                EJBQLExpression ejbqlQualifier = ejbqlQualifierForEntityAndSubclasses(
                        qualifier,
                        id.getEntityId());

                context.pushMarker(context.makeWhereMarker(), true);
                context.append(" WHERE");
                context.popMarker();

                context.pushMarker(context.makeEntityQualifierMarker(), false);

                ejbqlQualifier.visit(context
                        .getTranslatorFactory()
                        .getConditionTranslator(context));

                context.popMarker();
            }
        }

        return alias;
    }

    private EJBQLExpression ejbqlQualifierForEntityAndSubclasses(
            Expression qualifier,
            String entityId) {

        // parser only works on full queries, so prepend a dummy query and then strip it
        // out...
        String ejbqlChunk = qualifier.toEJBQL(entityId);
        EJBQLExpression expression = EJBQLParserFactory.getParser().parse(
                "DELETE FROM DUMMY WHERE " + ejbqlChunk);

        final EJBQLExpression[] result = new EJBQLExpression[1];
        expression.visit(new EJBQLBaseVisitor() {

            @Override
            public boolean visitWhere(EJBQLExpression expression) {
                result[0] = expression.getChild(0);
                return false;
            }
        });

        return result[0];
    }
}
