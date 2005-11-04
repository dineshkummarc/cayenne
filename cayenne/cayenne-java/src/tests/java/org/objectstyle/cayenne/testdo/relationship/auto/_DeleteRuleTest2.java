package org.objectstyle.cayenne.testdo.relationship.auto;

import java.util.List;

/** Class _DeleteRuleTest2 was generated by Cayenne.
  * It is probably a good idea to avoid changing this class manually, 
  * since it may be overwritten next time code is regenerated. 
  * If you need to make any customizations, please use subclass. 
  */
public class _DeleteRuleTest2 extends org.objectstyle.cayenne.CayenneDataObject {

    public static final String DELETE_RULE_TEST3ARRAY_PROPERTY = "deleteRuleTest3Array";
    public static final String TEST1ARRAY_PROPERTY = "test1Array";

    public static final String DEL_RULE_TEST2_ID_PK_COLUMN = "DEL_RULE_TEST2_ID";

    public void addToDeleteRuleTest3Array(org.objectstyle.cayenne.testdo.relationship.DeleteRuleTest3 obj) {
        addToManyTarget("deleteRuleTest3Array", obj, true);
    }
    public void removeFromDeleteRuleTest3Array(org.objectstyle.cayenne.testdo.relationship.DeleteRuleTest3 obj) {
        removeToManyTarget("deleteRuleTest3Array", obj, true);
    }
    public List getDeleteRuleTest3Array() {
        return (List)readProperty("deleteRuleTest3Array");
    }
    
    
    public void addToTest1Array(org.objectstyle.cayenne.testdo.relationship.DeleteRuleTest1 obj) {
        addToManyTarget("test1Array", obj, true);
    }
    public void removeFromTest1Array(org.objectstyle.cayenne.testdo.relationship.DeleteRuleTest1 obj) {
        removeToManyTarget("test1Array", obj, true);
    }
    public List getTest1Array() {
        return (List)readProperty("test1Array");
    }
    
    
}