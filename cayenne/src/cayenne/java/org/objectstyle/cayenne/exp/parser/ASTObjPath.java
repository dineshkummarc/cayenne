/* Generated By:JJTree: Do not edit this line. ASTObjPath.java */

package org.objectstyle.cayenne.exp.parser;

import java.io.PrintWriter;

import org.objectstyle.cayenne.exp.Expression;

class ASTObjPath extends SimpleNode {
    public ASTObjPath(int id) {
        super(id);
    }

    public ASTObjPath(ExpressionParser p, int id) {
        super(p, id);
    }

    public void encodeAsString(PrintWriter pw) {
        pw.print(value);
    }

    public int getType() {
        return Expression.OBJ_PATH;
    }

    public int getOperandCount() {
        return 1;
    }

    public Object getOperand(int index) {
        if (index == 0) {
            return value;
        }

        throw new ArrayIndexOutOfBoundsException(index);
    }
}
