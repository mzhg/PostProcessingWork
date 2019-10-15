package jet.opengl.demos.nvidia.waves.samples;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;

import jet.opengl.demos.nvidia.waves.wavework.GFSDK_WaveWorks;
import jet.opengl.demos.nvidia.waves.wavework.GFSDK_WaveWorks_Quadtree;
import jet.opengl.demos.nvidia.waves.wavework.GFSDK_WaveWorks_Quadtree_Params;
import jet.opengl.demos.nvidia.waves.wavework.GFSDK_WaveWorks_Result;
import jet.opengl.demos.nvidia.waves.wavework.GFSDK_WaveWorks_ShaderInput_Desc;
import jet.opengl.demos.nvidia.waves.wavework.GFSDK_WaveWorks_Simulation;
import jet.opengl.demos.nvidia.waves.wavework.GFSDK_WaveWorks_Simulation_GL_Pool;
import jet.opengl.demos.nvidia.waves.wavework.GFSDK_WaveWorks_Simulation_Settings;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.NvImage;

/**
 * Created by mazhen'gui on 2017/9/28.
 */

final class OceanSurfaceGL {
    // Constants
    int ScreenWidth;
    int ScreenHeight;
    int MultiSampleCount;
    int MultiSampleQuality;

    // Programs
    GLSLProgram WaterProgram;

    // Textures
    Texture2D FoamIntensityTextureID;
    int					FoamIntensityTextureBindLocation;
    Texture2D					FoamDiffuseTextureID;
    int					FoamDiffuseTextureBindLocation;

    // GL resources allocated for WaveWorks during ocean surface rendering
    GFSDK_WaveWorks_Simulation_GL_Pool glPool;

    // Camera reated variables
    final Vector3f          CameraPosition = new Vector3f();
    final Vector3f          LookAtPosition = new Vector3f();
    final Matrix4f          NormalViewMatrix = new Matrix4f();
    final Matrix4f          NormalProjMatrix = new Matrix4f();
    final Matrix4f          NormalViewProjMatrix = new Matrix4f();

    // Counters
    float					total_time;
    float					delta_time;

    // Input
    int						MouseX,MouseY;
    float					MouseDX,MouseDY;
    float					Alpha;
    float					Beta;
    private GLFuncProvider gl;

    OceanSurfaceGL(boolean use_texture_arrays){
        gl = GLFuncProviderFactory.getGLFuncProvider();
        // loading & compiling water shader
        final String shader_path = "nvidia/WaveWorks/shaders/";
        try {
            WaterProgram = GLSLProgram.createFromFiles(shader_path + "OceanWaveGLVS.vert", shader_path + "OceanWaveGLPS.frag", new Macro[]{new Macro("GFSDK_WAVEWORKS_GL", 1)});
        } catch (IOException e) {
            e.printStackTrace();
        }

        // initializing quadtree
        hOceanQuadTree				= null;

        // get the attribute count, so that we can search for any attrib shader inputs needed by waveworks
        int numAttrs = 1;
//        glGetProgramiv(WaterProgram,GL_ACTIVE_ATTRIBUTES,&numAttrs);

        // initializing quadtree shader input mappings
        int NumQuadtreeShaderInputs	= GFSDK_WaveWorks.GFSDK_WaveWorks_Quadtree_GetShaderInputCountGL2();
        pQuadTreeShaderInputMappings	= new int [NumQuadtreeShaderInputs];
        GFSDK_WaveWorks_ShaderInput_Desc[] quadtree_descs =  new GFSDK_WaveWorks_ShaderInput_Desc[NumQuadtreeShaderInputs];
        for(int i = 0; i < NumQuadtreeShaderInputs; i++)
        {
            quadtree_descs[i] = new GFSDK_WaveWorks_ShaderInput_Desc();
            GFSDK_WaveWorks.GFSDK_WaveWorks_Quadtree_GetShaderInputDescGL2(i, quadtree_descs[i]);
            switch(quadtree_descs[i].Type)
            {
                case GFSDK_WaveWorks_ShaderInput_Desc.GL_FragmentShader_UniformLocation:
                case GFSDK_WaveWorks_ShaderInput_Desc.GL_VertexShader_UniformLocation:
                case GFSDK_WaveWorks_ShaderInput_Desc.GL_TessEvalShader_UniformLocation:
                    pQuadTreeShaderInputMappings[i] = WaterProgram.getUniformLocation(quadtree_descs[i].Name) /*glGetUniformLocation(WaterProgram, quadtree_descs[i].Name)*/;
                    break;
                case GFSDK_WaveWorks_ShaderInput_Desc.GL_AttribLocation:
                    pQuadTreeShaderInputMappings[i] =  GFSDK_WaveWorks.GFSDK_WaveWorks_UnusedShaderInputRegisterMapping;
                    for(int attrIndex = 0; attrIndex != numAttrs; ++attrIndex)
                    {
                        /*char attribName[256];  TODO
                        GLint size;
                        GLenum type;
                        GLsizei nameLen;
                        glGetActiveAttrib(WaterProgram,attrIndex,sizeof(attribName),&nameLen,&size,&type,attribName);
                        if(GFSDK_WaveWorks_GLAttribIsShaderInput(attribName,quadtree_descs[i]))
                        {
                            pQuadTreeShaderInputMappings[i] = glGetAttribLocation(WaterProgram, attribName);
                        }*/
                    }

                    break;
            }
        }

        // initializing simulation shader input mappings
        int NumSimulationShaderInputs	= GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_GetShaderInputCountGL2();
        pSimulationShaderInputMappings	= new int [NumSimulationShaderInputs];
        GFSDK_WaveWorks_ShaderInput_Desc[] simulation_descs =  new GFSDK_WaveWorks_ShaderInput_Desc[NumSimulationShaderInputs];

        for(int i = 0; i < NumSimulationShaderInputs; i++)
        {
            simulation_descs[i] = new GFSDK_WaveWorks_ShaderInput_Desc();
            GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_GetShaderInputDescGL2(i, simulation_descs[i]);
            pSimulationShaderInputMappings[i] =  WaterProgram.getUniformLocation(simulation_descs[i].Name) /*glGetUniformLocation(WaterProgram, simulation_descs[i].Name)*/;
        }

        // reserve texture units for WaveWorks for use during ocean surface rendering
        for(int i = 0; i != GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_GetTextureUnitCountGL2(use_texture_arrays); ++i)
        {
            glPool.Reserved_Texture_Units[i] = /*Num_OceanSurfaceTextureUnits*/0 + i;
        }

        // creating textures
        CreateTextures();

        // binding textures to shader
        FoamDiffuseTextureBindLocation = WaterProgram.getUniformLocation("g_texFoamDiffuseMap");
        FoamIntensityTextureBindLocation = WaterProgram.getUniformLocation("g_texFoamIntensityMap");

        // initializing the rest of variables
//        CameraPosition[0] = -20.0f;
//        CameraPosition[1] = 20.0f;
//        CameraPosition[2] = 20.0f;
//
//        LookAtPosition[0] = 0.0f;
//        LookAtPosition[1] = 0.0f;
//        LookAtPosition[2] = 0.0f;  TODO

        Alpha = 0;
        Beta = 0;

        ScreenWidth = 1280;
        ScreenHeight = 720;
        total_time = 0;

        // cleanup
//        SAFE_DELETE_ARRAY(quadtree_descs);
//        SAFE_DELETE_ARRAY(simulation_descs);
    }

