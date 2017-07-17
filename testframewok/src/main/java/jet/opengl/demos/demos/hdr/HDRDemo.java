package jet.opengl.demos.demos.hdr;

import com.nvidia.developer.opengl.app.NvSampleApp;
import com.nvidia.developer.opengl.ui.NvTweakBar;
import com.nvidia.developer.opengl.ui.NvTweakEnumi;
import com.nvidia.developer.opengl.ui.NvTweakVarBase;
import com.nvidia.developer.opengl.utils.HDRImage;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.PostProcessing;
import jet.opengl.postprocessing.core.PostProcessingDownsampleProgram;
import jet.opengl.postprocessing.core.PostProcessingFrameAttribs;
import jet.opengl.postprocessing.texture.FramebufferGL;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureAttachDesc;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.FileUtils;
import jet.opengl.postprocessing.util.LogUtil;
import jet.opengl.postprocessing.util.Numeric;

/**
 * Created by mazhen'gui on 2017/5/4.
 */

public class HDRDemo extends NvSampleApp {

    // enum MATERIAL_TYPE
    private static final int MATERIAL_MAT = 0x00000001;
    private static final int MATERIAL_REFRACT = 0x00000002;
    private static final int MATERIAL_REFLECT = 0x00000003;
    private static final int MATERIAL_MATTE = 0x00000011;
    private static final int MATERIAL_ALUM = 0x00000013;
    private static final int MATERIAL_SILVER = 0x00000023;
    private static final int MATERIAL_GOLDEN = 0x00000033;
    private static final int MATERIAL_METALIC = 0x00000043;
    private static final int MATERIAL_DIAMOND = 0x00000012;
    private static final int MATERIAL_EMERALD = 0x00000022;
    private static final int MATERIAL_RUBY = 0x00000032;

    private static final int CAMERA_GLARE = 0;
    private static final int FILMIC_GLARE = 1;

    private static final float Z_NEAR = 0.4f;
    private static final float Z_FAR = 5000.0f;
    private static final float FOV = 90f;  // 3.14f*0.5f

    private static final String model_file[]={"venus","teapot","knot"};
    private static final String s_hdr_tex[]={"rnl_cross_mmp_s.hdr", "grace_cross_mmp_s.hdr","altar_cross_mmp_s.hdr","uffizi_cross_mmp_s.hdr"};
    private static final String s_hdr_tex_rough[]={"rnl_cross_rough_mmp_s.hdr", "grace_cross_rough_mmp_s.hdr","altar_cross_rough_mmp_s.hdr","uffizi_cross_rough_mmp_s.hdr"};
    private static final String s_hdr_tex_irrad[]={"rnl_cross_irrad_mmp_s.hdr", "grace_cross_irrad_mmp_s.hdr","altar_cross_irrad_mmp_s.hdr","uffizi_cross_irrad_mmp_s.hdr"};
    private static final String[] cube_names = {"posx", "negx","posy", "negy","posz", "negz",};
    private static final float exposureCompansation[]={3.0f,3.0f,10.0f,4.0f};

    final MTLData material[]={
            new MTLData(MATERIAL_MATTE, 1.0f, 1.0f, 1.0f, 0.0f),
            new MTLData(MATERIAL_ALUM, 1.0f, 1.0f, 1.0f, 0.5f),
            new MTLData(MATERIAL_SILVER, 1.0f, 1.0f, 1.0f, 0.9f),
            new MTLData(MATERIAL_GOLDEN, 1.0f, 0.9f, 0.4f, 0.9f),
            new MTLData(MATERIAL_METALIC,1.0f, 1.0f, 1.0f, 0.1f),
            new MTLData(MATERIAL_DIAMOND, 0.8f, 0.8f, 0.8f, 1.0f),
            new MTLData(MATERIAL_EMERALD, 0.2f, 0.8f, 0.2f, 1.0f),
            new MTLData(MATERIAL_RUBY, 0.9f, 0.1f, 0.4f, 1.0f),
    };

    // programs
    final BaseProgram[] programs = new BaseProgram[3];
    PostProcessingDownsampleProgram simple_program;
    SkyProgram sky_program;

    PostProcessing m_PostProcessing;
    PostProcessingFrameAttribs m_frameAttribs;

