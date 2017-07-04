package jet.opengl.demos.os;

import org.lwjgl.util.vector.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mazhen'gui on 2017/6/13.
 */

final class AppList {
    final Transform location = new Transform();
    float appHeight;

    private List<AppIcon> apps = new ArrayList<>();
    private final Transform temp = new Transform();
    private Group group;

    AppList(Group group) { this.group = group;}

    void add(AppIcon app){
        // add the transition animation
        int count = apps.size();
        if(!apps.contains(app)) {  // redundant cheching....
            apps.add(app);

            temp.set(location);
            temp.translate.y += appHeight * count;
            app.drawable.addTraslationAnimation(temp.translate.x, temp.translate.y, temp.translate.z, 1.0f);
            app.drawable.addScaleAnimation(temp.scale.x, temp.scale.y, temp.scale.z, 1.0f);
            app.drawable.addRotationAnimation(temp.rotation, 1.0f);
            app.drawable.setVisible(true);
        }
    }

    void remove(AppIcon app){
        int idx = apps.indexOf(app);
        if(idx >= 0){
            apps.remove(idx);

            for(int i = idx; i < apps.size(); i++){
                temp.set(location);
                temp.translate.y += appHeight * i;
                apps.get(i).drawable.addTraslationAnimation(temp.translate.x, temp.translate.y, temp.translate.z, 1.0f);
//                app.drawable.addScaleAnimation(temp.scale.x, temp.scale.y, temp.scale.z, 1.0f);
//                app.drawable.addRotationAnimation(temp.rotation, 1.0f);
            }
        }
    }

    void render(Matrix4f viewproj){
        int count = 0;
        for(AppIcon appIcon : apps){
            Drawable drawable = appIcon.drawable;
            temp.set(location);
            temp.translate.y += appHeight * count;
            count++;

            group.renderDrawable(drawable, viewproj);
        }
    }

    void update(float dt){
        for(AppIcon appIcon : apps){
            appIcon.drawable.update(dt);
        }
    }

    boolean contain(AppIcon appIcon) { return apps.contains(appIcon);}
}