    // Quadtree handle and shader mappings
    GFSDK_WaveWorks_Quadtree hOceanQuadTree;
    int[] pQuadTreeShaderInputMappings;
    int[] pSimulationShaderInputMappings;

    // Quadtree initialization
    GFSDK_WaveWorks_Result InitQuadTree(GFSDK_WaveWorks_Quadtree_Params params){
        if(null == hOceanQuadTree)
        {
            hOceanQuadTree = GFSDK_WaveWorks.GFSDK_WaveWorks_Quadtree_CreateGL2(params, WaterProgram.getProgram());
            return hOceanQuadTree != null ? GFSDK_WaveWorks_Result.OK : GFSDK_WaveWorks_Result.FAIL;
        }
        else
        {
            return GFSDK_WaveWorks.GFSDK_WaveWorks_Quadtree_UpdateParams(hOceanQuadTree, params);
        }
    }
    // Texture loading related methods
    void CreateTextures(){
        try {
            final String prefix = "nvidia/WaveWorks/textures/";
            int foam_intensity  = NvImage.uploadTextureFromDDSFile(prefix + "foam_intensity_perlin2_rgb.dds");
            int foam = NvImage.uploadTextureFromDDSFile(prefix + "foam24bit.dds"); // TODO origin is "foam.dds"
            FoamIntensityTextureID = TextureUtils.createTexture2D(GLenum.GL_TEXTURE_2D, foam_intensity);
            FoamDiffuseTextureID = TextureUtils.createTexture2D(GLenum.GL_TEXTURE_2D, foam);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void Render(Matrix4f viewMat, Matrix4f projMat,
            GFSDK_WaveWorks_Simulation hSim, GFSDK_WaveWorks_Simulation_Settings settings, boolean Wireframe){
        // setting up main buffer & GL state
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
        gl.glClearColor(0.38f, 0.45f, 0.56f, 0.0f);
        gl.glClearDepthf(1.0f);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT | GLenum.GL_DEPTH_BUFFER_BIT);
        gl.glViewport(0,0,ScreenWidth,ScreenHeight);
        if(Wireframe)
        {
            gl.glPolygonMode( GLenum.GL_FRONT_AND_BACK, GLenum.GL_LINE);
        }
        else
        {
            gl.glPolygonMode( GLenum.GL_FRONT_AND_BACK, GLenum.GL_FILL);
        }
        gl.glDepthFunc(GLenum.GL_LESS);
        gl.glEnable(GLenum.GL_DEPTH_TEST);
        gl.glEnable(GLenum.GL_CULL_FACE);

        // rendering water to main buffer
        // setting up program
        WaterProgram.enable();

//        glUniformMatrix4fv(glGetUniformLocation(WaterProgram, "u_ViewProjMatrix"), 1, GL_FALSE, (const GLfloat *) NormalViewProjMatrix);
        Matrix4f.mul(projMat, viewMat, NormalViewProjMatrix);
        int location =  WaterProgram.getUniformLocation("u_ViewProjMatrix");
        gl.glUniformMatrix4fv(location, false, CacheBuffer.wrap(NormalViewProjMatrix));

        //fprintf (stderr, "\n\nGFSDK_WaveWorks_Quadtree_GetShaderInputCountGL2: %i",nm);
        /*gfsdk_float4x4 ViewMatrix;
        gfsdk_float4x4 ProjMatrix;
        ViewMatrix._11 = NormalViewMatrix[0][0];
        ViewMatrix._12 = NormalViewMatrix[0][1];
        ViewMatrix._13 = NormalViewMatrix[0][2];
        ViewMatrix._14 = NormalViewMatrix[0][3];
        ViewMatrix._21 = NormalViewMatrix[1][0];
        ViewMatrix._22 = NormalViewMatrix[1][1];
        ViewMatrix._23 = NormalViewMatrix[1][2];
        ViewMatrix._24 = NormalViewMatrix[1][3];
        ViewMatrix._31 = NormalViewMatrix[2][0];
        ViewMatrix._32 = NormalViewMatrix[2][1];
        ViewMatrix._33 = NormalViewMatrix[2][2];
        ViewMatrix._34 = NormalViewMatrix[2][3];
        ViewMatrix._41 = NormalViewMatrix[3][0];
        ViewMatrix._42 = NormalViewMatrix[3][1];
        ViewMatrix._43 = NormalViewMatrix[3][2];
        ViewMatrix._44 = NormalViewMatrix[3][3];

        ProjMatrix._11 = NormalProjMatrix[0][0];
        ProjMatrix._12 = NormalProjMatrix[0][1];
        ProjMatrix._13 = NormalProjMatrix[0][2];
        ProjMatrix._14 = NormalProjMatrix[0][3];
        ProjMatrix._21 = NormalProjMatrix[1][0];
        ProjMatrix._22 = NormalProjMatrix[1][1];
        ProjMatrix._23 = NormalProjMatrix[1][2];
        ProjMatrix._24 = NormalProjMatrix[1][3];
        ProjMatrix._31 = NormalProjMatrix[2][0];
        ProjMatrix._32 = NormalProjMatrix[2][1];
        ProjMatrix._33 = NormalProjMatrix[2][2];
        ProjMatrix._34 = NormalProjMatrix[2][3];
        ProjMatrix._41 = NormalProjMatrix[3][0];
        ProjMatrix._42 = NormalProjMatrix[3][1];
        ProjMatrix._43 = NormalProjMatrix[3][2];
        ProjMatrix._44 = NormalProjMatrix[3][3];*/

        final int FoamDiffuseTextureUnit = 8;
        final int FoamIntensityTextureUnit = 9;

        // binding user textures
        gl.glActiveTexture(GLenum.GL_TEXTURE0 + FoamDiffuseTextureUnit);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D, FoamDiffuseTextureID.getTexture());
        gl.glTexParameterf(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MAX_ANISOTROPY_EXT, settings.aniso_level);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D,GLenum.GL_TEXTURE_MIN_FILTER,GLenum.GL_LINEAR_MIPMAP_LINEAR);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D,GLenum.GL_TEXTURE_MAG_FILTER,GLenum.GL_LINEAR);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D,GLenum.GL_TEXTURE_WRAP_S,GLenum.GL_REPEAT);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D,GLenum.GL_TEXTURE_WRAP_T,GLenum.GL_REPEAT);
        gl.glUniform1i(FoamDiffuseTextureBindLocation, FoamDiffuseTextureUnit);

        gl.glActiveTexture(GLenum.GL_TEXTURE0 + FoamIntensityTextureUnit);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D, FoamIntensityTextureID.getTexture());
        gl.glTexParameterf(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MAX_ANISOTROPY_EXT, settings.aniso_level);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D,GLenum.GL_TEXTURE_MIN_FILTER,GLenum.GL_LINEAR_MIPMAP_LINEAR);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D,GLenum.GL_TEXTURE_MAG_FILTER,GLenum.GL_LINEAR);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D,GLenum.GL_TEXTURE_WRAP_S,GLenum.GL_REPEAT);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D,GLenum.GL_TEXTURE_WRAP_T,GLenum.GL_REPEAT);
        gl.glUniform1i(FoamIntensityTextureBindLocation, FoamIntensityTextureUnit);

        GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_SetRenderStateGL2(hSim, viewMat, pSimulationShaderInputMappings, glPool);
        GFSDK_WaveWorks.GFSDK_WaveWorks_Quadtree_DrawGL2(hOceanQuadTree, viewMat, projMat, pQuadTreeShaderInputMappings);
        gl.glPolygonMode( GLenum.GL_FRONT_AND_BACK, GLenum.GL_FILL);
    }
}
