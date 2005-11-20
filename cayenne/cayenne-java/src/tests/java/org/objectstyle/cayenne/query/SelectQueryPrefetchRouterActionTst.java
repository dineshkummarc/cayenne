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
package org.objectstyle.cayenne.query;

import org.objectstyle.art.Artist;
import org.objectstyle.art.Gallery;
import org.objectstyle.art.Painting;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.unit.CayenneTestCase;

public class SelectQueryPrefetchRouterActionTst extends CayenneTestCase {

    public void testPaintings1() {
        ObjEntity paintingEntity = getDomain().getEntityResolver().lookupObjEntity(
                Painting.class);
        SelectQuery q = new SelectQuery(Artist.class, ExpressionFactory.matchExp(
                "artistName",
                "abc"));
        q.addPrefetch(Artist.PAINTING_ARRAY_PROPERTY);

        SelectQueryPrefetchRouterAction action = new SelectQueryPrefetchRouterAction();

        MockQueryRouter router = new MockQueryRouter();
        action.route(q, router, getDomain().getEntityResolver());
        assertEquals(1, router.getQueryCount());

        PrefetchSelectQuery prefetch = (PrefetchSelectQuery) router.getQueries().get(0);

        assertSame(paintingEntity, prefetch.getRoot());
        assertEquals(Expression.fromString("db:toArtist.ARTIST_NAME = 'abc'"), prefetch
                .getQualifier());
    }

    public void testPrefetchPaintings2() {
        ObjEntity paintingEntity = getDomain().getEntityResolver().lookupObjEntity(
                Painting.class);

        SelectQuery q = new SelectQuery(Artist.class, Expression
                .fromString("artistName = 'abc' or artistName = 'xyz'"));
        q.addPrefetch(Artist.PAINTING_ARRAY_PROPERTY);

        SelectQueryPrefetchRouterAction action = new SelectQueryPrefetchRouterAction();

        MockQueryRouter router = new MockQueryRouter();
        action.route(q, router, getDomain().getEntityResolver());
        assertEquals(1, router.getQueryCount());

        PrefetchSelectQuery prefetch = (PrefetchSelectQuery) router.getQueries().get(0);
        assertSame(paintingEntity, prefetch.getRoot());
        assertEquals(
                Expression
                        .fromString("db:toArtist.ARTIST_NAME = 'abc' or db:toArtist.ARTIST_NAME = 'xyz'"),
                prefetch.getQualifier());
    }

    public void testGalleries() {
        ObjEntity galleryEntity = getDomain().getEntityResolver().lookupObjEntity(
                Gallery.class);
        SelectQuery q = new SelectQuery(Artist.class, ExpressionFactory.matchExp(
                "artistName",
                "abc"));
        q.addPrefetch("paintingArray.toGallery");

        SelectQueryPrefetchRouterAction action = new SelectQueryPrefetchRouterAction();

        MockQueryRouter router = new MockQueryRouter();
        action.route(q, router, getDomain().getEntityResolver());
        assertEquals(1, router.getQueryCount());

        PrefetchSelectQuery prefetch = (PrefetchSelectQuery) router.getQueries().get(0);

        assertSame(galleryEntity, prefetch.getRoot());
        assertEquals(Expression
                .fromString("db:paintingArray.toArtist.ARTIST_NAME = 'abc'"), prefetch
                .getQualifier());
    }
}