package org.apache.art.auto;

/** Class _ClobTestEntity was generated by Cayenne.
  * It is probably a good idea to avoid changing this class manually, 
  * since it may be overwritten next time code is regenerated. 
  * If you need to make any customizations, please use subclass. 
  */
public class _ClobTestEntity extends org.apache.cayenne.CayenneDataObject {

    public static final String CLOB_COL_PROPERTY = "clobCol";

    public static final String CLOB_TEST_ID_PK_COLUMN = "CLOB_TEST_ID";

    public void setClobCol(String clobCol) {
        writeProperty("clobCol", clobCol);
    }
    public String getClobCol() {
        return (String)readProperty("clobCol");
    }
    
    
}