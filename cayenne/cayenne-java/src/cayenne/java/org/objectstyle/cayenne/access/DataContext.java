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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.Factory;
import org.apache.log4j.Level;
import org.objectstyle.cayenne.BaseResponse;
import org.objectstyle.cayenne.CayenneException;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataChannel;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.DataObjectUtils;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.Fault;
import org.objectstyle.cayenne.FaultFailureException;
import org.objectstyle.cayenne.ObjectContext;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.Persistent;
import org.objectstyle.cayenne.QueryResponse;
import org.objectstyle.cayenne.access.event.DataContextEvent;
import org.objectstyle.cayenne.access.util.IteratedSelectObserver;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.event.EventManager;
import org.objectstyle.cayenne.event.EventSubject;
import org.objectstyle.cayenne.graph.CompoundDiff;
import org.objectstyle.cayenne.graph.GraphDiff;
import org.objectstyle.cayenne.graph.GraphManager;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbJoin;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.property.ClassDescriptor;
import org.objectstyle.cayenne.query.NamedQuery;
import org.objectstyle.cayenne.query.PrefetchTreeNode;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.QueryMetadata;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.query.SingleObjectQuery;
import org.objectstyle.cayenne.util.Util;

/**
 * Class that provides applications with access to Cayenne persistence features. In most
 * cases this is the only access class directly used in the application.
 * <p>
 * Most common DataContext use pattern is to create one DataContext per session. "Session"
 * may be a an HttpSession in a web application, or any other similar concept in a
 * multiuser application.
 * </p>
 * <p>
 * DataObjects are registered with DataContext either implicitly when they are fetched via
 * a query, or read via a relationship from another object, or explicitly via calling
 * {@link #createAndRegisterNewObject(Class)}during new DataObject creation. DataContext
 * tracks changes made to its DataObjects in memory, and flushes them to the database when
 * {@link #commitChanges()}is called. Until DataContext is committed, changes made to its
 * objects are not visible in other DataContexts.
 * </p>
 * <p>
 * Each DataObject can belong only to a single DataContext. To create a replica of an
 * object from a different DataContext in a local context, use
 * {@link #localObject(ObjectId, Persistent)} method.
 * <p>
 * <i>For more information see <a href="../../../../../../userguide/index.html"
 * target="_top">Cayenne User Guide. </a> </i>
 * </p>
 * <p>
 * <i>Note that all QueryEngine interface methods are deprecated in the DataContext. Since
 * 1.2 release DataContext implements ObjectContext and DataChannel interfaces.</i>
 * </p>
 * 
 * @author Andrus Adamchik
 */
public class DataContext implements ObjectContext, DataChannel, QueryEngine, Serializable {

    // DataContext events
    public static final EventSubject WILL_COMMIT = EventSubject.getSubject(
            DataContext.class,
            "DataContextWillCommit");
    public static final EventSubject DID_COMMIT = EventSubject.getSubject(
            DataContext.class,
            "DataContextDidCommit");
    public static final EventSubject DID_ROLLBACK = EventSubject.getSubject(
            DataContext.class,
            "DataContextDidRollback");

    /**
     * A holder of a DataContext bound to the current thread.
     * 
     * @since 1.1
     */
    // TODO: Andrus, 11/7/2005 - should we use InheritableThreadLocal instead?
    protected static final ThreadLocal threadDataContext = new ThreadLocal();

    // event posting default for new DataContexts
    private static boolean transactionEventsEnabledDefault;

    // enable/disable event handling for individual instances
    private boolean transactionEventsEnabled;

    // Set of DataContextDelegates to be notified.
    private DataContextDelegate delegate;

    protected boolean usingSharedSnaphsotCache;
    protected boolean validatingObjectsOnCommit;
    protected ObjectStore objectStore;

    protected transient DataChannel channel;

    // note that entity resolver is initialized from the parent channel the first time it
    // is accessed, and later cached in the context
    protected transient EntityResolver entityResolver;

    transient GraphManager graphManager;

    /**
     * Stores user defined properties associated with this DataContext.
     * 
     * @since 1.2
     */
    protected Map userProperties;

    /**
     * Stores the name of parent DataDomain. Used to defer initialization of the parent
     * QueryEngine after deserialization. This helps avoid an issue with certain servlet
     * engines (e.g. Tomcat) where HttpSessions with DataContext's are deserialized at
     * startup before Cayenne stack is fully initialized.
     */
    protected transient String lazyInitParentDomainName;

    /**
     * Returns the DataContext bound to the current thread.
     * 
     * @since 1.1
     * @return the DataContext associated with caller thread.
     * @throws IllegalStateException if there is no DataContext bound to the current
     *             thread.
     * @see org.objectstyle.cayenne.conf.WebApplicationContextProvider
     */
    public static DataContext getThreadDataContext() throws IllegalStateException {
        DataContext dc = (DataContext) threadDataContext.get();
        if (dc == null) {
            throw new IllegalStateException("Current thread has no bound DataContext.");
        }

        return dc;
    }

    /**
     * Binds a DataContext to the current thread. DataContext can later be retrieved by
     * users in the same thread by calling {@link DataContext#getThreadDataContext}.
     * Using null parameter will unbind currently bound DataContext.
     * 
     * @since 1.1
     */
    public static void bindThreadDataContext(DataContext context) {
        threadDataContext.set(context);
    }

    /**
     * Factory method that creates and returns a new instance of DataContext based on
     * default domain. If more than one domain exists in the current configuration,
     * {@link DataContext#createDataContext(String)} must be used instead. ObjectStore
     * associated with created DataContext will have a cache stack configured using parent
     * domain settings.
     */
    public static DataContext createDataContext() {
        return Configuration.getSharedConfiguration().getDomain().createDataContext();
    }

    /**
     * Factory method that creates and returns a new instance of DataContext based on
     * default domain. If more than one domain exists in the current configuration,
     * {@link DataContext#createDataContext(String, boolean)} must be used instead.
     * ObjectStore associated with newly created DataContext will have a cache stack
     * configured according to the specified policy, overriding a parent domain setting.
     * 
     * @since 1.1
     */
    public static DataContext createDataContext(boolean useSharedCache) {
        return Configuration.getSharedConfiguration().getDomain().createDataContext(
                useSharedCache);
    }

    /**
     * Factory method that creates and returns a new instance of DataContext using named
     * domain as its parent. If there is no domain matching the name argument, an
     * exception is thrown.
     */
    public static DataContext createDataContext(String domainName) {
        DataDomain domain = Configuration.getSharedConfiguration().getDomain(domainName);
        if (domain == null) {
            throw new IllegalArgumentException("Non-existent domain: " + domainName);
        }
        return domain.createDataContext();
    }

