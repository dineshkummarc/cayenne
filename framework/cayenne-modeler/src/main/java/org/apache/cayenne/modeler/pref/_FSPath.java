package org.apache.cayenne.modeler.pref;

/** Class _FSPath was generated by Cayenne.
  * It is probably a good idea to avoid changing this class manually, 
  * since it may be overwritten next time code is regenerated. 
  * If you need to make any customizations, please use subclass. 
  */
public class _FSPath extends org.apache.cayenne.pref.PreferenceDetail {

    public static final String PATH_PROPERTY = "path";

    public static final String ID_PK_COLUMN = "id";

    public void setPath(String path) {
        writeProperty("path", path);
    }
    public String getPath() {
        return (String)readProperty("path");
    }
    
    
}
