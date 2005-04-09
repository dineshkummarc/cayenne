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
package org.objectstyle.cayenne.map;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.objectstyle.cayenne.map.event.AttributeEvent;
import org.objectstyle.cayenne.map.event.DbEntityListener;
import org.objectstyle.cayenne.map.event.DbAttributeListener;
import org.objectstyle.cayenne.map.event.DbRelationshipListener;
import org.objectstyle.cayenne.map.event.EntityEvent;
import org.objectstyle.cayenne.map.event.ObjEntityListener;
import org.objectstyle.cayenne.map.event.ObjAttributeListener;
import org.objectstyle.cayenne.map.event.ObjRelationshipListener;
import org.objectstyle.cayenne.map.event.RelationshipEvent;
import org.objectstyle.cayenne.project.Project;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.util.CayenneMap;
import org.objectstyle.cayenne.util.Util;
import org.objectstyle.cayenne.util.XMLEncoder;
import org.objectstyle.cayenne.util.XMLSerializable;

/**
 * Stores a collection of related mapping objects that describe database and object layers
 * of an application. DataMap contains DbEntities mapping database tables, ObjEntities -
 * mapping persistent Java classes, Procedures - mapping database stored procedures.
 * 
 * @author Michael Shengaout
 * @author Andrei Adamchik
 * @author Craig Miskell
 */
