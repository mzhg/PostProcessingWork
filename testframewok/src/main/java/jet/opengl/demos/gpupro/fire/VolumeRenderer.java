package jet.opengl.demos.gpupro.fire;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.buffer.VertexArrayObject;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/10/28.
 */

final class VolumeRenderer implements Disposeable{
    private boolean d_visible;  // indicates if the volume object is in front of the camera

    // references to framebuffers, textures, depthbuffer and display list
    // of geometry textured with slices of the flat 3d texture during
    // rendering of T1 (see VFA document)
    private final int[] d_framebuffers = new int[3];
    private final int[] d_textures = new int[3];
    private int d_depthbuffer;
    private Runnable d_rendGeometry;

    // configuration parameter for geometry d_rendGeometry
    private int d_noVertinRendGeom;

    private int d_firstSlice,		// reference number of the first slice in the flat 3D texture (Fig.2 in VFA)
    // which contains some data
    d_lastSlice,		// reference number of the last non-empty slice
            d_slicesAlongX,		// number of slices of the flat 3D texture in every row
    // along the "horizontal edge" of this texture (Fig.2 in VFA)
    d_slices;			// number of slices

    private float d_radius;		// radius of S1 (see Fig. 1 in the VFA document)
    private final Vector3f d_rf = new Vector3f();// position of the volumetric effect in the scene
    private int d_windowSizeY;	// is set to window_size_y from Fire.cpp
    private float d_fovy;		// is set to vertical_viewing_angle from Fire.cpp

    private float len_r_cf;		// distance between the camera and the volume effect


    // sizes of the flat 3d texture and auxiliary buffers
    private int d_flat3dTexX, d_flat3dTexY,
            d_raw3dEffectX, d_raw3dEffectY,
            d_blur3dEffectX, d_blur3dEffectY;

    // variables for vertex and fragment programs
    private int d_vProfile, d_fProfile;
    private GLSLProgram d_vProgram, d_fProgram;
    private GLSLProgram m_rectTexProgram;
//    CGcontext d_cgContext;
    private int d_dim, d_Proj, d_Model, d_blurInput;

    private final Matrix4f d_VolRendTrickyRotation  = new Matrix4f();	// corresponds to M (see VFA document)
    private final float[] d_VolRendTrickyProjection = new float[16];  // corresponds to P (see VFA document)
    private float d_invFluidR;	// reversed d_radius
    private float d_fluidR2;	// square of d_radius
    private float d_nearBase;	// length of Q1 edges (see VFA)

    // variables and function below are used to decide
    // whether T1 should be blurred or not (see VFA document);
    // if T1 is close enough to the observer it should be blurred
    private boolean d_doLevel2;
    private int d_texture_no;
    private float d_factor;
    private VertexArrayObject m_vao;
    private GLFuncProvider gl;

    boolean createRendDataStruct(){
//			if (!glewIsSupported("GL_EXT_framebuffer_object")){
//				cerr << "GL_EXT_framebuffer_object not supported" << endl;
//				return false;
//			}
//
//			if (!glewIsSupported("GL_ARB_texture_rectangle")){
//				cerr << "GL_ARB_texture_rectangle not supported" << endl;
//				return false;
//			}

        IntBuffer framebuffers = CacheBuffer.getCachedIntBuffer(3);
        gl.glGenFramebuffers(framebuffers);
        framebuffers.get(d_framebuffers);

        IntBuffer textures = CacheBuffer.getCachedIntBuffer(3);
        gl.glGenTextures(textures);
        textures.get(d_textures);

        d_depthbuffer = gl.glGenRenderbuffer();

        /**************************************************************************************************************/

        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER_EXT,d_framebuffers[0]);
        gl.glBindTexture(GLenum.GL_TEXTURE_RECTANGLE_ARB,d_textures[0]);
        gl.glTexImage2D(GLenum.GL_TEXTURE_RECTANGLE_ARB,0,GLenum.GL_RGBA8,d_flat3dTexX,d_flat3dTexY,0,GLenum.GL_RGBA, GLenum.GL_UNSIGNED_BYTE, null);
        gl.glTexParameteri(GLenum.GL_TEXTURE_RECTANGLE_ARB,GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);
//        gl.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_REPLACE);
        gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER_EXT, GLenum.GL_COLOR_ATTACHMENT0_EXT,GLenum.GL_TEXTURE_RECTANGLE_ARB,d_textures[0],0);
        GLCheck.checkFramebufferStatus();

        /**************************************************************************************************************/

        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER_EXT,d_framebuffers[1]);
        gl.glBindTexture(GLenum.GL_TEXTURE_RECTANGLE_ARB,d_textures[1]);
        gl.glTexImage2D(GLenum.GL_TEXTURE_RECTANGLE_ARB,0,GLenum.GL_RGBA8,d_raw3dEffectX,d_raw3dEffectY,0,GLenum.GL_RGBA,GLenum.GL_UNSIGNED_BYTE,null);
        gl.glTexParameteri(GLenum.GL_TEXTURE_RECTANGLE_ARB,GLenum.GL_TEXTURE_MAG_FILTER,GLenum.GL_LINEAR);
