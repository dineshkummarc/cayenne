/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.merge;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.MappingNamespace;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.util.EntityMergeSupport;

/**
 * Common abstract superclass for all {@link MergerToken}s going from the database to the
 * model.
 * 
 * @author halset
 */
public abstract class AbstractToModelToken implements MergerToken {

    public final MergeDirection getDirection() {
        return MergeDirection.TO_MODEL;
    }

    protected void synchronizeWithObjEntity(DbEntity entity) {
        for (ObjEntity objEntity : objEntitiesMappedToDbEntity(entity)) {
            new EntityMergeSupport(objEntity.getDataMap())
                    .synchronizeWithDbEntity(objEntity);
        }
    }

    protected Collection<ObjEntity> objEntitiesMappedToDbEntity(DbEntity entity) {
        Set<ObjEntity> objEntities = new HashSet<ObjEntity>();
        MappingNamespace mns = entity.getDataMap().getNamespace();
        for (ObjEntity objEntity : mns.getObjEntities()) {
            if (objEntity.getDbEntity().equals(entity)) {
                objEntities.add(objEntity);
            }
        }
        return objEntities;
    }
    
    protected void remove(DbRelationship rel, boolean reverse) {
        if (rel == null) {
            return;
        }
        if (reverse) {
            remove(rel.getReverseRelationship(), false);
        }

        DbEntity dbEntity = (DbEntity) rel.getSourceEntity();
        for (ObjEntity objEntity : objEntitiesMappedToDbEntity(dbEntity)) {
            remove(objEntity.getRelationshipForDbRelationship(rel), true);
        }
        
        rel.getSourceEntity().removeRelationship(rel.getName());
    }

    protected void remove(ObjRelationship rel, boolean reverse) {
        if (rel == null) {
            return;
        }
        if (reverse) {
            remove(rel.getReverseRelationship(), false);
        }
        rel.getSourceEntity().removeRelationship(rel.getName());
    }

    @Override
    public String toString() {
        StringBuilder ts = new StringBuilder();
        ts.append(getTokenName());
        ts.append(' ');
        ts.append(getTokenValue());
        ts.append(' ');
        ts.append(getDirection());
        return ts.toString();
    }

}
