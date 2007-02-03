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

package org.apache.cayenne.jpa.reflect;

import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.reflect.Accessor;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.ClassDescriptorMap;
import org.apache.cayenne.reflect.PersistentDescriptor;
import org.apache.cayenne.reflect.pojo.EnhancedPojoDescriptorFactory;

public class JpaClassDescriptorFactory extends EnhancedPojoDescriptorFactory {

    public JpaClassDescriptorFactory(ClassDescriptorMap descriptorMap) {
        super(descriptorMap);
    }

    @Override
    protected void createToManyProperty(
            PersistentDescriptor descriptor,
            ObjRelationship relationship) {
        ClassDescriptor targetDescriptor = descriptorMap.getDescriptor(relationship
                .getTargetEntityName());
        String reverseName = relationship.getReverseRelationshipName();

        Accessor accessor = new JpaCollectionFieldAccessor(
                descriptor.getObjectClass(),
                relationship.getName(),
                null);
        descriptor.addDeclaredProperty(new JpaToManyProperty(
                descriptor,
                targetDescriptor,
                accessor,
                reverseName));
    }
}