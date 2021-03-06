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

import javax.persistence.AttributeOverride;

import org.apache.cayenne.util.TreeNodeChild;
import org.apache.cayenne.util.XMLEncoder;

public class JpaAttributeOverride extends JpaAttribute {

    protected JpaColumn column;

    public JpaAttributeOverride() {

    }

    public JpaAttributeOverride(AttributeOverride annotation) {
        name = annotation.name();

        if (annotation.column() != null) {
            column = new JpaColumn(annotation.column());
        }
    }

    @Override
    public void encodeAsXML(XMLEncoder encoder) {
        encoder.print("<attribute-override");
        if (name != null) {
            encoder.print(" name=\"" + name + "\"");
        }

        encoder.println(">");
        encoder.indent(1);
        if (column != null) {
            column.encodeAsXML(encoder);
        }

        encoder.indent(-1);
        encoder.println("</attribute-override>");
    }

    @TreeNodeChild
    public JpaColumn getColumn() {
        return column;
    }

    public void setColumn(JpaColumn column) {
        this.column = column;
    }
}
