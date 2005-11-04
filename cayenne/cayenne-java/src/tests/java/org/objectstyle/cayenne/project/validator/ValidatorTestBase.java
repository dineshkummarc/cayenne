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

import java.io.File;
import java.sql.Types;

import junit.framework.TestCase;

import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.project.ApplicationProject;

/**
 * @author Andrus Adamchik
 */
public class ValidatorTestBase extends TestCase {

    protected static int counter = 1;

    protected Validator validator;
    protected ApplicationProject project;

    protected void setUp() throws Exception {
        super.setUp();
        project = new ApplicationProject(new File(System.getProperty("user.dir")));
        validator = new Validator(project);
    }

    protected void assertValidator(int errorLevel) throws Exception {
        assertEquals(errorLevel, validator.getMaxSeverity());
    }

    protected DbRelationship buildValidDbRelationship(DataMap map, String name) {
        DbEntity src = new DbEntity("e1" + counter++);
        DbEntity target = new DbEntity("e2" + counter++);
        map.addDbEntity(src);
        map.addDbEntity(target);
        DbRelationship dr1 = new DbRelationship(name);
        dr1.setSourceEntity(src);
        dr1.setTargetEntity(target);
        src.addRelationship(dr1);
        return dr1;
    }

    protected ObjRelationship buildValidObjRelationship(DataMap map, String name) {
        DbRelationship dr1 = buildValidDbRelationship(map, "d" + name);

        ObjEntity src = new ObjEntity("ey" + counter++);
        src.setClassName("src");
        map.addObjEntity(src);
        src.setDbEntity((DbEntity) dr1.getSourceEntity());

        ObjEntity target = new ObjEntity("oey" + counter++);
        target.setClassName("target");
        map.addObjEntity(target);
        target.setDbEntity((DbEntity) dr1.getTargetEntity());

        ObjRelationship r1 = new ObjRelationship(name);
        r1.setTargetEntity(target);
        src.addRelationship(r1);

        r1.addDbRelationship(dr1);
        return r1;
    }

    protected ObjAttribute buildValidObjAttribute(DataMap map, String name) {
        DbAttribute a1 = new DbAttribute();
        a1.setName("d" + name);
        a1.setType(Types.CHAR);
        a1.setMaxLength(2);
        DbEntity e1 = new DbEntity("ex" + counter++);
        map.addDbEntity(e1);
        e1.addAttribute(a1);

        ObjEntity oe1 = new ObjEntity("oex" + counter++);
        map.addObjEntity(oe1);
        oe1.setDbEntity(e1);

        ObjAttribute oa1 = new ObjAttribute(name, "java.lang.Integer", oe1);
        oe1.addAttribute(oa1);
        oa1.setDbAttribute(a1);

        return oa1;
    }
}