package com.nvidia.developer.opengl.demos.os;

import org.lwjgl.util.vector.Vector3f;

import java.io.File;
import java.io.IOException;

import jet.opengl.postprocessing.buffer.AttribDesc;
import jet.opengl.postprocessing.buffer.BufferBinding;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.buffer.VertexArrayObject;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.Numeric;

/**
 * Created by mazhen'gui on 2017/6/5.
 */

final class OSRenderer implements Constant{

    final Group[] m_Groups = new Group[MAX_NUM_GROUPS];
    SingleTextureProgram m_DefaultProgram;
    VertexArrayObject m_RectVAO;
    
    void initlize(){
        final float PI = Numeric.PI;
        final float THETA = (float)Math.toRadians(15.0f);
        final float ALPHA = (float)Math.toRadians(5.0f);
        final float G     = PI / 3.0f - THETA;
        final float BETA  = (G - 2.0f * ALPHA)/6.0f;

        final float R     = 10.0f;  // radius
        final float L     = (float) (R * Math.cos(BETA));
        final float RATIO = 0.8f;  // width/height
        final float HW    = (float) (R * Math.sin(BETA));
        final float HH    = HW / RATIO;
        final float HP    = HH * 0.15f;
        
        final Vector3f translate = new Vector3f();
        final Vector3f scale = new Vector3f(1,1,1);

        for(int groudID = 0; groudID < MAX_NUM_GROUPS; groudID++)
        {
            final Transform unfocusedTransform = new Transform();
            // TODO

            for(int j = 0; j < MAX_NUM_ICONS_PER_GROUP;j++)
            {
//                m_Groups[groudID][j] = Item();  // empty group
//                m_Groups[groudID][j].groudID = groudID;
//                m_Groups[groudID][j].index = j;
                m_Groups[groudID] = new Group(this, groudID);
                Drawable drawable = createDrawable(groudID, j);
                Transform readyLocation = new Transform();
                // TODO init readyLocation
                AppIcon appIcon = new AppIcon(unfocusedTransform, readyLocation, groudID, j);
                appIcon.drawable = drawable;

                // Calculate the transform of the every item.
                int index = j % 3;
                int row   = j / 3;

                float baseAngle;
                if (index == 0){
                    baseAngle = 2 * BETA + ALPHA;
                }else if (index == 1){
                    baseAngle = 0;
                }else{ // index == 2
                    baseAngle = -(2 * BETA + ALPHA);
                }

                baseAngle += groudID * PI/3;
                translate.z = (float) (R * Math.cos(baseAngle));
                translate.x = (float) (R * Math.sin(baseAngle));
                translate.y = 0;

                if (row == 0){
                    translate.y -= (2.0f * HH + HP);
                }else if (row == 2){
                    translate.y += (2.0f * HH + HP);
                }

                scale.x = HW * 2.0f;
                scale.y = HH * 2.0f;
                scale.z = 1.0f;
//                m_Groups[groudID][j].transform = OSTransform(translate, scale, Math.angleAxis(baseAngle, Math.vec3(0,1,0)));
                appIcon.worldLocation.translate.set(translate);
                appIcon.worldLocation.scale.set(scale);
                appIcon.worldLocation.rotation.setFromAxisAngle(0,1,0, baseAngle);

                appIcon.name = ("GroupID_") + (groudID) + ("_Index_") + (j);
                m_Groups[groudID].setAppIcon(j, appIcon);

//                Drawable* pOpenedDrawable;
//                createRoundSceneDrawable<Rect>(strAppName.c_str(), m_pMainScene, m_DefaultTex, &pOpenedDrawable);

//                final OSTransform& transform = m_Groups[groudID][j].transform;
//                pOpenedDrawable->SetPosition(transform.Position.x, transform.Position.y, transform.Position.z);
//                pOpenedDrawable->SetScale(transform.Scale.x, transform.Scale.y, transform.Scale.z);
//                pOpenedDrawable->SetRotation(transform.Rotation);
//                pOpenedDrawable->SetVisible(true);
//                m_Groups[groudID][j].pDrawable = pOpenedDrawable;
//                m_Groups[groudID][j].textureID = m_DefaultTex;
            }
        }
    }

    private Drawable createDrawable(int groupId, int index){
        if(m_DefaultProgram == null){
            m_DefaultProgram = new SingleTextureProgram();
        }

        if(m_RectVAO == null){
            BufferGL buffer = new BufferGL();
            float[] data = // Triangle strip
            {
                -1,-1,
                +1,-1,
                -1,+1,
                +1,+1
            };
            buffer.initlize(GLenum.GL_ARRAY_BUFFER, data.length * 4, CacheBuffer.wrap(data), GLenum.GL_STATIC_DRAW);
            AttribDesc desc = new AttribDesc();
            desc.size = 2;

            m_RectVAO = new VertexArrayObject();
            m_RectVAO.initlize(new BufferBinding[] { new BufferBinding(buffer, desc)}, null);
            m_RectVAO.unbind();
            buffer.unbind();
        }


        int textureIdx = groupId * MAX_NUM_ICONS_PER_GROUP + index;
        String filename = "OS/textures/Icon" + textureIdx + ".png";
        try {
            Texture2D texture = TextureUtils.createTexture2DFromFile(filename, true);
            Drawable drawable = new Drawable();
            drawable.texture = texture;
            drawable.program = m_DefaultProgram;
            drawable.buffer = m_RectVAO;
            return drawable;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    void beginRender(){

    }

    void draw(Drawable drawable){

    }

    void endRender(){

    }

    public static void main(String[] args){
        String root = "E:\\textures\\appicons\\";
        File[] files = new File(root).listFiles();

        int index = 0;
        for(File file : files){
            File newFile = new File(root + "Icon" + index + ".png");
            file.renameTo(newFile);
            index ++;
        }
    }
}
