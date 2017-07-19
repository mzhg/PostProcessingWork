package jet.opengl.demos.intel.cloud;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.GLSLUtil;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.shader.ProgramResources;
import jet.opengl.postprocessing.shader.ShaderLoader;
import jet.opengl.postprocessing.shader.ShaderSourceItem;
import jet.opengl.postprocessing.shader.ShaderType;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/7/7.
 */

final class CRenderTechnique extends GLSLProgram{

    public static final int START_TEXTURE_UNIT = 0;
    public static final int TEX2D_LIGHT_SPACE_DEPTH = START_TEXTURE_UNIT+0;
    public static final int TEX2D_CLOUD_TRANSPARENCY = START_TEXTURE_UNIT+1;
    public static final int TEX2D_CLOUD_MIN_MAX_DEPTH = START_TEXTURE_UNIT+2;
    public static final int TEX2D_CLOUD_DENSITY = START_TEXTURE_UNIT+3;
    public static final int TEX2D_WHITE_NOISE = START_TEXTURE_UNIT+4;
    public static final int TEX3D_NOISE = START_TEXTURE_UNIT+5;
    public static final int TEX2D_MAX_DENSITY = START_TEXTURE_UNIT+6;
    public static final int TEX3D_LIGHT_ATTEN_MASS = START_TEXTURE_UNIT+7;
    public static final int TEX3D_CELL_DENSITY = START_TEXTURE_UNIT+8;
    public static final int TEX2D_AMB_SKY_LIGHT = START_TEXTURE_UNIT+9;
    public static final int TEX3D_LIGHT_CLOUD_TRANSPARENCY = START_TEXTURE_UNIT+10;
    public static final int TEX3D_LIGHT_CLOUD_MIN_MAX_DEPTH = START_TEXTURE_UNIT+11;
    public static final int TEX3D_PARTICLE_DENSITY_LUT = START_TEXTURE_UNIT+12;
    public static final int TEX3D_SINGLE_SCATT_IN_PART_LUT = START_TEXTURE_UNIT+13;
    public static final int TEX3D_MULTIL_SCATT_IN_PART_LUT = START_TEXTURE_UNIT+14;

    private int m_glDownscaledBackBufferWidthLoc = -1;
    private int m_glBackBufferHeightLoc = -1;
    private int m_glDensityGenerationMethodLoc = -1;
    private int m_g_GlobalCloudAttribs_mParticleTilingLoc = -1;
    private int m_g_GlobalCloudAttribs_fParticleCutOffDistLoc = -1;
    private int m_glNumParticleLayersLoc = -1;
    private int m_glNumRingsLoc = -1;
    private int m_g_GlobalCloudAttribs_fTimeLoc = -1;
    private int m_glBackBufferWidthLoc = -1;
    private int m_g_GlobalCloudAttribs_fTileTexWidthLoc = -1;
    private int m_g_GlobalCloudAttribs_f4ParameterLoc = -1;
    private int m_glMaxParticlesLoc = -1;
    private int m_g_GlobalCloudAttribs_fCloudThicknessLoc = -1;
    private int m_g_GlobalCloudAttribs_f2LiSpCloudDensityDimLoc = -1;
    private int m_g_GlobalCloudAttribs_fTileTexHeightLoc = -1;
    private int m_g_GlobalCloudAttribs_fCloudVolumeDensityLoc = -1;
    private int m_g_GlobalCloudAttribs_fReferenceParticleRadiusLoc = -1;
    private int m_glDownscaleFactorLoc = -1;
    private int m_g_GlobalCloudAttribs_fCloudAltitudeLoc = -1;
    private int m_g_GlobalCloudAttribs_fScatteringCoeffLoc = -1;
    private int m_glDownscaledBackBufferHeightLoc = -1;
    private int m_g_GlobalCloudAttribs_fCloudDensityThresholdLoc = -1;
    private int m_glRingExtensionLoc = -1;
    private int m_g_GlobalCloudAttribs_bVolumetricBlendingLoc = -1;
    private int m_glParameterLoc = -1;
    private int m_glNumCellsLoc = -1;
    private int m_glLiSpFirstListIndTexDimLoc = -1;
    private int m_glRingDimensionLoc = -1;
    private int m_glDensityBufferScaleLoc = -1;
    private int m_glMaxLayersLoc = -1;
    private int m_g_GlobalCloudAttribs_fAttenuationCoeffLoc = -1;
    private int m_glInnerRingDimLoc = -1;
    private int m_glNumCascadesLoc = -1;

