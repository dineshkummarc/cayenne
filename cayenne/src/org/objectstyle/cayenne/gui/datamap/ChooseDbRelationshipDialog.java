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
package org.objectstyle.cayenne.gui.datamap;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

import javax.swing.*;

import org.objectstyle.cayenne.gui.Editor;
import org.objectstyle.cayenne.gui.PanelFactory;
import org.objectstyle.cayenne.gui.util.GUIUtil;
import org.objectstyle.cayenne.gui.util.RelationshipWrapper;
import org.objectstyle.cayenne.map.*;

/** 	Used to select the DbRelationship for ObjRelationship mapping. 
 *  	Allows selecting the relaitonship, canceling, edit the relationship and
 *  create new relationship. It is needed for the
 *  cases when there is more than one DbRelationship between start and
 *  end entities, like in the case when the DbRelationship starts and
 *  ends in the same DbEntity.
 *  	The choice is returned in getChoice() method. If choice is SELECT or EDIT,
 *  the selected DbRelationship may be retrieved by getDbRelationship(),
 *  which will return the list with one DbRelationship. List is used for the
 *  future expansion, when one ObjRelaitonship will be mapped for 
 *  multiple DbRelaitonship-s. If user clicks SELECT and no relationship is selected,
 *  
 *  Existing mapping for this ObjRelationship is pre-selected in the combo box.
 *  Combo box contains the DbRelaitonship-s between the start and  end DbEntity-s
 */
public class ChooseDbRelationshipDialog extends JDialog
implements ActionListener
{
	public static final int SELECT 	= 0;
	public static final int CANCEL 	= 1;
	public static final int NEW		= 2;
	public static final int EDIT	= 3;

	private DataMap map;
	private DbEntity start;
	private DbEntity end;
	private DbRelationship dbRel;
	private ArrayList relList = new ArrayList();
	
	JComboBox relSelect	= new JComboBox();
	JButton select		= new JButton("Select");
	JButton cancel		= new JButton("Cancel");
	JButton create		= new JButton("New");
	JButton edit		= new JButton("Edit");
	private int choice  = CANCEL;

	public ChooseDbRelationshipDialog(DataMap temp_map, java.util.List db_rel_list
				, DbEntity temp_start, DbEntity temp_end, boolean to_many)
	{
		super(Editor.getFrame(), "Select DbRelationship", true);
		map = temp_map;
		start = temp_start;
		end = temp_end;
		
		// Find matching relationship in the start DbEntity
		java.util.List list = temp_start.getRelationshipList();
		Iterator iter = list.iterator();
		while (iter.hasNext()) {
			DbRelationship db_rel = (DbRelationship)iter.next();
			if (db_rel.getTargetEntity() == temp_end) {
				relList.add(db_rel);
			}
		}

		// If DbRelationship does not exist, create it.
		if (null != db_rel_list && db_rel_list.size() > 0) {
			dbRel = (DbRelationship)db_rel_list.get(0);
		}
		
		init();
		
		this.pack();
        GUIUtil.centerWindow(this);

		select.addActionListener(this);
		cancel.addActionListener(this);
		create.addActionListener(this);
		edit.addActionListener(this);
	}
	
	
	/** Sets up the graphical components. */
	private void init() {
		getContentPane().setLayout(new BorderLayout());	
		
		relSelect.setBackground(Color.WHITE);		
		
		DefaultComboBoxModel model = new DefaultComboBoxModel();
		RelationshipWrapper sel_item = new RelationshipWrapper(null);
		model.addElement(sel_item);
		Iterator iter = relList.iterator();
		while (iter.hasNext()) {
			DbRelationship db_rel = (DbRelationship) iter.next();
			RelationshipWrapper wrap = new RelationshipWrapper(db_rel);
			model.addElement(wrap);
			if (dbRel != null && db_rel == dbRel) {	
				sel_item = wrap;
			}
		}		
		model.setSelectedItem(sel_item);
		relSelect.setModel(model);
		
		
		JPanel buttons = GUIUtil.createButtonPanel(new JButton[] {select, cancel, create, edit});		
		
		Component[] left = new Component[] {
			new JLabel("Relationships: "), new JLabel()
		};

		Component[] right = new Component[] {
			relSelect, buttons
		};
		
		JPanel panel = PanelFactory.createForm(left, right, 5, 5, 5, 5);
		getContentPane().add(panel, BorderLayout.CENTER);
	}
	
	
	public java.util.List getDbRelationshipList() {
		if (getChoice() != SELECT && getChoice() != EDIT)
			return null;
		ArrayList list = new ArrayList();
		if (dbRel != null)
			list.add(dbRel);
		return list;
	}


	public int getChoice() {
		return choice;
	}
		
			
	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();
		if (src == this.select) {
			processSelect();
		} else if (src == this.cancel) {
			processCancel();
		} else if (src == this.edit) {
			processEdit();
		} else if (src == this.create) {
			processNew();
		}
	}

	private void processSelect() {
		RelationshipWrapper wrap;
		wrap = (RelationshipWrapper)relSelect.getSelectedItem();
		if (null != wrap && wrap.getRelationship() != null)
			dbRel = (DbRelationship)wrap.getRelationship();
		else
			dbRel = null;
		choice = SELECT;
		hide();
	}

	private void processEdit() {
		RelationshipWrapper wrap;
		wrap = (RelationshipWrapper)relSelect.getSelectedItem();
		if (null == wrap || wrap.getRelationship() == null) {
			JOptionPane.showMessageDialog(Editor.getFrame(),
											"Select the relationship");
			return;
		}
		dbRel = (DbRelationship)wrap.getRelationship();
		choice = EDIT;
		hide();
	}
	
	private void processCancel() {
		dbRel = null;
		choice = CANCEL;
		hide();
	}
	
	private void processNew() {
		dbRel = null;
		choice = NEW;
		hide();
	}
	

}