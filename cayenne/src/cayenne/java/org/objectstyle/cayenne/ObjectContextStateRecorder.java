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

package org.objectstyle.cayenne;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.objectstyle.cayenne.graph.GraphChangeHandler;
import org.objectstyle.cayenne.graph.GraphManager;

/**
 * Tracks dirty Persistent objects.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
class ObjectContextStateRecorder implements GraphChangeHandler {

    Set dirtyIds;
    GraphManager graphManager;

    ObjectContextStateRecorder(GraphManager graphManager) {
        this.dirtyIds = new HashSet();
        this.graphManager = graphManager;
    }

    void clear() {
        dirtyIds = new HashSet();
    }

    /**
     * Updates dirty objects state and clears dirty ids map.
     */
    void graphCommitted() {
        Iterator it = dirtyIds.iterator();
        while (it.hasNext()) {
            Object node = graphManager.getNode(it.next());
            if (node instanceof Persistent) {
                Persistent persistentNode = (Persistent) node;
                switch (persistentNode.getPersistenceState()) {
                    case PersistenceState.MODIFIED:
                    case PersistenceState.NEW:
                        persistentNode.setPersistenceState(PersistenceState.COMMITTED);
                        break;
                    case PersistenceState.DELETED:
                        persistentNode.setPersistenceState(PersistenceState.TRANSIENT);
                        break;
                }
            }
        }

        clear();
    }

    void graphReverted() {
        Iterator it = dirtyIds.iterator();
        while (it.hasNext()) {
            Object node = graphManager.getNode(it.next());
            if (node instanceof Persistent) {
                Persistent persistentNode = (Persistent) node;
                switch (persistentNode.getPersistenceState()) {
                    case PersistenceState.MODIFIED:
                    case PersistenceState.DELETED:
                        persistentNode.setPersistenceState(PersistenceState.COMMITTED);
                        break;
                    case PersistenceState.NEW:
                        persistentNode.setPersistenceState(PersistenceState.TRANSIENT);
                        break;
                }
            }
        }

        clear();
    }

    boolean hasChanges() {
        return !dirtyIds.isEmpty();
    }

    Collection dirtyNodes() {
        if (dirtyIds.isEmpty()) {
            return Collections.EMPTY_SET;
        }

        List objects = new ArrayList(dirtyIds.size());
        Iterator it = dirtyIds.iterator();
        while (it.hasNext()) {
            objects.add(graphManager.getNode(it.next()));
        }

        return objects;
    }

    Collection dirtyNodes(int state) {
        if (dirtyIds.isEmpty()) {
            return Collections.EMPTY_SET;
        }

        int size = dirtyIds.size();
        List objects = new ArrayList(size > 50 ? size / 2 : size);
        Iterator it = dirtyIds.iterator();
        while (it.hasNext()) {
            Persistent o = (Persistent) graphManager.getNode(it.next());

            if (o.getPersistenceState() == state) {
                objects.add(o);
            }
        }

        return objects;
    }

    // *** GraphChangeHandler methods

    public void nodeIdChanged(Object nodeId, Object newId) {
        if (dirtyIds.remove(nodeId)) {
            dirtyIds.add(newId);
        }
    }

    public void nodeCreated(Object nodeId) {
        dirtyIds.add(nodeId);
    }

    public void nodeRemoved(Object nodeId) {
        dirtyIds.add(nodeId);
    }

    public void nodePropertyChanged(
            Object nodeId,
            String property,
            Object oldValue,
            Object newValue) {
        dirtyIds.add(nodeId);
    }

    public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
        dirtyIds.add(nodeId);
    }

    public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {
        dirtyIds.add(nodeId);
    }
}