    /**
     * Creates and returns new DataContext that will use a named DataDomain as its parent.
     * ObjectStore associated with newly created DataContext will have a cache stack
     * configured according to the specified policy, overriding a parent domain setting.
     * 
     * @since 1.1
     */
    public static DataContext createDataContext(String domainName, boolean useSharedCache) {

        DataDomain domain = Configuration.getSharedConfiguration().getDomain(domainName);
        if (domain == null) {
            throw new IllegalArgumentException("Non-existent domain: " + domainName);
        }
        return domain.createDataContext(useSharedCache);
    }

    /**
     * Creates a new DataContext that is not attached to the Cayenne stack.
     */
    public DataContext() {
        this((DataChannel) null, null);
    }

    /**
     * Creates a DataContext with parent QueryEngine and a DataRowStore that should be
     * used by the ObjectStore.
     * 
     * @since 1.1
     * @param parent parent QueryEngine used to communicate with the data source.
     * @param objectStore ObjectStore used by DataContext.
     * @deprecated since 1.2 - use {@link #DataContext(DataChannel, ObjectStore)}
     *             constructor instead. Note that DataDomain is both a DataChannel and a
     *             QueryEngine, so you may need to do a cast:
     *             <code>new DataContext((DataChannel) domain, objectStore)</code>.
     */
    public DataContext(QueryEngine parent, ObjectStore objectStore) {
        this((DataChannel) parent, objectStore);
    }

    /**
     * Creates a new DataContext with parent DataChannel and ObjectStore.
     * 
     * @since 1.2
     */
    public DataContext(DataChannel channel, ObjectStore objectStore) {
        // use a setter to properly initialize EntityResolver
        setChannel(channel);

        this.objectStore = objectStore;

        this.setTransactionEventsEnabled(transactionEventsEnabledDefault);
        this.usingSharedSnaphsotCache = getParentDataDomain() != null
                && objectStore.getDataRowCache() == getParentDataDomain()
                        .getSharedSnapshotCache();
    }

    /**
     * Returns a map of user-defined properties associated with this DataContext.
     * 
     * @since 1.2
     */
    protected Map getUserProperties() {
        // as not all users will take advantage of properties, creating the
        // map on demand to keep DataContext lean...
        if (userProperties == null) {
            userProperties = new HashMap();
        }

        return userProperties;
    }

    /**
     * Creates and returns a new child DataContext.
     * 
     * @since 1.2
     */
    public DataContext createChildDataContext() {
        DataContextFactory factory = getParentDataDomain().getDataContextFactory();
        ObjectStore objectStore = new ObjectStore(getObjectStore().getDataRowCache());

        DataContext child = factory != null ? factory
                .createDataContext(this, objectStore) : new DataContext(
                (DataChannel) this,
                objectStore);

        child.setValidatingObjectsOnCommit(isValidatingObjectsOnCommit());
        return child;
    }

    /**
     * Returns a user-defined property previously set via 'setUserProperty'. Note that it
     * is a caller responsibility to synchronize access to properties.
     * 
     * @since 1.2
     */
    public Object getUserProperty(String key) {
        return getUserProperties().get(key);
    }

    /**
     * Sets a user-defined property. Note that it is a caller responsibility to
     * synchronize access to properties.
     * 
     * @since 1.2
     */
    public void setUserProperty(String key, Object value) {
        getUserProperties().put(key, value);
    }

    /**
     * Returns parent QueryEngine object. In most cases returned object is an instance of
     * DataDomain.
     * 
     * @deprecated since 1.2. Use 'getParentDataDomain()' or 'getChannel()' instead.
     */
    public QueryEngine getParent() {
        return getParentDataDomain();
    }

    /**
     * Sets direct parent of this DataContext.
     * 
     * @deprecated since 1.2, use setChannel instead.
     */
    public void setParent(QueryEngine parent) {
        if (parent == null || parent instanceof DataChannel) {
            setChannel((DataChannel) parent);
        }
        else {
            throw new CayenneRuntimeException(
                    "Only parents that implement DataChannel are supported.");
        }
    }

    /**
     * Returns parent DataChannel, that is normally a DataDomain or another DataContext.
     * 
     * @since 1.2
     */
    public DataChannel getChannel() {
        return channel;
    }

    /**
     * @since 1.2
     */
    public void setChannel(DataChannel channel) {
        if (this.channel != channel) {
            this.channel = channel;

            // cache entity resolver, as we have no idea how expensive it is to query it
            // on the channel every time
            this.entityResolver = channel != null ? channel.getEntityResolver() : null;
        }
    }

    /**
     * Returns a DataDomain used by this DataContext. DataDomain is looked up in the
     * OPPCHannel hierarchy. If a channel is not a DataDomain or a DataContext, null is
     * returned.
     * 
     * @return DataDomain that is a direct or indirect parent of this DataContext in the
     *         DataChannel hierarchy.
     * @since 1.1
     */
    public DataDomain getParentDataDomain() {
        awakeFromDeserialization();

        if (channel == null) {
            return null;
        }

        if (channel instanceof DataDomain) {
            return (DataDomain) channel;
        }

        if (channel instanceof DataContext) {
            return ((DataContext) channel).getParentDataDomain();
        }

        return null;
    }

    /**
     * Sets a DataContextDelegate for this context. Delegate is notified of certain events
     * in the DataContext lifecycle and can customize DataContext behavior.
     * 
     * @since 1.1
     */
    public void setDelegate(DataContextDelegate delegate) {
        this.delegate = delegate;
    }

    /**
     * Returns a delegate currently associated with this DataContext.
     * 
     * @since 1.1
     */
    public DataContextDelegate getDelegate() {
        return delegate;
    }

    /**
     * @return a delegate instance if it is initialized, or a shared noop implementation
     *         the context has no delegate. Useful to prevent extra null checks and
     *         conditional logic in the code.
     * @since 1.1
     */
    DataContextDelegate nonNullDelegate() {
        return (delegate != null) ? delegate : NoopDelegate.noopDelegate;
    }

    /**
     * Returns ObjectStore associated with this DataContext.
     */
    public ObjectStore getObjectStore() {
        return objectStore;
    }

    /**
     * Returns <code>true</code> if there are any modified, deleted or new objects
     * registered with this DataContext, <code>false</code> otherwise.
     */
    public boolean hasChanges() {
        return getObjectStore().hasChanges();
    }

    /**
     * Returns a list of objects that are registered with this DataContext and have a
     * state PersistenceState.NEW
     */
    public Collection newObjects() {
        return getObjectStore().objectsInState(PersistenceState.NEW);
    }

