package dev.mfarm.com.mfarm.models;

/**
 * Created by SIMGICH on 7/25/2016.
 */
public class menulist {
    String id;

    public menulist(String id, String menuitem, String imgtoview) {
        this.id = id;
        this.menuitem = menuitem;
        this.imgtoview = imgtoview;
    }

    String menuitem;

    public String getImgtoview() {
        return imgtoview;
    }

    public void setImgtoview(String imgtoview) {
        this.imgtoview = imgtoview;
    }

    String imgtoview;

    public menulist() {
    }

    public String getMenuitem() {
        return menuitem;
    }

    public void setMenuitem(String menuitem) {
        this.menuitem = menuitem;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


}
