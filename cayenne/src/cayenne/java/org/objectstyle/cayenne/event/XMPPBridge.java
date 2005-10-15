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
package org.objectstyle.cayenne.event;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.jivesoftware.smack.GroupChat;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.util.Base64Codec;
import org.objectstyle.cayenne.util.Util;

/**
 * An EventBridge implementation based on XMPP protocol and Smack XMPP client library.
 * What's good about XMPP (Extensible Messaging and Presence Protocol, an IETF standard
 * protocol that grew up from Jabber IM) is that generally it has fewer or no deployment
 * limitations (unlike JMS and JGroups that are generally a good solution for local
 * controlled networks). Also it provides lots of additional information for free, such as
 * presence, and much more.
 * <p>
 * This implememtation is based on Smack XMPP client library from JiveSoftware.
 * </p>
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class XMPPBridge extends EventBridge {

    protected String xmppHost;
    protected int xmppPort;
    protected String sessionHandle;

    protected XMPPConnection connection;
    protected GroupChat groupChat;
    protected boolean connected;

    /**
     * Creates an XMPPBridge. External subject will be used as the chat group name.
     */
    public XMPPBridge(EventSubject localSubject, String externalSubject) {
        super(localSubject, externalSubject);

        // generate a unique session handle... users can override it to use a specific
        // handle...
        this.sessionHandle = "cayenne-xmpp-" + System.currentTimeMillis();
    }

    public String getXmppHost() {
        return xmppHost;
    }

    public void setXmppHost(String xmppHost) {
        this.xmppHost = xmppHost;
    }

    public int getXmppPort() {
        return xmppPort;
    }

    public void setXmppPort(int xmppPort) {
        this.xmppPort = xmppPort;
    }

    public String getSessionHandle() {
        return sessionHandle;
    }

    public void setSessionHandle(String sessionHandle) {
        this.sessionHandle = sessionHandle;
    }

    protected void startupExternal() throws Exception {

        // validate settings
        if (xmppHost == null) {
            throw new CayenneRuntimeException("Null 'xmppHost', can't start XMPPBridge");
        }

        // shutdown old bridge
        if (connected) {
            shutdownExternal();
        }

        // connect
        this.connection = xmppPort > 0
                ? new XMPPConnection(xmppHost, xmppPort)
                : new XMPPConnection(xmppHost);

        connection.loginAnonymously();

        this.groupChat = connection.createGroupChat(externalSubject
                + "."
                + connection.getHost());
        groupChat.join(sessionHandle);
        groupChat.addMessageListener(new XMPPListener());
        this.connected = true;
    }

    protected void shutdownExternal() throws Exception {
        if (groupChat != null) {
            groupChat.leave();
            groupChat = null;
        }

        if (connection != null) {
            connection.close();
            connection = null;
        }

        connected = false;
    }

    protected void sendExternalEvent(CayenneEvent localEvent) throws Exception {
        groupChat.sendMessage(serializeToString(localEvent));
    }

    class XMPPListener implements PacketListener {

        public void processPacket(Packet packet) {

            if (packet instanceof Message) {
                Message message = (Message) packet;
                String payload = message.getBody();

                try {
                    Object event = deserializeFromString(payload);
                    if (event instanceof CayenneEvent) {
                        onExternalEvent((CayenneEvent) event);
                    }
                }
                catch (Exception ex) {
                    // ignore for now... need to add logging.
                }
            }
        }
    }

    /**
     * Decodes the String (assuming it is using Base64 encoding), and then deserializes
     * object from the byte array.
     */
    static Object deserializeFromString(String string) throws Exception {
        if (Util.isEmptyString(string)) {
            return null;
        }

        byte[] bytes = Base64Codec.decodeBase64(string.getBytes());
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
        Object object = in.readObject();
        in.close();
        return object;
    }

    /**
     * Serializes object and then encodes it using Base64 encoding.
     */
    static String serializeToString(Object object) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bytes);
        out.writeObject(object);
        out.close();

        return new String(Base64Codec.encodeBase64(bytes.toByteArray()));
    }
}