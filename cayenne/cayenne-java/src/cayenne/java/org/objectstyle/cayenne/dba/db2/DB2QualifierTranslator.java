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

package org.objectstyle.cayenne.dba.db2;

import java.sql.Types;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.trans.QueryAssembler;
import org.objectstyle.cayenne.access.trans.TrimmingQualifierTranslator;
import org.objectstyle.cayenne.dba.TypesMapping;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.map.DbAttribute;

/**
 * @author Andrei Adamchik
 */
public class DB2QualifierTranslator extends TrimmingQualifierTranslator {

    public DB2QualifierTranslator() {
        super();
    }

    public DB2QualifierTranslator(
        QueryAssembler queryAssembler,
        String trimFunction) {
        super(queryAssembler, trimFunction);
    }

    protected void appendLiteralDirect(
        StringBuffer buf,
        Object val,
        DbAttribute attr,
        Expression parentExpression) {

        boolean castNeeded = false;

        if (parentExpression != null) {
            int type = parentExpression.getType();

            castNeeded =
                attr != null
                    && (type == Expression.LIKE
                        || type == Expression.LIKE_IGNORE_CASE
                        || type == Expression.NOT_LIKE
                        || type == Expression.NOT_LIKE_IGNORE_CASE);
        }

        if (castNeeded) {
            buf.append("CAST (");
        }

        super.appendLiteralDirect(buf, val, attr, parentExpression);

        if (castNeeded) {
            int jdbcType = attr.getType();
            int len = attr.getMaxLength();

            // determine CAST type

            // LIKE on CHAR may produce unpredictible results
            // LIKE on LONVARCHAR doesn't seem to be supported 
            if (jdbcType == Types.CHAR || jdbcType == Types.LONGVARCHAR) {
                jdbcType = Types.VARCHAR;

                // length is required for VARCHAR
                if (len <= 0) {
                    len = 254;
                }
            }

            buf.append(" AS ");
            String[] types =
                getQueryAssembler().getAdapter().externalTypesForJdbcType(
                    jdbcType);

            if (types == null || types.length == 0) {
                throw new CayenneRuntimeException(
                    "Can't find database type for JDBC type '"
                        + TypesMapping.getSqlNameByType(jdbcType));
            }

            buf.append(types[0]);
            if (len > 0 && TypesMapping.supportsLength(jdbcType)) {
                buf.append("(").append(len).append(")");
            }

            buf.append(")");
        }
    }
}