    // vao
    final int[] object_ids = new int[3];
    final int[] triangles_count = new int[3];
    private VertexBufferObject m_skybox;

    FramebufferGL scene_buffer;
    Texture2D     scene_color;
    Texture2D     scene_depth;

    GLFuncProvider gl;

    final Matrix4f projection_mat = new Matrix4f();
    final Vector3f eye_pos = new Vector3f();

    // hdr images
//	private HDRImage[] image = new HDRImage[4];
    private int[] hdr_tex = new int[4];
    //	private HDRImage[] image_rough = new HDRImage[4];
    private int[] hdr_tex_rough = new int[4];
    //	private HDRImage[] image_irrad = new HDRImage[4];
    private int[] hdr_tex_irrad = new int[4];

    int m_sceneIndex = 2;
    int m_objectIndex;
    int m_materialIndex = 5;
    boolean m_autoSpin = false;
    boolean m_drawBackground = true;
    boolean m_hdr_post = false;
    float m_expAdjust = 1.4f;
    int m_glareType;

    final Matrix4f temp = new Matrix4f();
    final Matrix4f eye_mvp = new Matrix4f();

    @Override
    public void initUI() {
        @SuppressWarnings("unused")
        NvTweakVarBase var;

        NvTweakBar tweakBar = mTweakBar;
        var = tweakBar.addValue("Auto Spin", createControl("m_autoSpin"));
//        var = tweakBar.addValue("Draw Background", createControl("m_drawBackground"));
        var = tweakBar.addValue("HDR Post", createControl("m_hdr_post"));
//        var = tweakBar.addValue("Adaptive Exposure", createControl(hdr_params, "autoExposure"));
//        var = tweakBar.addValue("Exposure", createControl("m_expAdjust"), 1.0f, 5.0f);

//        tweakBar.addPadding();
        tweakBar.addPadding();
        NvTweakEnumi sceneIndex[] =
        {
                new NvTweakEnumi( "Nature", 0 ),
                new NvTweakEnumi( "Grace", 1 ),
                new NvTweakEnumi( "Altar", 2 ),
                new NvTweakEnumi( "Uffizi", 3 ),
        };
        tweakBar.addMenu("Select Scene:", createControl("m_sceneIndex"), sceneIndex, 0x22);

        tweakBar.addPadding();
        NvTweakEnumi materialIndex[] =
        {
                new NvTweakEnumi( "Matte", 0 ),
                new NvTweakEnumi( "Alum", 1 ),
                new NvTweakEnumi( "Silver", 2 ),
                new NvTweakEnumi( "Golden", 3 ),
                new NvTweakEnumi( "Metalic", 4 ),
                new NvTweakEnumi( "Diamond", 5 ),
                new NvTweakEnumi( "Emerald", 6 ),
                new NvTweakEnumi( "Ruby", 7 ),
        };
        tweakBar.addMenu("Select Material:", createControl("m_materialIndex"), materialIndex, 0x33);

        tweakBar.addPadding();
        tweakBar.addPadding();
        NvTweakEnumi objectIndex[] =
        {
                new NvTweakEnumi( "Venus", 0 ),
                new NvTweakEnumi( "Teapot", 1 ),
                new NvTweakEnumi( "Knot", 2 ),
        };

        tweakBar.addEnum("Select Object:", createControl("m_objectIndex"), objectIndex, 0x55);

        tweakBar.addPadding();
        NvTweakEnumi glareType[] =
        {
                new NvTweakEnumi( "Camera", CAMERA_GLARE ),
                new NvTweakEnumi( "Filmic", FILMIC_GLARE ),
        };

        tweakBar.addEnum("Glare Type:", createControl("m_glareType"), glareType, 0x44);
        tweakBar.syncValues();
    }

