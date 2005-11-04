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
package org.objectstyle.cayenne.pref;

import java.util.Collections;

import org.objectstyle.cayenne.access.DataContext;

/**
 * An editor for modifying CayennePreferenceService.
 * 
 * @author Andrei Adamchik
 */
public abstract class CayennePreferenceEditor implements PreferenceEditor {

    protected CayennePreferenceService service;
    protected DataContext editingContext;
    protected boolean restartRequired;
    protected int saveInterval;

    public CayennePreferenceEditor(CayennePreferenceService service) {
        this.service = service;
        this.editingContext = service
                .getDataContext()
                .getParentDataDomain()
                .createDataContext();
        this.saveInterval = service.getSaveInterval();
    }

    protected boolean isRestartRequired() {
        return restartRequired;
    }

    protected void setRestartRequired(boolean restartOnSave) {
        this.restartRequired = restartOnSave;
    }

    protected DataContext getEditingContext() {
        return editingContext;
    }

    public Domain editableInstance(Domain object) {
        if (object.getDataContext() == getEditingContext()) {
            return object;
        }

        return (Domain) getEditingContext().localObjects(
                Collections.singletonList(object)).get(0);
    }

    public PreferenceDetail createDetail(Domain domain, String key) {
        domain = editableInstance(domain);
        DomainPreference preference = (DomainPreference) getEditingContext()
                .createAndRegisterNewObject(DomainPreference.class);
        preference.setDomain(domain);
        preference.setKey(key);

        return preference.getPreference();
    }

    public PreferenceDetail createDetail(Domain domain, String key, Class javaClass) {
        domain = editableInstance(domain);
        DomainPreference preferenceLink = (DomainPreference) getEditingContext()
                .createAndRegisterNewObject(DomainPreference.class);
        preferenceLink.setDomain(domain);
        preferenceLink.setKey(key);

        PreferenceDetail detail = (PreferenceDetail) getEditingContext()
                .createAndRegisterNewObject(javaClass);

        detail.setDomainPreference(preferenceLink);
        return detail;
    }

    public PreferenceDetail deleteDetail(Domain domain, String key) {
        domain = editableInstance(domain);
        PreferenceDetail detail = domain.getDetail(key, false);

        if (detail != null) {
            DomainPreference preference = detail.getDomainPreference();
            preference.setDomain(null);
            getEditingContext().deleteObject(preference);
            getEditingContext().deleteObject(detail);
        }

        return detail;
    }

    public int getSaveInterval() {
        return saveInterval;
    }

    public void setSaveInterval(int ms) {
        if (saveInterval != ms) {
            saveInterval = ms;
            restartRequired = true;
        }
    }

    public PreferenceService getService() {
        return service;
    }

    public void save() {
        service.setSaveInterval(saveInterval);
        editingContext.commitChanges();

        if (restartRequired) {
            restart();
        }
    }

    public void revert() {
        editingContext.rollbackChanges();
        restartRequired = false;
    }

    protected abstract void restart();
}