package jet.opengl.demos.gpupro.ibl;

import com.nvidia.developer.opengl.app.NvSampleApp;
import com.nvidia.developer.opengl.ui.NvTweakEnumi;
import com.nvidia.developer.opengl.ui.NvTweakVarBase;

import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;

import jet.opengl.demos.nvidia.waves.samples.SkyBoxRender;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.texture.SamplerDesc;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureCube;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.DebugTools;
import jet.opengl.postprocessing.util.FileUtils;
import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.postprocessing.util.NvImage;

public class IndirectLighting extends NvSampleApp {

    private static final int ENV_TYPE_IRRAD = 0,
        ENV_TYPE_UE4 = 1,
        ENV_TYPE_FROSTBITE = 2,
        ENV_TYPE_COUNT = 3;

    private int mEnvType = ENV_TYPE_IRRAD;

    private SkyBoxRender mSkyBox;
    private IrradianceCubeMap mIrraMap;
    private TextureCube mOriginEnvMap;

    private GLSLProgram[] mShadingProg = new GLSLProgram[ENV_TYPE_COUNT];

    private GLFuncProvider gl;
    private final Matrix4f mProj = new Matrix4f();
    private final Matrix4f mView = new Matrix4f();
    private final Matrix4f mMVP = new Matrix4f();
    private final Matrix3f mNormalMat = new Matrix3f();
    private final Vector3f mEyePos = new Vector3f();

    private final int[] object_ids = new int[3];
    private final int[] triangles_count = new int[3];

    private static final String model_file[]={"venus","teapot","knot"};

    private boolean mShowIrraMap;

    // UE4 resources
    private TextureCube mUE4Speuclar;
    private Texture2D mUE4PreIntegrateGF;
    private int mUE4DiffuseMip;
    private boolean mShowUE4GF = false;

    // Frostbite resources
    private TextureCube mFBSpecularLD;
    private TextureCube mFBDiffuseLD;
    private Texture2D   mFBDFG;
    private boolean mShowFBDiffuse;
    private boolean mShowDFG;

    private boolean mEnableDiffuse = true;
    private boolean mEnabledSpecular = true;
    private float mRoughness = 0.1f;

    private int mSamplerLinear;

    @Override
    public void initUI() {
        NvTweakEnumi[] envTypes = {
            new NvTweakEnumi("IrradianceMap", ENV_TYPE_IRRAD),
            new NvTweakEnumi("Unreal4", ENV_TYPE_UE4),
            new NvTweakEnumi("Frostbite", ENV_TYPE_FROSTBITE),
        };
        NvTweakVarBase var = mTweakBar.addEnum("EnvType", createControl("mEnvType"), envTypes, 0);

        mTweakBar.subgroupSwitchStart(var);
        mTweakBar.subgroupSwitchCase(ENV_TYPE_IRRAD);
        mTweakBar.addValue("Show IrradianceCubeMap", createControl("mShowIrraMap"));
        mTweakBar.subgroupSwitchCase(ENV_TYPE_UE4);
        mTweakBar.addValue("Diffuse Mip", createControl("mUE4DiffuseMip"), 0, mFBSpecularLD.getMipLevels());
        mTweakBar.addValue("Show GF", createControl("mShowUE4GF"));
        mTweakBar.subgroupSwitchCase(ENV_TYPE_FROSTBITE);
        mTweakBar.addValue("Show Diffse Map", createControl("mShowFBDiffuse"));
        mTweakBar.addValue("Show DFG", createControl("mShowDFG"));
        mTweakBar.subgroupSwitchEnd();

        mTweakBar.addValue("Enable Diffuse", createControl("mEnableDiffuse"));
        mTweakBar.addValue("Enable Specular", createControl("mEnabledSpecular"));
        mTweakBar.addValue("Roughness", createControl("mRoughness"), 0.0001f, 1.0f);
    }