    @Override
    protected void initRendering() {
        getGLContext().setSwapInterval(0);
        gl = GLFuncProviderFactory.getGLFuncProvider();

        try {
            programs[0] = new MatteProgram();
            programs[1] = new RefractProgram();
            programs[2] = new ReflectProgram();
            sky_program = new SkyProgram();
            simple_program = new PostProcessingDownsampleProgram(0);
        } catch (IOException e) {
            e.printStackTrace();
        }

        GLCheck.checkError("HDRDemo::initRendering()");

        loadHdrImages();
        loadModels();
        m_transformer.setTranslationVec(new Vector3f(0.0f, 0.0f, -202.2f));
        m_transformer.setRotationVec(new Vector3f(-0.2f, -0.3f, 0));

        m_PostProcessing = new PostProcessing();
        m_frameAttribs = new PostProcessingFrameAttribs();
    }

    @Override
    public void display() {
        m_transformer.setRotationVel(new Vector3f(0.0f, m_autoSpin ? (Numeric.PI *0.05f) : 0.0f, 0.0f));
        scene_buffer.bind();

        //we only need to clear depth
        gl.glDisable(GLenum.GL_BLEND);
        gl.glClear(GLenum.GL_DEPTH_BUFFER_BIT);
        GLCheck.checkError("draw0");

        gl.glDisable(GLenum.GL_DEPTH_TEST);
        gl.glDisable(GLenum.GL_CULL_FACE);
        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(GLenum.GL_TEXTURE_CUBE_MAP, hdr_tex[m_sceneIndex]);
        GLCheck.checkError("draw1");
        Matrix4f mvp;

        m_transformer.getModelViewMat(temp);
        Matrix4f.decompseRigidMatrix(temp, eye_pos, null, null);
        Matrix4f view_inverse = eye_mvp;
        view_inverse.load(temp);
        view_inverse.invert();

        mvp = Matrix4f.mul(projection_mat, temp, temp);
//	    	sky_program.applyEyePos(eye_pos.getX(), eye_pos.getY(), eye_pos.getZ());
        // 1. draw the sky box
        m_drawBackground = true;
        if(m_drawBackground){
            sky_program.enable();
            sky_program.applyProjMat(projection_mat);
            sky_program.applyViewMat(view_inverse);

            gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, m_skybox.getVBO());
            gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, m_skybox.getIBO());

            gl.glEnableVertexAttribArray(sky_program.getAttribPosition());
            gl.glVertexAttribPointer(sky_program.getAttribPosition(), 3, GLenum.GL_FLOAT, false, 32, 0);

            gl.glDrawElements(GLenum.GL_TRIANGLES, 6*6, GLenum.GL_UNSIGNED_SHORT, 0);

            gl.glDisableVertexAttribArray(sky_program.getAttribPosition());

            gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
            gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);
        }
        // 2. draw the models.
        gl.glEnable(GLenum.GL_DEPTH_TEST);
        gl.glEnable(GLenum.GL_CULL_FACE);
        gl.glFrontFace(GLenum.GL_CCW);
        gl.glCullFace(GLenum.GL_FRONT);

        int mtlClass = material[m_materialIndex].type & 0xF;
//	    	programs[mtlClass].enable();
        BaseProgram program = null;
        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(GLenum.GL_TEXTURE_CUBE_MAP, hdr_tex[m_sceneIndex]);
        switch (mtlClass) {
            case MATERIAL_MAT:
            default:
//                System.out.println("MATERIAL_MAT");
                gl.glActiveTexture(GLenum.GL_TEXTURE1);
                gl.glBindTexture(GLenum.GL_TEXTURE_CUBE_MAP, hdr_tex_irrad[m_sceneIndex]);
                program = programs[0];
                break;
            case MATERIAL_REFLECT:
//                System.out.println("MATERIAL_REFLECT");
                gl.glActiveTexture(GLenum.GL_TEXTURE1);
                gl.glBindTexture(GLenum.GL_TEXTURE_CUBE_MAP, hdr_tex_rough[m_sceneIndex]);
                program = programs[2];
                break;
            case MATERIAL_REFRACT:
                program = programs[1];
                break;
        }
        program.enable();
        program.applyMVP(mvp);
        program.applyModelView(Matrix4f.IDENTITY);
        program.applyEyePos(eye_pos);
        program.applyEmission(0, 0, 0);
        program.applyColor(material[m_materialIndex].r, material[m_materialIndex].g, material[m_materialIndex].b, material[m_materialIndex].a);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, object_ids[m_objectIndex]);
        gl.glEnableVertexAttribArray(program.getAttribPosition());
        gl.glEnableVertexAttribArray(program.getAttribNormal());
        gl.glVertexAttribPointer(program.getAttribPosition(), 3, GLenum.GL_FLOAT, false, 32, 0);
        gl.glVertexAttribPointer(program.getAttribNormal(), 3, GLenum.GL_FLOAT, false, 32, 12);
