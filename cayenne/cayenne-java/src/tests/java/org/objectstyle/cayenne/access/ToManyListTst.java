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
package org.objectstyle.cayenne.access;

import java.util.Collections;

import org.objectstyle.art.Artist;
import org.objectstyle.art.Painting;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.unit.CayenneTestCase;

public class ToManyListTst extends CayenneTestCase {
    protected DataContext context;

    protected void setUp() throws Exception {
        super.setUp();
        
        context = createDataContext();
    }

    private ToManyList createForNewArtist() {
        Artist artist = (Artist) context.createAndRegisterNewObject(Artist.class);
        return new ToManyList(artist, Artist.PAINTING_ARRAY_PROPERTY);
    }

    private ToManyList createForExistingArtist() {
        Artist artist = (Artist) context.createAndRegisterNewObject(Artist.class);
        artist.setArtistName("aa");
        context.commitChanges();
        return new ToManyList(artist, Artist.PAINTING_ARRAY_PROPERTY);
    }

    public void testNewAddRemove() throws Exception {
        ToManyList list = createForNewArtist();
        assertFalse(
            "Expected resolved list when created with a new object",
            list.needsFetch());
        assertEquals(0, list.size());

        Painting p1 = (Painting) context.createAndRegisterNewObject(Painting.class);
        list.add(p1);
        assertEquals(1, list.size());

        Painting p2 = (Painting) context.createAndRegisterNewObject(Painting.class);
        list.add(p2);
        assertEquals(2, list.size());

        list.remove(p1);
        assertEquals(1, list.size());
    }

    public void testSavedUnresolvedAddRemove() throws Exception {
        ToManyList list = createForExistingArtist();

        // immediately tag Artist as MODIFIED, since we are messing up with relationship
        // bypassing normal CayenneDataObject methods
         ((Artist) list.getSource()).setPersistenceState(PersistenceState.MODIFIED);

        assertTrue("List must be unresolved for an existing object", list.needsFetch());

        Painting p1 = (Painting) context.createAndRegisterNewObject(Painting.class);
        list.add(p1);
        assertTrue("List must be unresolved when adding an object...", list.needsFetch());
        assertTrue(list.addedToUnresolved.contains(p1));

        Painting p2 = (Painting) context.createAndRegisterNewObject(Painting.class);
        list.add(p2);
        assertTrue("List must be unresolved when adding an object...", list.needsFetch());
        assertTrue(list.addedToUnresolved.contains(p2));

        list.remove(p1);
        assertTrue(
            "List must be unresolved when removing an object...",
            list.needsFetch());
        assertFalse(list.addedToUnresolved.contains(p1));
        assertTrue(list.removedFromUnresolved.contains(p1));

        // now resolve
        int size = list.size();
        assertFalse("List must be resolved after checking a size...", list.needsFetch());
        assertEquals(1, size);
        assertTrue(list.objectList.contains(p2));
    }

    public void testSavedUnresolvedMerge() throws Exception {
        ToManyList list = createForExistingArtist();

        Painting p1 = (Painting) context.createAndRegisterNewObject(Painting.class);
        p1.setPaintingTitle("p1");

        // list being tested is a separate copy from 
        // the relationship list that Artist has, so adding a painting
        // here will not add the painting to the array being tested
         ((Artist) list.getSource()).addToPaintingArray(p1);
        context.commitChanges();

        // immediately tag Artist as MODIFIED, since we are messing up with relationship
        // bypassing normal CayenneDataObject methods
         ((Artist) list.getSource()).setPersistenceState(PersistenceState.MODIFIED);

        assertTrue("List must be unresolved...", list.needsFetch());
        list.add(p1);
        assertTrue("List must be unresolved when adding an object...", list.needsFetch());
        assertTrue(list.addedToUnresolved.contains(p1));

        Painting p2 = (Painting) context.createAndRegisterNewObject(Painting.class);
        list.add(p2);
        assertTrue("List must be unresolved when adding an object...", list.needsFetch());
        assertTrue(list.addedToUnresolved.contains(p2));

        // now resolve the list and see how merge worked
        int size = list.size();
        assertFalse("List must be resolved after checking a size...", list.needsFetch());
        assertEquals(2, size);
        assertTrue(list.objectList.contains(p2));
        assertTrue(list.objectList.contains(p1));
    }

