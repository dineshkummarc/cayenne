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
package org.objectstyle.cayenne.opp.hessian;

import org.objectstyle.cayenne.opp.OPPMessage;

/**
 * Service interface needed for server-side deployment with HessianConnector. A mapping in
 * <code>web.xml</code> may look like this:
 * 
 * <pre>
 *   &lt;servlet&gt;
 *     &lt;servlet-name&gt;cayenne&lt;/servlet-name&gt;
 *     &lt;servlet-class&gt;org.objectstyle.cayenne.opp.hessian.HessianServlet&lt;/servlet-class&gt;
 *   &lt;/servlet&gt;            
 *   &lt;servlet-mapping&gt;
 *     &lt;servlet-name&gt;cayenne&lt;/servlet-name&gt;
 *     &lt;url-pattern&gt;/cayenne&lt;/url-pattern&gt;
 *   &lt;/servlet-mapping&gt;
 * </pre>
 * 
 * To deploy with XMPP event bridge that allows clients to receive server events you will
 * need to add a few more "init-params" to the "servlet" section:
 * 
 * <pre>
 *     &lt;init-param&gt;
 *        &lt;param-name&gt;cayenne.HessianService.EventBridge.factory&lt;/param-name&gt;
 *        &lt;param-value&gt;org.objectstyle.cayenne.event.XMPPBridgeFactory&lt;/param-value&gt;
 *     &lt;/init-param&gt;
 *     &lt;init-param&gt;
 *        &lt;param-name&gt;cayenne.XMPPBridge.xmppHost&lt;/param-name&gt;
 *        &lt;param-value&gt;my-xmpp-server.com&lt;/param-value&gt;
 *     &lt;/init-param&gt;
 *     &lt;init-param&gt;
 *        &lt;param-name&gt;cayenne.XMPPBridge.xmppPort&lt;/param-name&gt;
 *        &lt;param-value&gt;3333&lt;/param-value&gt;
 *     &lt;/init-param&gt;
 *     &lt;init-param&gt;
 *        &lt;param-name&gt;cayenne.XMPPBridge.xmppChatService&lt;/param-name&gt;
 *        &lt;param-value&gt;conferencelt;/param-value&gt;
 *     &lt;/init-param&gt;
 * </pre>
 * 
 * <i>Parameters above will likely be configurable via the Modeler in the future.</i>
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public interface HessianService {

    public static final String EVENT_BRIDGE_FACTORY_PROPERTY = "cayenne.HessianService.EventBridge.factory";

    /**
     * Establishes a dedicated session with Cayenne OPPChannel, returning session id.
     */
    HessianSession establishSession();

    /**
     * Creates a new session with the specified or joins an existing one. This method is
     * used to bootstrap collaborating clients of a single "group chat".
     */
    HessianSession establishSharedSession(String name);

    /**
     * Processes message on a remote server, returning the result of such processing.
     */
    Object processMessage(String sessionId, OPPMessage message) throws Throwable;
}