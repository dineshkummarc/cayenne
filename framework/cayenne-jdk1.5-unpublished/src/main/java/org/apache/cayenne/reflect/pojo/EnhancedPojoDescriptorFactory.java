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
package org.apache.cayenne.reflect.pojo;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.reflect.Accessor;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.ClassDescriptorFactory;
import org.apache.cayenne.reflect.ClassDescriptorMap;
import org.apache.cayenne.reflect.FaultFactory;
import org.apache.cayenne.reflect.PersistentDescriptor;
import org.apache.cayenne.reflect.PersistentDescriptorFactory;
import org.apache.cayenne.reflect.Property;

/**
 * A {@link ClassDescriptorFactory} used to create descriptors for the enhanced POJO's.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
public class EnhancedPojoDescriptorFactory extends PersistentDescriptorFactory {

    static final String PERSISTENCE_STATE_FIELD = "$cay_persistenceState";

    protected FaultFactory faultFactory;

    public EnhancedPojoDescriptorFactory(ClassDescriptorMap descriptorMap,
            FaultFactory faultFactory) {
        super(descriptorMap);
        this.faultFactory = faultFactory;
    }

    protected ClassDescriptor getDescriptor(ObjEntity entity, Class entityClass) {
        // check whether we are dealing with enhaced pojo
        try {
            Integer.TYPE.equals(entityClass
                    .getDeclaredField(PERSISTENCE_STATE_FIELD)
                    .getType());
        }
        catch (Throwable th) {
            return null;
        }

        return super.getDescriptor(entity, entityClass);
    }

    protected void createToManyListProperty(
            PersistentDescriptor descriptor,
            ObjRelationship relationship) {
        ClassDescriptor targetDescriptor = descriptorMap.getDescriptor(relationship
                .getTargetEntityName());
        String reverseName = relationship.getReverseRelationshipName();

        Accessor accessor = createAccessor(descriptor, relationship.getName(), List.class);
        Property property = new EnhancedPojoListProperty(
                descriptor,
                targetDescriptor,
                accessor,
                reverseName);

        descriptor.addDeclaredProperty(property);
    }

    protected void createToManyMapProperty(
            PersistentDescriptor descriptor,
            ObjRelationship relationship) {
        ClassDescriptor targetDescriptor = descriptorMap.getDescriptor(relationship
                .getTargetEntityName());
        String reverseName = relationship.getReverseRelationshipName();
        Accessor accessor = createAccessor(descriptor, relationship.getName(), Map.class);
        Accessor mapKeyAccessor = createMapKeyAccessor(relationship, targetDescriptor);
        Property property = new EnhancedPojoMapProperty(
                descriptor,
                targetDescriptor,
                accessor,
                reverseName,
                mapKeyAccessor);

        descriptor.addDeclaredProperty(property);
    }

    protected void createToManySetProperty(
            PersistentDescriptor descriptor,
            ObjRelationship relationship) {
        ClassDescriptor targetDescriptor = descriptorMap.getDescriptor(relationship
                .getTargetEntityName());
        String reverseName = relationship.getReverseRelationshipName();
        Accessor accessor = createAccessor(descriptor, relationship.getName(), Set.class);
        Property property = new EnhancedPojoSetProperty(
                descriptor,
                targetDescriptor,
                accessor,
                reverseName);

        descriptor.addDeclaredProperty(property);
    }

    protected void createToManyCollectionProperty(
            PersistentDescriptor descriptor,
            ObjRelationship relationship) {
        ClassDescriptor targetDescriptor = descriptorMap.getDescriptor(relationship
                .getTargetEntityName());
        String reverseName = relationship.getReverseRelationshipName();
        Accessor accessor = createAccessor(
                descriptor,
                relationship.getName(),
                Collection.class);
        Property property = new EnhancedPojoListProperty(
                descriptor,
                targetDescriptor,
                accessor,
                reverseName);

        descriptor.addDeclaredProperty(property);
    }

    protected void createToOneProperty(
            PersistentDescriptor descriptor,
            ObjRelationship relationship) {

        ClassDescriptor targetDescriptor = descriptorMap.getDescriptor(relationship
                .getTargetEntityName());
        String reverseName = relationship.getReverseRelationshipName();

        Accessor accessor = createAccessor(
                descriptor,
                relationship.getName(),
                targetDescriptor.getObjectClass());
        descriptor.addDeclaredProperty(new EnhancedPojoToOneProperty(
                descriptor,
                targetDescriptor,
                accessor,
                reverseName,
                faultFactory.getToOneFault()));
    }
}