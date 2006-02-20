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
package org.objectstyle.cayenne.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.objectstyle.cayenne.dba.TypesMapping;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbJoin;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.Entity;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.project.NamedObjectFactory;

/**
 * Implements methods for entity merging.
 * 
 * @author Andrei Adamchik
 */
public class EntityMergeSupport {

    protected DataMap map;

    public EntityMergeSupport(DataMap map) {
        this.map = map;
    }
    

    /**
     * Updates each one of the collection of ObjEntities, adding attributes and relationships
     * based on the current state of its DbEntity.
     * 
     * @since 1.2 changed signature to use Collection instead of List.
     */
    public void synchronizeWithDbEntities(Collection objEntities) {
        Iterator it = objEntities.iterator();
        while (it.hasNext()) {
            this.synchronizeWithDbEntity((ObjEntity) it.next());
        }
    }

    /**
     * Updates ObjEntity attributes and relationships based on the current state of its
     * DbEntity.
     */
    public void synchronizeWithDbEntity(ObjEntity entity) {

        if (entity == null || entity.getDbEntity() == null) {
            return;
        }

        // synchronization on DataMap is some (weak) protection
        // against simulteneous modification of the map (like double-clicking on sync
        // button)
        synchronized (map) {
            List removeAttributes = getAttributesToRemove(entity);
            
            // get rid of attributes that are now src attributes for relationships
            Iterator rait = removeAttributes.iterator();
            while (rait.hasNext()) {
                DbAttribute da = (DbAttribute) rait.next();
                ObjAttribute oa = entity.getAttributeForDbAttribute(da);
                while (oa != null){
                    String attrName = oa.getName();
                    entity.removeAttribute(attrName);
                    oa = entity.getAttributeForDbAttribute(da);
                }
            }
            
            List addAttributes = getAttributesToAdd(entity);

            // add missing attributes
            Iterator ait = addAttributes.iterator();
            while (ait.hasNext()) {
                DbAttribute da = (DbAttribute) ait.next();
                String attrName = NameConverter.underscoredToJava(da.getName(), false);

                // avoid duplicate names
                attrName = NamedObjectFactory.createName(
                        ObjAttribute.class,
                        entity,
                        attrName);

                String type = TypesMapping.getJavaBySqlType(da.getType());

                ObjAttribute oa = new ObjAttribute(attrName, type, entity);
                oa.setDbAttribute(da);
                entity.addAttribute(oa);
            }
            
            List addRelationships = getRelationshipsToAdd(entity);

            // add missing relationships
            Iterator rit = addRelationships.iterator();
            while (rit.hasNext()) {
                DbRelationship dr = (DbRelationship) rit.next();
                DbEntity dbEntity = (DbEntity) dr.getTargetEntity();

                Iterator targets = map.getMappedEntities(dbEntity).iterator();
                if (targets.hasNext()) {

                    Entity mappedTarget = (Entity) targets.next();

                    // avoid duplicate names
                    String relationshipName = NameConverter.underscoredToJava(dr.getName(), false);
                    relationshipName = NamedObjectFactory.createName(
                            ObjRelationship.class,
                            entity,
                            relationshipName);

                    ObjRelationship or = new ObjRelationship(relationshipName);
                    or.addDbRelationship(dr);
                    or.setSourceEntity(entity);
                    or.setTargetEntity(mappedTarget);
                    entity.addRelationship(or);
                }
            }
        }
    }

    /**
     * Returns a list of src attributes for the DbEntity relationships (Foreign Keys), 
     * these do not need to be attributes of the ObjEntity.
     * 
     * @since 1.2
     */
    protected List getAttributesToRemove(ObjEntity objEntity){
        List removeList = new ArrayList();
        Iterator it = objEntity.getDbEntity().getRelationships().iterator();
        while (it.hasNext()) {
            DbRelationship dbrel = (DbRelationship) it.next();
            
            // check if adding it makes sense at all
            if (dbrel.getName() == null) {
                continue;
            }

            // get all of the srcAttributes for the relationship
            Iterator srcAttIterator = dbrel.getSourceAttributes().iterator();
            while(srcAttIterator.hasNext()){
                removeList.add(srcAttIterator.next());
            }
        }
        
        return removeList;        
    }

    /**
     * Returns a list of attributes that exist in the DbEntity, but are missing from the
     * ObjEntity.
     */
    protected List getAttributesToAdd(ObjEntity objEntity) {
        List missing = new ArrayList();
        Iterator it = objEntity.getDbEntity().getAttributes().iterator();
        Collection rels = objEntity.getDbEntity().getRelationships();

        while (it.hasNext()) {
            DbAttribute dba = (DbAttribute) it.next();
            // already there
            if (objEntity.getAttributeForDbAttribute(dba) != null) {
                continue;
            }

            // check if adding it makes sense at all
            if (dba.getName() == null || dba.isPrimaryKey()) {
                continue;
            }

            // check FK's
            boolean isFK = false;
            Iterator rit = rels.iterator();
            while (!isFK && rit.hasNext()) {
                DbRelationship rel = (DbRelationship) rit.next();
                Iterator jit = rel.getJoins().iterator();
                while (jit.hasNext()) {
                    DbJoin join = (DbJoin) jit.next();
                    if (join.getSource() == dba) {
                        isFK = true;
                        break;
                    }
                }
            }

            if (isFK) {
                continue;
            }

            missing.add(dba);
        }

        return missing;
    }

    protected List getRelationshipsToAdd(ObjEntity objEntity) {
        List missing = new ArrayList();
        Iterator it = objEntity.getDbEntity().getRelationships().iterator();
        while (it.hasNext()) {
            DbRelationship dbrel = (DbRelationship) it.next();
            // check if adding it makes sense at all
            if (dbrel.getName() == null) {
                continue;
            }

            if (objEntity.getRelationshipForDbRelationship(dbrel) == null) {
                missing.add(dbrel);
            }
        }

        return missing;
    }
    
    public DataMap getMap() {
        return map;
    }

    public void setMap(DataMap map) {
        this.map = map;
    }
}