    private int m_ViewProjInvLoc = -1;
    private int m_WorldViewProjLoc = -1;
    private int m_CameraPosLoc = -1;
    private int m_DirOnLightLoc = -1;
    private int m_ViewFrustumPlanesLoc;

    private boolean m_printOnce;

    CRenderTechnique(String filename, Macro[] macros){
        try {
            setSourceFromFiles("shader_libs/PostProcessingDefaultScreenSpaceVS.vert", "shader_libs/OutdoorSctr/" + filename, macros);
        } catch (IOException e) {
            e.printStackTrace();
        }

        int dot = filename.lastIndexOf('.');
        if(dot >= 0){
            setName(filename.substring(0, dot));
        }else{
            setName(filename);
        }
        initUniforms();
    }

    CRenderTechnique(String vertfile, String gemofile, String fragfile, Macro[] macros){
        try {
            CharSequence vertSrc = ShaderLoader.loadShaderFile("shader_libs/OutdoorSctr/" + vertfile, false);
            ShaderSourceItem vs_item = new ShaderSourceItem(vertSrc, ShaderType.VERTEX);
            vs_item.macros = macros;

            CharSequence gemoSrc = ShaderLoader.loadShaderFile("shader_libs/OutdoorSctr/" + gemofile, false);
            ShaderSourceItem gs_item = new ShaderSourceItem(gemoSrc, ShaderType.GEOMETRY);
            gs_item.macros = macros;

            CharSequence fragSrc = ShaderLoader.loadShaderFile("shader_libs/OutdoorSctr/" + fragfile, false);
            ShaderSourceItem ps_item = new ShaderSourceItem(fragSrc, ShaderType.FRAGMENT);
            ps_item.macros = macros;

            setSourceFromStrings(vs_item, gs_item, ps_item);
        } catch (IOException e) {
            e.printStackTrace();
        }

        int dot = vertfile.lastIndexOf('.');
        if(dot >= 0){
            setName(vertfile.substring(0, dot));
        }else{
            setName(vertfile);
        }
        initUniforms();
    }

    CRenderTechnique(Void unused, String filename, Macro[] macros){
        try {
            CharSequence computeSrc = ShaderLoader.loadShaderFile("shader_libs/OutdoorSctr/" + filename, false);
            ShaderSourceItem cs_item = new ShaderSourceItem(computeSrc, ShaderType.COMPUTE);
            cs_item.macros = macros;
            setSourceFromStrings(cs_item);
        } catch (IOException e) {
            e.printStackTrace();
        }

        int dot = filename.lastIndexOf('.');
        if(dot >= 0){
            setName(filename.substring(0, dot));
        }else{
            setName(filename);
        }
        initUniforms();
    }

    public void printPrograminfo(){
        if(m_printOnce) return;

        m_printOnce = true;
        System.out.println("----------------------------"+getName() +"-----------------------------------------" );
//        ProgramProperties props = GLSLUtil.getProperties(getProgram());
        ProgramResources resources = GLSLUtil.getProgramResources(getProgram());
        System.out.println(resources);
    }

