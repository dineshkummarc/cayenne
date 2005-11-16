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
package org.objectstyle.cayenne.query;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.objectstyle.cayenne.map.Entity;
import org.objectstyle.cayenne.util.Util;
import org.objectstyle.cayenne.util.XMLEncoder;
import org.objectstyle.cayenne.util.XMLSerializable;

/**
 * Defines a node in a prefetch tree.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class PrefetchTreeNode implements Serializable, XMLSerializable {

    public static final int UNDEFINED_SEMANTICS = 0;
    public static final int JOINT_PREFETCH_SEMANTICS = 1;
    public static final int DISJOINT_PREFETCH_SEMANTICS = 2;

    protected String name;
    protected boolean phantom;
    protected int semantics;

    protected PrefetchTreeNode parent;

    // Using Collection instead of Map for children storage (even though there cases of
    // lookup by segment) is a reasonable tradeoff considering that
    // each node has no more than a few children and lookup by name doesn't happen on
    // traversal, only during creation.
    protected Collection children;

    /**
     * Creates a root node of the prefetch tree. Children can be added to the parent by
     * calling "addPath".
     */
    public PrefetchTreeNode() {
        this(null, null);
    }

    /**
     * Creates a phantom PrefetchTreeNode, initializing it with parent node and a name of
     * a relationship segment connecting this node with the parent.
     */
    protected PrefetchTreeNode(PrefetchTreeNode parent, String segmentPath) {
        this.parent = parent;
        this.name = segmentPath;
        this.phantom = true;
        this.semantics = UNDEFINED_SEMANTICS;
    }

    public void encodeAsXML(XMLEncoder encoder) {
        traverse(new XMLEncoderOperation(encoder));
    }

    /**
     * Returns the root of the node tree. Root is the topmost parent node that itself has
     * no parent set.
     */
    public PrefetchTreeNode getRoot() {
        return (parent != null) ? parent.getRoot() : this;
    }

    /**
     * Returns full prefetch path, that is a dot separated String of node names starting
     * from root and up to and including this node. Note that root "name" is considered to
     * be an empty string.
     */
    public String getPath() {
        if (parent == null) {
            return "";
        }

        StringBuffer path = new StringBuffer(getName());
        PrefetchTreeNode node = this.getParent();

        // root node has no path
        while (node.getParent() != null) {
            path.insert(0, node.getName() + ".");
            node = node.getParent();
        }

        return path.toString();
    }

    /**
     * Returns a collection of PrefetchTreeNodes with joint semantics.
     */
    public Collection jointNodes() {
        Collection c = new ArrayList();
        traverse(new CollectionBuilderOperation(c, false, true, false, false));
        return c;
    }

    /**
     * Returns a collection of PrefetchTreeNodes with dijoint semantics.
     */
    public Collection disjointNodes() {
        Collection c = new ArrayList();
        traverse(new CollectionBuilderOperation(c, true, false, false, false));
        return c;
    }

    /**
     * Returns a collection of PrefetchTreeNodes that are not phantoms.
     */
    public Collection nonPhantomNodes() {
        Collection c = new ArrayList();
        traverse(new CollectionBuilderOperation(c, true, true, true, false));
        return c;
    }

    /**
     * Traverses the tree depth-first, invoking callback methods of the processor when
     * passing through the nodes.
     */
    public void traverse(PrefetchProcessor processor) {

        boolean result = false;

        if (isPhantom()) {
            result = processor.startPhantomPrefetch(this);
        }
        else if (isDisjointPrefetch()) {
            result = processor.startDisjointPrefetch(this);
        }
        else if (isJointPrefetch()) {
            result = processor.startJointPrefetch(this);
        }
        else {
            result = processor.startUnknownPrefetch(this);
        }

        // process children unless processing is blocked...
        if (result && children != null) {
            Iterator it = children.iterator();
            while (it.hasNext()) {
                ((PrefetchTreeNode) it.next()).traverse(processor);
            }
        }

        // call finish regardless of whether children were processed
        processor.finishPrefetch(this);
    }

    /**
     * Looks up an existing node in the tree desribed by the dot-separated path. Will
     * return null if no matching child exists.
     */
    public PrefetchTreeNode getNode(String path) {
        if (Util.isEmptyString(path)) {
            throw new IllegalArgumentException("Empty path: " + path);
        }

        PrefetchTreeNode node = this;
        StringTokenizer toks = new StringTokenizer(path, Entity.PATH_SEPARATOR);
        while (toks.hasMoreTokens() && node != null) {
            String segment = toks.nextToken();
            node = node.getChild(segment);
        }

        return node;
    }

    /**
     * Adds a "path" with specified semantics to this prefetch node. All yet non-existent
     * nodes in the created path will be marked as phantom.
     * 
     * @return the last segment in the created path.
     */
    public PrefetchTreeNode addPath(String path) {
        if (Util.isEmptyString(path)) {
            throw new IllegalArgumentException("Empty path: " + path);
        }

        PrefetchTreeNode node = this;
        StringTokenizer toks = new StringTokenizer(path, Entity.PATH_SEPARATOR);
        while (toks.hasMoreTokens()) {
            String segment = toks.nextToken();

            PrefetchTreeNode child = node.getChild(segment);
            if (child == null) {
                child = new PrefetchTreeNode(node, segment);
                node.addChild(child);
            }

            node = child;
        }

        return node;
    }

    /**
     * Removes or makes phantom a node defined by this path. If the node for this path
     * doesn't have any children, it is removed, otherwise it is made phantom.
     */
    public void removePath(String path) {

        PrefetchTreeNode node = getNode(path);
        while (node != null) {

            if (node.children != null) {
                node.setPhantom(true);
                break;
            }

            String segment = node.getName();

            node = node.getParent();

            if (node != null) {
                node.removeChild(segment);
            }
        }
    }

    public void addChild(PrefetchTreeNode child) {

        if (Util.isEmptyString(child.getName())) {
            throw new IllegalArgumentException("Child has no segmentPath: " + child);
        }

        if (child.getParent() != this) {
            child.getParent().removeChild(child.getName());
            child.parent = this;
        }

        if (children == null) {
            children = new ArrayList(4);
        }

        children.add(child);
    }

    public void removeChild(PrefetchTreeNode child) {
        if (children != null && child != null) {
            children.remove(child);
            child.parent = null;
        }
    }

    protected void removeChild(String segment) {
        if (children != null) {
            PrefetchTreeNode child = (PrefetchTreeNode) getChild(segment);
            if (child != null) {
                children.remove(child);
                child.parent = null;
            }
        }
    }

    protected PrefetchTreeNode getChild(String segment) {
        if (children != null) {
            Iterator it = children.iterator();
            while (it.hasNext()) {
                PrefetchTreeNode next = (PrefetchTreeNode) it.next();
                if (segment.equals(next.getName())) {
                    return next;
                }
            }
        }

        return null;
    }

    public PrefetchTreeNode getParent() {
        return parent;
    }

    /**
     * Returns an unmodifiable collection of children.
     */
    public Collection getChildren() {
        return children == null ? Collections.EMPTY_SET : Collections
                .unmodifiableCollection(children);
    }

    public boolean hasChildren() {
        return children != null && !children.isEmpty();
    }

    public String getName() {
        return name;
    }

    public boolean isPhantom() {
        return phantom;
    }

    public void setPhantom(boolean phantom) {
        this.phantom = phantom;
    }

    public int getSemantics() {
        return semantics;
    }

    public void setSemantics(int semantics) {
        this.semantics = semantics;
    }

    public boolean isJointPrefetch() {
        return semantics == JOINT_PREFETCH_SEMANTICS;
    }

    public boolean isDisjointPrefetch() {
        return semantics == DISJOINT_PREFETCH_SEMANTICS;
    }

    // **** common tree operations

    // An operation that encodes prefetch tree as XML.
    class XMLEncoderOperation implements PrefetchProcessor {

        XMLEncoder encoder;

        XMLEncoderOperation(XMLEncoder encoder) {
            this.encoder = encoder;
        }

        public boolean startPhantomPrefetch(PrefetchTreeNode node) {
            // don't encode phantoms
            return true;
        }

        public boolean startDisjointPrefetch(PrefetchTreeNode node) {
            encoder.print("<prefetch type=\"disjoint\">");
            encoder.print(node.getPath());
            encoder.println("</prefetch>");
            return true;
        }

        public boolean startJointPrefetch(PrefetchTreeNode node) {
            encoder.print("<prefetch type=\"joint\">");
            encoder.print(node.getPath());
            encoder.println("</prefetch>");
            return true;
        }

        public boolean startUnknownPrefetch(PrefetchTreeNode node) {
            encoder.print("<prefetch>");
            encoder.print(node.getPath());
            encoder.println("</prefetch>");

            return true;
        }

        public void finishPrefetch(PrefetchTreeNode node) {
            // noop
        }
    }

    // An operation that collects all nodes in a single collection.
    class CollectionBuilderOperation implements PrefetchProcessor {

        Collection nodes;
        boolean includePhantom;
        boolean includeDisjoint;
        boolean includeJoint;
        boolean includeUnknown;

        CollectionBuilderOperation(Collection nodes, boolean includeDisjoint,
                boolean includeJoint, boolean includeUnknown, boolean includePhantom) {
            this.nodes = nodes;

            this.includeDisjoint = includeDisjoint;
            this.includeJoint = includeJoint;
            this.includeUnknown = includeUnknown;
            this.includePhantom = includePhantom;
        }

        public boolean startPhantomPrefetch(PrefetchTreeNode node) {
            if (includePhantom) {
                nodes.add(node);
            }

            return true;
        }

        public boolean startDisjointPrefetch(PrefetchTreeNode node) {
            if (includeDisjoint) {
                nodes.add(node);
            }
            return true;
        }

        public boolean startJointPrefetch(PrefetchTreeNode node) {
            if (includeJoint) {
                nodes.add(node);
            }
            return true;
        }

        public boolean startUnknownPrefetch(PrefetchTreeNode node) {
            if (includeUnknown) {
                nodes.add(node);
            }
            return true;
        }

        public void finishPrefetch(PrefetchTreeNode node) {
        }
    }
}