//        gl.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_REPLACE);
        gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER_EXT, GLenum.GL_COLOR_ATTACHMENT0_EXT,GLenum.GL_TEXTURE_RECTANGLE_ARB,d_textures[1],0);
        gl.glBindRenderbuffer(GLenum.GL_RENDERBUFFER_EXT,d_depthbuffer);
        gl.glRenderbufferStorage(GLenum.GL_RENDERBUFFER_EXT,GLenum.GL_DEPTH_COMPONENT24,d_raw3dEffectX,d_raw3dEffectY);
        gl.glFramebufferRenderbuffer(GLenum.GL_FRAMEBUFFER_EXT,GLenum.GL_DEPTH_ATTACHMENT_EXT, GLenum.GL_RENDERBUFFER_EXT,d_depthbuffer);
        GLCheck.checkFramebufferStatus();

        /*************************************************************************************************************/

        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER_EXT,d_framebuffers[2]);
        gl.glBindTexture(GLenum.GL_TEXTURE_RECTANGLE_ARB,d_textures[2]);
        gl.glTexImage2D(GLenum.GL_TEXTURE_RECTANGLE_ARB,0,GLenum.GL_RGBA8,d_blur3dEffectX,d_blur3dEffectY,0,GLenum.GL_RGBA,GLenum.GL_UNSIGNED_BYTE,null);
        gl.glTexParameteri(GLenum.GL_TEXTURE_RECTANGLE_ARB,GLenum.GL_TEXTURE_MAG_FILTER,GLenum.GL_LINEAR);
