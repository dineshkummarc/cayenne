package org.apache.cayenne.testdo.mt.auto;

import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.testdo.mt.MtDeleteRule;

/**
 * Class _MtDeleteNullify was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _MtDeleteNullify extends CayenneDataObject {

    public static final String NAME_PROPERTY = "name";
    public static final String NULLIFY_PROPERTY = "nullify";

    public static final String DELETE_NULLIFY_ID_PK_COLUMN = "DELETE_NULLIFY_ID";

    public void setName(String name) {
        writeProperty("name", name);
    }
    public String getName() {
        return (String)readProperty("name");
    }

    public void setNullify(MtDeleteRule nullify) {
        setToOneTarget("nullify", nullify, true);
    }

    public MtDeleteRule getNullify() {
        return (MtDeleteRule)readProperty("nullify");
    }


}
