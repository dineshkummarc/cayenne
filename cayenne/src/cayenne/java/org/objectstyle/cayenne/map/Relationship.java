/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002-2003 The ObjectStyle Group 
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
package org.objectstyle.cayenne.map;

/** Superclass of metadata relationship classes. */
public abstract class Relationship extends MapObject {
	protected String targetEntityName;
	protected boolean toMany;

	public Relationship() {
		super();
	}

	public Relationship(String name) {
		this();
		this.setName(name);
	}

	/** Returns relationship source entity. */
	public Entity getSourceEntity() {
		return (Entity)this.getParent();
	}

	/** Sets relationship source entity. */
	public void setSourceEntity(Entity sourceEntity) {
		setParent(sourceEntity);
	}

	/** Returns relationship target entity. */
	public abstract Entity getTargetEntity();

	/** 
	 * Sets relationship target entity. Internally
	 * calls <code>setTargetEntityName</code>.
	 */
	public void setTargetEntity(Entity targetEntity) {
		if (targetEntity != null) {
			this.setTargetEntityName(targetEntity.getName());
		} else {
			this.setTargetEntityName(null);
		}
	}

	/**
	 * Returns the targetEntityName.
	 * @return String
	 */
	public String getTargetEntityName() {
		return targetEntityName;
	}

	/**
	 * Sets the targetEntityName.
	 * @param targetEntityName The targetEntityName to set
	 */
	public void setTargetEntityName(String targetEntityName) {
		this.targetEntityName = targetEntityName;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer(this.getClass().getName());
		sb.append(" - relationship name '").append(this.getName());
		if (toMany) {
			sb.append("' (to-many)\n");
		}
		else {
			sb.append("' (to-one)\n");
		}

		sb.append("Source entity: ");
		Entity src = this.getSourceEntity();
		if (src == null) {
			sb.append("<null>");
		} else {
			sb.append(src.getName());
		}

		sb.append(" Target entity: ");
		Entity target = this.getTargetEntity();
		if (target == null)
			sb.append("<null>");
		else
			sb.append(target.getName());

		sb.append("\n------------------\n");
		return sb.toString();
	}
	
	/** Is relationship from source to target to-one or to-many.
	  * If one-to-many, getxxx() method of the data object class would 
	  * return a list, otherwise it returns a single DataObject
	  * There is explicitly no setToMany on Relationship.. only DbRelationship
	  * supports such a notion, and ObjRelationship derives it's value from the
	  * underlying DbRelationship(s) */
	public boolean isToMany() {
		return toMany;
	}
	
}