public class DataMap implements XMLSerializable, MappingNamespace, 
								DbEntityListener, DbAttributeListener, DbRelationshipListener, 
								ObjEntityListener, ObjAttributeListener, ObjRelationshipListener
{

    /**
     * Defines the name of the property for default DB schema.
     * 
     * @since 1.1
     */
    public static final String DEFAULT_SCHEMA_PROPERTY = "defaultSchema";

    /**
     * Defines the name of the property for default Java class package.
     * 
     * @since 1.1
     */
    public static final String DEFAULT_PACKAGE_PROPERTY = "defaultPackage";

    /**
     * Defines the name of the property for default DB schema.
     * 
     * @since 1.1
     */
    public static final String DEFAULT_SUPERCLASS_PROPERTY = "defaultSuperclass";

    /**
     * Defines the name of the property for default DB schema.
     * 
     * @since 1.1
     */
    public static final String DEFAULT_LOCK_TYPE_PROPERTY = "defaultLockType";

    protected String name;
    protected String location;
    protected MappingNamespace namespace;

    protected String defaultSchema;
    protected String defaultPackage;
    protected String defaultSuperclass;
    protected int defaultLockType;

    // ====================================================
    // ObjEntities
    // ====================================================
    private CayenneMap objEntityMap = new CayenneMap(this);

    // read-through reference for public access
    private SortedMap objEntityMapRef = Collections.unmodifiableSortedMap(objEntityMap);

    // read-through reference for public access to the ObjEntities
    private Collection objEntityValuesRef = Collections
            .unmodifiableCollection(objEntityMap.values());

    // ====================================================
    // DbEntities
    // ====================================================
    private CayenneMap dbEntityMap = new CayenneMap(this);

    // read-through reference for public access
    private SortedMap dbEntityMapRef = Collections.unmodifiableSortedMap(dbEntityMap);

    // read-through reference for public access
    private Collection dbEntityValuesRef = Collections.unmodifiableCollection(dbEntityMap
            .values());

    // ====================================================
    // Procedures
    // ====================================================
    private CayenneMap procedureMap = new CayenneMap(this);

    // read-through reference for public access
    private SortedMap procedureMapRef = Collections.unmodifiableSortedMap(procedureMap);

    // read-through reference for public access
    private Collection procedureValuesRef = Collections
            .unmodifiableCollection(procedureMap.values());

    // ====================================================
    // Queries
    // ====================================================
    private CayenneMap queryMap = new CayenneMap(this);

    // read-through reference for public access
    private SortedMap queryMapRef = Collections.unmodifiableSortedMap(queryMap);

    // read-through reference for public access
    private Collection queriesValuesRef = Collections.unmodifiableCollection(queryMap
            .values());

    /**
     * Creates a new DataMap.
     */
    public DataMap() {
        // must do this to setup defaults
        initWithProperties(Collections.EMPTY_MAP);
    }

    /**
     * Creates a new named DataMap.
     */
    public DataMap(String mapName) {
        this();
        this.setName(mapName);
    }

    /**
     * Performs DataMap initialization from a set of properties, using defaults for the
     * missing properties.
     * 
     * @since 1.1
     */
    public void initWithProperties(Map properties) {
        // must init defaults even if properties are empty
        if (properties == null) {
            properties = Collections.EMPTY_MAP;
        }

        Object lockType = properties.get(DEFAULT_LOCK_TYPE_PROPERTY);
        Object packageName = properties.get(DEFAULT_PACKAGE_PROPERTY);
        Object schema = properties.get(DEFAULT_SCHEMA_PROPERTY);
        Object superclass = properties.get(DEFAULT_SUPERCLASS_PROPERTY);

        this.defaultLockType = "optimistic".equals(lockType)
                ? ObjEntity.LOCK_TYPE_OPTIMISTIC
                : ObjEntity.LOCK_TYPE_NONE;

        this.defaultPackage = (packageName != null) ? packageName.toString() : null;
        this.defaultSchema = (schema != null) ? schema.toString() : null;
        this.defaultSuperclass = (superclass != null) ? superclass.toString() : null;
    }

    /**
     * Prints itself as a well-formed complete XML document. In comparison,
     * {@link #encodeAsXML(XMLEncoder)}stores DataMap assuming it is a part of a bigger
     * document.
     * 
     * @since 1.1
     */
    public void encodeAsXML(PrintWriter pw) {
        XMLEncoder encoder = new XMLEncoder(pw, "\t");
        encoder.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        encodeAsXML(encoder);
    }

    /**
     * Prints itself as XML to the provided PrintWriter.
     * 
     * @since 1.1
     */
    public void encodeAsXML(XMLEncoder encoder) {
        encoder.print("<data-map project-version=\"");
        encoder.print(String.valueOf(Project.CURRENT_PROJECT_VERSION));
        encoder.println("\">");

        encoder.indent(1);

        // properties
        if (defaultLockType == ObjEntity.LOCK_TYPE_OPTIMISTIC) {
            encoder.printProperty(DEFAULT_LOCK_TYPE_PROPERTY, "optimistic");
        }

        if (!Util.isEmptyString(defaultPackage)) {
            encoder.printProperty(DEFAULT_PACKAGE_PROPERTY, defaultPackage);
        }

        if (!Util.isEmptyString(defaultSchema)) {
            encoder.printProperty(DEFAULT_SCHEMA_PROPERTY, defaultSchema);
        }

        if (!Util.isEmptyString(defaultSuperclass)) {
            encoder.printProperty(DEFAULT_SUPERCLASS_PROPERTY, defaultSuperclass);
        }

        // procedures
        encoder.print(getProcedureMap());

        // DbEntities
        boolean hasDerived = false;
        Iterator dbEntities = getDbEntityMap().entrySet().iterator();
        while (dbEntities.hasNext()) {
            Map.Entry entry = (Map.Entry) dbEntities.next();
            DbEntity dbe = (DbEntity) entry.getValue();

            // skip derived, store them after regular DbEntities
            if (dbe instanceof DerivedDbEntity) {
                hasDerived = true;
            }
            else {
                dbe.encodeAsXML(encoder);
            }
        }

        // DerivedDbEntities
        if (hasDerived) {
            Iterator derivedDbEntities = getDbEntityMap().entrySet().iterator();
            while (derivedDbEntities.hasNext()) {
                Map.Entry entry = (Map.Entry) derivedDbEntities.next();
                DbEntity dbe = (DbEntity) entry.getValue();

                // only store derived...
                if (dbe instanceof DerivedDbEntity) {
                    dbe.encodeAsXML(encoder);
                }
            }
        }

        // others...
        encoder.print(getObjEntityMap());
        encodeDBRelationshipsAsXML(getDbEntityMap(), encoder);
        encodeOBJRelationshipsAsXML(getObjEntityMap(), encoder);
        encoder.print(getQueryMap());

        encoder.indent(-1);
        encoder.println("</data-map>");
    }

    // stores relationships of for the map of entities
    private final void encodeDBRelationshipsAsXML(Map entityMap, XMLEncoder encoder) {
        Iterator it = entityMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            Entity entity = (Entity) entry.getValue();
            encoder.print(entity.getRelationships());
        }
    }

    // stores relationships of for the map of entities
    private final void encodeOBJRelationshipsAsXML(Map entityMap, XMLEncoder encoder) {
        Iterator it = entityMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            ObjEntity entity = (ObjEntity) entry.getValue();
            encoder.print(entity.getDeclaredRelationships());
        }
    }

    public String toString() {
        ToStringBuilder builder = new ToStringBuilder(this);
        builder.append(getName());
        return builder.toString();
    }

    /**
     * Returns "name" property value.
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = (name != null ? name : "unnamed");
    }

    /**
     * Adds all Object and DB entities and Queries from another map to this map. Overwrites all
     * existing entities and queries with the new ones.
     * <p>
     * <i>TODO: will need to implement advanced merge that allows different policies for
     * overwriting entities / queries. </i>
     * </p>
     */
    public void mergeWithDataMap(DataMap map) {
        Iterator dbs = new ArrayList(map.getDbEntities()).iterator();
        while (dbs.hasNext()) {
            DbEntity ent = (DbEntity) dbs.next();
            this.removeDbEntity(ent.getName());
            this.addDbEntity(ent);
        }

        Iterator objs = new ArrayList(map.getObjEntities()).iterator();
        while (objs.hasNext()) {
            ObjEntity ent = (ObjEntity) objs.next();
            this.removeObjEntity(ent.getName());
            this.addObjEntity(ent);
        }
        
        Iterator queries = new ArrayList(map.getQueries()).iterator();
        while (queries.hasNext()) {
            Query query = (Query) queries.next();
            this.removeQuery(query.getName());
            this.addQuery(query);
        }
    }

    /**
     * Returns "location" property value. Location is abstract and can depend on how the
     * DataMap was loaded. E.g. location can be a File on the filesystem or a location
     * within a JAR.
     */
    public String getLocation() {
        return location;
    }

    /**
     * Sets "location" property.
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Returns a sorted unmodifiable map of ObjEntities contained in this DataMap, keyed
     * by ObjEntity name.
     */
    public SortedMap getObjEntityMap() {
        return objEntityMapRef;
    }

    /**
     * Returns a sorted unmodifiable map of DbEntities contained in this DataMap, keyed by
     * DbEntity name.
     */
    public SortedMap getDbEntityMap() {
        return dbEntityMapRef;
    }

    /**
     * Returns a named query associated with this DataMap.
     * 
     * @since 1.1
     */
    public Query getQuery(String queryName) {
        Query query = (Query) queryMap.get(queryName);
        if (query != null) {
            return query;
        }

        return namespace != null ? namespace.getQuery(queryName) : null;
    }

    /**
     * Stores a query under its name.
     * 
     * @since 1.1
     */
    public void addQuery(Query query) {
        if (query == null) {
            throw new NullPointerException("Can't add null query.");
        }

        if (query.getName() == null) {
            throw new NullPointerException("Query name can't be null.");
        }

        queryMap.put(query.getName(), query);
    }

    /**
     * Removes a named query from the DataMap.
     * 
     * @since 1.1
     */
    public void removeQuery(String queryName) {
        queryMap.remove(queryName);
    }

    /**
     * @since 1.1
     */
    public void clearQueries() {
        queryMap.clear();
    }

    /**
     * @since 1.1
     */
    public SortedMap getQueryMap() {
        return queryMapRef;
    }

    /**
     * @since 1.1
     */
    public Collection getQueries() {
        return queriesValuesRef;
    }

    /**
     * Adds a new ObjEntity to this DataMap.
     */
    public void addObjEntity(ObjEntity objEntity) {
        if (objEntity.getName() == null) {
            throw new NullPointerException("Attempt to add ObjEntity with no name.");
        }

        objEntityMap.put(objEntity.getName(), objEntity);
    }

    /**
     * Adds a new DbEntity to this DataMap.
     */
    public void addDbEntity(DbEntity dbEntity) {
        if (dbEntity.getName() == null) {
            throw new NullPointerException("Attempt to add DbEntity with no name.");
        }

        dbEntityMap.put(dbEntity.getName(), dbEntity);
    }

    /**
     * Returns a list of ObjEntities stored in this DataMap.
     */
    public Collection getObjEntities() {
        return objEntityValuesRef;
    }

    /**
     * Returns all ObjEntities in this DataMap, including entities from dependent maps if
     * <code>includeDeps</code> is <code>true</code>.
     * 
     * @deprecated since 1.2 use getObjEntities().
     */
    public Collection getObjEntities(boolean includeDeps) {
        return getObjEntities();
    }

    /**
     * Returns all DbEntities in this DataMap.
     */
    public Collection getDbEntities() {
        return dbEntityValuesRef;
    }

    /**
     * Returns DbEntity matching the <code>name</code> parameter. No dependencies will
     * be searched.
     */
    public DbEntity getDbEntity(String dbEntityName) {
        DbEntity entity = (DbEntity) dbEntityMap.get(dbEntityName);

        if (entity != null) {
            return entity;
        }

        return namespace != null ? namespace.getDbEntity(dbEntityName) : null;
    }

    /**
     * Returns an ObjEntity for a DataObject class name.
     * 
     * @since 1.1
     */
    public ObjEntity getObjEntityForJavaClass(String javaClassName) {
        if (javaClassName == null) {
            return null;
        }

        Iterator it = getObjEntityMap().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            ObjEntity entity = (ObjEntity) entry.getValue();
            if (javaClassName.equals(entity.getClassName())) {
                return entity;
            }
        }

        return null;
    }

    /**
     * Returns an ObjEntity for a given name. If it is not found in this DataMap, it will
     * search a parent EntityNamespace.
     */
    public ObjEntity getObjEntity(String objEntityName) {
        ObjEntity entity = (ObjEntity) objEntityMap.get(objEntityName);
        if (entity != null) {
            return entity;
        }

        return namespace != null ? namespace.getObjEntity(objEntityName) : null;
    }

    /**
     * Returns a list of ObjEntities mapped to the given DbEntity.
     */
    public Collection getMappedEntities(DbEntity dbEntity) {
        if (dbEntity == null) {
            return Collections.EMPTY_LIST;
        }

        Collection allEntities = (namespace != null)
                ? namespace.getObjEntities()
                : objEntityValuesRef;

        if (allEntities.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        Collection result = new ArrayList();
        Iterator iter = allEntities.iterator();
        while (iter.hasNext()) {
            ObjEntity objEnt = (ObjEntity) iter.next();
            if (objEnt.getDbEntity() == dbEntity) {
                result.add(objEnt);
            }
        }

        return result;
    }

    /**
     * "Dirty" remove of the DbEntity from the data map.
     */
    public void removeDbEntity(String dbEntityName) {
        removeDbEntity(dbEntityName, false);
    }

    /**
     * Removes DbEntity from the DataMap. If <code>clearDependencies</code> is true, all
     * DbRelationships that reference this entity are also removed. ObjEntities that rely
     * on this entity are cleaned up.
     * 
     * @since 1.1
     */
    public void removeDbEntity(String dbEntityName, boolean clearDependencies) {
        DbEntity dbEntityToDelete = (DbEntity) dbEntityMap.remove(dbEntityName);

        if (dbEntityToDelete != null && clearDependencies) {
            Iterator dbEnts = this.getDbEntities().iterator();
            while (dbEnts.hasNext()) {
                DbEntity dbEnt = (DbEntity) dbEnts.next();
                // take a copy since we're going to modifiy the entity
                Iterator rels = new ArrayList(dbEnt.getRelationships()).iterator();
                while (rels.hasNext()) {
                    DbRelationship rel = (DbRelationship) rels.next();
                    if (dbEntityName.equals(rel.getTargetEntityName())) {
                        dbEnt.removeRelationship(rel.getName());
                    }
                }
            }

            // Remove all obj relationships referencing removed DbRelationships.
            Iterator objEnts = this.getObjEntities().iterator();
            while (objEnts.hasNext()) {
                ObjEntity objEnt = (ObjEntity) objEnts.next();
                if (objEnt.getDbEntity() == dbEntityToDelete) {
                    objEnt.clearDbMapping();
                }
                else {
                    Iterator iter = objEnt.getRelationships().iterator();
                    while (iter.hasNext()) {
                        ObjRelationship rel = (ObjRelationship) iter.next();
                        Iterator dbRels = rel.getDbRelationships().iterator();
                        while (dbRels.hasNext()) {
                            DbRelationship dbRel = (DbRelationship) dbRels.next();
                            if (dbRel.getTargetEntity() == dbEntityToDelete) {
                                rel.clearDbRelationships();
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * "Dirty" remove of the ObjEntity from the data map.
     */
    public void removeObjEntity(String objEntityName) {
        removeObjEntity(objEntityName, false);
    }

    /**
     * Removes ObjEntity from the DataMap. If <code>clearDependencies</code> is true,
     * all ObjRelationships that reference this entity are also removed.
     * 
     * @since 1.1
     */
    public void removeObjEntity(String objEntityName, boolean clearDependencies) {
        ObjEntity entity = (ObjEntity) objEntityMap.remove(objEntityName);

        if (entity != null && clearDependencies) {
            Iterator entities = this.getObjEntityMap().values().iterator();
            while (entities.hasNext()) {
                ObjEntity ent = (ObjEntity) entities.next();
                // take a copy since we're going to modifiy the entity
                Iterator rels = new ArrayList(ent.getRelationships()).iterator();
                while (rels.hasNext()) {
                    ObjRelationship rel = (ObjRelationship) rels.next();
                    if (objEntityName.equals(rel.getTargetEntityName())
                            || objEntityName.equals(rel.getTargetEntityName())) {
                        ent.removeRelationship(rel.getName());
                    }
                }
            }
        }
    }

    /**
     * Returns stored procedures associated with this DataMap.
     */
    public Collection getProcedures() {
        return procedureValuesRef;
    }

    /**
     * Returns a Procedure for a given name or null if no such procedure exists. If
     * Procedure is not found in this DataMap, a parent EntityNamcespace is searched.
     */
    public Procedure getProcedure(String procedureName) {
        Procedure procedure = (Procedure) procedureMap.get(procedureName);
        if (procedure != null) {
            return procedure;
        }

        return namespace != null ? namespace.getProcedure(procedureName) : null;
    }

    /**
     * Adds stored procedure to the list of procedures. If there is another procedure
     * registered under the same name, throws an IllegalArgumentException.
     */
    public void addProcedure(Procedure procedure) {
        if (procedure.getName() == null) {
            throw new NullPointerException("Attempt to add procedure with no name.");
        }

        procedureMap.put(procedure.getName(), procedure);
    }

    public void removeProcedure(String name) {
        procedureMap.remove(name);
    }

    /**
     * Returns a sorted unmodifiable map of Procedures in this DataMap keyed by name.
     */
    public SortedMap getProcedureMap() {
        return procedureMapRef;
    }

    /**
     * Returns a parent namespace where this DataMap resides. Parent EntityNamespace is
     * used to establish relationships with entities in other DataMaps.
     * 
     * @since 1.1
     */
    public MappingNamespace getNamespace() {
        return namespace;
    }

    /**
     * Sets a parent namespace where this DataMap resides. Parent EntityNamespace is used
     * to establish relationships with entities in other DataMaps.
     * 
     * @since 1.1
     */
    public void setNamespace(MappingNamespace namespace) {
        this.namespace = namespace;
    }

    /**
     * @since 1.1
     */
    public int getDefaultLockType() {
        return defaultLockType;
    }

    /**
     * @since 1.1
     */
    public void setDefaultLockType(int defaultLockType) {
        this.defaultLockType = defaultLockType;
    }
    
    /**
     * Returns default server package with ".client" suffix.
     * 
     * @since 1.2
     */
    public String getDefaultClientPackage() {
        return Util.isEmptyString(getDefaultPackage()) ? null : getDefaultPackage()
                + ".client";
    }

    /**
     * @since 1.1
     */
    public String getDefaultPackage() {
        return defaultPackage;
    }

    /**
     * @since 1.1
     */
    public void setDefaultPackage(String defaultPackage) {
        this.defaultPackage = defaultPackage;
    }

    /**
     * @since 1.1
     */
    public String getDefaultSchema() {
        return defaultSchema;
    }

    /**
     * @since 1.1
     */
    public void setDefaultSchema(String defaultSchema) {
        this.defaultSchema = defaultSchema;
    }

    /**
     * @since 1.1
     */
    public String getDefaultSuperclass() {
        return defaultSuperclass;
    }

    /**
     * @since 1.1
     */
    public void setDefaultSuperclass(String defaultSuperclass) {
        this.defaultSuperclass = defaultSuperclass;
    }

	/**
     * DbEntity property changed. 
	 * May be name, attribute or relationship added or removed, etc. 
	 * Attribute and relationship property changes are handled in
	 * respective listeners.
	 * @since 1.2 
	 */
    public void dbEntityChanged(EntityEvent e){
	    Entity entity = e.getEntity();
	    if (entity instanceof DbEntity){
	        ((DbEntity)entity).dbEntityChanged(e);
	        
	        // finish up the name change here because we
	        // do not have direct access to the dbEntityMap
	        if (e.isNameChange()){
		        // remove the entity from the map with the old name
		        dbEntityMap.remove(e.getOldName());

		        // add the entity back in with the new name
		        dbEntityMap.put(e.getNewName(), entity);
		        
	            // important - clear parent namespace:
	            MappingNamespace ns = getNamespace();
	            if (ns instanceof EntityResolver) {
	                ((EntityResolver) ns).clearCache();
	            }	            
	        }
	    }
	}
   
	/** New entity has been created/added.*/
	public void dbEntityAdded(EntityEvent e){
	    // does nothing currently
	}
	/** Entity has been removed.*/
	public void dbEntityRemoved(EntityEvent e){
	    // does nothing currently
	}

	/** Attribute property changed. */
	public void dbAttributeChanged(AttributeEvent e){
	    Entity entity = e.getEntity();
	    if (entity instanceof DbEntity){
	        ((DbEntity)entity).dbAttributeChanged(e);
	    }
	}
	
	/** New attribute has been created/added.*/
	public void dbAttributeAdded(AttributeEvent e){
	    // does nothing currently
	}
	
	/** Attribute has been removed.*/
	public void dbAttributeRemoved(AttributeEvent e){
	    // does nothing currently
	}

	/** Relationship property changed. */
	public void dbRelationshipChanged(RelationshipEvent e){
	    Entity entity = e.getEntity();
	    if (entity instanceof DbEntity){
	        ((DbEntity)entity).dbRelationshipChanged(e);
	    }
	}
	
	/** Relationship has been created/added.*/
	public void dbRelationshipAdded(RelationshipEvent e){
	    // does nothing currently
	}
	
	/** Relationship has been removed.*/
	public void dbRelationshipRemoved(RelationshipEvent e){
	    // does nothing currently
	}

    /**
     * ObjEntity property changed. 
	 * May be name, attribute or relationship added or removed, etc. 
	 * Attribute and relationship property changes are handled in
	 * respective listeners.
	 * @since 1.2 
	 */
    public void objEntityChanged(EntityEvent e){
	    Entity entity = e.getEntity();
	    if (entity instanceof ObjEntity){
	        ((ObjEntity)entity).objEntityChanged(e);

	        // finish up the name change here because we
	        // do not have direct access to the objEntityMap
	        if (e.isNameChange()){
		        // remove the entity from the map with the old name
		        objEntityMap.remove(e.getOldName());

		        // add the entity back in with the new name
		        objEntityMap.put(e.getNewName(), entity);
		        
	            // important - clear parent namespace:
	            MappingNamespace ns = getNamespace();
	            if (ns instanceof EntityResolver) {
	                ((EntityResolver) ns).clearCache();
	            }	            
	        }
	    }
	}
    
	/** New entity has been created/added.*/
	public void objEntityAdded(EntityEvent e){
	    // does nothing currently
	}
	
	/** Entity has been removed.*/
	public void objEntityRemoved(EntityEvent e){
	    // does nothing currently
	}

	/** Attribute property changed. */
	public void objAttributeChanged(AttributeEvent e){
	    // does nothing currently
	}
	
	/** New attribute has been created/added.*/
	public void objAttributeAdded(AttributeEvent e){
	    // does nothing currently
	}
	
	/** Attribute has been removed.*/
	public void objAttributeRemoved(AttributeEvent e){
	    // does nothing currently
	}

	/** Relationship property changed. */
    public void objRelationshipChanged(RelationshipEvent e){
	    // does nothing currently
	}

    /** Relationship has been created/added. */
    public void objRelationshipAdded(RelationshipEvent e){
	    // does nothing currently
	}

    /** Relationship has been removed. */
    public void objRelationshipRemoved(RelationshipEvent e){
	    // does nothing currently
	}
}