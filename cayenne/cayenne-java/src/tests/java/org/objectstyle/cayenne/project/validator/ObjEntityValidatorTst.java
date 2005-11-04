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
package org.objectstyle.cayenne.project.validator;

import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.project.ProjectPath;

/**
 * @author Andrei Adamchik
 * @author Craig Miskell
 */
public class ObjEntityValidatorTst extends ValidatorTestBase {
    protected DataDomain domain;
    protected DataMap map;

    protected void setUp() throws Exception {
        super.setUp();
        domain = new DataDomain("d1");

        map = new DataMap("m1");
        domain.addMap(map);

    }

    public void testValidateNoName() throws Exception {
        ObjEntity oe1 = new ObjEntity("oe1");
        DbEntity de1 = new DbEntity("de1");
        oe1.setDbEntityName("de1");
        oe1.setClassName("some.javaclass.name");
        map.addObjEntity(oe1);
        map.addDbEntity(de1);

        validator.reset();
        new ObjEntityValidator().validateObject(
            new ProjectPath(new Object[] { project, domain, map, oe1 }),
            validator);
        assertValidator(ValidationInfo.VALID);

        // now remove the name
        oe1.setName(null);

        validator.reset();
        new ObjEntityValidator().validateObject(
            new ProjectPath(new Object[] { project, domain, map, oe1 }),
            validator);
        assertValidator(ValidationInfo.ERROR);
    }

    public void testValidateNoClassName() throws Exception {
        ObjEntity oe1 = new ObjEntity("oe1");
        oe1.setDbEntity(new DbEntity("de1"));
        oe1.setClassName(null);
        map.addObjEntity(oe1);

        validator.reset();
        new ObjEntityValidator().validateObject(
            new ProjectPath(new Object[] { project, domain, map, oe1 }),
            validator);
        assertValidator(ValidationInfo.WARNING);
        //WARNING is ok - null class name will give that, but ERROR is bad

    }

    public void testValidateMultipleNullClassNames() throws Exception {
        ObjEntity oe1 = new ObjEntity("oe1");
        ObjEntity oe2 = new ObjEntity("oe2");
        DbEntity de1 = new DbEntity("de1");
        DbEntity de2 = new DbEntity("de2");

        oe1.setDbEntity(de1);
        oe1.setClassName(null);

        oe2.setDbEntity(de2);
        oe2.setClassName(null);

        map.addObjEntity(oe1);
        map.addObjEntity(oe2);
        map.addDbEntity(de1);
        map.addDbEntity(de2);

        validator.reset();
        new ObjEntityValidator().validateObject(
            new ProjectPath(new Object[] { project, domain, map, oe1 }),
            validator);
        assertValidator(ValidationInfo.WARNING);
        //WARNING is ok - null class name will give that, but ERROR is bad

        //Give one a class name - still valid
        oe1.setClassName("some.javaclassclass.name");
        validator.reset();
        new ObjEntityValidator().validateObject(
            new ProjectPath(new Object[] { project, domain, map, oe1 }),
            validator);
        assertValidator(ValidationInfo.VALID); //Must be valid - this class has a name

        //Give the other the same class name - no longer valid
        oe2.setClassName("some.javaclassclass.name");
        validator.reset();
        new ObjEntityValidator().validateObject(
            new ProjectPath(new Object[] { project, domain, map, oe1 }),
            validator);
        assertValidator(ValidationInfo.WARNING);
        // WARNING - it is OK to save multiple entities with the
    }

}