//	    	GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, 32, 24);
        gl.glDrawArrays( GLenum.GL_TRIANGLES, 0, triangles_count[m_objectIndex]);
        gl.glDisableVertexAttribArray(program.getAttribPosition());
        gl.glDisableVertexAttribArray(program.getAttribNormal());
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
        gl.glActiveTexture(GLenum.GL_TEXTURE1);
        gl.glBindTexture(GLenum.GL_TEXTURE_CUBE_MAP, 0);
        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(GLenum.GL_TEXTURE_CUBE_MAP, 0);

        gl.glCullFace(GLenum.GL_BACK);  // reset to the default value.
        gl.glDisable(GLenum.GL_CULL_FACE);
        gl.glDisable(GLenum.GL_BLEND);
        gl.glDisable(GLenum.GL_DEPTH_TEST);
        scene_buffer.unbind();

        if(m_hdr_post){
            m_frameAttribs.sceneColorTexture =scene_color;
            m_frameAttribs.viewport.set(0,0, getGLContext().width(), getGLContext().height());
            m_frameAttribs.outputTexture = null;

            m_PostProcessing.addEyeAdaptation();
            m_PostProcessing.addLightEffect(true, true, getFrameDeltaTime());
            m_PostProcessing.performancePostProcessing(m_frameAttribs);
//            gl.glViewport(0,0, getGLContext().width(), getGLContext().height());
        }else{
            gl.glViewport(0, 0, getGLContext().width(), getGLContext().height());
            gl.glClear(GLenum.GL_DEPTH_BUFFER_BIT | GLenum.GL_COLOR_BUFFER_BIT);
            gl.glActiveTexture(GLenum.GL_TEXTURE0);
            gl.glDisable(GLenum.GL_DEPTH_TEST);
//            scene_buffer.bindColorTexture(0);
            gl.glActiveTexture(GLenum.GL_TEXTURE0);
            gl.glBindTexture(scene_color.getTarget(), scene_color.getTexture());
            simple_program.enable();
            gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
        }

        gl.glUseProgram(0);
    }

    @Override
    protected void reshape(int width, int height) {
        gl.glViewport(0, 0, width, height);
        if(width == 0 && height == 0)
            return;

        GLCheck.checkError("reshape0");

        if(scene_color !=null && scene_color.getWidth() == width && scene_color.getHeight() == height)
            return;
        if(scene_buffer != null)
            scene_color.dispose();

        scene_buffer = new FramebufferGL();
        int format = GLenum.GL_RGB16F;
        scene_buffer.bind();
        scene_color = scene_buffer.addTexture2D(new Texture2DDesc(width, height, format), new TextureAttachDesc());
        scene_depth = scene_buffer.addTexture2D(new Texture2DDesc(width, height, GLenum.GL_DEPTH_COMPONENT16), new TextureAttachDesc());
        scene_buffer.unbind();

        gl.glViewport(0,0, width, height);

        Matrix4f.perspective(FOV * 0.5f, (float)width/(float)height, Z_NEAR, Z_FAR, projection_mat);
        GLCheck.checkError("reshape1");
    }

    void loadModels (){
        for(int i = 0; i < 3; i++){
//            byte[] data = NvAssetLoader.read("hdr_shaders/" + model_file[i]);
            byte[] data = null;
            try {
                data = FileUtils.loadBytes("HDR/models/" + model_file[i]);
            } catch (IOException e) {
                e.printStackTrace();
            }
            int bufID;
            bufID = gl.glGenBuffer();
            gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, bufID);
            gl.glBufferData(GLenum.GL_ARRAY_BUFFER, CacheBuffer.wrap(data), GLenum.GL_STATIC_DRAW);
            gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);

            object_ids[i] = bufID;
            triangles_count[i] = data.length/32;
        }

        m_skybox = new VertexBufferObject();
        m_skybox.genVertexData(CacheBuffer.wrap(CubeData.verticesCube), 4*8*6*4, false);
        m_skybox.genIndexData(CacheBuffer.wrap(CubeData.indicesCube), 6*6*2, false);

        GLCheck.checkError("loadModels done!");
    }

    void loadHdrImages(){
        //load all HDRImages we need
        int i;
        for (i=0;i<4;i++) {
            HDRImage image = new HDRImage();
            if (!image.loadHDRIFromFile("HDR/textures/" + s_hdr_tex[i])) {
                LogUtil.e(LogUtil.LogType.NV_FRAMEWROK, String.format("Error loading image file '%s'\n", s_hdr_tex[i]));
            }
            if (!image.convertCrossToCubemap()) {
                LogUtil.e(LogUtil.LogType.NV_FRAMEWROK,"Error converting image to cubemap\n");
            };
            hdr_tex[i] = createCubemapTexture(image, GLenum.GL_RGB, true);
        }
        for (i=0;i<4;i++) {
            HDRImage image_rough = new HDRImage();
            if (!image_rough.loadHDRIFromFile("HDR/textures/" + s_hdr_tex_rough[i])) {
                LogUtil.e(LogUtil.LogType.NV_FRAMEWROK, String.format("Error loading image file '%s'", s_hdr_tex_rough[i]));
            }
            if (!image_rough.convertCrossToCubemap()) {
                LogUtil.e(LogUtil.LogType.NV_FRAMEWROK,"Error converting image to cubemap\n");
            };
            hdr_tex_rough[i] = createCubemapTexture(image_rough, GLenum.GL_RGB, true);
        }
        for (i=0;i<4;i++) {
            HDRImage image_irrad = new HDRImage();
            if (!image_irrad.loadHDRIFromFile("HDR/textures/" + s_hdr_tex_irrad[i])) {
                LogUtil.e(LogUtil.LogType.NV_FRAMEWROK, String.format("Error loading image file '%s'", s_hdr_tex_irrad[i]));
            }
            if (!image_irrad.convertCrossToCubemap()) {
                LogUtil.e(LogUtil.LogType.NV_FRAMEWROK, "Error converting image to cubemap");
            };
            hdr_tex_irrad[i] = createCubemapTexture(image_irrad, GLenum.GL_RGB, true);
        }

        GLCheck.checkError("Load Images");
    }

    int createCubemapTexture(HDRImage img, int internalformat, boolean filtering)
    {
        int tex = gl.glGenTexture();
        gl.glBindTexture(GLenum.GL_TEXTURE_CUBE_MAP, tex);

        gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_MAG_FILTER, filtering ? GLenum.GL_LINEAR : GLenum.GL_NEAREST);
        gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_MIN_FILTER, filtering ? GLenum.GL_LINEAR_MIPMAP_LINEAR : GLenum.GL_NEAREST_MIPMAP_NEAREST);
        gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_WRAP_R, GLenum.GL_CLAMP_TO_EDGE);

        gl.glPixelStorei(GLenum.GL_UNPACK_ALIGNMENT, 1);

        GLCheck.checkError("creating cube map0");
        short[] out = new short[img.getWidth()*img.getHeight()*3];
        for(int i=0; i<6; i++) {
            HDRImage.fp32toFp16(img.getLevel(0, GLenum.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i), 0, out, 0, img.getWidth(), img.getHeight());
            gl.glTexImage2D(GLenum.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0,
                    GLenum.GL_RGB16F, img.getWidth(), img.getHeight(), 0,
                    GLenum.GL_RGB, GLenum.GL_HALF_FLOAT, CacheBuffer.wrap(out));
        }

        GLCheck.checkError("creating cube map1");
        gl.glGenerateMipmap(GLenum.GL_TEXTURE_CUBE_MAP);
        GLCheck.checkError("creating cube map2..");
        gl.glPixelStorei(GLenum.GL_UNPACK_ALIGNMENT, 4);
        GLCheck.checkError("creating cube map3");

        return tex;
    }

    private final static class MTLData {
        int type;
        float r,g,b,a;

        MTLData(int type, float r, float g, float b, float a) {
            this.type = type;
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
        }
    }
}
