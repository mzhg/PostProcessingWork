package jet.opengl.demos.demos.os;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;

import jet.opengl.postprocessing.buffer.AttribDesc;
import jet.opengl.postprocessing.buffer.BufferBinding;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.buffer.VertexArrayObject;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.AttachType;
import jet.opengl.postprocessing.texture.FramebufferGL;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureAttachDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;
import jet.opengl.postprocessing.util.Numeric;

/**
 * Created by mazhen'gui on 2017/6/5.
 */

final class OSRenderer implements Constant{

    static final float REGION_INTERVAL = Numeric.PI * 2 / 6;

    final Group[] m_Groups = new Group[MAX_NUM_GROUPS];

    boolean           m_bEnableMultisample = false;
    FramebufferGL     m_ScreenFBO;
    Texture2D         m_ScreenColor;
    Texture2D         m_ScreenDepth;
    SingleTextureProgram m_DefaultProgram;
    VertexArrayObject m_RectVAO;
    BufferGL          m_RectVBO;
    Background        m_Background;
    Eyepoint          m_Eyepoint;
    GLFuncProvider gl;

    final Matrix4f m_Model = new Matrix4f();
    final Matrix4f m_Proj = new Matrix4f();
    final Matrix4f m_ProjView = new Matrix4f();

    final Ray      m_ViewRay = new Ray();
    float          m_ScreenRatio;
//    SceneRaycastSelector m_Rayselector;
    boolean        m_Tocuhed;
    Drawable       m_WatchedDrawable;
    Drawable       m_CloseDrawable;
    float          m_WatchingTime;

//    final Ray      m_TouchRay = new Ray();

    int m_ActivedGroupID = -1;  // -1 mean invalid id
    
    void initlize(){
        gl = GLFuncProviderFactory.getGLFuncProvider();
        initAppIcons();

        m_Eyepoint = new Eyepoint();
        m_Eyepoint.initlize();
    }

    private void initAppIcons(){
        m_CloseDrawable = createDrawable(-1,-1);

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
            final float factor = 1;
            scale.x = HW * factor;
            scale.y = HH * factor;
            scale.z = 1.0f;
            final float middleAngle = groudID * PI/3;

            m_Groups[groudID] = new Group(this, groudID, m_CloseDrawable);
            Transform screenLocation = m_Groups[groudID].screenLocation;
            screenLocation.translate.z = (float) (R * Math.cos(middleAngle)) * 0.4f;
            screenLocation.translate.x = (float) (R * Math.sin(middleAngle)) * 0.4f;
            screenLocation.translate.y = 0;
            screenLocation.scale.set(scale);
            screenLocation.scale.y *= 9f/16f;
            screenLocation.scale.scale(4);
            screenLocation.rotation.setFromAxisAngle(Vector3f.Y_AXIS, middleAngle);

            AppList appList = m_Groups[groudID].getAppList();
            // calculating the location of AppList
            Transform transform = appList.location;
            transform.translate.z = (float) (R * Math.cos(middleAngle)) * 1.4f;  // more far
            transform.translate.x = (float) (R * Math.sin(middleAngle)) * 1.4f;
            transform.translate.y = 5;
            transform.scale.set(scale);
            transform.scale.y *= 9f/16f;
            transform.scale.scale(4);
            transform.rotation.setFromAxisAngle(Vector3f.Y_AXIS, middleAngle);
            appList.appHeight = transform.scale.y * 2;


            for(int j = 0; j < MAX_NUM_ICONS_PER_GROUP;j++)
            {
                final Transform unfocusedTransform = new Transform();

                unfocusedTransform.rotation.setFromAxisAngle(Vector3f.X_AXIS, (float)Math.toRadians(90));
                unfocusedTransform.translate.z = (float) (R * Math.cos(middleAngle));
                unfocusedTransform.translate.x = (float) (R * Math.sin(middleAngle));
                unfocusedTransform.translate.y = (float) (-R * Math.sqrt(2) * 0.5) + (j-4) * 0.1f;

                Drawable drawable = createDrawable(groudID, j);
                Transform readyLocation = new Transform();
                readyLocation.rotation.set(unfocusedTransform.rotation);

                unfocusedTransform.scale.set(scale);

                // TODO init readyLocation
                AppIcon appIcon = new AppIcon(unfocusedTransform, readyLocation, groudID, j);
                appIcon.drawable = drawable;
                m_Groups[groudID].icons[j] = appIcon;

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

//                m_Groups[groudID][j].transform = OSTransform(translate, scale, Math.angleAxis(baseAngle, Math.vec3(0,1,0)));
                appIcon.worldLocation.translate.set(translate);
                appIcon.worldLocation.scale.set(scale);
                appIcon.worldLocation.rotation.setFromAxisAngle(0,1,0, baseAngle);

                {
                    translate.z = (float) (R * Math.cos(middleAngle));
                    translate.x = (float) (R * Math.sin(middleAngle));
                    translate.y = 0;

                    readyLocation.scale.set(scale);
                    Matrix4f view = Matrix4f.lookAt(Vector3f.ZERO, translate, Vector3f.Y_AXIS, m_ProjView);
                    view.invert();

                    readyLocation.translate.y = (float) (-R * Math.sqrt(2) * 0.5);
                    readyLocation.translate.x = (index - 1) * (2 * HW);
                    readyLocation.translate.z = (1 - row) * (2 * HH) + readyLocation.translate.y;
                    // transform the position in view space to world space.
                    Matrix4f.transformVector(view, readyLocation.translate, readyLocation.translate);

                    // calculate the rotation
                    view.setIdentity();
                    view.rotate(-middleAngle, Vector3f.Z_AXIS);
                    view.rotate((float)Math.toRadians(90), Vector3f.X_AXIS);

                    readyLocation.rotation.setFromMatrix(view);
                }

                drawable.transform.set(appIcon.unfocusedLocation);

                appIcon.name = ("GroupID_") + (groudID) + ("_Index_") + (j);
            }
        }

