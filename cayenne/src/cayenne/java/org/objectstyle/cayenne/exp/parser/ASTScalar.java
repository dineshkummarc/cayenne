/* Generated By:JJTree: Do not edit this line. ASTScalar.java */

package org.objectstyle.cayenne.exp.parser;

class ASTScalar extends SimpleNode {
    public ASTScalar(int id) {
        super(id);
    }

    public ASTScalar(ExpressionParser p, int id) {
        super(p, id);
    }

    protected void toStringBuffer(StringBuffer buffer) {
        boolean quote = value instanceof String;

        if (quote) {
            buffer.append('\"');
        }
        escapeString(buffer, String.valueOf(value));
        if (quote) {
            buffer.append('\"');
        }
    }

    private static void escapeString(StringBuffer buffer, String source) {
        int len = source.length();
        for (int i = 0; i < len; i++) {
            char c = source.charAt(i);

            switch (c) {
                case '\n' :
                    buffer.append("\\n");
                    continue;
                case '\r' :
                    buffer.append("\\r");
                    continue;
                case '\t' :
                    buffer.append("\\t");
                    continue;
                case '\b' :
                    buffer.append("\\b");
                    continue;
                case '\f' :
                    buffer.append("\\f");
                    continue;
                case '\\' :
                    buffer.append("\\\\");
                    continue;
                case '\'' :
                    buffer.append("\\'");
                    continue;
                case '\"' :
                    buffer.append("\\\"");
                    continue;
                default :
                    buffer.append(c);
            }
        }
    }
}
