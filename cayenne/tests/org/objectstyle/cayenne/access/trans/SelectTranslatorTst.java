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

import java.sql.Connection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.objectstyle.TestMain;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.query.Ordering;
import org.objectstyle.cayenne.query.SelectQuery;

public class SelectTranslatorTst extends TestCase {
	static Logger logObj =
		Logger.getLogger(SelectTranslatorTst.class.getName());

	protected SelectQuery q;
	protected DbEntity artistEnt;

	public SelectTranslatorTst(String name) {
		super(name);
	}

	protected void setUp() throws java.lang.Exception {
		q = new SelectQuery();
		artistEnt =
			TestMain.getSharedDomain().lookupEntity("Artist").getDbEntity();
	}

	private SelectTranslator buildTranslator(Connection con) throws Exception {
		SelectTranslator t = new SelectTranslator();
		t.setAdapter(TestMain.getSharedNode().getAdapter());
		t.setCon(con);
		t.setEngine(TestMain.getSharedDomain());
		t.setQuery(q);
		return t;
	}

	public void testCreateSqlString1() throws java.lang.Exception {
		Connection con = TestMain.getSharedConnection();

		try {
			// query with qualifier and ordering
			q.setObjEntityName("Artist");
			q.setQualifier(
				ExpressionFactory.binaryExp(
					Expression.LIKE,
					"artistName",
					"a%"));
			q.addOrdering("dateOfBirth", Ordering.ASC);

			String generatedSql = buildTranslator(con).createSqlString();

			// do some simple assertions to make sure all parts are in
			assertNotNull(generatedSql);
			assertTrue(generatedSql.startsWith("SELECT "));
			assertTrue(generatedSql.indexOf(" FROM ") > 0);
			assertTrue(
				generatedSql.indexOf(" WHERE ")
					> generatedSql.indexOf(" FROM "));
			assertTrue(
				generatedSql.indexOf(" ORDER BY ")
					> generatedSql.indexOf(" WHERE "));
		} finally {
			con.close();
		}
	}

	public void testCreateSqlString2() throws java.lang.Exception {
		Connection con = TestMain.getSharedConnection();
		try {
			// query with "distinct" set
			q.setObjEntityName("Artist");
			q.setDistinct(true);

			String generatedSql = buildTranslator(con).createSqlString();

			// do some simple assertions to make sure all parts are in
			assertNotNull(generatedSql);
			assertTrue(generatedSql.startsWith("SELECT DISTINCT"));
		} finally {
			con.close();
		}
	}

	public void testBuildColumnList() throws java.lang.Exception {
		Connection con = TestMain.getSharedConnection();

		try {
			q.setObjEntityName("Artist");
			SelectTranslator transl = buildTranslator(con);
			transl.createSqlString();

			List columns = transl.getColumnList();
			List dbAttrs = artistEnt.getAttributeList();
			
			assertEquals(dbAttrs.size(), columns.size());
			Iterator it = dbAttrs.iterator();
			while(it.hasNext()) {
				assertTrue(columns.contains(it.next()));
			}

		} finally {
			con.close();
		}
	}
}