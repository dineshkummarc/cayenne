
/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
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
package org.objectstyle.cayenne.exp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectstyle.art.Artist;
import org.objectstyle.art.Painting;
import org.objectstyle.cayenne.access.DataContextTestBase;
import org.objectstyle.cayenne.query.SelectQuery;

/**
 * @author Andrei Adamchik
 */
public class ParsedExpQualifierCompatTst extends DataContextTestBase {

    private List execute(Class root, Expression qualifier) {
        SelectQuery query = new SelectQuery(root, qualifier);
        return context.performQuery(query);
    }

    public void testOr() throws Exception {
        Expression parsed =
            ExpressionFactory.expressionFromString(
                "artistName='artist1' or artistName='artist3'");
        assertEquals(2, execute(Artist.class, parsed).size());

        parsed =
            ExpressionFactory.expressionFromString(
                "artistName='artist1' or artistName='artist3' or artistName='artist5'");
        assertEquals(3, execute(Artist.class, parsed).size());
    }

    public void testAnd() throws Exception {
        Expression parsed =
            ExpressionFactory.expressionFromString(
                "artistName='artist1' and artistName='artist1'");
        assertEquals(1, execute(Artist.class, parsed).size());

        parsed =
            ExpressionFactory.expressionFromString(
                "artistName='artist1' and artistName='artist3'");
        assertEquals(0, execute(Artist.class, parsed).size());
    }

    public void testNot() throws Exception {
        Expression parsed1 =
            ExpressionFactory.expressionFromString("not artistName='artist3'");
        assertEquals(artistCount - 1, execute(Artist.class, parsed1).size());

        Expression parsed2 =
            ExpressionFactory.expressionFromString("not artistName='artist3'");
        assertEquals(artistCount - 1, execute(Artist.class, parsed2).size());
    }

    public void testEqual() throws Exception {
        Expression parsed1 =
            ExpressionFactory.expressionFromString("artistName='artist3'");
        assertEquals(1, execute(Artist.class, parsed1).size());

        Expression parsed2 =
            ExpressionFactory.expressionFromString("artistName=='artist3'");
        assertEquals(1, execute(Artist.class, parsed2).size());
    }

    public void testNotEqual() throws Exception {
        Expression parsed1 =
            ExpressionFactory.expressionFromString("artistName!='artist3'");
        assertEquals(artistCount - 1, execute(Artist.class, parsed1).size());

        Expression parsed2 =
            ExpressionFactory.expressionFromString("artistName<>'artist3'");
        assertEquals(artistCount - 1, execute(Artist.class, parsed2).size());
    }

    public void testLessThan() throws Exception {
        populatePaintings();
        Expression parsed1 =
            ExpressionFactory.expressionFromString("estimatedPrice < 2000.0");
        assertEquals(1, execute(Painting.class, parsed1).size());
    }

    public void testLessThanEqualTo() throws Exception {
        populatePaintings();
        Expression parsed1 =
            ExpressionFactory.expressionFromString("estimatedPrice <= 2000.0");
        assertEquals(2, execute(Painting.class, parsed1).size());
    }

    public void testGreaterThan() throws Exception {
        populatePaintings();
        Expression parsed1 =
            ExpressionFactory.expressionFromString("estimatedPrice > 2000");
        assertEquals(artistCount - 2, execute(Painting.class, parsed1).size());
    }

    public void testGreaterThanEqualTo() throws Exception {
        populatePaintings();
        Expression parsed1 =
            ExpressionFactory.expressionFromString("estimatedPrice >= 2000");
        assertEquals(artistCount - 1, execute(Painting.class, parsed1).size());
    }

    public void testLike() throws Exception {
        Expression parsed1 =
            ExpressionFactory.expressionFromString("artistName like 'artist%2'");
        assertEquals(3, execute(Artist.class, parsed1).size());
    }

    public void testLikeIgnoreCase() throws Exception {
        Expression parsed1 =
            ExpressionFactory.expressionFromString(
                "artistName likeIgnoreCase 'artist%2'");
        assertEquals(3, execute(Artist.class, parsed1).size());
    }

    public void testNotLike() throws Exception {
        Expression parsed1 =
            ExpressionFactory.expressionFromString("artistName not like 'artist%2'");
        assertEquals(artistCount - 3, execute(Artist.class, parsed1).size());
    }

    public void testNotLikeIgnoreCase() throws Exception {
        Expression parsed1 =
            ExpressionFactory.expressionFromString(
                "artistName not likeIgnoreCase 'artist%2'");
        assertEquals(artistCount - 3, execute(Artist.class, parsed1).size());
    }

    public void testIn() throws Exception {
        Expression parsed1 =
            ExpressionFactory.expressionFromString(
                "artistName in ('artist1', 'artist3', 'artist19')");
        assertEquals(3, execute(Artist.class, parsed1).size());
    }

    public void testNotIn() throws Exception {
        Expression parsed1 =
            ExpressionFactory.expressionFromString(
                "artistName not in ('artist1', 'artist3', 'artist19')");
        assertEquals(artistCount - 3, execute(Artist.class, parsed1).size());
    }

    public void testBetween() throws Exception {
        populatePaintings();
        Expression parsed1 =
            ExpressionFactory.expressionFromString(
                "estimatedPrice between 2000.0 and 4000.0");
        assertEquals(3, execute(Painting.class, parsed1).size());
    }

    public void testNotBetween() throws Exception {
        populatePaintings();
        Expression parsed1 =
            ExpressionFactory.expressionFromString(
                "estimatedPrice not between 2000.0 and 4000.0");
        assertEquals(artistCount - 3, execute(Painting.class, parsed1).size());
    }

    public void testParameter() throws Exception {
        Map parameters = new HashMap();
        parameters.put("artistName", "artist5");
        Expression parsed1 =
            ExpressionFactory.expressionFromString("artistName=$artistName");
        parsed1 = parsed1.expWithParameters(parameters);
        assertEquals(1, execute(Artist.class, parsed1).size());
    }

    public void testDbExpression() throws Exception {
        Expression parsed1 =
            ExpressionFactory.expressionFromString("db:ARTIST_NAME='artist3'");
        assertEquals(1, execute(Artist.class, parsed1).size());
    }

    public void testFloatExpression() throws Exception {
        populatePaintings();
        Expression parsed1 =
            ExpressionFactory.expressionFromString("estimatedPrice < 2000.01");
        assertEquals(2, execute(Painting.class, parsed1).size());
    }

    public void testNullExpression() throws Exception {
        Expression parsed1 = ExpressionFactory.expressionFromString("artistName!=null");
        assertEquals(artistCount, execute(Artist.class, parsed1).size());

        Expression parsed2 = ExpressionFactory.expressionFromString("artistName = null");
        assertEquals(0, execute(Artist.class, parsed2).size());
    }
}