    public void testThrowOutDeleted() throws Exception {
        ToManyList list = createForExistingArtist();

        Painting p1 = (Painting) context.createAndRegisterNewObject(Painting.class);
        p1.setPaintingTitle("p1");
        Painting p2 = (Painting) context.createAndRegisterNewObject(Painting.class);
        p2.setPaintingTitle("p2");

        // list being tested is a separate copy from 
        // the relationship list that Artist has, so adding a painting
        // here will not add the painting to the array being tested
         ((Artist) list.getSource()).addToPaintingArray(p1);
        ((Artist) list.getSource()).addToPaintingArray(p2);
        context.commitChanges();

        // immediately tag Artist as MODIFIED, since we are messing up with relationship
        // bypassing normal CayenneDataObject methods
         ((Artist) list.getSource()).setPersistenceState(PersistenceState.MODIFIED);

        assertTrue("List must be unresolved...", list.needsFetch());
        list.add(p1);
        list.add(p2);
        assertTrue("List must be unresolved when adding an object...", list.needsFetch());
        assertTrue(list.addedToUnresolved.contains(p2));
        assertTrue(list.addedToUnresolved.contains(p1));

        // now delete p2 and resolve list
         ((Artist) list.getSource()).removeFromPaintingArray(p2);
        context.deleteObject(p2);
        context.commitChanges();

        assertTrue("List must be unresolved...", list.needsFetch());
        assertTrue(
            "List must be unresolved when an object was deleted externally...",
            list.needsFetch());
        assertTrue(list.addedToUnresolved.contains(p2));
        assertTrue(list.addedToUnresolved.contains(p1));

        // now resolve the list and see how merge worked
        int size = list.size();
        assertFalse("List must be resolved after checking a size...", list.needsFetch());
        assertEquals("Deleted object must have been purged...", 1, size);
        assertTrue(list.objectList.contains(p1));
        assertFalse(
            "Deleted object must have been purged...",
            list.objectList.contains(p2));
    }

    public void testRealRelationship() throws Exception {
        Artist artist = (Artist) context.createAndRegisterNewObject(Artist.class);
        artist.setArtistName("aaa");

        Painting p1 = (Painting) context.createAndRegisterNewObject(Painting.class);
        p1.setPaintingTitle("p1");

        context.commitChanges();
        context.invalidateObjects(Collections.singletonList(artist));

        ToManyList list = (ToManyList) artist.getPaintingArray();
        assertTrue("List must be unresolved...", list.needsFetch());

        Painting p2 = (Painting) context.createAndRegisterNewObject(Painting.class);
        p2.setPaintingTitle("p2");

        artist.addToPaintingArray(p1);
        artist.addToPaintingArray(p2);
        assertTrue("List must be unresolved...", list.needsFetch());

        context.commitChanges();

        assertTrue("List must be unresolved...", list.needsFetch());

        int size = list.size();
        assertFalse("List must be resolved...", list.needsFetch());
        assertTrue(list.contains(p1));
        assertTrue(list.contains(p2));
        assertEquals(2, size);
    }

    public void testRealRelationshipRollback() throws Exception {
        Artist artist = (Artist) context.createAndRegisterNewObject(Artist.class);
        artist.setArtistName("aaa");

        Painting p1 = (Painting) context.createAndRegisterNewObject(Painting.class);
        p1.setPaintingTitle("p1");
        artist.addToPaintingArray(p1);
        context.commitChanges();
        context.invalidateObjects(Collections.singletonList(artist));

        ToManyList list = (ToManyList) artist.getPaintingArray();
        assertTrue("List must be unresolved...", list.needsFetch());

        Painting p2 = (Painting) context.createAndRegisterNewObject(Painting.class);

        artist.addToPaintingArray(p2);
        assertTrue("List must be unresolved...", list.needsFetch());
        assertTrue(list.addedToUnresolved.contains(p2));

        context.rollbackChanges();

        assertTrue("List must be unresolved...", list.needsFetch());

        // call to "contains" must trigger list resolution
        assertTrue(list.contains(p1));
        assertFalse(list.contains(p2));
        assertFalse("List must be resolved...", list.needsFetch());
    }
}