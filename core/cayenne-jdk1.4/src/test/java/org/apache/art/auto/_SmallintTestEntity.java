package org.apache.art.auto;

/** Class _SmallintTestEntity was generated by Cayenne.
  * It is probably a good idea to avoid changing this class manually, 
  * since it may be overwritten next time code is regenerated. 
  * If you need to make any customizations, please use subclass. 
  */
public class _SmallintTestEntity extends org.apache.cayenne.CayenneDataObject {

    public static final String SMALLINT_COL_PROPERTY = "smallintCol";

    public static final String ID_PK_COLUMN = "ID";

    public void setSmallintCol(Short smallintCol) {
        writeProperty("smallintCol", smallintCol);
    }
    public Short getSmallintCol() {
        return (Short)readProperty("smallintCol");
    }
    
    
}
