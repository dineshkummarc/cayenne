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


package org.apache.cayenne.jpa.map;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.cayenne.util.TreeNodeChild;

/**
 * A collection of entity listener descriptors.
 * 
 * @author Andrus Adamchik
 */
// andrus: I'd rather we flatten this object into JpaEntity, but since we have to follow
// the schema structure, we need this meaningless object.
public class JpaEntityListeners {

    protected Collection<JpaEntityListener> entityListeners;

    @TreeNodeChild(type = JpaEntityListener.class)
    public Collection<JpaEntityListener> getEntityListeners() {
        if (entityListeners == null) {
            entityListeners = new ArrayList<JpaEntityListener>();
        }
        return entityListeners;
    }
}