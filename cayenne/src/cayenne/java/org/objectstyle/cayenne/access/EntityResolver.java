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
package org.objectstyle.cayenne.access;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.query.Query;

/**
 * EntityResolver encapsulates resolving between ObjEntities, DbEntities, 
 * DataObject Classes, and Entity names. An instance is typically obtained 
 * from a QueryEngine by getEntityResolver. EntityResolver is thread-safe.
 * 
 * @author Craig Miskell
 * @author Andrei Adamchik
 */
public class EntityResolver {
    protected Map dbEntityCache;
    protected Map objEntityCache;
    protected List maps;

    public EntityResolver() {
        this.maps = new ArrayList();
        this.dbEntityCache = new HashMap();
        this.objEntityCache = new HashMap();
    }

    /**
     * Constructor for EntityResolver.
     */
    public EntityResolver(List dataMaps) {
        this();
        maps.addAll(dataMaps); //Take a copy
        this.constructCache();
    }

    /**
     * Adds a DataMap to the list handled by resolver.
     */
    public synchronized void addDataMap(DataMap map) {
        if (!maps.contains(map)) {
            maps.add(map);
            clearCache();
        }
    }

    public synchronized void removeDataMap(DataMap map) {
        if (maps.remove(map)) {
            clearCache();
        }
    }

    /**
     * Returns a DataMap matching the name.
     */
    public synchronized DataMap getDataMap(String mapName) {
        if (mapName == null) {
            return null;
        }

        Iterator it = maps.iterator();
        while (it.hasNext()) {
            DataMap map = (DataMap) it.next();
            if (mapName.equals(map.getName())) {
                return map;
            }
        }

        return null;
    }

    public synchronized void setDataMaps(List maps) {
        this.maps.clear();
        this.maps.addAll(maps);
        clearCache();
    }

    /**
     * Returns a list of internal DataMaps by copy.
     */
    public synchronized List getDataMapsList() {
        return new ArrayList(maps);
    }

    /**
     * Removes all entity mappings from the cache.
     * Cache can be rebuilt either explicitly by calling
     * <code>constructCache</code>, or on demand by calling any of the
     * <code>lookup...</code> methods.
     */
    protected synchronized void clearCache() {
        dbEntityCache.clear();
        objEntityCache.clear();
    }

    /** 
     * Creates caches of DbEntities by ObjEntity, 
     * DataObject class, and ObjEntity name using internal 
     * list of maps.
     */
    protected synchronized void constructCache() {
        clearCache();
        Iterator mapIterator = maps.iterator();
        while (mapIterator.hasNext()) {
            int i;
            DataMap thisMap = (DataMap) mapIterator.next();

            ObjEntity[] objEntities = thisMap.getObjEntities();

            for (i = 0; i < objEntities.length; i++) {
                ObjEntity oe = objEntities[i];
                DbEntity de = oe.getDbEntity();
                dbEntityCache.put(oe, de);
                Class entityClass;
                try {
                    //CTM Should this be using  Configuration.getResourceLoader().loadClass(oe.getClassName()) ???
                    entityClass = Class.forName(oe.getClassName());
                } catch (ClassNotFoundException e) {
                    throw new CayenneRuntimeException(
                        "Cannot find class " + oe.getClassName());
                }

                if (objEntityCache.get(entityClass) != null) {
                    throw new CayenneRuntimeException(
                        getClass().getName()
                            + ": More than one ObjEntity ("
                            + oe.getName()
                            + " and "
                            + ((ObjEntity) objEntityCache.get(entityClass)).getName()
                            + ") uses the class "
                            + entityClass.getName());
                }

                dbEntityCache.put(entityClass, de);
                objEntityCache.put(entityClass, oe);
                dbEntityCache.put(oe.getName(), de);
                objEntityCache.put(oe.getName(), oe);
            }
        }
    }

    /**
     * Looks in the DataMap's that this object was created with for theDbEntity that maps to the
     * specified object.  Object may be a Entity name, ObjEntity, or DataObject class 
     * (Class object for a class which implements the DataObject interface)
     * 
     * @return the required DbEntity, or null if none matches the specifier
     */
    public synchronized DbEntity lookupDbEntity(Object object) {
        if (object instanceof DbEntity) {
            return (DbEntity) object;
        }

        DbEntity result = (DbEntity) dbEntityCache.get(object);
        if (result == null) {
            // reconstruct cache just in case some of the datamaps 
            // have changed and now contain the required information
            constructCache();
            result = (DbEntity) dbEntityCache.get(object);
        }
        return result;
    }

    /**
     * Looks up the DbEntity for the given query by using the query's getRoot method and passing to lookupDbEntity
     * @return the root DbEntity of the query
     */
    public DbEntity lookupDbEntity(Query q) {
        return this.lookupDbEntity(q.getRoot());
    }

    /**
     * Looks in the DataMap's that this object was created with for the ObjEntity that maps to the
     * specified object. Object may be a Entity name, or DataObject class (Class object for a class which 
     * implements the DataObject interface)
     * 
     * @return the required ObjEntity or null if there is none that matches the specifier
     */
    public synchronized ObjEntity lookupObjEntity(Object object) {
        if (object instanceof ObjEntity) {
            return (ObjEntity) object;
        }

        ObjEntity result = (ObjEntity) objEntityCache.get(object);
        if (result == null) {
            // reconstruct cache just in case some of the datamaps 
            // have changed and now contain the required information
            constructCache();
            result = (ObjEntity) objEntityCache.get(object);
        }
        return result;
    }

    /**
     * Looks up the ObjEntity for the given query by using the query's getRoot method and passing to lookupObjEntity
     * @return the root ObjEntity of the query
     * @throws CayenneRuntimeException if the root of the query is a DbEntity (it is not reliably possible to map 
     * from a DbEntity to an ObjEntity as a DbEntity may be the source for multiple ObjEntities.  It is not safe
     * to rely on such behaviour).
     */
    public ObjEntity lookupObjEntity(Query q) {
        Object root = q.getRoot();
        if (root instanceof DbEntity) {
            throw new CayenneRuntimeException(
                "Cannot safely resolve the ObjEntity for the query "
                    + q
                    + " because the root of the query is a DbEntity");
        }
        return this.lookupObjEntity(root);
    }

}
