package jet.opengl.demos.os;

/**
 * Created by mazhen'gui on 2017/6/5.
 */

final class AppIcon {
    String name;
    Drawable drawable;

    // 该App图标在世界坐标系中的位置
    final Transform worldLocation = new Transform();

    final Transform unfocusedLocation;
    final Transform readyLocation;

    final int groupID;
    final int localID;

    AppIcon(Transform unfocusedLocation, Transform readyLocation, int groupID, int localID){
        this.unfocusedLocation = unfocusedLocation;
        this.readyLocation = readyLocation;
        this.groupID = groupID;
        this.localID = localID;
    }

    void setWorldLocationToDrawable() { drawable.transform.set(worldLocation);}
}