    @Override
    protected void initRendering() {
        gl = GLFuncProviderFactory.getGLFuncProvider();

        mSkyBox = new SkyBoxRender();
        try {
            NvImage image = new NvImage();
            image.loadImageFromFile("nvidia/WaveWorks/textures/sky_cube.dds");

            if(!image.isCubeMap()){
                throw new IllegalArgumentException("The nvidia/WaveWorks/textures/sky_cube.dds doesn't a cube map file.");
            }

            int cubeMap = image.updaloadTexture();
            mOriginEnvMap = TextureUtils.createTextureCube(GLenum.GL_TEXTURE_CUBE_MAP, cubeMap);
            if(mOriginEnvMap.getMipLevels() == 1){
                gl.glGenerateTextureMipmap(mOriginEnvMap.getTexture());
                gl.glTextureParameteri(mOriginEnvMap.getTexture(), GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR_MIPMAP_LINEAR);
            }

            final String root = "gpupro\\IBL\\shaders\\";
            mShadingProg[ENV_TYPE_IRRAD] = GLSLProgram.createFromFiles(root + "MatteObject.vert", root + "MatteObject.frag", new Macro("ENV_TYPE", ENV_TYPE_IRRAD));
            mShadingProg[ENV_TYPE_UE4] = GLSLProgram.createFromFiles(root + "MatteObject.vert", root + "MatteObject.frag", new Macro("ENV_TYPE", ENV_TYPE_UE4));
            mShadingProg[ENV_TYPE_FROSTBITE] = GLSLProgram.createFromFiles(root + "MatteObject.vert", root + "MatteObject.frag", new Macro("ENV_TYPE", ENV_TYPE_FROSTBITE));
        } catch (IOException e) {
            e.printStackTrace();
        }

        loadModels();

        m_transformer.setTranslationVec(new Vector3f(0.0f, 0.0f, -200.2f));
        m_transformer.setRotationVec(new Vector3f(-0.2f, -0.3f, 0));

        SamplerDesc desc = new SamplerDesc();
        desc.minFilter = desc.magFilter = GLenum.GL_NEAREST;
        mSamplerLinear = SamplerUtils.createSampler(desc);

        initFrostbiteResources();
        initIrradianceMap();
        initUE4Resources();
    }

    private void initIrradianceMap(){
        IBLDesc desc = new IBLDesc();
        desc.outputToInternalCubeMap = true;
        desc.outputSize = 64;
        desc.sourceFromFile = false;
        desc.sourceEnvMap  = mOriginEnvMap;

        mIrraMap = new IrradianceCubeMap();
        mIrraMap.generateCubeMap(desc);
    }