    /**
     * Returns a list of objects that are registered with this DataContext and have a
     * state PersistenceState.DELETED
     */
    public Collection deletedObjects() {
        return getObjectStore().objectsInState(PersistenceState.DELETED);
    }

    /**
     * Returns a list of objects that are registered with this DataContext and have a
     * state PersistenceState.MODIFIED
     */
    public Collection modifiedObjects() {
        return getObjectStore().objectsInState(PersistenceState.MODIFIED);
    }

    /**
     * Returns a collection of all uncommitted registered objects.
     * 
     * @since 1.2
     */
    public Collection uncommittedObjects() {

        int len = getObjectStore().registeredObjectsCount();
        if (len == 0) {
            return Collections.EMPTY_LIST;
        }

        // guess target collection size
        Collection objects = new ArrayList(len > 100 ? len / 2 : len);

        Iterator it = getObjectStore().getObjectIterator();
        while (it.hasNext()) {
            Persistent object = (Persistent) it.next();
            int state = object.getPersistenceState();
            if (state == PersistenceState.MODIFIED
                    || state == PersistenceState.NEW
                    || state == PersistenceState.DELETED) {

                objects.add(object);
            }
        }

        return objects;
    }

    /**
     * Returns an object for a given ObjectId. When an object is not yet registered with
     * this context's ObjectStore, the behavior of this method depends on whether ObjectId
     * is permanent or temporary and whether a DataContext is a part of a nested context
     * hierarchy or not. More specifically the following rules are applied in order:
     * <ul>
     * <li>If a matching registered object is found in this DataContext, it is
     * immediately returned.</li>
     * <li>If a context is nested (i.e. it has another DataContext as its parent
     * channel), an attempt is made to locate a registered object up the hierarchy chain,
     * until it is found. Such object is transferred to this DataContext and returned to
     * the caller.</li>
     * <li>If the ObjectId is temporary, null is returned; if it is permanent, a HOLLOW
     * object (aka fault) is created and returned.</li>
     * </ul>
     * 
     * @deprecated since 1.2 use 'localObject(id, null)'
     */
    public DataObject registeredObject(ObjectId id) {
        return (DataObject) localObject(id, null);
    }

    /**
     * Returns a DataRow reflecting current, possibly uncommitted, object state.
     * <p>
     * <strong>Warning:</strong> This method will return a partial snapshot if an object
     * or one of its related objects that propagate their keys to this object have
     * temporary ids. DO NOT USE this method if you expect a DataRow to represent a
     * complete object state.
     * </p>
     * 
     * @since 1.1
     */
    public DataRow currentSnapshot(DataObject object) {
        ObjEntity entity = getEntityResolver().lookupObjEntity(object);

        // for a HOLLOW object return snapshot from cache
        if (object.getPersistenceState() == PersistenceState.HOLLOW
                && object.getDataContext() != null) {

            ObjectId id = object.getObjectId();
            return getObjectStore().getSnapshot(id, this.getChannel());
        }

        DataRow snapshot = new DataRow(10);

        Iterator attributes = entity.getAttributeMap().entrySet().iterator();
        while (attributes.hasNext()) {
            Map.Entry entry = (Map.Entry) attributes.next();
            String attrName = (String) entry.getKey();
            ObjAttribute objAttr = (ObjAttribute) entry.getValue();

            // processing compound attributes correctly
            snapshot.put(objAttr.getDbAttributePath(), object
                    .readPropertyDirectly(attrName));
        }

        Iterator relationships = entity.getRelationshipMap().entrySet().iterator();
        while (relationships.hasNext()) {
            Map.Entry entry = (Map.Entry) relationships.next();
            ObjRelationship rel = (ObjRelationship) entry.getValue();

            // if target doesn't propagates its key value, skip it
            if (rel.isSourceIndependentFromTargetChange()) {
                continue;
            }

            Object targetObject = object.readPropertyDirectly(rel.getName());
            if (targetObject == null) {
                continue;
            }

            // if target is Fault, get id attributes from stored snapshot
            // to avoid unneeded fault triggering
            if (targetObject instanceof Fault) {
                DataRow storedSnapshot = getObjectStore().getSnapshot(
                        object.getObjectId(),
                        getChannel());
                if (storedSnapshot == null) {
                    throw new CayenneRuntimeException(
                            "No matching objects found for ObjectId "
                                    + object.getObjectId()
                                    + ". Object may have been deleted externally.");
                }

                DbRelationship dbRel = (DbRelationship) rel.getDbRelationships().get(0);
                Iterator joins = dbRel.getJoins().iterator();
                while (joins.hasNext()) {
                    DbJoin join = (DbJoin) joins.next();
                    String key = join.getSourceName();
                    snapshot.put(key, storedSnapshot.get(key));
                }

                continue;
            }

            // target is resolved and we have an FK->PK to it,
            // so extract it from target...
            DataObject target = (DataObject) targetObject;
            Map idParts = target.getObjectId().getIdSnapshot();

            // this may happen in uncommitted objects - see the warning in the JavaDoc of
            // this method.
            if (idParts.isEmpty()) {
                continue;
            }

            DbRelationship dbRel = (DbRelationship) rel.getDbRelationships().get(0);
            Map fk = dbRel.srcFkSnapshotWithTargetSnapshot(idParts);
            snapshot.putAll(fk);
        }

        // process object id map
        // we should ignore any object id values if a corresponding attribute
        // is a part of relationship "toMasterPK", since those values have been
        // set above when db relationships where processed.
        Map thisIdParts = object.getObjectId().getIdSnapshot();
        if (thisIdParts != null) {

            // put only those that do not exist in the map
            Iterator idIterator = thisIdParts.entrySet().iterator();
            while (idIterator.hasNext()) {
                Map.Entry entry = (Map.Entry) idIterator.next();
                Object nextKey = entry.getKey();
                if (!snapshot.containsKey(nextKey)) {
                    snapshot.put(nextKey, entry.getValue());
                }
            }
        }

        return snapshot;
    }

    /**
     * Creates a list of DataObjects local to this DataContext from a list of DataObjects
     * coming from a different DataContext. This method is a way to <b>map</b> objects
     * from one context into the other (as opposed to "synchronize"). This means that the
     * state of modified objects will be reflected only if this context is a child of an
     * original DataObject context. If it is a peer or parent, you won't see any
     * uncommitted changes from the original context.
     * <p>
     * Note that the objects in the list do not have to be of the same type or even from
     * the same DataContext.
     * 
     * @since 1.0.3
     * @deprecated since 1.2 - use {@link #localObject(ObjectId, Persistent)} to specify
     *             how each local object should be handled.
     */
    public List localObjects(List objects) {
        List localObjects = new ArrayList(objects.size());

        Iterator it = objects.iterator();
        while (it.hasNext()) {
            DataObject object = (DataObject) it.next();

            if (object == null) {
                throw new CayenneRuntimeException("Null object");
            }

            localObjects.add(localObject(object.getObjectId(), null));
        }

        return localObjects;
    }

