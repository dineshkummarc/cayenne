/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.modeler.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.DefaultButtonModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.modeler.util.CellRenderers;

/**
 * Swing component displaying results produced by search feature.
 */
public class FindDialogView extends JDialog {

    private JButton okButton;
    private java.util.List entityButtons;
    private static JScrollPane scrollPane;

    public FindDialogView(Map objEntityNames, Map dbEntityNames, Map attrNames,
            Map relatNames) {
        entityButtons = new ArrayList();

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        if (objEntityNames.isEmpty()
                && dbEntityNames.isEmpty()
                && attrNames.isEmpty()
                && relatNames.isEmpty()) {
            panel.add(new JLabel("Nothing found!"));
        } else {
            panel.add(createResultPanel(objEntityNames, CellRenderers
                    .iconForObject(new ObjEntity())));
            panel.add(createResultPanel(dbEntityNames, CellRenderers
                    .iconForObject(new DbEntity())));
            panel.add(createResultPanel(attrNames, CellRenderers
                    .iconForObject(new ObjAttribute())));
            panel.add(createResultPanel(relatNames, CellRenderers
                    .iconForObject(new ObjRelationship())));
        }

            JPanel okPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            okButton = new JButton("OK");
            okPanel.add(okButton);  
            
            JComponent contentPane = (JComponent) getContentPane();
            
            contentPane.setLayout(new BorderLayout());
            
            scrollPane = new JScrollPane(
                    panel,
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            
            contentPane.add(scrollPane);
            contentPane.add(okPanel, BorderLayout.SOUTH);
            
            contentPane.setPreferredSize(new Dimension(400, 325));            
            setTitle("Search results"); 
    }

    private JPanel createResultPanel(Map names, Icon icon) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        Iterator it = names.keySet().iterator();
        while (it.hasNext()) {
            Integer index = (Integer) it.next();
            JButton b = new JButton((String) names.get(index), icon);
            b.setBorder(new EmptyBorder(2, 10, 2, 10)); // top, left, bottom, right
            b.setModel(new EntityButtonModel(index));
            panel.add(b);

            entityButtons.add(b);
        }

        return panel;
    }

    public JButton getOkButton() {
        return okButton;
    }

    public java.util.List getEntityButtons() {
        return entityButtons;
    }

    public class EntityButtonModel extends DefaultButtonModel {

        private Integer index;

        EntityButtonModel(Integer index) {
            super();
            this.index = index;
        }

        public Integer getIndex() {
            return index;
        }
    }

    
    public static void scrollPaneToBottom() {
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                scrollPane.getVerticalScrollBar().setValue(
                        scrollPane.getVerticalScrollBar().getMaximum());
            }
        });
    }
    
    public static void scrollPaneToUp() {
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                scrollPane.getVerticalScrollBar().setValue(
                        scrollPane.getVerticalScrollBar().getMinimum());
            }
        });
    }
  
    public static void scrollPaneToPosition(final int position) {
      
      SwingUtilities.invokeLater(new Runnable() {
          public void run() {
              scrollPane.getVerticalScrollBar().setValue(position);
          }
      });
   }
}