    private TextureCube createCubemap(int size, int mipLevels, int format){
        int cubeMap = gl.glGenTexture();
        gl.glBindTexture(GLenum.GL_TEXTURE_CUBE_MAP, cubeMap);
        gl.glTexStorage2D(GLenum.GL_TEXTURE_CUBE_MAP, mipLevels, format, size, size);
        gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);
        gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_MIN_FILTER, mipLevels > 1 ? GLenum.GL_LINEAR_MIPMAP_LINEAR : GLenum.GL_LINEAR);
        gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_WRAP_R, GLenum.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_CLAMP_TO_EDGE);

        return TextureUtils.createTextureCube(GLenum.GL_TEXTURE_CUBE_MAP, cubeMap);
    }

    private void initUE4Resources(){
        int mipLevels = Numeric.calculateMipLevels(Math.min(mOriginEnvMap.getWidth(), 256));
        mUE4Speuclar = createCubemap(Math.min(mOriginEnvMap.getWidth(), 256), mipLevels, GLenum.GL_RGBA16F);

        Texture2DDesc desc = new Texture2DDesc(128, 128, GLenum.GL_RG16F);
        mUE4PreIntegrateGF = TextureUtils.createTexture2D(desc, null);

        mUE4DiffuseMip = 4;

        SpecularIBLUE4 generator = new SpecularIBLUE4();
        generator.generate(mOriginEnvMap, mUE4Speuclar, mUE4PreIntegrateGF);
        generator.dispose();
    }

    private void initFrostbiteResources(){
        int mipLevels = Numeric.calculateMipLevels(Math.min(mOriginEnvMap.getWidth(), 256));
        mFBSpecularLD = createCubemap(Math.min(mOriginEnvMap.getWidth(), 256), mipLevels, GLenum.GL_RGBA16F);
        mFBDiffuseLD = createCubemap(Math.min(mOriginEnvMap.getWidth(), 32), 1, GLenum.GL_RGBA16F);

        Texture2DDesc desc = new Texture2DDesc(128, 128, GLenum.GL_RGBA16F);
        mFBDFG = TextureUtils.createTexture2D(desc, null);

        FrostbiteIBL generator = new FrostbiteIBL();
        generator.generate(mOriginEnvMap, mFBSpecularLD, mFBDiffuseLD, mFBDFG);
        generator.dispose();

        /*try {
            DebugTools.saveTextureAsImageFile(mFBDFG, "E:/textures/DFG2.png");
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }

    @Override
    public void display() {
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
        gl.glViewport(0,0, getGLContext().width(), getGLContext().height());
        gl.glClearColor(0,0,0,0);
        gl.glClearDepthf(1);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT|GLenum.GL_DEPTH_BUFFER_BIT);

        m_transformer.getModelViewMat(mView);
        Matrix4f.getNormalMatrix(mView, mNormalMat);
        Matrix4f.mul(mProj, mView, mMVP);
        Matrix4f.decompseRigidMatrix(mView, mEyePos, null, null);

        switch (mEnvType){
            case ENV_TYPE_IRRAD:
                if(mShowIrraMap){
                    mSkyBox.setCubemap(mIrraMap.getOutput().getTexture());
                }else{
                    mSkyBox.setCubemap(mOriginEnvMap.getTexture());
                }
                break;
            case ENV_TYPE_UE4:

                break;
            case ENV_TYPE_FROSTBITE:
                if(mShowFBDiffuse){
                    mSkyBox.setCubemap(mFBDiffuseLD.getTexture());
                }else{
                    mSkyBox.setCubemap(mOriginEnvMap.getTexture());
                }
                break;
        }

        mSkyBox.setRotateMatrix(mView);
        mSkyBox.setProjectionMatrix(mProj);
        mSkyBox.draw();
        GLCheck.checkError();

        // Render the model
        gl.glEnable(GLenum.GL_DEPTH_TEST);
        mShadingProg[mEnvType].enable();
        int normalMat = mShadingProg[mEnvType].getUniformLocation("g_NormalMat", true);
        int worldMat = mShadingProg[mEnvType].getUniformLocation("g_World");
        int viewProjMat = mShadingProg[mEnvType].getUniformLocation("g_ViewProj");
        int eyePos = mShadingProg[mEnvType].getUniformLocation("g_EyePos", true);
        int mipmap = mShadingProg[mEnvType].getUniformLocation("gDiffuseMip", true);
        int enableDiffuse = mShadingProg[mEnvType].getUniformLocation("g_EnableDiffuse", true);
        int enableSpecular = mShadingProg[mEnvType].getUniformLocation("g_EnableSpecular", true);
        int roughness = mShadingProg[mEnvType].getUniformLocation("g_Roughness", true);

        if(normalMat >=0) gl.glUniformMatrix3fv(normalMat, false, CacheBuffer.wrap(mNormalMat));
        if(viewProjMat >= 0) gl.glUniformMatrix4fv(viewProjMat, false, CacheBuffer.wrap(mMVP));
        if(worldMat >= 0) gl.glUniformMatrix4fv(worldMat, false, CacheBuffer.wrap(Matrix4f.IDENTITY));
        if(eyePos >=0)    gl.glUniform3f(eyePos, mEyePos.x, mEyePos.y, mEyePos.z);
        if(mipmap >= 0)   gl.glUniform1f(mipmap, mUE4DiffuseMip);
        if(enableDiffuse >=0) gl.glUniform1i(enableDiffuse, mEnableDiffuse?1:0);
        if(enableSpecular >=0) gl.glUniform1i(enableSpecular, mEnabledSpecular?1:0);
        if(roughness >=0) gl.glUniform1f(roughness, mRoughness);

        switch (mEnvType){
            case ENV_TYPE_IRRAD:
                gl.glActiveTexture(GLenum.GL_TEXTURE0);
                gl.glBindTexture(mIrraMap.getOutput().getTarget(), mIrraMap.getOutput().getTexture());
                break;
            case ENV_TYPE_UE4:
                gl.glActiveTexture(GLenum.GL_TEXTURE1);
                gl.glBindTexture(mUE4Speuclar.getTarget(), mUE4Speuclar.getTexture());
                gl.glActiveTexture(GLenum.GL_TEXTURE2);
                gl.glBindTexture(mUE4PreIntegrateGF.getTarget(), mUE4PreIntegrateGF.getTexture());
                gl.glBindSampler(2, mSamplerLinear);
                break;
            case ENV_TYPE_FROSTBITE:
                gl.glActiveTexture(GLenum.GL_TEXTURE0);
                gl.glBindTexture(mFBDiffuseLD.getTarget(), mFBDiffuseLD.getTexture());
                gl.glActiveTexture(GLenum.GL_TEXTURE1);
                gl.glBindTexture(mFBSpecularLD.getTarget(), mFBSpecularLD.getTexture());
                gl.glActiveTexture(GLenum.GL_TEXTURE2);
                gl.glBindTexture(mFBDFG.getTarget(), mFBDFG.getTexture());
                gl.glBindSampler(2, mSamplerLinear);
                break;
        }

        drawModel(0, 0, 1);

        gl.glBindSampler(2, 0);
    }

    @Override
    protected void reshape(int width, int height) {
        if(width <= 0 || height <= 0)
            return;

        Matrix4f.perspective(60, (float)width/height, 0.1f, 1000.f, mProj);
    }

    private void loadModels (){
        for(int i = 0; i < 3; i++){
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

        GLCheck.checkError("loadModels done!");
    }

    private void drawModel(int index, int position, int normal){
        gl.glDisable(GLenum.GL_CULL_FACE);
        gl.glBindVertexArray(0);

        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, object_ids[index]);
        gl.glEnableVertexAttribArray(position);
        gl.glEnableVertexAttribArray(normal);
        gl.glVertexAttribPointer(position, 3, GLenum.GL_FLOAT, false, 32, 0);
        gl.glVertexAttribPointer(normal, 3, GLenum.GL_FLOAT, false, 32, 12);
        gl.glDrawArrays( GLenum.GL_TRIANGLES, 0, triangles_count[index]);
        gl.glDisableVertexAttribArray(position);
        gl.glDisableVertexAttribArray(normal);

        GLCheck.checkError();
    }
}