    private final void initUniforms(){
        m_glDownscaledBackBufferWidthLoc = gl.glGetUniformLocation(m_program, "g_GlobalCloudAttribs.uiDownscaledBackBufferWidth");
        m_glBackBufferHeightLoc = gl.glGetUniformLocation(m_program, "g_GlobalCloudAttribs.uiBackBufferHeight");
        m_glDensityGenerationMethodLoc = gl.glGetUniformLocation(m_program, "g_GlobalCloudAttribs.uiDensityGenerationMethod");
        m_g_GlobalCloudAttribs_mParticleTilingLoc = gl.glGetUniformLocation(m_program, "g_GlobalCloudAttribs.mParticleTiling");
        m_g_GlobalCloudAttribs_fParticleCutOffDistLoc = gl.glGetUniformLocation(m_program, "g_GlobalCloudAttribs.fParticleCutOffDist");
        m_glNumParticleLayersLoc = gl.glGetUniformLocation(m_program, "g_GlobalCloudAttribs.uiNumParticleLayers");
        m_glNumRingsLoc = gl.glGetUniformLocation(m_program, "g_GlobalCloudAttribs.uiNumRings");
        m_g_GlobalCloudAttribs_fTimeLoc = gl.glGetUniformLocation(m_program, "g_GlobalCloudAttribs.fTime");
        m_glBackBufferWidthLoc = gl.glGetUniformLocation(m_program, "g_GlobalCloudAttribs.uiBackBufferWidth");
        m_g_GlobalCloudAttribs_fTileTexWidthLoc = gl.glGetUniformLocation(m_program, "g_GlobalCloudAttribs.fTileTexWidth");
        m_g_GlobalCloudAttribs_f4ParameterLoc = gl.glGetUniformLocation(m_program, "g_GlobalCloudAttribs.f4Parameter");
        m_glMaxParticlesLoc = gl.glGetUniformLocation(m_program, "g_GlobalCloudAttribs.uiMaxParticles");
        m_g_GlobalCloudAttribs_fCloudThicknessLoc = gl.glGetUniformLocation(m_program, "g_GlobalCloudAttribs.fCloudThickness");
        m_g_GlobalCloudAttribs_f2LiSpCloudDensityDimLoc = gl.glGetUniformLocation(m_program, "g_GlobalCloudAttribs.f2LiSpCloudDensityDim");
        m_g_GlobalCloudAttribs_fTileTexHeightLoc = gl.glGetUniformLocation(m_program, "g_GlobalCloudAttribs.fTileTexHeight");
        m_g_GlobalCloudAttribs_fCloudVolumeDensityLoc = gl.glGetUniformLocation(m_program, "g_GlobalCloudAttribs.fCloudVolumeDensity");
        m_g_GlobalCloudAttribs_fReferenceParticleRadiusLoc = gl.glGetUniformLocation(m_program, "g_GlobalCloudAttribs.fReferenceParticleRadius");
        m_glDownscaleFactorLoc = gl.glGetUniformLocation(m_program, "g_GlobalCloudAttribs.uiDownscaleFactor");
        m_g_GlobalCloudAttribs_fCloudAltitudeLoc = gl.glGetUniformLocation(m_program, "g_GlobalCloudAttribs.fCloudAltitude");
        m_g_GlobalCloudAttribs_fScatteringCoeffLoc = gl.glGetUniformLocation(m_program, "g_GlobalCloudAttribs.fScatteringCoeff");
        m_glDownscaledBackBufferHeightLoc = gl.glGetUniformLocation(m_program, "g_GlobalCloudAttribs.uiDownscaledBackBufferHeight");
        m_g_GlobalCloudAttribs_fCloudDensityThresholdLoc = gl.glGetUniformLocation(m_program, "g_GlobalCloudAttribs.fCloudDensityThreshold");
        m_glRingExtensionLoc = gl.glGetUniformLocation(m_program, "g_GlobalCloudAttribs.uiRingExtension");
        m_g_GlobalCloudAttribs_bVolumetricBlendingLoc = gl.glGetUniformLocation(m_program, "g_GlobalCloudAttribs.bVolumetricBlending");
        m_glParameterLoc = gl.glGetUniformLocation(m_program, "g_GlobalCloudAttribs.uiParameter");
        m_glNumCellsLoc = gl.glGetUniformLocation(m_program, "g_GlobalCloudAttribs.uiNumCells");
        m_glLiSpFirstListIndTexDimLoc = gl.glGetUniformLocation(m_program, "g_GlobalCloudAttribs.uiLiSpFirstListIndTexDim");
        m_glRingDimensionLoc = gl.glGetUniformLocation(m_program, "g_GlobalCloudAttribs.uiRingDimension");
        m_glDensityBufferScaleLoc = gl.glGetUniformLocation(m_program, "g_GlobalCloudAttribs.uiDensityBufferScale");
        m_glMaxLayersLoc = gl.glGetUniformLocation(m_program, "g_GlobalCloudAttribs.uiMaxLayers");
        m_g_GlobalCloudAttribs_fAttenuationCoeffLoc = gl.glGetUniformLocation(m_program, "g_GlobalCloudAttribs.fAttenuationCoeff");
        m_glInnerRingDimLoc = gl.glGetUniformLocation(m_program, "g_GlobalCloudAttribs.uiInnerRingDim");
        m_glNumCascadesLoc = gl.glGetUniformLocation(m_program, "g_GlobalCloudAttribs.uiNumCascades");

        m_ViewProjInvLoc = gl.glGetUniformLocation(m_program, "g_ViewProjInv");
        m_WorldViewProjLoc = gl.glGetUniformLocation(m_program, "g_WorldViewProj");
        m_CameraPosLoc = gl.glGetUniformLocation(m_program, "g_f4CameraPos");
        m_DirOnLightLoc = gl.glGetUniformLocation(m_program, "g_f4DirOnLight");
        m_ViewFrustumPlanesLoc = gl.glGetUniformLocation(m_program, "g_f4ViewFrustumPlanes");

        gl.glUseProgram(0);
    }
    
