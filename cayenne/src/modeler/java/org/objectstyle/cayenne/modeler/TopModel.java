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
package org.objectstyle.cayenne.modeler;

import javax.swing.ActionMap;

import org.objectstyle.cayenne.modeler.action.AboutAction;
import org.objectstyle.cayenne.modeler.action.CayenneAction;
import org.objectstyle.cayenne.modeler.action.ConfigureClasspathAction;
import org.objectstyle.cayenne.modeler.action.CreateAttributeAction;
import org.objectstyle.cayenne.modeler.action.CreateDataMapAction;
import org.objectstyle.cayenne.modeler.action.CreateDbEntityAction;
import org.objectstyle.cayenne.modeler.action.CreateDerivedDbEntityAction;
import org.objectstyle.cayenne.modeler.action.CreateDomainAction;
import org.objectstyle.cayenne.modeler.action.CreateNodeAction;
import org.objectstyle.cayenne.modeler.action.CreateObjEntityAction;
import org.objectstyle.cayenne.modeler.action.CreateQueryAction;
import org.objectstyle.cayenne.modeler.action.CreateRelationshipAction;
import org.objectstyle.cayenne.modeler.action.CreateStoredProcedureAction;
import org.objectstyle.cayenne.modeler.action.DerivedEntitySyncAction;
import org.objectstyle.cayenne.modeler.action.ExitAction;
import org.objectstyle.cayenne.modeler.action.GenerateClassesAction;
import org.objectstyle.cayenne.modeler.action.GenerateDbAction;
import org.objectstyle.cayenne.modeler.action.ImportDataMapAction;
import org.objectstyle.cayenne.modeler.action.ImportDbAction;
import org.objectstyle.cayenne.modeler.action.ImportEOModelAction;
import org.objectstyle.cayenne.modeler.action.NewProjectAction;
import org.objectstyle.cayenne.modeler.action.ObjEntitySyncAction;
import org.objectstyle.cayenne.modeler.action.OpenProjectAction;
import org.objectstyle.cayenne.modeler.action.ProjectAction;
import org.objectstyle.cayenne.modeler.action.RemoveAction;
import org.objectstyle.cayenne.modeler.action.SaveAction;
import org.objectstyle.cayenne.modeler.action.SaveAsAction;
import org.objectstyle.cayenne.modeler.action.ValidateAction;
import org.objectstyle.cayenne.project.Project;
import org.objectstyle.cayenne.project.ProjectPath;

/**
 * A top level MVC model object in CayenneModeler.
 * 
 * @author Andrei Adamchik
 */
public class TopModel {
    public static final String STATUS_MESSAGE_KEY = "statusMessage";

    protected Project currentProject;
    protected String statusMessage;
    protected ProjectPath selectedPath;
    protected ActionMap actionMap;

    /**
     * Constructor for TopModel.
     */
    public TopModel() {
        super();
        initEmptyActions();
    }

