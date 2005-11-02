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

package org.objectstyle.cayenne.regression;

import javax.sql.DataSource;

import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.conn.DataSourceInfo;
import org.objectstyle.cayenne.conn.PoolDataSource;
import org.objectstyle.cayenne.conn.PoolManager;
import org.objectstyle.cayenne.dba.AutoAdapter;

/**
 * Runs regression test based on a set of properties
 * 
 * @author Andrei Adamchik
 */
public class AntMain extends Main {

    /**
     * Uses System properties to configure regression tests.
     */
    public static void main(String[] args) {
        TestPreferences prefs;

        Configuration.configureCommonLogging();

        try {
            prefs = new TestPreferences(System.getProperties());
        }
        catch (Exception ex) {
            System.out.println("Fatal Error: " + ex.getMessage());
            System.exit(1);
            return;
        }

        if (new AntMain(prefs).execute()) {
            System.exit(1);
        }

        System.exit(0);
    }

    /**
     * Constructor for AntMain.
     */
    public AntMain(TestPreferences prefs) {
        super(prefs);
    }

    protected DataDomain createDomain() throws Exception {
        ClassLoader loader = new DOStubClassLoader();
        Thread.currentThread().setContextClassLoader(loader);

        DataSourceInfo info = ((TestPreferences) prefs).getConnectionInfo();

        // data source
        PoolDataSource poolDS = new PoolDataSource(info.getJdbcDriver(), info
                .getDataSourceUrl());
        DataSource ds = new PoolManager(poolDS, 1, 1, info.getUserName(), info
                .getPassword());

   
        DataNode node = new DataNode("node");
        node.setAdapter(new AutoAdapter(node));
        node.setDataSource(ds);

        // domain
        DataDomain domain = new DataDomain("domain");
        domain.addNode(node);
        return domain;
    }
}
