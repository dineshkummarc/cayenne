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

package org.apache.cayenne.map;

import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.event.AttributeEvent;
import org.apache.cayenne.map.event.DbAttributeListener;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.util.XMLEncoder;

/**
 * A DbAttribute defines a descriptor for a single database table column.
 * 
 * @author Misha Shengaout
 * @author Andrus Adamchik
 */
public class DbAttribute extends Attribute {

    /**
     * Defines JDBC type of the column.
     */
    protected int type = TypesMapping.NOT_DEFINED;

    /**
     * Defines whether the attribute allows nulls.
     */
    protected boolean mandatory;

    /**
     * Defines whether the attribute is a part of the table primary key.
     */
    protected boolean primaryKey;

    /**
     * Defines whether this column value is generated by the database. Other terms for
     * such columns are "auto-increment" or "identity".
     * 
     * @since 1.2
     */
    protected boolean generated;

    // The length of CHAR or VARCHAr or max num of digits for DECIMAL.
    protected int maxLength = -1;

    /**
     * @since 3.0
     */
    protected int scale = -1;

    /**
     * @since 3.0
     */
    // must call it 'attributePrecison' as 'precision' in 1.2 really meant 'scale'
    protected int attributePrecision = -1;

    public DbAttribute() {
        super();
    }

    public DbAttribute(String name) {
        super(name);
    }

    public DbAttribute(String name, int type, DbEntity entity) {
        this.setName(name);
        this.setType(type);
        this.setEntity(entity);
    }

    /**
     * Prints itself as XML to the provided XMLEncoder.
     * 
     * @since 1.1
     */
    public void encodeAsXML(XMLEncoder encoder) {

        encoder.print("<db-attribute name=\"");
        encoder.print(Util.encodeXmlAttribute(getName()));
        encoder.print('\"');

        String type = TypesMapping.getSqlNameByType(getType());
        if (type != null) {
            encoder.print(" type=\"" + type + '\"');
        }

        if (isPrimaryKey()) {
            encoder.print(" isPrimaryKey=\"true\"");

            // only allow generated if an attribute is a PK.
            if (isGenerated()) {
                encoder.print(" isGenerated=\"true\"");
            }
        }

        if (isMandatory()) {
            encoder.print(" isMandatory=\"true\"");
        }

        if (getMaxLength() > 0) {
            encoder.print(" length=\"");
            encoder.print(getMaxLength());
            encoder.print('\"');
        }

        if (getScale() > 0) {
            encoder.print(" scale=\"");
            encoder.print(getScale());
            encoder.print('\"');
        }

        if (getAttributePrecision() > 0) {
            encoder.print(" attributePrecision=\"");
            encoder.print(getAttributePrecision());
            encoder.print('\"');
        }

        encoder.println("/>");
    }

    public String getAliasedName(String alias) {
        return (alias != null) ? alias + '.' + this.getName() : this.getName();
    }

    /**
     * Returns the SQL type of the column.
     * 
     * @see java.sql.Types
     */
    public int getType() {
        return type;
    }

    /**
     * Sets the SQL type for the column.
     * 
     * @see java.sql.Types
     */
    public void setType(int type) {
        this.type = type;
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    /**
     * Returns <code>true</code> if the DB column represented by this attribute is a
     * foreign key, referencing another table.
     * 
     * @since 1.1
     */
    public boolean isForeignKey() {
        String name = getName();
        if (name == null) {
            // won't be able to match joins...
            return false;
        }

        for (Relationship relationship : getEntity().getRelationships()) {
            for (DbJoin join : ((DbRelationship)relationship).getJoins()) {
                if (name.equals(join.getSourceName())) {
                    DbAttribute target = join.getTarget();
                    if (target != null && target.isPrimaryKey()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Updates attribute "primaryKey" property.
     */
    public void setPrimaryKey(boolean primaryKey) {
        if (this.primaryKey != primaryKey) {
            this.primaryKey = primaryKey;

            Entity e = this.getEntity();
            if (e instanceof DbAttributeListener) {
                ((DbAttributeListener) e).dbAttributeChanged(new AttributeEvent(
                        this,
                        this,
                        e));
            }
        }
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    /**
     * Returns the length of database column described by this attribute.
     */
    public int getMaxLength() {
        return maxLength;
    }

    /**
     * Sets the length of character or binary type or max num of digits for DECIMAL.
     */
    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

    /**
     * Returns the number of digits after period for DECIMAL.
     * 
     * @deprecated since 3.0 as this property really referred to 'scale'. Use
     *             {@link #getScale()} instead.
     */
    public int getPrecision() {
        return getScale();
    }

    /**
     * @deprecated since 3.0 as this property really referred to 'scale'. Use
     *             {@link #setScale(int)} instead.
     */
    public void setPrecision(int i) {
        setScale(i);
    }

    /**
     * Returns true if this column value is generated by the database. Other terms for
     * such columns are "auto-increment" or "identity".
     * 
     * @since 1.2
     */
    public boolean isGenerated() {
        return generated;
    }

    /**
     * Updates attribute "generated" property.
     * 
     * @since 1.2
     */
    public void setGenerated(boolean generated) {
        if (this.generated != generated) {
            this.generated = generated;

            Entity e = this.getEntity();
            if (e instanceof DbAttributeListener) {
                ((DbAttributeListener) e).dbAttributeChanged(new AttributeEvent(
                        this,
                        this,
                        e));
            }
        }
    }

    /**
     * @since 3.0
     */
    public int getAttributePrecision() {
        return attributePrecision;
    }

    /**
     * @since 3.0
     */
    public void setAttributePrecision(int attributePrecision) {
        this.attributePrecision = attributePrecision;
    }

    /**
     * Returns the number of digits after period for decimal attributes. Returns "-1" if
     * not set.
     * 
     * @since 3.0
     */
    public int getScale() {
        return scale;
    }

    /**
     * @since 3.0
     */
    public void setScale(int scale) {
        this.scale = scale;
    }
}