        m_Background = new Background();
        m_Background.initlize();
    }

    private void setActivedGroupID(int newId){
        if(m_ActivedGroupID != newId){
            m_Groups[newId].setFocused(true);

            if(m_ActivedGroupID >= 0){
                m_Groups[m_ActivedGroupID].setFocused(false);
            }

            m_ActivedGroupID = newId;
        }
    }

    private void update(Matrix4f viewMat, float dt){
        // calculate the view ray
        Matrix4f.decompseRigidMatrix(viewMat, m_ViewRay.m_orig, null, null, m_ViewRay.m_dir);
        m_ViewRay.m_dir.scale(-1);

        int groupID;
        double _theta = Math.acos(m_ViewRay.m_dir.y);
        double sin_theta = Math.sin(_theta);
        double cos_fei = m_ViewRay.m_dir.z / sin_theta;  // x
        double sin_fei = m_ViewRay.m_dir.x / sin_theta;  // y
        double angle_fei = Math.acos(cos_fei);
        if(sin_fei < 0.0){
            angle_fei = 2.0 * Numeric.PI - angle_fei;
        }

        angle_fei += REGION_INTERVAL /2;
        if(angle_fei > 2 * Numeric.PI)
            angle_fei -= 2 * Numeric.PI;

        groupID = (int)(angle_fei / REGION_INTERVAL);
        setActivedGroupID(groupID);

        /*
        if(m_State == STATE_NONE) {
            m_Drawables.clear();
            for (AppIcon appIcon : m_Groups[groupID].icons) {
                m_Drawables.add(appIcon.drawable);
            }
            m_Rayselector.update(m_Drawables, dt);
        }else if(m_State == STATE_FILE){
            m_Drawables.clear();
            m_Drawables.add(m_CloseDrawable);
            m_Rayselector.update(m_Drawables, dt);
        }*/

        // update the groups
        for(Group group: m_Groups){
            group.update(viewMat, dt);
        }
    }

    void drawScenes(Matrix4f viewMat, float dt){
        update(viewMat, dt);

        if(m_bEnableMultisample)
            m_ScreenFBO.bind();
        renderScene(viewMat);

        if(m_bEnableMultisample) {
            // resolve the framebuffer
            gl.glBindFramebuffer(GLenum.GL_READ_FRAMEBUFFER, m_ScreenFBO.getFramebuffer());
            gl.glBindFramebuffer(GLenum.GL_DRAW_FRAMEBUFFER, 0);
            gl.glBlitFramebuffer(0, 0, m_ScreenColor.getWidth(), m_ScreenColor.getHeight(),
                    0, 0, m_ScreenColor.getWidth(), m_ScreenColor.getHeight(),
                    GLenum.GL_COLOR_BUFFER_BIT, GLenum.GL_NEAREST);
            gl.glBindFramebuffer(GLenum.GL_READ_FRAMEBUFFER, 0);
        }
    }

    private void renderScene(Matrix4f viewMat){
        gl.glClearColor(0,0,0,0);
        gl.glClearDepthf(1.0f);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT|GLenum.GL_DEPTH_BUFFER_BIT);
        gl.glDisable(GLenum.GL_DEPTH_TEST);
        gl.glDisable(GLenum.GL_BLEND);
        gl.glDepthFunc(GLenum.GL_LESS);

        Matrix4f.mul(m_Proj, viewMat, m_ProjView);
        m_Background.draw(m_DefaultProgram, m_ProjView);

        gl.glEnable(GLenum.GL_DEPTH_TEST);
        gl.glEnable(GLenum.GL_BLEND);
        gl.glBlendFunc(GLenum.GL_SRC_ALPHA, GLenum.GL_ONE_MINUS_SRC_ALPHA);
        // render the group to the scene  TODO
        for(Group group : m_Groups){
            group.renderer(m_ProjView);
        }

        gl.glDisable(GLenum.GL_DEPTH_TEST);
        gl.glDisable(GLenum.GL_BLEND);

        float sizeX = 0.035f;
        float sizeY = sizeX * m_ScreenRatio;
        m_Eyepoint.draw(m_DefaultProgram, m_RectVAO, sizeX, sizeY, m_Tocuhed);
    }

    void onResize(int width, int height){
        m_ScreenRatio = (float)width/height;
        Matrix4f.perspective(90, m_ScreenRatio, 0.1f, 100.0f, m_Proj);

        if(!m_bEnableMultisample)
            return;

        if(m_ScreenColor == null || m_ScreenColor.getWidth() != width || m_ScreenColor.getHeight() != height){
            if(m_ScreenColor != null)
                m_ScreenFBO.dispose();
        }

        m_ScreenFBO = new FramebufferGL();
        m_ScreenFBO.bind();
        m_ScreenColor = m_ScreenFBO.addTexture2D(new Texture2DDesc(width, height, 1, 1,GLenum.GL_RGBA8, 4), new TextureAttachDesc(0, AttachType.TEXTURE_2D, 0,0));
        m_ScreenDepth = m_ScreenFBO.addTexture2D(new Texture2DDesc(width, height, 1,1, GLenum.GL_DEPTH_COMPONENT16,4), new TextureAttachDesc(0, AttachType.TEXTURE_2D, 0,0));
        m_ScreenFBO.unbind();
    }

    private Drawable createDrawable(int groupId, int index){
        if(m_DefaultProgram == null){
            m_DefaultProgram = new SingleTextureProgram();
        }

        if(m_RectVAO == null){
            BufferGL buffer = new BufferGL();
            float[] data = // Triangle strip
            {
                -1,-1, 0, 0,
                +1,-1, 1, 0,
                -1,+1, 0, 1,
                +1,+1, 1, 1
            };
            buffer.initlize(GLenum.GL_ARRAY_BUFFER, data.length * 4, CacheBuffer.wrap(data), GLenum.GL_STATIC_DRAW);
            AttribDesc pos_desc = new AttribDesc();
            pos_desc.size = 2;
            pos_desc.stride = 16;

            AttribDesc tex_desc = new AttribDesc();
            tex_desc.index = 1;
            tex_desc.size = 2;
            tex_desc.stride = 16;
            tex_desc.offset = 8;

            m_RectVAO = new VertexArrayObject();
            m_RectVAO.initlize(CommonUtil.toArray(new BufferBinding(buffer, pos_desc, tex_desc)), null);
            m_RectVAO.unbind();
            buffer.unbind();

            m_RectVBO = buffer;
        }

        int textureIdx = groupId * MAX_NUM_ICONS_PER_GROUP + index;
        String filename;
        if(textureIdx >= 0){
            filename = "OS/textures/Icon" + textureIdx + ".png";
        }else{
            filename = "OS/textures/Icon12.png";
        }

        try {
            Texture2D texture = TextureUtils.createTexture2DFromFile(filename, true);
            gl.glBindTexture(texture.getTarget(), texture.getTexture());
            gl.glGenerateMipmap(texture.getTarget());
            gl.glTexParameteri(texture.getTarget(), GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR_MIPMAP_LINEAR);
            gl.glTexParameteri(texture.getTarget(), GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);

            int largest = gl.glGetInteger(GLenum.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT);
            gl.glTexParameteri(texture.getTarget(), GLenum.GL_TEXTURE_MAX_ANISOTROPY_EXT, largest);

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


    /*
    public static void main(String[] args){
        float theta = 0.7f;
        float fei = Numeric.PI * 0.89f;

        float x = (float) (Math.sin(theta) * Math.cos(fei));
        float y = (float) (Math.sin(theta) * Math.sin(fei));
        float z = (float) Math.cos(theta);

        System.out.println("fei = " + fei);
//        double _fei = Math.atan(y/ x);
//        System.out.println("_fei0 = " + _fei);
//        if(_fei < 0.0){
//            System.out.println("_fei is neg: " + _fei);
//            // remap to [0, 2pi]
//            _fei = 2.0 * Numeric.PI + _fei;
//        }

        double _theta = Math.acos(z);
        double sin_theta = Math.sin(_theta);
        double cos_fei = x / sin_theta;
        double sin_fei = y / sin_theta;
        double angle_fei = Math.acos(cos_fei);
        if(sin_fei < 0.0){
            angle_fei = 2.0 * Numeric.PI - angle_fei;
        }

        System.out.println("angle_fei = " + angle_fei);
    }*/
}
