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

import org.apache.cayenne.ejbql.EJBQLBaseVisitor;
import org.apache.cayenne.ejbql.EJBQLException;
import org.apache.cayenne.ejbql.EJBQLExpression;
import org.apache.cayenne.ejbql.parser.EJBQLPath;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjAttribute;

/**
 * Translator of the EJBQL select clause.
 * 
 * @author Andrus Adamchik
 * @since 3.0
 */
class EJBQLSelectColumnsTranslator extends EJBQLBaseVisitor {

    private EJBQLTranslationContext context;
    private int expressionsCount;

    EJBQLSelectColumnsTranslator(EJBQLTranslationContext context) {
        this.context = context;
    }

    public boolean visitSelectExpression(EJBQLExpression expression) {
        if (expressionsCount++ > 0) {
            context.append(",");
        }

        return true;
    }

    public boolean visitAggregate(EJBQLExpression expression) {
        expression.visit(new EJBQLAggregateColumnTranslator(context));
        return false;
    }

    public boolean visitPath(EJBQLPath expression, int finishedChildIndex) {

        EJBQLConditionPathTranslator pathTranslator = new EJBQLConditionPathTranslator(
                context) {

            protected void appendMultiColumnPath(EJBQLMultiColumnOperand operand) {
                throw new EJBQLException("Can't use multi-column paths in column clause");
            }

            protected void processTerminatingAttribute(ObjAttribute attribute) {
                DbEntity table = currentEntity.getDbEntity();
                String alias = this.lastAlias != null ? lastAlias : context
                        .getTableAlias(idPath, table.getFullyQualifiedName());

                context.append(" #result('").append(alias).append('.').append(
                        attribute.getDbAttributeName()).append("' '").append(
                        attribute.getType()).append("' '").append(
                        context.nextColumnAlias()).append("')");
            }
        };

        expression.visit(pathTranslator);
        return false;
    }

    public boolean visitIdentifier(EJBQLExpression expression) {
        expression.visit(new EJBQLIdentifierColumnsTranslator(context, true));
        return false;
    }
}