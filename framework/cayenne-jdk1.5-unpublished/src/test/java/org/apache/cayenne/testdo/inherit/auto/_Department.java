package org.apache.cayenne.testdo.inherit.auto;

import java.util.List;

/** Class _Department was generated by Cayenne.
  * It is probably a good idea to avoid changing this class manually, 
  * since it may be overwritten next time code is regenerated. 
  * If you need to make any customizations, please use subclass. 
  */
public class _Department extends org.apache.cayenne.CayenneDataObject {

    public static final String NAME_PROPERTY = "name";
    public static final String EMPLOYEES_PROPERTY = "employees";
    public static final String TO_MANAGER_PROPERTY = "toManager";

    public static final String DEPARTMENT_ID_PK_COLUMN = "DEPARTMENT_ID";

    public void setName(String name) {
        writeProperty("name", name);
    }
    public String getName() {
        return (String)readProperty("name");
    }
    
    
    public void addToEmployees(org.apache.cayenne.testdo.inherit.Employee obj) {
        addToManyTarget("employees", obj, true);
    }
    public void removeFromEmployees(org.apache.cayenne.testdo.inherit.Employee obj) {
        removeToManyTarget("employees", obj, true);
    }
    public List getEmployees() {
        return (List)readProperty("employees");
    }
    
    
    public void setToManager(org.apache.cayenne.testdo.inherit.Manager toManager) {
        setToOneTarget("toManager", toManager, true);
    }

    public org.apache.cayenne.testdo.inherit.Manager getToManager() {
        return (org.apache.cayenne.testdo.inherit.Manager)readProperty("toManager");
    } 
    
    
}