    protected void initEmptyActions() {
        // build action map
        actionMap = new ActionMap();

        CayenneAction closeProjectAction = new ProjectAction();
        actionMap.put(closeProjectAction.getKey(), closeProjectAction);

        CayenneAction newProjectAction = new NewProjectAction();
        newProjectAction.setAlwaysOn(true);
        actionMap.put(newProjectAction.getKey(), newProjectAction);

        CayenneAction openProjectAction = new OpenProjectAction();
        openProjectAction.setAlwaysOn(true);
        actionMap.put(openProjectAction.getKey(), openProjectAction);

        CayenneAction importMapAction = new ImportDataMapAction();
        actionMap.put(importMapAction.getKey(), importMapAction);

        CayenneAction saveAction = new SaveAction();
        actionMap.put(saveAction.getKey(), saveAction);
        
        CayenneAction saveAsAction = new SaveAsAction();
        actionMap.put(saveAsAction.getKey(), saveAsAction);

        CayenneAction validateAction = new ValidateAction();
        actionMap.put(validateAction.getKey(), validateAction);

        CayenneAction removeAction = new RemoveAction();
        actionMap.put(removeAction.getKey(), removeAction);

        CayenneAction createDomainAction = new CreateDomainAction();
        actionMap.put(createDomainAction.getKey(), createDomainAction);

        CayenneAction createNodeAction = new CreateNodeAction();
        actionMap.put(createNodeAction.getKey(), createNodeAction);

        CayenneAction createMapAction = new CreateDataMapAction();
        actionMap.put(createMapAction.getKey(), createMapAction);

        CayenneAction genClassesAction = new GenerateClassesAction();
        actionMap.put(genClassesAction.getKey(), genClassesAction);

        CayenneAction createOEAction = new CreateObjEntityAction();
        actionMap.put(createOEAction.getKey(), createOEAction);

        CayenneAction createDEAction = new CreateDbEntityAction();
        actionMap.put(createDEAction.getKey(), createDEAction);

        CayenneAction createDDEAction = new CreateDerivedDbEntityAction();
        actionMap.put(createDDEAction.getKey(), createDDEAction);

        CayenneAction createSPAction = new CreateStoredProcedureAction();
        actionMap.put(createSPAction.getKey(), createSPAction);
        
        CayenneAction createQueryAction = new CreateQueryAction();
        actionMap.put(createQueryAction.getKey(), createQueryAction);

        CayenneAction createAttrAction = new CreateAttributeAction();
        actionMap.put(createAttrAction.getKey(), createAttrAction);

        CayenneAction createRelAction = new CreateRelationshipAction();
        actionMap.put(createRelAction.getKey(), createRelAction);

        CayenneAction entSyncAction = new ObjEntitySyncAction();
        actionMap.put(entSyncAction.getKey(), entSyncAction);

        CayenneAction derivedResetAction = new DerivedEntitySyncAction();
        actionMap.put(derivedResetAction.getKey(), derivedResetAction);

        CayenneAction importDbAction = new ImportDbAction();
        actionMap.put(importDbAction.getKey(), importDbAction);

        CayenneAction importEOModelAction = new ImportEOModelAction();
        actionMap.put(importEOModelAction.getKey(), importEOModelAction);

        CayenneAction genDbAction = new GenerateDbAction();
        actionMap.put(genDbAction.getKey(), genDbAction);

        CayenneAction aboutAction = new AboutAction();
        aboutAction.setAlwaysOn(true);
        actionMap.put(aboutAction.getKey(), aboutAction);

        CayenneAction configClasspath = new ConfigureClasspathAction();
        configClasspath.setAlwaysOn(true);
        actionMap.put(configClasspath.getKey(), configClasspath);

        CayenneAction exitAction = new ExitAction();
        exitAction.setAlwaysOn(true);
        actionMap.put(exitAction.getKey(), exitAction);
    }

    /**
     * Returns an action for key.
     */
    public CayenneAction getAction(String key) {
        return (CayenneAction) actionMap.get(key);
    }

    public ActionMap getActionMap() {
        return actionMap;
    }

    /**
     * Returns the currentProject.
     * @return Project
     */
    public Project getCurrentProject() {
        return currentProject;
    }

    /**
     * Returns current project selection.
     */
    public ProjectPath getSelectedPath() {
        return selectedPath;
    }

    /**
     * Returns the statusMessage.
     * @return String
     */
    public String getStatusMessage() {
        return statusMessage;
    }

    /**
     * Sets the currentProject.
     * @param currentProject The currentProject to set
     */
    public void setCurrentProject(Project currentProject) {
        this.currentProject = currentProject;
    }

    /**
     * Sets the selectedPath.
     * @param selectedPath The selectedPath to set
     */
    public void setSelectedPath(ProjectPath selectedPath) {
        this.selectedPath = selectedPath;
    }

    public void setSelectedPath(Object obj) {
        selectedPath = new ProjectPath(obj);
    }

    /**
     * Sets the statusMessage.
     * @param statusMessage The statusMessage to set
     */
    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }
}