    /**
     * Converts a list of data rows to a list of DataObjects.
     * 
     * @since 1.1
     */
    public List objectsFromDataRows(
            ObjEntity entity,
            List dataRows,
            boolean refresh,
            boolean resolveInheritanceHierarchy) {

        return new ObjectResolver(this, entity, refresh, resolveInheritanceHierarchy)
                .synchronizedObjectsFromDataRows(dataRows);
    }

    /**
     * Converts a list of DataRows to a List of DataObject registered with this
     * DataContext. Internally calls
     * {@link #objectsFromDataRows(ObjEntity,List,boolean,boolean)}.
     * 
     * @since 1.1
     * @see DataRow
     * @see DataObject
     */
    public List objectsFromDataRows(
            Class objectClass,
            List dataRows,
            boolean refresh,
            boolean resolveInheritanceHierarchy) {
        ObjEntity entity = this.getEntityResolver().lookupObjEntity(objectClass);

        if (entity == null) {
            throw new CayenneRuntimeException("Unmapped Java class: " + objectClass);
        }

        return objectsFromDataRows(entity, dataRows, refresh, resolveInheritanceHierarchy);
    }

    /**
     * Creates a DataObject from DataRow. This is a convenience shortcut to
     * {@link #objectsFromDataRows(Class,java.util.List,boolean,boolean)}.
     * 
     * @see DataRow
     * @see DataObject
     */
    public DataObject objectFromDataRow(
            Class objectClass,
            DataRow dataRow,
            boolean refresh) {
        List list = objectsFromDataRows(
                objectClass,
                Collections.singletonList(dataRow),
                refresh,
                true);
        return (DataObject) list.get(0);
    }

    /**
     * Instantiates new object and registers it with itself. Object class is determined
     * from ObjEntity. Object class must have a default constructor.
     * <p>
     * <i>Note: preferred way to create new objects is via
     * {@link #createAndRegisterNewObject(Class)}method. It works exactly the same way,
     * but makes the application type-safe. </i>
     * </p>
     * 
     * @see #createAndRegisterNewObject(Class)
     */
    public DataObject createAndRegisterNewObject(String objEntityName) {
        ObjEntity entity = this.getEntityResolver().getObjEntity(objEntityName);

        if (entity == null) {
            throw new IllegalArgumentException("Invalid entity name: " + objEntityName);
        }

        DataObject dataObject;
        try {
            dataObject = (DataObject) entity.getJavaClass().newInstance();
        }
        catch (Exception ex) {
            throw new CayenneRuntimeException("Error instantiating object.", ex);
        }

        registerNewObjectWithEntity(dataObject, entity);
        return dataObject;
    }

    /**
     * Creates and registers new persistent object. This is an ObjectContext version of
     * 'createAndRegisterNewObject'.
     * 
     * @since 1.2
     */
    public Persistent newObject(Class persistentClass) {
        if (persistentClass == null) {
            throw new NullPointerException("Null 'persistentClass'");
        }

        // TODO: only supports DataObject subclasses
        if (!DataObject.class.isAssignableFrom(persistentClass)) {
            throw new IllegalArgumentException(
                    this
                            + ": this implementation of ObjectContext only supports full DataObjects. Class "
                            + persistentClass
                            + " is invalid.");
        }

        return createAndRegisterNewObject(persistentClass);
    }

    /**
     * Instantiates new object and registers it with itself. Object class must have a
     * default constructor.
     * 
     * @since 1.1
     */
    public DataObject createAndRegisterNewObject(Class objectClass) {
        if (objectClass == null) {
            throw new NullPointerException("DataObject class can't be null.");
        }

        ObjEntity entity = getEntityResolver().lookupObjEntity(objectClass);
        if (entity == null) {
            throw new IllegalArgumentException("Class is not mapped with Cayenne: "
                    + objectClass.getName());
        }

        DataObject dataObject = null;
        try {
            dataObject = (DataObject) objectClass.newInstance();
        }
        catch (Exception ex) {
            throw new CayenneRuntimeException("Error instantiating object.", ex);
        }

        registerNewObjectWithEntity(dataObject, entity);
        return dataObject;
    }

    /**
     * Registers a new object (that is not yet persistent) with itself.
     * 
     * @param dataObject new object that we want to make persistent.
     */
    public void registerNewObject(DataObject dataObject) {
        if (dataObject == null) {
            throw new NullPointerException("Can't register null object.");
        }

        // sanity check - maybe already registered
        if (dataObject.getObjectId() != null) {
            if (dataObject.getDataContext() == this) {
                // already registered, just ignore
                return;
            }
            else if (dataObject.getDataContext() != null) {
                throw new IllegalStateException(
                        "DataObject is already registered with another DataContext. Try using 'localObjects()' instead.");
            }
        }

        ObjEntity entity = getEntityResolver().lookupObjEntity(dataObject);
        if (entity == null) {
            throw new IllegalArgumentException(
                    "Can't find ObjEntity for DataObject class: "
                            + dataObject.getClass().getName()
                            + ", class is likely not mapped.");
        }

        registerNewObjectWithEntity(dataObject, entity);
    }

    private void registerNewObjectWithEntity(DataObject dataObject, ObjEntity objEntity) {
        // method is private ... assuming all sanity checks on the DataObject have been
        // performed by the caller depending on the invocation context

        if (dataObject.getObjectId() == null) {
            dataObject.setObjectId(new ObjectId(objEntity.getName()));
        }

        // initialize to-many relationships with a fault
        Iterator it = objEntity.getRelationships().iterator();
        while (it.hasNext()) {
            ObjRelationship rel = (ObjRelationship) it.next();
            if (rel.isToMany()) {
                dataObject.writePropertyDirectly(rel.getName(), Fault.getToManyFault());
            }
        }

        getObjectStore().addObject(dataObject);
        dataObject.setDataContext(this);
        dataObject.setPersistenceState(PersistenceState.NEW);
    }

    /**
     * Unregisters a Collection of DataObjects from the DataContext and the underlying
     * ObjectStore. This operation also unsets DataContext and ObjectId for each object
     * and changes its state to TRANSIENT.
     */
    public void unregisterObjects(Collection dataObjects) {
        getObjectStore().objectsUnregistered(dataObjects);
    }

    /**
     * "Invalidates" a Collection of DataObject. This operation would remove each object's
     * snapshot from cache and change object's state to HOLLOW. On the next access to this
     * object, it will be refetched.
     */
    public void invalidateObjects(Collection dataObjects) {
        getObjectStore().objectsInvalidated(dataObjects);
    }

