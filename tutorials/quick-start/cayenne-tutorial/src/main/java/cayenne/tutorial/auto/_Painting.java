package cayenne.tutorial.auto;

/** Class _Painting was generated by Cayenne.
  * It is probably a good idea to avoid changing this class manually, 
  * since it may be overwritten next time code is regenerated. 
  * If you need to make any customizations, please use subclass. 
  */
public class _Painting extends org.apache.cayenne.CayenneDataObject {

    public static final String NAME_PROPERTY = "name";
    public static final String ARTIST_PROPERTY = "artist";
    public static final String GALLERY_PROPERTY = "gallery";

    public static final String ID_PK_COLUMN = "ID";

    public void setName(String name) {
        writeProperty("name", name);
    }
    public String getName() {
        return (String)readProperty("name");
    }
    
    
    public void setArtist(cayenne.tutorial.Artist artist) {
        setToOneTarget("artist", artist, true);
    }

    public cayenne.tutorial.Artist getArtist() {
        return (cayenne.tutorial.Artist)readProperty("artist");
    } 
    
    
    public void setGallery(cayenne.tutorial.Gallery gallery) {
        setToOneTarget("gallery", gallery, true);
    }

    public cayenne.tutorial.Gallery getGallery() {
        return (cayenne.tutorial.Gallery)readProperty("gallery");
    } 
    
    
}