/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
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

package org.objectstyle.cayenne.modeler.editor;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.dba.TypesMapping;
import org.objectstyle.cayenne.map.Procedure;
import org.objectstyle.cayenne.map.ProcedureParameter;
import org.objectstyle.cayenne.map.event.MapEvent;
import org.objectstyle.cayenne.map.event.ProcedureEvent;
import org.objectstyle.cayenne.map.event.ProcedureParameterEvent;
import org.objectstyle.cayenne.map.event.ProcedureParameterListener;
import org.objectstyle.cayenne.modeler.EventController;
import org.objectstyle.cayenne.modeler.PanelFactory;
import org.objectstyle.cayenne.modeler.event.ProcedureDisplayEvent;
import org.objectstyle.cayenne.modeler.event.ProcedureDisplayListener;
import org.objectstyle.cayenne.modeler.event.ProcedureParameterDisplayEvent;
import org.objectstyle.cayenne.modeler.util.CayenneTable;
import org.objectstyle.cayenne.modeler.util.CayenneWidgetFactory;
import org.objectstyle.cayenne.modeler.util.UIUtil;

/**
 * @author Andrei Adamchik
 */
public class ProcedureParameterTab
    extends JPanel
    implements
        ProcedureParameterListener,
        ProcedureDisplayListener,
        ExistingSelectionProcessor,
        ActionListener {

    private static Logger logObj = Logger.getLogger(ProcedureParameterTab.class);

    protected EventController eventController;

    protected CayenneTable table;
    protected JButton moveUp;
    protected JButton moveDown;

    public ProcedureParameterTab(EventController eventController) {
        this.eventController = eventController;

        init();

        eventController.addProcedureDisplayListener(this);
        eventController.addProcedureParameterListener(this);

        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                ProcedureParameterTab.this.processExistingSelection();
            }
        });
        
        moveDown.addActionListener(this);
        moveUp.addActionListener(this);
    }

    protected void init() {
        setLayout(new BorderLayout());

        moveUp = new JButton("Up");
        moveDown = new JButton("Down");

        // Create table with two columns and no rows.
        table = new CayenneTable();
        JPanel panel =
            PanelFactory.createTablePanel(table, new JButton[] { moveUp, moveDown });
        add(panel, BorderLayout.CENTER);
    }

    public void processExistingSelection() {

        ProcedureParameter parameter = null;
        boolean enableButtons = false;

        if (table.getSelectedRow() >= 0) {
            ProcedureParameterTableModel model =
                (ProcedureParameterTableModel) table.getModel();
            parameter = model.getParameter(table.getSelectedRow());

            // scroll table
            UIUtil.scrollToSelectedRow(table);

            if (table.getRowCount() > 1) {
                enableButtons = true;
            }
        }

        moveUp.setEnabled(enableButtons);
        moveDown.setEnabled(enableButtons);

        ProcedureParameterDisplayEvent e =
            new ProcedureParameterDisplayEvent(
                this,
                parameter,
                eventController.getCurrentProcedure(),
                eventController.getCurrentDataMap(),
                eventController.getCurrentDataDomain());
        eventController.fireProcedureParameterDisplayEvent(e);
    }

    /**
      * Invoked when currently selected Procedure object is changed.
      */
    public void currentProcedureChanged(ProcedureDisplayEvent e) {
        Procedure procedure = e.getProcedure();
        if (procedure != null && e.isProcedureChanged()) {
            rebuildTable(procedure);
        }
    }

    /**
     * Selects a specified parameter.
     */
    public void selectParameter(ProcedureParameter parameter) {
        if (parameter == null) {
            return;
        }

        ProcedureParameterTableModel model =
            (ProcedureParameterTableModel) table.getModel();
        java.util.List parameters = model.getObjectList();
        int pos = parameters.indexOf(parameter);
        if (pos >= 0) {
            table.select(pos);
        }
    }

    protected void rebuildTable(Procedure procedure) {
        ProcedureParameterTableModel model =
            new ProcedureParameterTableModel(procedure, eventController, this);

        table.setModel(model);
        table.setRowHeight(25);
        table.setRowMargin(3);

        // number column tweaking
        TableColumn numberColumn =
            table.getColumnModel().getColumn(
                ProcedureParameterTableModel.PARAMETER_NUMBER);
        numberColumn.setPreferredWidth(35);
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setHorizontalAlignment(SwingConstants.CENTER);
        numberColumn.setCellRenderer(renderer);

        // name column tweaking
        TableColumn nameColumn =
            table.getColumnModel().getColumn(ProcedureParameterTableModel.PARAMETER_NAME);
        nameColumn.setMinWidth(150);

        // types column tweaking
        TableColumn typesColumn =
            table.getColumnModel().getColumn(ProcedureParameterTableModel.PARAMETER_TYPE);
        typesColumn.setMinWidth(90);

        JComboBox typesEditor =
            CayenneWidgetFactory.createComboBox(TypesMapping.getDatabaseTypes(), true);
        typesEditor.setEditable(true);
        typesColumn.setCellEditor(new DefaultCellEditor(typesEditor));

        // direction column tweaking
        TableColumn directionColumn =
            table.getColumnModel().getColumn(
                ProcedureParameterTableModel.PARAMETER_DIRECTION);
        directionColumn.setMinWidth(90);

        JComboBox directionEditor =
            CayenneWidgetFactory.createComboBox(
                ProcedureParameterTableModel.PARAMETER_DIRECTION_NAMES,
                false);
        directionEditor.setEditable(false);
        directionColumn.setCellEditor(new DefaultCellEditor(directionEditor));

        moveUp.setEnabled(false);
        moveDown.setEnabled(false);
    }

    public void procedureParameterAdded(ProcedureParameterEvent e) {
        rebuildTable(e.getParameter().getProcedure());
        table.select(e.getParameter());
    }

    public void procedureParameterChanged(ProcedureParameterEvent e) {
        table.select(e.getParameter());
    }

    public void procedureParameterRemoved(ProcedureParameterEvent e) {
        ProcedureParameterTableModel model =
            (ProcedureParameterTableModel) table.getModel();
        int ind = model.getObjectList().indexOf(e.getParameter());
        model.removeRow(e.getParameter());
        table.select(ind);
    }

    public void actionPerformed(ActionEvent e) {
        ProcedureParameterTableModel model =
            (ProcedureParameterTableModel) table.getModel();
        ProcedureParameter parameter = eventController.getCurrentProcedureParameter();
        int index = -1;

        if (e.getSource() == moveUp) {
            index = model.moveRowUp(parameter);
        }
        else if (e.getSource() == moveDown) {
            index = model.moveRowDown(parameter);
        }

        if (index >= 0) {
            table.select(index);
            
            // note that 'setCallParameters' is donw by copy internally
			parameter.getProcedure().setCallParameters(model.getObjectList());
            eventController.fireProcedureEvent(
                new ProcedureEvent(this, parameter.getProcedure(), MapEvent.CHANGE));
        }
    }
}