    private void setDownscaledBackBufferWidth(int i) { if(m_glDownscaledBackBufferWidthLoc >=0)gl.glUniform1ui(m_glDownscaledBackBufferWidthLoc, i);}
    private void setBackBufferHeight(int i) { if(m_glBackBufferHeightLoc >=0)gl.glUniform1ui(m_glBackBufferHeightLoc, i);}
    private void setDensityGenerationMethod(int i) { if(m_glDensityGenerationMethodLoc >=0)gl.glUniform1ui(m_glDensityGenerationMethodLoc, i);}
    private void setParticleTiling(Matrix4f mat) { if(m_g_GlobalCloudAttribs_mParticleTilingLoc >=0)gl.glUniformMatrix4fv(m_g_GlobalCloudAttribs_mParticleTilingLoc, false, CacheBuffer.wrap(mat));}
    private void setParticleCutOffDist(float f) { if(m_g_GlobalCloudAttribs_fParticleCutOffDistLoc >=0)gl.glUniform1f(m_g_GlobalCloudAttribs_fParticleCutOffDistLoc, f);}
    private void setNumParticleLayers(int i) { if(m_glNumParticleLayersLoc >=0)gl.glUniform1ui(m_glNumParticleLayersLoc, i);}
    private void setNumRings(int i) { if(m_glNumRingsLoc >=0)gl.glUniform1ui(m_glNumRingsLoc, i);}
    private void setTime(float f) { if(m_g_GlobalCloudAttribs_fTimeLoc >=0)gl.glUniform1f(m_g_GlobalCloudAttribs_fTimeLoc, f);}
    private void setBackBufferWidth(int i) { if(m_glBackBufferWidthLoc >=0)gl.glUniform1ui(m_glBackBufferWidthLoc, i);}
    private void setTileTexWidth(float f) { if(m_g_GlobalCloudAttribs_fTileTexWidthLoc >=0)gl.glUniform1f(m_g_GlobalCloudAttribs_fTileTexWidthLoc, f);}
    private void setParameter(Vector4f v) { if(m_g_GlobalCloudAttribs_f4ParameterLoc >=0)gl.glUniform4f(m_g_GlobalCloudAttribs_f4ParameterLoc, v.x, v.y, v.z, v.w);}
    private void setMaxParticles(int i) { if(m_glMaxParticlesLoc >=0)gl.glUniform1ui(m_glMaxParticlesLoc, i);}
    private void setCloudThickness(float f) { if(m_g_GlobalCloudAttribs_fCloudThicknessLoc >=0)gl.glUniform1f(m_g_GlobalCloudAttribs_fCloudThicknessLoc, f);}
    private void setLiSpCloudDensityDim(Vector2f v) { if(m_g_GlobalCloudAttribs_f2LiSpCloudDensityDimLoc >=0)gl.glUniform2f(m_g_GlobalCloudAttribs_f2LiSpCloudDensityDimLoc, v.x, v.y);}
    private void setTileTexHeight(float f) { if(m_g_GlobalCloudAttribs_fTileTexHeightLoc >=0)gl.glUniform1f(m_g_GlobalCloudAttribs_fTileTexHeightLoc, f);}
    private void setCloudVolumeDensity(float f) { if(m_g_GlobalCloudAttribs_fCloudVolumeDensityLoc >=0)gl.glUniform1f(m_g_GlobalCloudAttribs_fCloudVolumeDensityLoc, f);}
    private void setReferenceParticleRadius(float f) { if(m_g_GlobalCloudAttribs_fReferenceParticleRadiusLoc >=0)gl.glUniform1f(m_g_GlobalCloudAttribs_fReferenceParticleRadiusLoc, f);}
    private void setDownscaleFactor(int i) { if(m_glDownscaleFactorLoc >=0)gl.glUniform1ui(m_glDownscaleFactorLoc, i);}
    private void setCloudAltitude(float f) { if(m_g_GlobalCloudAttribs_fCloudAltitudeLoc >=0)gl.glUniform1f(m_g_GlobalCloudAttribs_fCloudAltitudeLoc, f);}
    private void setScatteringCoeff(float f) { if(m_g_GlobalCloudAttribs_fScatteringCoeffLoc >=0)gl.glUniform1f(m_g_GlobalCloudAttribs_fScatteringCoeffLoc, f);}
    private void setDownscaledBackBufferHeight(int i) { if(m_glDownscaledBackBufferHeightLoc >=0)gl.glUniform1ui(m_glDownscaledBackBufferHeightLoc, i);}
    private void setCloudDensityThreshold(float f) { if(m_g_GlobalCloudAttribs_fCloudDensityThresholdLoc >=0)gl.glUniform1f(m_g_GlobalCloudAttribs_fCloudDensityThresholdLoc, f);}
    private void setRingExtension(int i) { if(m_glRingExtensionLoc >=0)gl.glUniform1ui(m_glRingExtensionLoc, i);}
    private void setVolumetricBlending(boolean b) { if(m_g_GlobalCloudAttribs_bVolumetricBlendingLoc >=0)gl.glUniform1ui(m_g_GlobalCloudAttribs_bVolumetricBlendingLoc, b ? 1 : 0);}
    private void setParameter(int i) { if(m_glParameterLoc >=0)gl.glUniform1ui(m_glParameterLoc, i);}
    private void setNumCells(int i) { if(m_glNumCellsLoc >=0)gl.glUniform1ui(m_glNumCellsLoc, i);}
    private void setLiSpFirstListIndTexDim(int i) { if(m_glLiSpFirstListIndTexDimLoc >=0)gl.glUniform1ui(m_glLiSpFirstListIndTexDimLoc, i);}
    private void setRingDimension(int i) { if(m_glRingDimensionLoc >=0)gl.glUniform1ui(m_glRingDimensionLoc, i);}
    private void setDensityBufferScale(int i) { if(m_glDensityBufferScaleLoc >=0)gl.glUniform1ui(m_glDensityBufferScaleLoc, i);}
    private void setMaxLayers(int i) { if(m_glMaxLayersLoc >=0)gl.glUniform1ui(m_glMaxLayersLoc, i);}
    private void setAttenuationCoeff(float f) { if(m_g_GlobalCloudAttribs_fAttenuationCoeffLoc >=0)gl.glUniform1f(m_g_GlobalCloudAttribs_fAttenuationCoeffLoc, f);}
    private void setInnerRingDim(int i) { if(m_glInnerRingDimLoc >=0)gl.glUniform1ui(m_glInnerRingDimLoc, i);}
    private void setNumCascades(int i) { if(m_glNumCascadesLoc >=0)gl.glUniform1ui(m_glNumCascadesLoc, i);}

