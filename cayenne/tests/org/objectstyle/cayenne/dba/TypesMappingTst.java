package org.objectstyle.cayenne.dba;
/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002 The ObjectStyle Group 
 * and individual authors of the software.  All rights reserved.
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
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:  
 *       "This product includes software developed by the 
 *        ObjectStyle Group (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "ObjectStyle Group" and "Cayenne" 
 *    must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written 
 *    permission, please contact andrus@objectstyle.org.
 *
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    nor may "ObjectStyle" appear in their names without prior written
 *    permission of the ObjectStyle Group.
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
 * individuals on behalf of the ObjectStyle Group.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 *
 */ 

import junit.framework.*;
import junit.runner.*;
import java.util.logging.*;
import java.util.*;
import java.io.*;
import org.objectstyle.util.*;
import java.sql.*;


public class TypesMappingTst extends TypesMappingBase {
    
    
    public TypesMappingTst(String name) {
        super(name);
    }
    
    
    public void testFuzzyDataType() throws java.lang.Exception {
        assertNotNull(typeMap.getFuzzyDataType(Types.INTEGER));
    }
    
    public void testTypeInfo() throws java.lang.Exception {
        
        // we can not make real assertions about database types,
        // since the test suite might be run on different databases
        // instead do some formal assertions
        assertTrue(typeMap.getDatabaseTypes(Types.NUMERIC).length > 0);
        assertTrue(typeMap.getDatabaseTypes(Types.INTEGER).length > 0);
        assertTrue(typeMap.getDatabaseTypes(Types.CHAR).length > 0);
        // at least 1 date type should be there 
        assertTrue(
        typeMap.getDatabaseTypes(Types.DATE).length > 0  ||
        typeMap.getDatabaseTypes(Types.TIMESTAMP).length > 0
        );
   }
   
   public void testTypeInfoCompleteness() throws java.lang.Exception {
       // check counts 
       // since more then 1 database type can map to a single JDBC type
       Connection conn = org.objectstyle.TestMain.getSharedConnection();
       DatabaseMetaData md = conn.getMetaData();
       ResultSet rs = md.getTypeInfo();
       
       int len = 0;
       try {
           while(rs.next()) {
               len++;
           }
       }
       finally {
           rs.close();
       }
       
       int actualLen = 0;
       Iterator it = typeMap.databaseTypes.keySet().iterator();
       while(it.hasNext()) {
           ArrayList vals = (ArrayList)typeMap.databaseTypes.get(it.next());
           actualLen += vals.size();
       }
       
       // this is bad assertion, since due to some hacks 
       // the same database types may map more then once,
       // so we have to use <=
       assertTrue(len <= actualLen);
    }
}
