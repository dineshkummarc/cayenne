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
package org.objectstyle.cayenne.conf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Iterator;

/**
 * Interface defines API to check the status of Cayenne configuration.
 * 
 * @author Andrei Adamchik
 */
public class ConfigStatus {

    protected List otherFailures = new ArrayList();
    protected Map failedMaps = new HashMap();
    protected Map failedAdapters = new HashMap();
    protected Map failedDataSources = new HashMap();
    protected List failedMapRefs = new ArrayList();

    /**
     * Returns a String description of failed configuration pieces.
     * Returns a canned "no failures" message if no failures occurred.
     */
    public String describeFailures() {
    	if(!hasFailures()) {
    		return "[No failures]";
    	}
    	
    	StringBuffer buf = new StringBuffer();
    	
    	Iterator it = failedMaps.keySet().iterator();
    	while(it.hasNext()) {
    		String name = (String)it.next();
    		String location = (String)failedMaps.get(name);
    		buf.append("\n\tdomain.map.name=").append(name).append(", domain.map.location=").append(location);
    	}
 
		it = failedAdapters.keySet().iterator();
		while(it.hasNext()) {
			String node = (String)it.next();
			String adapter = (String)failedAdapters.get(node);
			buf.append("\n\tdomain.node.name=").append(node).append(", domain.node.adapter=").append(adapter);
		}
		
    	it = failedDataSources.keySet().iterator();
		while(it.hasNext()) {
			String node = (String)it.next();
			String location = (String)failedDataSources.get(node);
			buf.append("\n\tdomain.node.name=").append(node).append(", domain.node.datasource=").append(location);
		}
		
		it = failedMapRefs.iterator();
		while(it.hasNext()) {
			String mapName = (String)it.next();
			// don't report failed links if the DataMap itself failed to load
			if(failedMaps.get(mapName) == null) {
				buf.append("\n\tdomain.node.map-ref.name=").append(mapName);
			}
		}
    	return buf.toString();
    }
    
    /**
     * Returns a list of error messages not directly associated with project
     * objects, such as XML pare exceptions, IOExceptions, etc.
     */
    public List getOtherFailures() {
        return otherFailures;
    }

    /** 
     * Returns a list of map reference names that failed to load.
     */
    public List getFailedMapRefs() {
        return failedMapRefs;
    }

    /** 
     * Returns a map of locations for names of the data maps that failed to
     * load.
     */
    public Map getFailedMaps() {
        return failedMaps;
    }

    /**
     * Returns a map of DataSource locations for node names that failed to load.
     */
    public Map getFailedDataSources() {
        return failedDataSources;
    }

    /** 
     * Returns a map of adapter classes for node names that failed to load.
     */
    public Map getFailedAdapters() {
        return failedAdapters;
    }

    /**
     * Returns true if any of the "failed.." methods return true.
     */
    public boolean hasFailures() {
        return failedMaps.size() > 0
            || failedDataSources.size() > 0
            || failedAdapters.size() > 0
            || failedMapRefs.size() > 0
            || otherFailures.size() > 0;
    }
}