    /**
     * Schedules all objects in the collection for deletion on the next commit of this
     * DataContext. Object's persistence state is changed to PersistenceState.DELETED;
     * objects related to this object are processed according to delete rules, i.e.
     * relationships can be unset ("nullify" rule), deletion operation is cascaded
     * (cascade rule).
     * <p>
     * <i>"Nullify" delete rule side effect: </i> passing a collection representing
     * to-many relationship with nullify delete rule may result in objects being removed
     * from collection.
     * </p>
     * 
     * @since 1.2
     */
    public void deleteObjects(Collection objects) {
        if (objects.isEmpty()) {
            return;
        }

        // clone object list... this maybe a relationship collection with nullify delete
        // rule, so modifying
        Iterator it = new ArrayList(objects).iterator();
        while (it.hasNext()) {
            DataObject object = (DataObject) it.next();
            deleteObject(object);
        }
    }

    /**
     * Schedules an object for deletion on the next commit of this DataContext. Object's
     * persistence state is changed to PersistenceState.DELETED; objects related to this
     * object are processed according to delete rules, i.e. relationships can be unset
     * ("nullify" rule), deletion operation is cascaded (cascade rule).
     * 
     * @param object a persistent object that we want to delete.
     * @throws DeleteDenyException if a DENY delete rule is applicable for object
     *             deletion.
     * @throws NullPointerException if object is null.
     */
    public void deleteObject(Persistent object) throws DeleteDenyException {
        new DataContextDeleteAction(this).performDelete(object);
    }

    /**
     * Refetches object data for ObjectId. This method is used internally by Cayenne to
     * resolve objects in state <code>PersistenceState.HOLLOW</code>. It can also be
     * used to refresh certain objects.
     * 
     * @throws CayenneRuntimeException if object id doesn't match any records, or if there
     *             is more than one object is fetched.
     */
    public DataObject refetchObject(ObjectId oid) {

        if (oid == null) {
            throw new NullPointerException("Null ObjectId");
        }

        if (oid.isTemporary()) {
            throw new CayenneRuntimeException("Can't refetch ObjectId "
                    + oid
                    + ", as it is a temporary id.");
        }

        synchronized (getObjectStore()) {
            DataObject object = objectStore.getObject(oid);

            // clean up any cached data for this object
            if (object != null) {
                this.invalidateObjects(Collections.singleton(object));
            }
        }

        DataObject object = DataObjectUtils.objectForQuery(this, new SingleObjectQuery(
                oid));

        if (object == null) {
            throw new CayenneRuntimeException(
                    "Refetch failure: no matching objects found for ObjectId " + oid);
        }

        return object;
    }

    /**
     * Returns a DataNode that should handle queries for all DataMap components.
     * 
     * @since 1.1
     * @deprecated since 1.2 DataContext's QueryEngine implementation is replaced by
     *             DataChannel. Use "getParentDataDomain().lookupDataNode(..)".
     */
    public DataNode lookupDataNode(DataMap dataMap) {

        DataDomain domain = getParentDataDomain();
        if (domain == null) {
            throw new CayenneRuntimeException(
                    "DataContext is not attached to a DataDomain ");
        }

        return domain.lookupDataNode(dataMap);
    }

    /**
     * If the parent channel is a DataContext, sends the changes to the parent; if the
     * parent is DataDomain, commits the changes to DB.
     * 
     * @since 1.2
     */
    // TODO: Andrus, 1/19/2006: implement for nested DataContexts
    public void flushChanges() {
        if (getChannel() instanceof DataDomain) {
            commitChanges();
        }
        else {
            throw new CayenneRuntimeException("Implementation pending.");
        }
    }

    /**
     * If the parent channel is a DataContext, reverts local changes to make this context
     * look like the parent, if the parent channel is a DataDomain, reverts all changes.
     * 
     * @since 1.2
     */
    // TODO: Andrus, 1/19/2006: implement for nested DataContexts
    public void revertChanges() {
        if (getChannel() instanceof DataDomain) {
            rollbackChanges();
        }
        else {
            throw new CayenneRuntimeException("Implementation pending.");
        }
    }

    /**
     * Reverts any changes that have occurred to objects registered with DataContext; also
     * performs cascading rollback of all parent DataContexts.
     */
    public void rollbackChanges() {
        getObjectStore().objectsRolledBack();

        if (channel != null) {
            channel.onSync(this, DataChannel.ROLLBACK_SYNC_TYPE, null);
        }
    }

    /**
     * @deprecated Since 1.2, use {@link #commitChanges()} instead.
     */
    public void commitChanges(Level logLevel) throws CayenneRuntimeException {
        commitChanges();
    }

    /**
     * Synchronizes object graph with the database. Executes needed insert, update and
     * delete queries (generated internally).
     */
    public void commitChanges() throws CayenneRuntimeException {
        doCommitChanges();
    }

    /**
     * Commit worker method.
     * 
     * @since 1.2
     */
    GraphDiff doCommitChanges() throws CayenneRuntimeException {
        if (this.getChannel() == null) {
            throw new CayenneRuntimeException(
                    "Cannot commit changes - channel is not set.");
        }

        if (this.getChannel() instanceof DataDomain) {

            // prevent multiple commits occuring simulteneously
            synchronized (getObjectStore()) {

                // TODO: Andrus, 12/13/2005 - in the OPP spirit, PK generation should be
                // done
                // on the DataDomain end and passed back as a diff. At the same time the
                // problem is that PK generation is the only way to detect some phantom
                // modifications, and thus is a part of DataContext precommit - need to
                // resolve this conflict somehow.
                DataContextPrecommitAction precommit = new DataContextPrecommitAction();
                if (!precommit.precommit(this)) {
                    return new CompoundDiff();
                }

                try {
                    // TODO: Andrus, 12/06/2005 - this is a violation of OPP rules, as we
                    // do not pass changes down the stack. Instead this code assumes that
                    // a channel will get them directly from the context.
                    return getChannel().onSync(this, DataChannel.COMMIT_SYNC_TYPE, null);
                }
                // needed to unwrap OptimisticLockExceptions
                catch (CayenneRuntimeException ex) {
                    Throwable unwound = Util.unwindException(ex);

                    if (unwound instanceof CayenneRuntimeException) {
                        throw (CayenneRuntimeException) unwound;
                    }
                    else {
                        throw new CayenneRuntimeException("Commit Exception", unwound);
                    }
                }
            }
        }
        else {
            // TODO: Andrus, 1/19/2006: implement for nested contexts...
            throw new CayenneRuntimeException("Not implemented");
        }
    }

