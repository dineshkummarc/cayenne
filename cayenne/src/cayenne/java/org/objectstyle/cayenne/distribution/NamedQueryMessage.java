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
package org.objectstyle.cayenne.distribution;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A ClientCommand that describes a call to a server-side named query.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class NamedQueryMessage extends AbstractMessage {

    protected String queryName;
    protected String[] parameterKeys;
    protected Object[] parameterValues;
    protected boolean selecting;
    protected boolean refresh;

    public NamedQueryMessage(String name, Map parameters, boolean selecting,
            boolean refresh) {

        this.queryName = name;
        this.selecting = selecting;
        this.refresh = refresh;

        parametersFromMap(parameters);
    }

    public NamedQueryMessage(String name, String[] parameterKeys,
            String[] parameterValues, boolean selecting, boolean refresh) {

        this.queryName = name;
        this.parameterKeys = parameterKeys;
        this.parameterValues = parameterValues;
        this.selecting = selecting;
        this.refresh = refresh;
    }

    /**
     * Converts a map of parameters to the internal representation as arrays of keys and
     * values.
     */
    protected void parametersFromMap(Map parameters) {
        int size = (parameters != null) ? parameters.size() : 0;

        if (size == 0) {
            this.parameterKeys = null;
            this.parameterValues = null;
            return;
        }

        String[] keys = new String[size];
        Object[] values = new Object[size];

        Iterator it = parameters.entrySet().iterator();
        for (int i = 0; i < size; i++) {
            Map.Entry entry = (Map.Entry) it.next();

            if (entry.getKey() != null) {
                keys[i] = entry.getKey().toString();
            }

            values[i] = entry.getValue();
        }

        this.parameterKeys = keys;
        this.parameterValues = values;
    }

    public Map getParameters() {
        if (parameterKeys == null) {
            return Collections.EMPTY_MAP;
        }

        Map map = new HashMap();
        for (int i = 0; i < parameterKeys.length; i++) {
            map.put(parameterKeys[i], parameterValues[i]);
        }

        return map;
    }

    public Object onReceive(ClientMessageHandler handler) {
        return handler.onNamedQuery(this);
    }

    /**
     * Invoked by the message sender to perform a remote selecting query.
     */
    public List sendPerformQuery(CayenneConnector connector) {
        return (List) send(connector, List.class);
    }

    /**
     * Invoked by the message sender to perform a remote non-selecting query.
     */
    public int[] sendPerformNonSelectingQuery(CayenneConnector connector) {
        return (int[]) send(connector, int[].class);
    }

    public String getQueryName() {
        return queryName;
    }

    public String[] getParameterKeys() {
        return parameterKeys;
    }

    public Object[] getParameterValues() {
        return parameterValues;
    }

    public boolean isRefresh() {
        return refresh;
    }

    public boolean isSelecting() {
        return selecting;
    }
}