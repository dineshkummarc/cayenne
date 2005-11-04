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
package org.objectstyle.cayenne.property;

import java.lang.reflect.Field;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.util.Util;

/**
 * A property descriptor that provides access to an object property implemented as a
 * Field.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class FieldProperty implements Property {

    protected String propertyName;
    protected Field field;

    /**
     * A default value used when a null is passed for the primitive field.
     */
    protected Object nullValue;

    public FieldProperty(Class beanClass, String propertyName) {
        this(beanClass, propertyName, null);
    }

    /**
     * Creates a descriptor for a simple bean property.
     */
    public FieldProperty(Class beanClass, String propertyName, Class propertyType) {
        // sanity check
        if (propertyName == null) {
            throw new IllegalArgumentException("Null property name");
        }

        if (beanClass == null) {
            throw new IllegalArgumentException("Null beanClass");
        }

        this.propertyName = propertyName;
        this.field = prepareField(beanClass, propertyName, propertyType);
        this.nullValue = defaultNullValueForType(field.getType());
    }

    public String getPropertyName() {
        return propertyName;
    }

    public Class getPropertyType() {
        return field.getType();
    }

    /**
     * Returns a value that is used to set the property when the caller passed null
     * argument. By default this value is "null" for all object types and default (zero or
     * false) for primitive types.
     */
    public Object getNullValue() {
        return nullValue;
    }

    /**
     * Does nothing.
     */
    public void prepareForAccess(Object object) throws PropertyAccessException {
        // noop
    }

    public void copyValue(Object from, Object to) throws PropertyAccessException {
        writeValue(to, readField(to), readField(from));
    }

    public Object readValue(Object object) throws PropertyAccessException {
        return readField(object);
    }

    public void writeValue(Object object, Object oldValue, Object newValue)
            throws PropertyAccessException {
        writeField(object, newValue);
    }

    protected Object readField(Object object) throws PropertyAccessException {
        try {
            return field.get(object);
        }
        catch (Throwable th) {
            throw new PropertyAccessException(
                    "Error reading field: " + field.getName(),
                    this,
                    object,
                    th);
        }
    }

    protected void writeField(Object object, Object newValue)
            throws PropertyAccessException {

        // this would take care of primitives.
        if (newValue == null) {
            newValue = this.nullValue;
        }

        try {
            field.set(object, newValue);
        }
        catch (Throwable th) {
            throw new PropertyAccessException(
                    "Error writing field: " + field.getName(),
                    this,
                    object,
                    th);
        }
    }

    /**
     * Finds a field for the property, ensuring that direct access via reflection is
     * possible.
     */
    protected Field prepareField(Class beanClass, String propertyName, Class propertyType) {
        Field field;

        // locate field
        try {
            field = lookupFieldInHierarchy(beanClass, propertyName);
        }
        catch (SecurityException e) {
            throw new CayenneRuntimeException("Error accessing field '"
                    + propertyName
                    + "' in class: "
                    + beanClass.getName(), e);
        }
        catch (NoSuchFieldException e) {
            throw new CayenneRuntimeException("No field '"
                    + propertyName
                    + "' is defined in class: "
                    + beanClass.getName(), e);
        }

        // set accessability
        if (!Util.isAccessible(field)) {
            field.setAccessible(true);
        }

        if (propertyType != null) {

            // check that the field is of expected class...
            if (!propertyType.isAssignableFrom(field.getType())) {

                // allow primitive to object conversions...
                if (!normalizeType(propertyType).isAssignableFrom(
                        normalizeType(field.getType()))) {
                    throw new CayenneRuntimeException("Expected property type '"
                            + propertyType.getName()
                            + "', got '"
                            + field.getType().getName()
                            + "'. Property: "
                            + beanClass.getName()
                            + "."
                            + propertyName);
                }
            }
        }

        return field;
    }

    /**
     * Recursively looks for a named field in a class hierarchy.
     */
    protected Field lookupFieldInHierarchy(Class beanClass, String fieldName)
            throws SecurityException, NoSuchFieldException {

        // TODO: support property names following other common naming patterns, such as
        // "_propertyName"

        try {
            return beanClass.getDeclaredField(fieldName);
        }
        catch (NoSuchFieldException e) {

            Class superClass = beanClass.getSuperclass();
            if (superClass == null || superClass.getName().equals(Object.class.getName())) {
                throw e;
            }

            return lookupFieldInHierarchy(superClass, fieldName);
        }
    }

    /**
     * "Normalizes" passed type, converting primitive types to their object counterparts.
     */
    Class normalizeType(Class type) {
        if (type.isPrimitive()) {

            String className = type.getName();
            if ("byte".equals(className)) {
                return Byte.class;
            }
            else if ("int".equals(className)) {
                return Integer.class;
            }
            else if ("short".equals(className)) {
                return Short.class;
            }
            else if ("char".equals(className)) {
                return Character.class;
            }
            else if ("double".equals(className)) {
                return Double.class;
            }
            else if ("float".equals(className)) {
                return Float.class;
            }
            else if ("boolean".equals(className)) {
                return Boolean.class;
            }
        }

        return type;
    }

    /**
     * Returns default value that should be used for nulls. For non-primitive types, null
     * is returned. For primitive types a default such as zero or false is returned.
     */
    Object defaultNullValueForType(Class type) {
        if (type.isPrimitive()) {

            String className = type.getName();
            if ("byte".equals(className)) {
                return new Byte((byte) 0);
            }
            else if ("int".equals(className)) {
                return new Integer(0);
            }
            else if ("short".equals(className)) {
                return new Short((short) 0);
            }
            else if ("char".equals(className)) {
                return new Character((char) 0);
            }
            else if ("double".equals(className)) {
                return new Double(0d);
            }
            else if ("float".equals(className)) {
                return new Float(0f);
            }
            else if ("boolean".equals(className)) {
                return Boolean.FALSE;
            }
        }

        return null;
    }
}