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

import javax.persistence.Table;

import org.apache.cayenne.util.TreeNodeChild;

public class JpaTable {

    protected String name;
    protected String catalog;
    protected String schema;
    protected Collection<JpaUniqueConstraint> uniqueConstraints;

    public JpaTable() {

    }

    public JpaTable(Table annotation) {
        if (!"".equals(annotation.name())) {
            name = annotation.name();
        }

        if (!"".equals(annotation.catalog())) {
            catalog = annotation.catalog();
        }

        if (!"".equals(annotation.schema())) {
            schema = annotation.schema();
        }

        // resolve collection
        getUniqueConstraints();

        for (int i = 0; i < annotation.uniqueConstraints().length; i++) {
            uniqueConstraints.add(new JpaUniqueConstraint(
                    annotation.uniqueConstraints()[i]));
        }
    }

    public String getCatalog() {
        return catalog;
    }

    public void setCatalog(String catalog) {
        this.catalog = catalog;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    @TreeNodeChild(type = JpaUniqueConstraint.class)
    public Collection<JpaUniqueConstraint> getUniqueConstraints() {
        if (uniqueConstraints == null) {
            uniqueConstraints = new ArrayList<JpaUniqueConstraint>(2);
        }

        return uniqueConstraints;
    }
    
    @Override
    public String toString() {
        return "JpaTable:" + name;
    }
}