//        gl.glTexEnvf(GL11.GL_TEXTURE_ENV,GL11.GL_TEXTURE_ENV_MODE,GL11.GL_REPLACE);
        gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER_EXT,GLenum.GL_COLOR_ATTACHMENT0_EXT, GLenum.GL_TEXTURE_RECTANGLE_ARB,d_textures[2],0);
        GLCheck.checkFramebufferStatus();

        /************************************************************************************************************/
        GLCheck.checkError();
        return true;
    }

    boolean createRendPrograms(){
        /*d_vProgram = CgGL.cgCreateProgramFromFile(d_cgContext, CgGL.CG_SOURCE, file_dir + "v_flat3Dgen.cg", d_vProfile, "v_flat3Dgen", null);
        d_fProgram = CgGL.cgCreateProgramFromFile(d_cgContext, CgGL.CG_SOURCE, file_dir + "f_blur.cg", d_fProfile, "f_blur", null);*/
        try {
            d_vProgram = GLSLProgram.createFromFiles("gpupro/Fire/shaders/Flat3DgenVS.vert", "gpupro/Fire/shaders/Flat3DgenPS.frag");
            d_fProgram = GLSLProgram.createFromFiles("gpupro/Fire/shaders/BlurVS.vert", "gpupro/Fire/shaders/BlurPS.frag");
            m_rectTexProgram =GLSLProgram.createFromFiles("gpupro/Fire/shaders/SimpleRectTextureVS.vert", "gpupro/Fire/shaders/SimpleRectTexturePS.frag");
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*d_dim = CgGL.cgGetNamedParameter(d_vProgram,"dim");
        CgGL.cgSetParameterVariability(d_dim, CgGL.CG_LITERAL);
        CgGL.cgGLSetParameter4f(d_dim, d_slices, d_slices, d_slices, d_slicesAlongX);
        d_blurInput = CgGL.cgGetNamedParameter(d_fProgram,"blur_input");
        CgGL.cgGLSetTextureParameter(d_blurInput,d_textures[1]);
        CgGL.cgGLLoadProgram(d_vProgram);
        CgGL.cgGLLoadProgram(d_fProgram);*/

        d_dim = d_vProgram.getUniformLocation("dim");
        d_vProgram.enable();
        gl.glUniform4f(d_dim, d_slices, d_slices, d_slices, d_slicesAlongX);
        d_blurInput = 0;

        return true;
    }

    public void dispose(){
        if( d_depthbuffer != 0) gl.glDeleteRenderbuffer(d_depthbuffer);
        if( d_framebuffers[0] != 0) gl.glDeleteFramebuffers(CacheBuffer.wrap(d_framebuffers));
        if( d_textures[0] != 0) gl.glDeleteTextures(CacheBuffer.wrap(d_textures));
//        if( d_rendGeometry != 0)gl. glDeleteLists(d_rendGeometry,1);
    }

    boolean init(int slices, int slicesAlongX, int firstSlice, int lastSlice,
                 float radius, int windowSizeY, float fovy, Vector3f r_f, Vector3f r_c, Vector3f d_c, Vector3f u_c){

        gl = GLFuncProviderFactory.getGLFuncProvider();
        if( slices%slicesAlongX !=0) return false;

        d_slices = slices;
        d_slicesAlongX = slicesAlongX;
        d_firstSlice = firstSlice;
        d_lastSlice = lastSlice;

        d_radius = radius;
        d_fluidR2 = d_radius*d_radius;
        d_invFluidR = 1.0f/d_radius;

        d_nearBase = 2.0f*d_radius;

        d_windowSizeY = windowSizeY;
        d_fovy = fovy;

        d_rf.set(r_f);

        d_flat3dTexX = slicesAlongX*slices;
        d_flat3dTexY = slices*slices/slicesAlongX;
        d_raw3dEffectX = d_raw3dEffectY = d_blur3dEffectX = d_blur3dEffectY = slices;

        d_factor = (float) (1.0 / Math.tan( 0.5*d_fovy*3.1415/180.0 ));

        for(int i=0;i<16;i++){
//            if( i%5 != 0)
//                d_VolRendTrickyRotation[i] = 0.0f;
//            else d_VolRendTrickyRotation[i] = 1.0f;

            d_VolRendTrickyProjection[i] = 0.0f;
        }

        d_VolRendTrickyProjection[11] = -1.0f;

        if(!createRendDataStruct()) return false;
        if(!createRendPrograms()) return false;

//        d_rendGeometry = GL11.glGenLists(1);
        d_noVertinRendGeom = 6;
        createRendGeom(/*d_rendGeometry,*/ d_radius, d_noVertinRendGeom, d_firstSlice, d_lastSlice,
                d_slicesAlongX, d_slices);

        update(r_f, r_c, d_c, u_c);

        return true;
    }

    void update(Vector3f r_f, Vector3f r_c, Vector3f d_c, Vector3f u_c){
        d_visible = true;

        d_rf.set(r_f);
        Vector3f r_cf = Vector3f.sub(r_c, r_f, null);

        float r_cf_d_c = Vector3f.dot(r_cf,d_c);

        float len_r_cf2 = Vector3f.dot(r_cf,r_cf);
        len_r_cf = (float) Math.sqrt(len_r_cf2);

        if( r_cf_d_c > 0.0f) d_visible = false;
        else {
            Vector3f v_Z = new Vector3f( r_cf.x/len_r_cf , r_cf.y/len_r_cf , r_cf.z/len_r_cf  );

            Vector3f v_X = new Vector3f( u_c.y * v_Z.z - u_c.z * v_Z.y , u_c.z * v_Z.x - u_c.x * v_Z.z , u_c.x * v_Z.y - u_c.y * v_Z.x );
            float temp = (float) Math.sqrt(Vector3f.dot(v_X,v_X));
            if(temp != 0.0f){
                v_X.x/=temp;
                v_X.y/=temp;
                v_X.z/=temp;
            }
            d_VolRendTrickyRotation.m00 = v_X.x;
            d_VolRendTrickyRotation.m01 = v_X.y;
            d_VolRendTrickyRotation.m02 = v_X.z;

            d_VolRendTrickyRotation.m10 = v_Z.y * v_X.z - v_Z.z * v_X.y;
            d_VolRendTrickyRotation.m11 = v_Z.z * v_X.x - v_Z.x * v_X.z;
            d_VolRendTrickyRotation.m12 = v_Z.x * v_X.y - v_Z.y * v_X.x;

            d_VolRendTrickyRotation.m20 = v_Z.x;
            d_VolRendTrickyRotation.m21 = v_Z.y;
            d_VolRendTrickyRotation.m22 = v_Z.z;

            /********************************************************************************************************/
            float factor2 = len_r_cf2 - d_fluidR2;
            float factor1 =  (float) Math.sqrt( factor2 );
            d_nearBase = d_radius*(len_r_cf - d_radius)/factor1;

            d_VolRendTrickyProjection[0] = d_invFluidR*factor1;
            d_VolRendTrickyProjection[5] = d_VolRendTrickyProjection[0];
            d_VolRendTrickyProjection[10] = - len_r_cf * d_invFluidR;
            d_VolRendTrickyProjection[14] = -d_invFluidR*factor2;
        }

        setMipmapping();
    }

    void reshape(int height){
        d_windowSizeY = height;
        setMipmapping();
    }

    void setMipmapping(){
        int d_effective_size = (int) (d_factor*d_nearBase*d_windowSizeY/len_r_cf);
        if( d_effective_size < d_slices ){
            d_doLevel2 = false;
            d_texture_no = d_textures[1];
            return;
        }
        d_doLevel2 = true;
        d_texture_no = d_textures[2];
    }

    int createRendGeom(float R, int pol_vert, int first_slice, int last_slice, int X_ROW, int layers){
        if(pol_vert<3) return 0;

        float[] scaling_factors = new float[layers];

        float ds = 2.0f/layers;
        float start = -1.0f + ds*0.5f;
        float r;
        float R2 = R*R;

        for(int i=0;i<layers;i++){
            r = start + i*ds;
            if( (r<0 ? -r : r) > R ) scaling_factors[i] = 0.0f;
            else scaling_factors[i] = (float) (Math.sqrt(R2 - r*r)/R);
        }

        float angle = 2*3.14f/pol_vert;
        float RR = (float) (R/Math.cos(0.5*angle));

        int layers0_5=layers/2;
        float XX,YY,scale;
        int fac_x,fac_y;

        start = -1.0f + ds*0.5f;
        float ZZ;
//        GL11.glNewList(nr,GL11.GL_COMPILE);
        int[] indicesCountAndType = new int[2];
        {
//            GL20.glBlendEquationSeparate(GL14.GL_FUNC_ADD,GL14.GL_MAX);
//            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
//            GL11.glEnable(GL11.GL_BLEND);
            List<PolygonTrianguler> polygons = new ArrayList<>();
            for(int j=first_slice;j<=last_slice;j++){
                ZZ = start+j*ds;
                scale = scaling_factors[j];
                fac_y = j/X_ROW;
                fac_x = j%X_ROW;
//                GL11.glBegin(GL11.GL_POLYGON);
                {
                    PolygonTrianguler trianguler = new PolygonTrianguler(pol_vert);
                    for(int i=0;i<pol_vert;i++){
                        XX = (float) (RR*Math.cos(angle*i)*scale);
                        YY = (float) (RR*Math.sin(angle*i)*scale);
//                        GL11.glTexCoord2f( (XX+1.0f)*layers0_5 + fac_x*layers , (YY+1.0f)*layers0_5 + fac_y*layers );
//                        GL11.glVertex3f( XX , YY , ZZ );
                        trianguler.addVertex(XX , YY , ZZ,
                                (XX+1.0f)*layers0_5 + fac_x*layers , (YY+1.0f)*layers0_5 + fac_y*layers);
                    }

                    polygons.add(trianguler);
                }
//                GL11.glEnd();
            }

            m_vao = PolygonTrianguler.triangular(polygons.toArray(new PolygonTrianguler[polygons.size()]), indicesCountAndType);
//            GL11.glDisable(GL11.GL_BLEND);
        }

        final int count = indicesCountAndType[0];
        final int type = indicesCountAndType[1];
        d_rendGeometry = ()->
        {
            gl.glBlendEquationSeparate(GLenum.GL_FUNC_ADD,GLenum.GL_MAX);
            gl.glBlendFunc(GLenum.GL_SRC_ALPHA, GLenum.GL_ONE_MINUS_SRC_ALPHA);
            gl.glEnable(GLenum.GL_BLEND);

            m_vao.bind();
            gl.glDrawElements(GLenum.GL_TRIANGLES, count,type, 0);
            m_vao.unbind();

            gl.glBlendEquationSeparate(GLenum.GL_FUNC_ADD,GLenum.GL_FUNC_ADD);
            gl.glDisable(GLenum.GL_BLEND);
        };
//        GL11.glEndList();

        return 1;
    }

    void beginEffectData(Matrix4f proj){
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER_EXT,d_framebuffers[0]);
        gl.glDrawBuffers(GLenum.GL_COLOR_ATTACHMENT0_EXT);
        gl.glViewport(0,0,d_flat3dTexX,d_flat3dTexY);
        d_vProgram.enable();

        gl.glClearColor(0,0,0,0);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT  );

        /*gl.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0,d_flat3dTexX,-0.5,d_flat3dTexY-0.5,-1,1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);

        CgGL.cgGLEnableProfile(d_vProfile);
        CgGL.cgGLBindProgram(d_vProgram);*/

        Matrix4f.ortho(0,d_flat3dTexX,-0.5f,d_flat3dTexY-0.5f,-1,1, proj);
    }

    void applyEffectData(Matrix4f proj, Matrix4f modelView){
        int projLoc = d_vProgram.getUniformLocation("Proj");
        if(projLoc < 0){
            throw new IllegalArgumentException();
        }

        gl.glUniformMatrix4fv(projLoc, false, CacheBuffer.wrap(proj));

        int modelViewLoc = d_vProgram.getUniformLocation("Model");
        if(modelViewLoc < 0){
            throw new IllegalArgumentException();
        }

        gl.glUniformMatrix4fv(modelViewLoc, false, CacheBuffer.wrap(modelView));
    }

    void endEffectData(){
//        CgGL.cgGLDisableProfile(d_vProfile);
        d_vProgram.disable();
    }

    void beginObstacles(Matrix4f proj){
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER_EXT,d_framebuffers[1]);
        gl.glDrawBuffers( GLenum.GL_COLOR_ATTACHMENT0_EXT );

        gl.glViewport(0,0,d_raw3dEffectX,d_raw3dEffectY);

        /*GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadMatrix( wrap16(d_VolRendTrickyProjection) );   TODO
        GL11.glMatrixMode(GL11.GL_MODELVIEW);*/
        proj.load(d_VolRendTrickyProjection, 0);
        gl.glEnable(GLenum.GL_DEPTH_TEST);
        gl.glClearColor(0,0,0,0);
        gl.glClear( GLenum.GL_DEPTH_BUFFER_BIT|GLenum.GL_COLOR_BUFFER_BIT );
    }

    void endObstacles(ModelViewStack mat, Matrix4f temp){
        gl.glClear( GLenum.GL_COLOR_BUFFER_BIT );
//        GL11.glEnable(GLenum.GL_TEXTURE_RECTANGLE_ARB);
        gl.glBindTexture(GLenum.GL_TEXTURE_RECTANGLE_ARB,d_textures[0]);

        /*GL11.glPushMatrix();
        {
            GL11.glLoadIdentity();
            GL11.glTranslatef(0.0f,0.0f,-len_r_cf);
            GL11.glCallList(d_rendGeometry);
        }
        GL11.glPopMatrix();*/
        mat.loadIdentity();
        mat.translate(0.0f,0.0f,-len_r_cf);
        temp.load(d_VolRendTrickyProjection, 0);
        Matrix4f modelView = mat.getTotalMatrix();
        Matrix4f.mul(temp, modelView, temp);

        m_rectTexProgram.enable();
        int mvpLoc = m_rectTexProgram.getUniformLocation("g_MVP");
        gl.glUniformMatrix4fv(mvpLoc, false, CacheBuffer.wrap(temp));

        d_rendGeometry.run();

//        gl.glDisable(ARBTextureRectangle.GL_TEXTURE_RECTANGLE_ARB);
        gl.glDisable(GLenum.GL_DEPTH_TEST);
    }

    void createEffectBillboard(){
        if(d_doLevel2){
            gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER_EXT,d_framebuffers[2]);

            gl.glViewport(0,0,d_blur3dEffectX,d_blur3dEffectY);

            /*GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glLoadIdentity();
            GL11.glOrtho(-1,1,-1,1,-1,1);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);*/

            gl.glClearColor(0,0,0,0);
            gl.glClear(GLenum.GL_COLOR_BUFFER_BIT);

            /*CgGL.cgGLEnableProfile(d_fProfile);
            CgGL.cgGLBindProgram(d_fProgram);*/
            d_fProgram.enable();

//            gl.glEnable(ARBTextureRectangle.GL_TEXTURE_RECTANGLE_ARB);

            gl.glBindTexture(GLenum.GL_TEXTURE_RECTANGLE_ARB,d_textures[1]);

            int gora_x = d_blur3dEffectX, gora_y = d_blur3dEffectY;
            int dol_x = 0, dol_y = 0;

            /*GL11.glPushMatrix();
            {
                GL11.glLoadIdentity();
                GL11.glBegin(GL11.GL_QUADS);
                {
                    GL13.glMultiTexCoord4f(GL13.GL_TEXTURE0, dol_x, dol_y+1, dol_y, dol_y-1);
                    GL13.glMultiTexCoord4f(GL13.GL_TEXTURE1, dol_x+1, dol_y+1, dol_x-1, dol_y-1);
                    GL13.glMultiTexCoord3f(GL13.GL_TEXTURE2, dol_x+1, dol_x-1, dol_y);
                    GL11.glVertex3f(-1,-1, 0.0f );

                    GL13.glMultiTexCoord4f(GL13.GL_TEXTURE0, gora_x, dol_y+1, dol_y, dol_y-1);
                    GL13.glMultiTexCoord4f(GL13.GL_TEXTURE1, gora_x+1, dol_y+1, gora_x-1, dol_y-1);
                    GL13.glMultiTexCoord3f(GL13.GL_TEXTURE2, gora_x+1, gora_x-1, dol_y);
                    GL11.glVertex3f(1,-1, 0.0f );

                    GL13.glMultiTexCoord4f(GL13.GL_TEXTURE0, gora_x, gora_y+1, gora_y, gora_y-1);
                    GL13.glMultiTexCoord4f(GL13.GL_TEXTURE1, gora_x+1, gora_y+1, gora_x-1, gora_y-1);
                    GL13.glMultiTexCoord3f(GL13.GL_TEXTURE2, gora_x+1, gora_x-1, gora_y);
                    GL11.glVertex3f(1,1, 0.0f );

                    GL13.glMultiTexCoord4f(GL13.GL_TEXTURE0, dol_x, gora_y+1, gora_y, gora_y-1);
                    GL13.glMultiTexCoord4f(GL13.GL_TEXTURE1, dol_x+1, gora_y+1, dol_x-1, gora_y-1);
                    GL13.glMultiTexCoord3f(GL13.GL_TEXTURE2, dol_x+1, dol_x-1, gora_y);
                    GL11.glVertex3f(-1,1, 0.0f );
                }
                GL11.glEnd();
            }
            GL11.glPopMatrix();
            GL11.glDisable(ARBTextureRectangle.GL_TEXTURE_RECTANGLE_ARB);
            CgGL.cgGLDisableProfile(d_fProfile);*/

            final int vertex_count = 4 * 2 + 3 * 2;
            final int buffer_size = vertex_count * 4;
            FloatBuffer vertex_buffer = CacheBuffer.getCachedFloatBuffer(buffer_size);

            vertex_buffer.put(dol_x).put(dol_y+1).put(dol_y).put(dol_y-1);
            vertex_buffer.put(dol_x+1).put(dol_y+1).put(dol_x-1).put(dol_y-1);
            vertex_buffer.put(dol_x+1).put(dol_x-1).put(dol_y);
            vertex_buffer.put(-1).put(-1).put(0.f);

            vertex_buffer.put(gora_x).put(dol_y+1).put(dol_y).put(dol_y-1);
            vertex_buffer.put(gora_x+1).put(dol_y+1).put(gora_x-1).put(dol_y-1);
            vertex_buffer.put(gora_x+1).put(gora_x-1).put(dol_y);
            vertex_buffer.put(1).put(-1).put(0.f);

            vertex_buffer.put(gora_x).  put(gora_y+1).put(gora_y).put(gora_y-1);
            vertex_buffer.put(gora_x+1).put(gora_y+1).put(gora_x-1).put(gora_y-1);
            vertex_buffer.put(gora_x+1).put(gora_x-1).put(gora_y);
            vertex_buffer.put(1).put(1).put(0.f);

            vertex_buffer.put(dol_x).put(gora_y+1).put(gora_y).put(gora_y-1);
            vertex_buffer.put(dol_x+1).put(gora_y+1).put(dol_x-1).put(gora_y-1);
            vertex_buffer.put(dol_x+1).put(dol_x-1).put(gora_y);
            vertex_buffer.put(-1).put(1).put(0.f);
            vertex_buffer.flip();

            final int stride = vertex_count * 4;
            final int position_loc = 0, pos_offset = 11;
            final int texture0_loc = 1, tex0_offset = 0;
            final int texture1_loc = 2, tex1_offset = 4;
            final int texture2_loc = 3, tex2_offset = 8;

            gl.glVertexAttribPointer(position_loc, 3, GLenum.GL_FLOAT, false, stride, vertex_buffer.position(pos_offset));
            gl.glEnableVertexAttribArray(position_loc);
            gl.glVertexAttribPointer(texture0_loc, 4, GLenum.GL_FLOAT, false, stride, vertex_buffer.position(tex0_offset));
            gl.glEnableVertexAttribArray(texture0_loc);
            gl.glVertexAttribPointer(texture1_loc, 4, GLenum.GL_FLOAT, false, stride, vertex_buffer.position(tex1_offset));
            gl.glEnableVertexAttribArray(texture1_loc);
            gl.glVertexAttribPointer(texture2_loc, 3, GLenum.GL_FLOAT, false, stride, vertex_buffer.position(tex2_offset));
            gl.glEnableVertexAttribArray(texture2_loc);

            gl.glDrawArrays(GLenum.GL_TRIANGLE_FAN, 0, 4);

            gl.glDisableVertexAttribArray(position_loc);
            gl.glDisableVertexAttribArray(tex0_offset);
            gl.glDisableVertexAttribArray(tex1_offset);
            gl.glDisableVertexAttribArray(tex2_offset);
        }
    }

    void beginStencilMask(){
        gl.glClear(GLenum.GL_STENCIL_BUFFER_BIT);
        gl.glStencilFunc(GLenum.GL_ALWAYS,1,1);
        gl.glStencilOp(GLenum.GL_REPLACE,GLenum.GL_REPLACE,GLenum.GL_REPLACE);
    }

    void endStencilMask(){
        gl.glStencilOp(GLenum.GL_KEEP,GLenum.GL_KEEP,GLenum.GL_KEEP);
    }

    void stencilMaskOn(){
        gl.glStencilFunc(GLenum.GL_NOTEQUAL,1,1);
    }

    void blendInEffectBillboard(Matrix4f viewProj){
        gl.glBlendEquation(GLenum.GL_FUNC_ADD);
        gl.glBlendFuncSeparate(GLenum.GL_ONE,GLenum.GL_ONE_MINUS_SRC_ALPHA,GLenum.GL_ZERO,GLenum.GL_ONE);

        gl.glEnable(GLenum.GL_BLEND);
//        gl.glEnable(GLenum.GL_TEXTURE_RECTANGLE_ARB);
        gl.glBindTexture(GLenum.GL_TEXTURE_RECTANGLE_ARB,d_texture_no);

        int gora_x = d_blur3dEffectX;
        int gora_y = d_blur3dEffectY;
        int dol_x = 0, dol_y = 0;
        float fluid_base = d_nearBase;
        float fluid_radius = d_radius;

        /*GL11.glPushMatrix();
        {
            GL11.glTranslatef( d_rf.x, d_rf.y, d_rf.z);  TODO transform
            GL11.glMultMatrix( wrap16(d_VolRendTrickyRotation) );   TODO transform

            GL11.glBegin(GL11.GL_QUADS);
            {
                GL11.glTexCoord2f(dol_x,dol_y);
                GL11.glVertex3f( -fluid_base , -fluid_base, fluid_radius );

                GL11.glTexCoord2f(gora_x,dol_y);
                GL11.glVertex3f( fluid_base , -fluid_base , fluid_radius );

                GL11.glTexCoord2f(gora_x,gora_y);
                GL11.glVertex3f( fluid_base , fluid_base , fluid_radius );

                GL11.glTexCoord2f(dol_x,gora_y);
                GL11.glVertex3f( -fluid_base , fluid_base , fluid_radius );
            }
            GL11.glEnd();
        }
        GL11.glPopMatrix();*/

        viewProj.translate(d_rf.x, d_rf.y, d_rf.z);
        Matrix4f.mul(viewProj, d_VolRendTrickyRotation, viewProj);
        m_rectTexProgram.enable();
        int mvpLoc = m_rectTexProgram.getUniformLocation("g_MVP");
        gl.glUniformMatrix4fv(mvpLoc, false, CacheBuffer.wrap(viewProj));

        FloatBuffer vertex_buffer = CacheBuffer.getCachedFloatBuffer(5 * 4);
        vertex_buffer.put(dol_x).put(dol_y);
        vertex_buffer.put(-fluid_base).put(-fluid_base).put(fluid_radius);

        vertex_buffer.put(gora_x).put(dol_y);
        vertex_buffer.put(fluid_base).put(-fluid_base).put(fluid_radius);

        vertex_buffer.put(gora_x).put(gora_y);
        vertex_buffer.put(fluid_base).put(fluid_base).put(fluid_radius);

        vertex_buffer.put(dol_x).put(gora_y);
        vertex_buffer.put(-fluid_base).put(fluid_base).put(fluid_radius);
        vertex_buffer.flip();

        final int stride = 5 * 4;
        final int position_loc = 0, pos_offset = 2;
        final int texture0_loc = 1, tex0_offset = 0;

        gl.glVertexAttribPointer(position_loc, 3, GLenum.GL_FLOAT, false, stride, vertex_buffer.position(pos_offset));
        gl.glEnableVertexAttribArray(position_loc);
        gl.glVertexAttribPointer(texture0_loc, 2, GLenum.GL_FLOAT, false, stride, vertex_buffer.position(tex0_offset));
        gl.glEnableVertexAttribArray(texture0_loc);

        gl.glDrawArrays(GLenum.GL_TRIANGLE_FAN, 0, 4);

        gl.glDisableVertexAttribArray(position_loc);
        gl.glDisableVertexAttribArray(tex0_offset);

//        gl.glDisable(ARBTextureRectangle.GL_TEXTURE_RECTANGLE_ARB);
        gl.glDisable(GLenum.GL_BLEND);
    }

    void placeEffectData(ModelViewStack stack){
//        GL13.glLoadTransposeMatrix(wrap16(d_VolRendTrickyRotation));  TODO
        stack.loadTransposeMatrix(d_VolRendTrickyRotation);
    }

    void placeObstaclesMask(ModelViewStack mvp){
       /* GL11.glLoadIdentity();TODO
        GL11.glTranslatef(0.0f,0.0f,-len_r_cf);
        GL13.glMultTransposeMatrix(wrap16(d_VolRendTrickyRotation));*/
        mvp.loadIdentity();
       mvp.translate(0.0f,0.0f,-len_r_cf);
        mvp.multTransposeMatrix(d_VolRendTrickyRotation);
    }

    void placeObstacles(ModelViewStack mvp){
//        GL11.glTranslatef( d_rf.x, d_rf.y, d_rf.z);
        mvp.translate(d_rf.x, d_rf.y, d_rf.z);
    }
}
