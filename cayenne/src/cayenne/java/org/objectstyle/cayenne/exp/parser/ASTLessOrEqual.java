/* Generated By:JJTree: Do not edit this line. ASTLessOrEqual.java */

package org.objectstyle.cayenne.exp.parser;

import org.objectstyle.cayenne.exp.Expression;

class ASTLessOrEqual extends SimpleNode {
    public ASTLessOrEqual(int id) {
        super(id);
    }

    public ASTLessOrEqual(ExpressionParser p, int id) {
        super(p, id);
    }

    protected String getExpressionOperator(int index) {
        return "<=";
    }

    public int getType() {
        return Expression.LESS_THAN_EQUAL_TO;
    }
}
