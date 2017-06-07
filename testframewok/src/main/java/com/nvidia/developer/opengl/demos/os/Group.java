package com.nvidia.developer.opengl.demos.os;

import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

/**
 * Created by mazhen'gui on 2017/6/5.
 */

final class Group implements  Constant{
    final int groupID;
    final AppIcon[] icons = new AppIcon[MAX_NUM_ICONS_PER_GROUP];
    final Transform screenLocation = new Transform();
    OSRenderer renderer;
    boolean m_focused;
    final Vector2f interval = new Vector2f();

    Group(OSRenderer renderer, int groupID){
        this.renderer = renderer;
        this.groupID = groupID;
    }

    void setAppIcon(int index, AppIcon icon) { icons[index] = icon;}
    AppIcon get(int index) { return icons[index];}

    void setFocused(boolean focused){
        if(m_focused != focused){
            for(AppIcon icon : icons){
                Drawable drawable = icon.drawable;
                Vector3f destPos;
                Quaternion destRotation;
                if(focused){
                    // expand the icons
                    destPos = icon.worldLocation.translate;
                    destRotation = icon.worldLocation.rotation;
                }else{
                    destPos = icon.unfocusedLocation.translate;
                    destRotation = icon.unfocusedLocation.rotation;
                }

                drawable.addRotationAnimation(destRotation, 1.0f);
                drawable.addTraslationAnimation(destPos.x, destPos.y, destPos.z, 1.0f);
            }
        }

        m_focused = focused;
    }

    void setInterval(float min, float max){
    }

    void update(float dt){
        for(AppIcon icon : icons){
            icon.drawable.update(dt);
        }
    }

    boolean isFocused() { return m_focused;}
}
