package com.nvidia.developer.opengl.demos.os;

/**
 * Created by mazhen'gui on 2017/6/5.
 */

final class Group implements  Constant{
    final int groupID;
    final AppIcon[] icons = new AppIcon[MAX_NUM_ICONS_PER_GROUP];
    OSRenderer renderer;
    boolean focused;

    Group(OSRenderer renderer, int groupID){
        this.renderer = renderer;
        this.groupID = groupID;
    }

    void setAppIcon(int index, AppIcon icon) { icons[index] = icon;}

    void setFocused(boolean focused){
        this.focused = focused;
        // TODO
    }

    boolean isFocused() { return focused;}
}
