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
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.objectstyle.cayenne.event.EventManager;
import org.objectstyle.cayenne.graph.GraphDiff;
import org.objectstyle.cayenne.graph.GraphManager;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.opp.OPPChannel;
import org.objectstyle.cayenne.opp.SyncCommand;
import org.objectstyle.cayenne.property.ClassDescriptor;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.SingleObjectQuery;

/**
 * A default generic implementation of ObjectContext suitable for accessing Cayenne from
 * either an ORM or a client tiers. Communicates with Cayenne persistence stack by sending
 * OPP messages via a parent OPPChannel.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class CayenneContext implements ObjectContext {

    // if we are to pass CayenneContext around, channel should be left alone and
    // reinjected later if needed
    protected transient OPPChannel channel;
    protected EntityResolver entityResolver;

    ObjectContextGraphManager graphManager;

    // note that it is important to reuse the same action within the property change
    // thread to avoid a loop of "propertyChange" calls on handling reverse relationships.
    // Here we go further and make action a thread-safe ivar that tracks its own thread
    // state.
    CayenneContextGraphAction graphAction;

    // object that merges "backdoor" changes that come from the channel.
    ObjectContextMergeHandler mergeHandler;

    /**
     * Creates a new CayenneContext with no channel and disabled graph events.
     */
    public CayenneContext() {
        this(null);
    }

    /**
     * Creates a new CayenneContext, initializaing it with a channel instance.
     * CayenneContext created using this constructor WILL NOT broadcast graph change
     * events.
     */
    public CayenneContext(OPPChannel channel) {
        this(channel, false, false);
    }

    /**
     * Creates a new CayenneContext, initializaing it with a channel. If
     * <code>graphEventsEnabled</code> is true, this context will broadcast GraphEvents
     * using ObjectContext.GRAPH_CHANGE_SUBJECT.
     */
    public CayenneContext(OPPChannel channel, boolean changeEventsEnabled,
            boolean syncEventsEnabled) {

        this.graphAction = new CayenneContextGraphAction(this);
        this.graphManager = new ObjectContextGraphManager(
                this,
                changeEventsEnabled,
                syncEventsEnabled);

        setChannel(channel);
    }

    public OPPChannel getChannel() {
        return channel;
    }

    /**
     * Sets the context channel, setting up a listener for channel events.
     */
    public void setChannel(OPPChannel channel) {
        if (this.channel != channel) {

            if (this.mergeHandler != null) {
                this.mergeHandler.active = false;
                this.mergeHandler = null;
            }

            this.channel = channel;

            EventManager eventManager = (channel != null)
                    ? channel.getEventManager()
                    : null;
            if (eventManager != null) {
                this.mergeHandler = new ObjectContextMergeHandler(this);

                // listen to our channel events...
                // note that we must reset listener on channel switch, as there is no
                // guarantee that a new channel uses the same EventManager.
                ObjectContextUtils.listenForChannelEvents(channel, mergeHandler);
            }
        }
    }

    /**
     * Returns true if this context posts individual object modification events. Subject
     * used for these events is <code>ObjectContext.GRAPH_CHANGED_SUBJECT</code>.
     */
    public boolean isChangeEventsEnabled() {
        return graphManager.changeEventsEnabled;
    }

    /**
     * Returns true if this context posts lifecycle events. Subjects used for these events
     * are
     * <code>ObjectContext.GRAPH_COMMIT_STARTED_SUBJECT, ObjectContext.GRAPH_COMMITTED_SUBJECT,
     * ObjectContext.GRAPH_COMMIT_ABORTED_SUBJECT, ObjectContext.GRAPH_ROLLEDBACK_SUBJECT.</code>.
     */
    public boolean isLifecycleEventsEnabled() {
        return graphManager.lifecycleEventsEnabled;
    }

    /**
     * Returns an EntityResolver that provides mapping information needed for
     * CayenneContext operation. If EntityResolver is not set, this method would obtain
     * and cache one from the underlying OPPChannel by sending BootstrapMessage.
     */
    public EntityResolver getEntityResolver() {
        // load entity resolver on demand
        if (entityResolver == null) {
            synchronized (this) {
                if (entityResolver == null) {
                    setEntityResolver(channel.getEntityResolver());
                }
            }
        }

        return entityResolver;
    }

    public void setEntityResolver(EntityResolver entityResolver) {
        this.entityResolver = entityResolver;
    }

    public GraphManager getGraphManager() {
        return graphManager;
    }

    ObjectContextGraphManager internalGraphManager() {
        return graphManager;
    }

    CayenneContextGraphAction internalGraphAction() {
        return graphAction;
    }

    /**
     * Commits changes to uncommitted objects. First checks if there are changes in this
     * context and if any changes are detected, sends a commit message to remote Cayenne
     * service via an internal instance of CayenneConnector.
     */
    public void commitChanges() {
        doCommitChanges();
    }

    GraphDiff doCommitChanges() {
        GraphDiff commitDiff = null;

        synchronized (graphManager) {

            if (graphManager.hasChanges()) {

                graphManager.graphCommitStarted();

                try {
                    commitDiff = channel.synchronize(new SyncCommand(
                            this,
                            SyncCommand.COMMIT_TYPE,
                            graphManager.getDiffsSinceLastFlush()));
                }
                catch (Throwable th) {
                    graphManager.graphCommitAborted();

                    if (th instanceof CayenneRuntimeException) {
                        throw (CayenneRuntimeException) th;
                    }
                    else {
                        throw new CayenneRuntimeException("Commit error", th);
                    }
                }

                graphManager.graphCommitted(commitDiff);
            }
        }

        return commitDiff;
    }

    public void rollbackChanges() {
        synchronized (graphManager) {
            if (graphManager.hasChanges()) {

                GraphDiff diff = graphManager.getDiffs();
                graphManager.graphReverted();

                channel
                        .synchronize(new SyncCommand(
                                this,
                                SyncCommand.ROLLBACK_TYPE,
                                diff));
            }
        }
    }

    public void flushChanges() {
        synchronized (graphManager) {
            if (graphManager.hasChangesSinceLastFlush()) {
                GraphDiff diff = graphManager.getDiffsSinceLastFlush();
                graphManager.graphFlushed();
                channel.synchronize(new SyncCommand(this, SyncCommand.FLUSH_TYPE, diff));
            }
        }
    }

    public void revertChanges() {
        synchronized (graphManager) {
            if (graphManager.hasChanges()) {
                graphManager.graphReverted();
            }
        }
    }

    /**
     * Deletes an object locally, scheduling it for future deletion from the external data
     * store.
     */
    public void deleteObject(Persistent object) {
        if (object.getPersistenceState() == PersistenceState.TRANSIENT) {
            return;
        }

        if (object.getPersistenceState() == PersistenceState.DELETED) {
            return;
        }

        if (object.getPersistenceState() == PersistenceState.NEW) {
            // kick it out of context
            object.setPersistenceState(PersistenceState.TRANSIENT);
            graphManager.unregisterNode(object.getObjectId());
            return;
        }

        // TODO: no delete rules (yet)

        object.setPersistenceState(PersistenceState.DELETED);
        graphManager.nodeRemoved(object.getObjectId());
    }

    /**
     * Creates and registers a new Persistent object instance.
     */
    public Persistent newObject(Class persistentClass) {
        if (persistentClass == null) {
            throw new NullPointerException("Persistent class can't be null.");
        }

        ObjEntity entity = getEntityResolver().lookupObjEntity(persistentClass);
        if (entity == null) {
            throw new CayenneRuntimeException("No entity mapped for class: "
                    + persistentClass);
        }

        synchronized (graphManager) {
            return createNewObject(entity, new ObjectId(entity.getName()));
        }
    }

    public List performQuery(Query query) {
        List objects = channel.performQuery(query);
        if (objects.isEmpty()) {
            return objects;
        }

        // postprocess fetched objects...

        ListIterator it = objects.listIterator();

        synchronized (graphManager) {
            while (it.hasNext()) {

                Persistent fetchedObject = (Persistent) it.next();

                // sanity check
                if (fetchedObject.getObjectId() == null) {
                    throw new CayenneRuntimeException(
                            "Server returned an object without an id: " + fetchedObject);
                }

                Persistent cachedObject = (Persistent) graphManager.getNode(fetchedObject
                        .getObjectId());

                if (cachedObject != null) {

                    // TODO: implement smart merge for modified objects...
                    if (cachedObject.getPersistenceState() != PersistenceState.MODIFIED) {

                        // refresh existing object...

                        // lookup descriptor on the spot - we can be dealing with a mix of
                        // different objects in the inheritance hierarchy...
                        ClassDescriptor descriptor = getClassDescriptor(cachedObject);

                        if (cachedObject.getPersistenceState() == PersistenceState.HOLLOW) {
                            cachedObject.setPersistenceState(PersistenceState.COMMITTED);
                            descriptor.prepareForAccess(cachedObject);
                        }

                        descriptor.copyProperties(fetchedObject, cachedObject);
                    }

                    it.set(cachedObject);
                }
                else {

                    // lookup descriptor on the spot - we can deal with a mix of different
                    // objects in the hierarchy...
                    ClassDescriptor descriptor = getClassDescriptor(fetchedObject);

                    fetchedObject.setPersistenceState(PersistenceState.COMMITTED);
                    fetchedObject.setObjectContext(this);
                    descriptor.prepareForAccess(fetchedObject);
                    graphManager.registerNode(fetchedObject.getObjectId(), fetchedObject);
                }
            }
        }

        return objects;
    }

    /**
     * Converts a list of Persistent objects registered in some other ObjectContext to a
     * list of objects local to this ObjectContext.
     * <p>
     * <i>Current limitation: all objects in the source list must be either in COMMITTED
     * or in HOLLOW state.</i>
     * </p>
     */
    public List localObjects(List objects) {
        List localObjects = new ArrayList(objects.size());

        Iterator it = objects.iterator();
        while (it.hasNext()) {
            Persistent object = (Persistent) it.next();

            // sanity check
            if (object.getPersistenceState() != PersistenceState.COMMITTED
                    && object.getPersistenceState() != PersistenceState.HOLLOW) {
                throw new CayenneRuntimeException(
                        "Only COMMITTED and HOLLOW objects can be transferred between contexts. "
                                + "Invalid object state '"
                                + PersistenceState.persistenceStateName(object
                                        .getPersistenceState())
                                + "', ObjectId: "
                                + object.getObjectId());
            }

            if (object.getObjectContext() == this) {
                localObjects.add(object);
                continue;
            }

            ObjectId id = object.getObjectId();
            Object localObject = internalGraphManager().getNode(id);

            if (localObject == null) {
                if (id == null) {
                    throw new CayenneRuntimeException(
                            "Can't transfer an object without ObjectId to an ObjectContext.");
                }

                // TODO: Andrus, 12/28/2005 - should we copy all the values if the source
                // is not HOLLOW?
                ObjEntity entity = getEntityResolver()
                        .lookupObjEntity(id.getEntityName());
                if (entity == null) {
                    throw new CayenneRuntimeException("Unmapped entity: "
                            + id.getEntityName());
                }
                localObject = createFault(entity, id);

                // copy committed values...
                if (object.getPersistenceState() == PersistenceState.COMMITTED) {
                    entity.getClassDescriptor().copyProperties(object, localObject);
                    ((Persistent) localObject)
                            .setPersistenceState(PersistenceState.COMMITTED);
                }
            }

            localObjects.add(localObject);
        }

        return localObjects;
    }

    public QueryResponse performGenericQuery(Query query) {
        return channel.performGenericQuery(query);
    }

    /**
     * Resolves an object if it is HOLLOW.
     */
    public void prepareForAccess(Persistent object, String property) {
        if (object.getPersistenceState() == PersistenceState.HOLLOW) {

            ObjectId gid = object.getObjectId();
            List objects = performQuery(new SingleObjectQuery(gid));

            if (objects.size() == 0) {
                throw new FaultFailureException(
                        "Error resolving fault, no matching row exists in the database for GlobalID: "
                                + gid);
            }
            else if (objects.size() > 1) {
                throw new FaultFailureException(
                        "Error resolving fault, more than one row exists in the database for GlobalID: "
                                + gid);
            }
        }
    }

    public void propertyChanged(
            Persistent object,
            String property,
            Object oldValue,
            Object newValue) {

        graphAction.handlePropertyChange(object, property, oldValue, newValue);
    }

    public Collection uncommittedObjects() {
        synchronized (graphManager) {
            return graphManager.dirtyNodes();
        }
    }

    public Collection deletedObjects() {
        synchronized (graphManager) {
            return graphManager.dirtyNodes(PersistenceState.DELETED);
        }
    }

    public Collection modifiedObjects() {
        synchronized (graphManager) {
            return graphManager.dirtyNodes(PersistenceState.MODIFIED);
        }
    }

    public Collection newObjects() {
        synchronized (graphManager) {
            return graphManager.dirtyNodes(PersistenceState.NEW);
        }
    }

    protected ClassDescriptor getClassDescriptor(Persistent object) {
        ObjEntity entity = getEntityResolver().lookupObjEntity(
                object.getObjectId().getEntityName());
        return entity.getClassDescriptor();
    }

    // ****** non-public methods ******

    Persistent createNewObject(ObjEntity entity, ObjectId id) {
        ClassDescriptor descriptor = entity.getClassDescriptor();

        Persistent object = (Persistent) descriptor.createObject();

        object.setPersistenceState(PersistenceState.NEW);
        object.setObjectContext(this);
        object.setObjectId(id);

        descriptor.prepareForAccess(object);
        graphManager.registerNode(object.getObjectId(), object);
        graphManager.nodeCreated(object.getObjectId());

        return object;
    }

    Persistent createFault(ObjEntity entity, ObjectId id) {
        ClassDescriptor descriptor = entity.getClassDescriptor();

        Persistent object = (Persistent) descriptor.createObject();

        object.setPersistenceState(PersistenceState.HOLLOW);
        object.setObjectContext(this);
        object.setObjectId(id);

        // note that this must be called AFTER setting persistence state, otherwise we'd
        // get ValueHolders incorrectly marked as resolved
        descriptor.prepareForAccess(object);

        graphManager.registerNode(id, object);

        return object;
    }
}