    /**
     * Returns EventManager associated with the ObjectStore.
     * 
     * @since 1.2
     */
    public EventManager getEventManager() {
        return getObjectStore().getEventManager();
    }

    /**
     * An implementation of a {@link DataChannel} method that is used by child contexts to
     * synchronize state with this context. Not intended for direct use.
     * 
     * @since 1.2
     */
    public GraphDiff onSync(ObjectContext context, int syncType, GraphDiff contextChanges) {
        // sync client changes
        switch (syncType) {
            case DataChannel.ROLLBACK_SYNC_TYPE:
                return syncRollback();
            case DataChannel.FLUSH_SYNC_TYPE:
                return syncFlush(contextChanges);
            case DataChannel.COMMIT_SYNC_TYPE:
                return syncCommit(contextChanges);
            default:
                throw new CayenneRuntimeException("Unrecognized SyncMessage type: "
                        + syncType);
        }
    }

    GraphDiff syncRollback() {
        rollbackChanges();
        return new CompoundDiff();
    }

    /**
     * Applies child diff, without returning anything back.
     */
    GraphDiff syncFlush(GraphDiff childDiff) {
        childDiff.apply(new ChildDiffLoader(this));
        return new CompoundDiff();
    }

    /**
     * Applies child diff, and then commits.
     */
    GraphDiff syncCommit(GraphDiff childDiff) {
        childDiff.apply(new ChildDiffLoader(this));
        return doCommitChanges();
    }

    /**
     * Performs a single database select query returning result as a ResultIterator.
     * Returned ResultIterator will provide access to DataRows.
     */
    public ResultIterator performIteratedQuery(Query query) throws CayenneException {
        // note that for now OPP API does not support cursors (aka ResultIterator), so we
        // have to go directly to the DataDomain.
        IteratedSelectObserver observer = new IteratedSelectObserver();
        getParentDataDomain().performQueries(Collections.singletonList(query), observer);
        return observer.getResultIterator();
    }

    /**
     * Executes a query returning a generic response.
     * 
     * @since 1.2
     */
    public QueryResponse performGenericQuery(Query query) {

        query = nonNullDelegate().willPerformGenericQuery(this, query);
        if (query == null) {
            return new BaseResponse();
        }

        if (this.getChannel() == null) {
            throw new CayenneRuntimeException(
                    "Can't run query - parent DataChannel is not set.");
        }

        return onQuery(this, query);
    }

    /**
     * Performs a single selecting query. Various query setting control the behavior of
     * this method and the results returned:
     * <ul>
     * <li>Query caching policy defines whether the results are retrieved from cache or
     * fetched from the database. Note that queries that use caching must have a name that
     * is used as a caching key.</li>
     * <li>Query refreshing policy controls whether to refresh existing data objects and
     * ignore any cached values.</li>
     * <li>Query data rows policy defines whether the result should be returned as
     * DataObjects or DataRows.</li>
     * </ul>
     * <p>
     * <i>Since 1.2 takes any Query parameter, not just GenericSelectQuery</i>
     * </p>
     * 
     * @return A list of DataObjects or a DataRows, depending on the value returned by
     *         {@link QueryMetadata#isFetchingDataRows()}.
     */
    public List performQuery(Query query) {
        query = nonNullDelegate().willPerformQuery(this, query);
        if (query == null) {
            return new ArrayList(1);
        }

        // this filter should go away when we remove deprecated api from the Delegate.
        query = filterThroughDelegateDeprecated(query);
        if (query == null) {
            return new ArrayList(1);
        }

        List result = onQuery(this, query).firstList();
        return result != null ? result : new ArrayList(1);
    }

    /**
     * An implementation of a {@link DataChannel} method that is used by child contexts to
     * execute queries. Not intended for direct use.
     * 
     * @since 1.2
     */
    public QueryResponse onQuery(ObjectContext context, Query query) {
        return new DataContextQueryAction(this, context, query).execute();
    }

    /**
     * Performs a single database query that does not select rows. Returns an array of
     * update counts.
     * 
     * @since 1.1
     */
    public int[] performNonSelectingQuery(Query query) {
        int[] count = performGenericQuery(query).firstUpdateCount();
        return count != null ? count : new int[0];
    }

    /**
     * Performs a named mapped query that does not select rows. Returns an array of update
     * counts.
     * 
     * @since 1.1
     */
    public int[] performNonSelectingQuery(String queryName) {
        return performNonSelectingQuery(new NamedQuery(queryName));
    }

    /**
     * Performs a named mapped non-selecting query using a map of parameters. Returns an
     * array of update counts.
     * 
     * @since 1.1
     */
    public int[] performNonSelectingQuery(String queryName, Map parameters) {
        return performNonSelectingQuery(new NamedQuery(queryName, parameters));
    }

    /**
     * Executes all queries in collection.
     * 
     * @deprecated since 1.2 DataContext's QueryEngine implementation is replaced by
     *             DataChannel.
     */
    public void performQueries(Collection queries, OperationObserver callback) {

        // filter through the delegate

        List finalQueries = new ArrayList(queries.size());

        Iterator it = queries.iterator();
        while (it.hasNext()) {
            Query query = (Query) it.next();

            query = filterThroughDelegateDeprecated(query);

            if (query != null) {
                finalQueries.add(query);
            }
        }

        if (!finalQueries.isEmpty()) {
            getParentDataDomain().performQueries(queries, callback);
        }
    }

    /**
     * @since 1.2
     * @deprecated since 1.2
     */
    // deprecated code is extracted in a separate method to avoid Eclipse warnings...
    Query filterThroughDelegateDeprecated(Query query) {
        if (query instanceof org.objectstyle.cayenne.query.GenericSelectQuery) {
            org.objectstyle.cayenne.query.GenericSelectQuery genericSelect = (org.objectstyle.cayenne.query.GenericSelectQuery) query;
            return nonNullDelegate().willPerformSelect(this, genericSelect);
        }
        return query;
    }

    /**
     * Binds provided transaction to the current thread, and then runs queries.
     * 
     * @since 1.1
     * @deprecated since 1.2. Use Transaction.bindThreadTransaction(..) to provide custom
     *             transactions, besides DataContext's QueryEngine implementation is
     *             replaced by DataChannel.
     */
    public void performQueries(
            Collection queries,
            OperationObserver callback,
            Transaction transaction) {

        Transaction.bindThreadTransaction(transaction);

        try {
            performQueries(queries, callback);
        }
        finally {
            Transaction.bindThreadTransaction(null);
        }
    }