    public void setWorldViewProj(Matrix4f mat) { if(m_WorldViewProjLoc >=0) gl.glUniformMatrix4fv(m_WorldViewProjLoc, false, CacheBuffer.wrap(mat));}
    public void setViewProjInv(Matrix4f mat) { if(m_ViewProjInvLoc >=0) gl.glUniformMatrix4fv(m_ViewProjInvLoc, false, CacheBuffer.wrap(mat));}
    public void setCameraPos(Vector3f pos)  { if(m_CameraPosLoc >=0) gl.glUniform4f(m_CameraPosLoc, pos.x, pos.y, pos.z, 1.0f);}
    public void setDirOnLight(Vector3f dir)  { if(m_DirOnLightLoc >=0) gl.glUniform4f(m_DirOnLightLoc, dir.x, dir.y, dir.z, 0);}
    public void setViewFrustumPlanes(Vector4f[] dir)  { if(m_ViewFrustumPlanesLoc >=0) gl.glUniform4fv(m_ViewFrustumPlanesLoc, CacheBuffer.wrap(dir));}

    public void setUniforms(SGlobalCloudAttribs attribs){
        setDownscaledBackBufferWidth(attribs.uiDownscaledBackBufferWidth);
        setBackBufferHeight(attribs.uiBackBufferHeight);
        setDensityGenerationMethod(attribs.uiDensityGenerationMethod);
        setParticleTiling(attribs.mParticleTiling);
        setParticleCutOffDist(attribs.fParticleCutOffDist);
        setNumParticleLayers(attribs.uiNumParticleLayers);
        setNumRings(attribs.uiNumRings);
        setTime(attribs.fTime);
        setBackBufferWidth(attribs.uiBackBufferWidth);
        setTileTexWidth(attribs.fTileTexWidth);
        setParameter(attribs.f4Parameter);
        setMaxParticles(attribs.uiMaxParticles);
        setCloudThickness(attribs.fCloudThickness);
        setLiSpCloudDensityDim(attribs.f2LiSpCloudDensityDim);
        setTileTexHeight(attribs.fTileTexHeight);
        setCloudVolumeDensity(attribs.fCloudVolumeDensity);
        setReferenceParticleRadius(attribs.fReferenceParticleRadius);
        setDownscaleFactor(attribs.uiDownscaleFactor);
        setCloudAltitude(attribs.fCloudAltitude);
        setScatteringCoeff(attribs.fScatteringCoeff);
        setDownscaledBackBufferHeight(attribs.uiDownscaledBackBufferHeight);
        setCloudDensityThreshold(attribs.fCloudDensityThreshold);
        setRingExtension(attribs.uiRingExtension);
        setVolumetricBlending(attribs.bVolumetricBlending);
        setParameter(attribs.uiParameter);
        setNumCells(attribs.uiNumCells);
        setLiSpFirstListIndTexDim(attribs.uiLiSpFirstListIndTexDim);
        setRingDimension(attribs.uiRingDimension);
        setDensityBufferScale(attribs.uiDensityBufferScale);
        setMaxLayers(attribs.uiMaxLayers);
        setAttenuationCoeff(attribs.fAttenuationCoeff);
        setInnerRingDim(attribs.uiInnerRingDim);
        setNumCascades(attribs.uiNumCascades);
    }
}
