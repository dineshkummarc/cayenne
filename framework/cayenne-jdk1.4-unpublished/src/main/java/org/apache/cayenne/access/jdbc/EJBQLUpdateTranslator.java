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

import java.math.BigDecimal;

import org.apache.cayenne.ejbql.EJBQLBaseVisitor;
import org.apache.cayenne.ejbql.EJBQLException;
import org.apache.cayenne.ejbql.EJBQLExpression;

/**
 * A translator of EJBQL UPDATE statements into SQL.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
class EJBQLUpdateTranslator extends EJBQLBaseVisitor {

    private EJBQLTranslationContext context;
    private int itemCount;

    EJBQLUpdateTranslator(EJBQLTranslationContext context) {
        this.context = context;
    }

    EJBQLTranslationContext getContext() {
        return context;
    }

    public boolean visitUpdate(EJBQLExpression expression) {
        context.append("UPDATE");
        return true;
    }

    public boolean visitFrom(EJBQLExpression expression, int finishedChildIndex) {
        expression.visit(new EJBQLFromTranslator(context));

        return false;
    }

    public boolean visitUpdateItem(EJBQLExpression expression) {
        if (itemCount++ > 0) {
            context.append(',');
        }
        else {
            context.append(" SET");
        }

        return true;
    }

    public boolean visitUpdateField(EJBQLExpression expression, int finishedChildIndex) {

        EJBQLPathTranslator pathTranslator = new EJBQLPathTranslator(context) {

            protected void appendMultiColumnPath(EJBQLMultiColumnOperand operand) {
                throw new EJBQLException("Multi-column paths are unsupported in UPDATEs");
            }

            public boolean visitUpdateField(
                    EJBQLExpression expression,
                    int finishedChildIndex) {
                return visitPath(expression, finishedChildIndex);
            }
        };

        expression.visit(pathTranslator);
        return false;
    }

    public boolean visitUpdateValue(EJBQLExpression expression) {
        context.append(" =");
        return true;
    }

    // TODO: andrus, 7/31/2007 - all literal processing (visitStringLiteral,
    // visitIntegerLiteral, visitDecimalLiteral) is duplicated in
    // EJBQLConditionalTranslator - may need to refactor
    public boolean visitStringLiteral(EJBQLExpression expression) {
        if (expression.getText() == null) {
            context.append("null");
        }
        else {
            // note that String Literal text is already wrapped in single quotes, with
            // quotes that are part of the string escaped.
            context.append(" #bind(").append(expression.getText()).append(" 'VARCHAR')");
        }

        return true;
    }

    public boolean visitIntegerLiteral(EJBQLExpression expression) {
        if (expression.getText() == null) {
            context.append("null");
        }
        else {
            Object value;

            try {
                value = new Integer(expression.getText());
            }
            catch (NumberFormatException nfex) {
                throw new EJBQLException("Invalid integer: " + expression.getText());
            }

            String var = context.bindParameter(value);
            context.append(" #bind($").append(var).append(" 'INTEGER')");
        }
        return true;
    }

    public boolean visitDecimalLiteral(EJBQLExpression expression) {
        if (expression.getText() == null) {
            context.append("null");
        }
        else {
            Object value;

            try {
                value = new BigDecimal(expression.getText());
            }
            catch (NumberFormatException nfex) {
                throw new EJBQLException("Invalid decimal: " + expression.getText());
            }

            String var = context.bindParameter(value);
            context.append(" #bind($").append(var).append(" 'DECIMAL')");
        }
        return true;
    }
}