    /**
     * Performs prefetching. Prefetching would resolve a set of relationships for a list
     * of DataObjects in the most optimized way (preferrably in a single query per
     * relationship).
     * <p>
     * <i>WARNING: Currently supports only "one-step" to one relationships. This is an
     * arbitrary limitation and will be removed eventually. </i>
     * </p>
     * 
     * @deprecated Since 1.2. This is a utility method that handles a very specific case.
     *             It shouldn't be in DataContext.
     */
    public void prefetchRelationships(SelectQuery query, List objects) {
        QueryMetadata metadata = query.getMetaData(getEntityResolver());
        Collection prefetches = metadata.getPrefetchTree() != null ? query
                .getPrefetchTree()
                .nonPhantomNodes() : Collections.EMPTY_LIST;

        if (objects == null || objects.size() == 0 || prefetches.size() == 0) {
            return;
        }

        ObjEntity entity = metadata.getObjEntity();
        Iterator prefetchesIt = prefetches.iterator();
        while (prefetchesIt.hasNext()) {
            PrefetchTreeNode prefetch = (PrefetchTreeNode) prefetchesIt.next();
            String path = prefetch.getPath();
            if (path.indexOf('.') >= 0) {
                throw new CayenneRuntimeException("Only one-step relationships are "
                        + "supported at the moment, this will be fixed soon. "
                        + "Unsupported path : "
                        + path);
            }

            ObjRelationship relationship = (ObjRelationship) entity.getRelationship(path);
            if (relationship == null) {
                throw new CayenneRuntimeException("Invalid relationship: " + path);
            }

            if (relationship.isToMany()) {
                throw new CayenneRuntimeException(
                        "Only to-one relationships are supported at the moment. "
                                + "Can't prefetch to-many: "
                                + path);
            }

            org.objectstyle.cayenne.access.util.PrefetchHelper.resolveToOneRelations(
                    this,
                    objects,
                    path);
        }
    }

    /**
     * Returns a list of objects or DataRows for a named query stored in one of the
     * DataMaps. Internally Cayenne uses a caching policy defined in the named query. If
     * refresh flag is true, a refresh is forced no matter what the caching policy is.
     * 
     * @param queryName a name of a GenericSelectQuery defined in one of the DataMaps. If
     *            no such query is defined, this method will throw a
     *            CayenneRuntimeException.
     * @param refresh A flag that determines whether refresh is required in case a query
     *            uses caching.
     * @since 1.1
     */
    public List performQuery(String queryName, boolean refresh) {
        return performQuery(queryName, Collections.EMPTY_MAP, refresh);
    }

    /**
     * Returns a list of objects or DataRows for a named query stored in one of the
     * DataMaps. Internally Cayenne uses a caching policy defined in the named query. If
     * refresh flag is true, a refresh is forced no matter what the caching policy is.
     * 
     * @param queryName a name of a GenericSelectQuery defined in one of the DataMaps. If
     *            no such query is defined, this method will throw a
     *            CayenneRuntimeException.
     * @param parameters A map of parameters to use with stored query.
     * @param refresh A flag that determines whether refresh is required in case a query
     *            uses caching.
     * @since 1.1
     */
    public List performQuery(String queryName, Map parameters, boolean refresh) {
        NamedQuery query = new NamedQuery(queryName, parameters);
        query.setRefreshOverride(refresh ? Boolean.TRUE : Boolean.FALSE);
        return performQuery(query);
    }

    /**
     * Returns EntityResolver. EntityResolver can be null if DataContext has not been
     * attached to an DataChannel.
     */
    public EntityResolver getEntityResolver() {
        awakeFromDeserialization();
        return entityResolver;
    }

    /**
     * Sets default for posting transaction events by new DataContexts.
     */
    public static void setTransactionEventsEnabledDefault(boolean flag) {
        transactionEventsEnabledDefault = flag;
    }

    /**
     * Enables or disables posting of transaction events by this DataContext.
     */
    public void setTransactionEventsEnabled(boolean flag) {
        this.transactionEventsEnabled = flag;
    }

    public boolean isTransactionEventsEnabled() {
        return this.transactionEventsEnabled;
    }

    /**
     * Returns <code>true</code> if the ObjectStore uses shared cache of a parent
     * DataDomain.
     * 
     * @since 1.1
     */
    public boolean isUsingSharedSnapshotCache() {
        return usingSharedSnaphsotCache;
    }

    /**
     * Returns whether this DataContext performs object validation before commit is
     * executed.
     * 
     * @since 1.1
     */
    public boolean isValidatingObjectsOnCommit() {
        return validatingObjectsOnCommit;
    }

    /**
     * Sets the property defining whether this DataContext should perform object
     * validation before commit is executed.
     * 
     * @since 1.1
     */
    public void setValidatingObjectsOnCommit(boolean flag) {
        this.validatingObjectsOnCommit = flag;
    }

    /**
     * @deprecated since 1.2. Use 'getEntityResolver().getDataMaps()' instead.
     */
    public Collection getDataMaps() {
        return (getEntityResolver() != null)
                ? getEntityResolver().getDataMaps()
                : Collections.EMPTY_LIST;
    }

    void fireWillCommit() {
        // post event: WILL_COMMIT
        if (this.transactionEventsEnabled) {
            DataContextEvent commitChangesEvent = new DataContextEvent(this);
            getObjectStore().getEventManager().postEvent(
                    commitChangesEvent,
                    DataContext.WILL_COMMIT);
        }
    }

    void fireTransactionRolledback() {
        // post event: DID_ROLLBACK
        if ((this.transactionEventsEnabled)) {
            DataContextEvent commitChangesEvent = new DataContextEvent(this);
            getObjectStore().getEventManager().postEvent(
                    commitChangesEvent,
                    DataContext.DID_ROLLBACK);
        }
    }

    void fireTransactionCommitted() {
        // post event: DID_COMMIT
        if ((this.transactionEventsEnabled)) {
            DataContextEvent commitChangesEvent = new DataContextEvent(this);
            getObjectStore().getEventManager().postEvent(
                    commitChangesEvent,
                    DataContext.DID_COMMIT);
        }
    }

    // ---------------------------------------------
    // Serialization Support
    // ---------------------------------------------

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();

        // If the "parent" of this datacontext is a DataDomain, then just write the
        // name of it. Then when deserialization happens, we can get back the DataDomain
        // by name, from the shared configuration (which will either load it if need be,
        // or return an existing one.

        if (this.channel == null && this.lazyInitParentDomainName != null) {
            out.writeObject(lazyInitParentDomainName);
        }
        else if (this.channel instanceof DataDomain) {
            DataDomain domain = (DataDomain) this.channel;
            out.writeObject(domain.getName());
        }
        else {
            // Hope that whatever this.parent is, that it is Serializable
            out.writeObject(this.channel);
        }

