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
package org.objectstyle.cayenne.modeler.event;

import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.Entity;

/**
 * Represents a display event of an Entity.
 * 
 * @author Misha Shengaout
 * @author Andrei Adamchik
 */
public class EntityDisplayEvent extends DataMapDisplayEvent {
	protected Entity entity;

	/** Reset the tab display to tab 0 */
	protected boolean tabReset;

	/** True if different from current entity */
	protected boolean entityChanged = true;
	protected boolean unselectAttributes;

	public EntityDisplayEvent(Object src, Entity entity) {
		this(src, entity, null, null, null);
	}

	public EntityDisplayEvent(
		Object src,
		Entity entity,
		DataMap map,
		DataDomain domain) {

		this(src, entity, map, null, domain);
	}

	public EntityDisplayEvent(
		Object src,
		Entity entity,
		DataMap map,
		DataNode node,
		DataDomain domain) {

		super(src, map, domain, node);
		this.entity = entity;
		setDataMapChanged(false);
	}

	/** Get [new current] entity (obj or db). */
	public Entity getEntity() {
		return entity;
	}

	public boolean isTabReset() {
		return tabReset;
	}

	/** True if entity different from current entity. */
	public boolean isEntityChanged() {
		return entityChanged;
	}
	public void setEntityChanged(boolean temp) {
		entityChanged = temp;
	}

	/**
	 * Returns the unselectAttributes.
	 * @return boolean
	 */
	public boolean isUnselectAttributes() {
		return unselectAttributes;
	}

	/**
	 * Sets the unselectAttributes.
	 * @param unselectAttributes The unselectAttributes to set
	 */
	public void setUnselectAttributes(boolean unselectAttributes) {
		this.unselectAttributes = unselectAttributes;
	}

	/**
	 * Sets the entity.
	 * @param entity The entity to set
	 */
	public void setEntity(Entity entity) {
		this.entity = entity;
	}

	/**
	 * Sets the tabReset.
	 * @param tabReset The tabReset to set
	 */
	public void setTabReset(boolean tabReset) {
		this.tabReset = tabReset;
	}
}