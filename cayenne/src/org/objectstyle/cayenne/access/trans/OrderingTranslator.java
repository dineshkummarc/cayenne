package org.objectstyle.cayenne.access.trans;
/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002 The ObjectStyle Group 
 * and individual authors of the software.  All rights reserved.
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
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:  
 *       "This product includes software developed by the 
 *        ObjectStyle Group (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "ObjectStyle Group" and "Cayenne" 
 *    must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written 
 *    permission, please contact andrus@objectstyle.org.
 *
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    nor may "ObjectStyle" appear in their names without prior written
 *    permission of the ObjectStyle Group.
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
 * individuals on behalf of the ObjectStyle Group.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 *
 */

import java.util.List;
import java.util.logging.Logger;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.query.Ordering;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.SelectQuery;

/** Translates query qualifier to SQL. */
public class OrderingTranslator extends QueryAssemblerHelper {
	static Logger logObj = Logger.getLogger(OrderingTranslator.class.getName());

	public OrderingTranslator(QueryAssembler queryAssembler) {
		super(queryAssembler);
	}

	/** Translates query Ordering list to SQL ORDER BY clause. 
	 *  Ordering list is obtained from <code>queryAssembler</code>'s query object. 
	 *  In a process of building of ORDER BY clause, <code>queryAssembler</code> 
	 *  is notified when a join needs to be added. */
	public String doTranslation() {
		Query q = queryAssembler.getQuery();

		// only select queries can have ordering...
		if (q == null || !(q instanceof SelectQuery))
			return null;

		StringBuffer buf = new StringBuffer();
		List list = ((SelectQuery) q).getOrderingList();
		int len = list.size();

		for (int i = 0; i < len; i++) {
			if (i > 0)
				buf.append(", ");

			Ordering ord = (Ordering) list.get(i);
			Expression exp = ord.getSortSpec();

			if (exp.getType() == Expression.OBJ_PATH) {
				appendObjPath(buf, exp);
			}
			else if(exp.getType() == Expression.DB_NAME) {
				appendDbPath(buf, exp);
			}
			else {
				throw new CayenneRuntimeException("Unsupported ordering expression: " + exp);
			}

			// "ASC" is a noop, omit it from the query 
			if (!ord.isAscending()) {
				buf.append(" DESC");
			}
		}

		return buf.length() > 0 ? buf.toString() : null;
	}
}
