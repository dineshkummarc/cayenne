package org.apache.cayenne.testdo.relationship.auto;

/** Class _ToOneFK2 was generated by Cayenne.
  * It is probably a good idea to avoid changing this class manually, 
  * since it may be overwritten next time code is regenerated. 
  * If you need to make any customizations, please use subclass. 
  */
public class _ToOneFK2 extends org.apache.cayenne.CayenneDataObject {

    public static final String TO_ONE_TO_FK_PROPERTY = "toOneToFK";

    public static final String TO_ONEFK2_PK_PK_COLUMN = "TO_ONEFK2_PK";

    public void setToOneToFK(org.apache.cayenne.testdo.relationship.ToOneFK1 toOneToFK) {
        setToOneTarget("toOneToFK", toOneToFK, true);
    }

    public org.apache.cayenne.testdo.relationship.ToOneFK1 getToOneToFK() {
        return (org.apache.cayenne.testdo.relationship.ToOneFK1)readProperty("toOneToFK");
    } 
    
    
}