        // Serialize local snapshots cache
        if (!isUsingSharedSnapshotCache()) {
            out.writeObject(objectStore.getDataRowCache());
        }
    }

    // serialization support
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {

        // 1. read non-transient properties
        in.defaultReadObject();

        // 2. read parent or its name
        Object value = in.readObject();
        if (value instanceof DataChannel) {
            // A real QueryEngine object - use it
            this.channel = (DataChannel) value;
        }
        else if (value instanceof String) {
            // The name of a DataDomain - use it
            this.lazyInitParentDomainName = (String) value;
        }
        else {
            throw new CayenneRuntimeException(
                    "Parent attribute of DataContext was neither a QueryEngine nor "
                            + "the name of a valid DataDomain:"
                            + value);
        }

        // 3. Deserialize local snapshots cache
        if (!isUsingSharedSnapshotCache()) {
            DataRowStore cache = (DataRowStore) in.readObject();
            objectStore.setDataRowCache(cache);
        }
        else {
            // configure ObjectStore to do deferred initialization
            objectStore.setDataRowCacheFactory(new Factory() {

                public Object create() {
                    DataDomain domain = getParentDataDomain();
                    return (domain != null) ? domain.getSharedSnapshotCache() : null;
                }
            });
        }

        // CayenneDataObjects have a transient datacontext
        // because at deserialize time the datacontext may need to be different
        // than the one at serialize time (for programmer defined reasons).
        // So, when a dataobject is resurrected because it's datacontext was
        // serialized, it will then set the objects datacontext to the correctone
        // If deserialized "otherwise", it will not have a datacontext (good)

        synchronized (getObjectStore()) {
            Iterator it = objectStore.getObjectIterator();
            while (it.hasNext()) {
                DataObject object = (DataObject) it.next();
                object.setDataContext(this);
            }
        }
    }

    // Re-attaches itself to the parent domain with previously stored name.
    //
    // TODO: Andrus 11/7/2005 - this is one of the places where Cayenne
    // serialization relies on shared config... This is bad. We need some
    // sort of thread-local solution that would allow to use an alternative configuration.
    //
    private final void awakeFromDeserialization() {
        if (channel == null && lazyInitParentDomainName != null) {

            // call a setter to ensure EntityResolver is extracted from channel
            setChannel(Configuration.getSharedConfiguration().getDomain(
                    lazyInitParentDomainName));
        }
    }

    /**
     * Resolves object fault if needed. If a property is not null, it is assumed that the
     * object will be modified, so object snapshot is retained and object state is
     * changed.
     * 
     * @since 1.2
     */
    public void prepareForAccess(Persistent object, String property) {

        if (object.getPersistenceState() == PersistenceState.HOLLOW) {

            if (!(object instanceof DataObject)) {
                throw new CayenneRuntimeException("Can only resolve DataObjects. Got: "
                        + object);
            }

            getObjectStore().resolveHollow((DataObject) object);
            if (object.getPersistenceState() != PersistenceState.COMMITTED) {
                throw new FaultFailureException(
                        "Error resolving fault, no matching row exists in the database for ObjectId: "
                                + object.getObjectId());
            }
        }
    }

    /**
     * Retains DataObject snapshot and changes its state if needed.
     * 
     * @since 1.2
     */
    public void propertyChanged(
            Persistent object,
            String property,
            Object oldValue,
            Object newValue) {

        if (object.getPersistenceState() == PersistenceState.COMMITTED) {

            if (!(object instanceof DataObject)) {
                throw new CayenneRuntimeException("Can only handle DataObjects. Got: "
                        + object);
            }

            object.setPersistenceState(PersistenceState.MODIFIED);
            getObjectStore().retainSnapshot((DataObject) object);
        }
    }

    /**
     * Returns a GraphManager decorator of the ObjectStore.
     * 
     * @since 1.2
     */
    public GraphManager getGraphManager() {
        if (graphManager == null) {
            graphManager = new DataContextGraphManager(this);
        }

        return graphManager;
    }

    /**
     * Returns an object local to this DataContext and matching the ObjectId. If
     * <code>prototype</code> is not null, local object is refreshed with the prototype
     * values.
     * <p>
     * Note that this method may have a side effect on the prototype object. If a
     * prototype object is not registered with any ObjectContext at all, it will be
     * registered with this context. This prevents an unneeded copy operation when objects
     * are served from a channel that is not a context itself. In this case the state of
     * the prototype object is set to NEW if the ObjectId is temporary or COMMITTED
     * otherwise.
     * </p>
     * 
     * @since 1.2
     */
    public Persistent localObject(ObjectId id, Persistent prototype) {

        // ****** Warning: when changing the code below, don't forget to change
        // CayenneContext's implementation which right now relies on copy/paste "reuse"

        if (id == null) {
            throw new IllegalArgumentException("Null ObjectId");
        }

        // note that per-object ClassDescriptor lookup is needed as even if all
        // objects where fetched as a part of the same query, as they may belong to
        // different subclasses
        ClassDescriptor descriptor = getEntityResolver().getClassDescriptor(
                id.getEntityName());

        Persistent cachedObject = (Persistent) getGraphManager().getNode(id);

        // merge into an existing object
        if (cachedObject != null) {

            // TODO: Andrus, 1/24/2006 implement smart merge for modified objects...
            if (cachedObject != prototype
                    && cachedObject.getPersistenceState() != PersistenceState.MODIFIED
                    && cachedObject.getPersistenceState() != PersistenceState.DELETED) {

                if (prototype != null) {
                    descriptor.injectValueHolders(cachedObject);
                    descriptor.shallowMerge(prototype, cachedObject);
                }
            }

            return cachedObject;
        }
        // create and merge into a new object
        else {

            // Andrus, 1/26/2006 - note that there is a tricky case of a temporary object
            // passed from peer DataContext... In the past we used to throw an exception
            // or return null. Now that we can have a valid (but generally
            // indistinguishible) case of such object passed from parent, we let it
            // slip... Not sure what's the best way of handling it that does not involve
            // breaking encapsulation of the DataChannel to detect where in the hierarchy
            // this context is.

            Persistent localObject = (Persistent) descriptor.createObject();

            localObject.setObjectContext(this);
            localObject.setObjectId(id);

            getGraphManager().registerNode(id, localObject);

            if (prototype != null
                    && prototype.getPersistenceState() != PersistenceState.HOLLOW) {
                localObject.setPersistenceState(PersistenceState.COMMITTED);
                descriptor.injectValueHolders(localObject);
                descriptor.shallowMerge(prototype, localObject);
            }
            else {
                localObject.setPersistenceState(PersistenceState.HOLLOW);
            }

            return localObject;
        }
    }
}