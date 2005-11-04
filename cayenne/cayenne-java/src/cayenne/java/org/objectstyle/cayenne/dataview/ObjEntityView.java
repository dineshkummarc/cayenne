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
package org.objectstyle.cayenne.dataview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.Validate;
import org.objectstyle.cayenne.map.ObjEntity;

/**
 * A view rooted in an ObjEntity.
 * 
 * @since 1.1
 * @author Andriy Shapochka
 */
public class ObjEntityView {

    private DataView owner;
    private ObjEntity objEntity;
    private List fields = new ArrayList();
    private Map nameFieldMap = new HashMap();
    private List readOnlyFields = Collections.unmodifiableList(fields);
    private String name;

    public ObjEntityView() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        Validate.notNull(name);
        this.name = name;
    }

    public List getFields() {
        return readOnlyFields;
    }

    public List getVisibleFields() {
        int size = getFieldCount();
        List dst = new ArrayList(size);
        for (int i = 0; i < size; i++) {
            ObjEntityViewField field = getField(i);
            if (field.isVisible())
                dst.add(field);
        }
        return dst;
    }

    public List getEditableFields() {
        int size = getFieldCount();
        List dst = new ArrayList(size);
        for (int i = 0; i < size; i++) {
            ObjEntityViewField field = getField(i);
            if (field.isVisible() && field.isEditable())
                dst.add(field);
        }
        return dst;
    }

    public ObjEntity getObjEntity() {
        return objEntity;
    }

    public void setObjEntity(ObjEntity objEntity) {
        Validate.notNull(objEntity);
        this.objEntity = objEntity;
    }

    public ObjEntityViewField getField(int index) {
        return (ObjEntityViewField) fields.get(index);
    }

    public ObjEntityViewField getField(String fieldName) {
        return (ObjEntityViewField) nameFieldMap.get(fieldName);
    }

    public ObjEntityViewField getFieldForObjAttribute(String objAttributeName) {
        for (int i = 0; i < fields.size(); i++) {
            ObjEntityViewField field = getField(i);
            if (objAttributeName.equals(field.getObjAttribute().getName()))
                return field;
        }
        return null;
    }

    public int getFieldCount() {
        return fields.size();
    }

    public boolean hasFields() {
        return !fields.isEmpty();
    }

    public void clearFields() {
        for (int i = 0; i < getFieldCount(); i++) {
            ObjEntityViewField field = getField(i);
            field.setOwner(null);
            field.setIndex(-1);
        }
        fields.clear();
        nameFieldMap.clear();
    }

    public boolean removeField(ObjEntityViewField field) {
        if (field.getOwner() != this)
            return false;
        field.setOwner(null);
        field.setIndex(-1);
        nameFieldMap.remove(field.getName());
        return fields.remove(field);
    }

    public int insertField(ObjEntityViewField field) {
        if (field.getOwner() == this)
            return field.getIndex();

        Validate.notNull(field.getName());
        Validate.isTrue(!nameFieldMap.containsKey(field.getName()));
        field.setOwner(this);
        nameFieldMap.put(field.getName(), field);

        int prefIndex = field.getPreferredIndex();
        int fieldCount = getFieldCount();

        if (prefIndex < 0) {
            int newIndex = fieldCount;
            fields.add(field);
            field.setIndex(newIndex);
            return newIndex;
        }

        int index = 0;
        int curPrefIndex = -1;
        while (index < fieldCount
                && ((curPrefIndex = getField(index++).getPreferredIndex()) <= prefIndex)
                && curPrefIndex >= 0) {
        	// skip forward
        }

        fields.add(index, field);
        field.setIndex(index);
        fieldCount++;

        for (int i = (index + 1); i < fieldCount; i++) {
            getField(i).setIndex(i);
        }

        return index;
    }

    public DataView getOwner() {
        return owner;
    }

    void setOwner(DataView owner) {
        this.owner = owner;
    }
}