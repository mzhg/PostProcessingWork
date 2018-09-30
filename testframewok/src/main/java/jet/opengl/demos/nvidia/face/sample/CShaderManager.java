package jet.opengl.demos.nvidia.face.sample;

import org.lwjgl.util.vector.Matrix4f;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.HashMap;

import jet.opengl.demos.nvidia.face.libs.GFSDK_FaceWorks_CBData;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.GLSLProgramPipeline;
import jet.opengl.postprocessing.shader.ShaderLoader;
import jet.opengl.postprocessing.shader.ShaderProgram;
import jet.opengl.postprocessing.shader.ShaderSourceItem;
import jet.opengl.postprocessing.shader.ShaderType;
import jet.opengl.postprocessing.texture.SamplerDesc;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureCube;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.LogUtil;
import jet.opengl.postprocessing.util.Numeric;

/**
 * Created by mazhen'gui on 2017/9/5.
 */

final class CShaderManager {
    static final int SHDFEAT_None			= 0x00,
    SHDFEAT_Tessellation	= 0x01,
    SHDFEAT_SSS				= 0x02,
    SHDFEAT_DeepScatter		= 0x04,

    SHDFEAT_PSMask			= 0x06;				// Features that affect the pixel shader

    static final int CB_DEBUG						=(0);
    static final int CB_FRAME						=(1);
    static final int CB_SHADER						=(2);

    static final int TEX_CUBE_DIFFUSE				=(0);
    static final int TEX_CUBE_SPEC					=(1);
    static final int TEX_SHADOW_MAP					=(2);
    static final int TEX_VSM						=(3);
    static final int TEX_DIFFUSE0					=(4);
    static final int TEX_DIFFUSE1					=(5);
    static final int TEX_NORMAL						=(6);
    static final int TEX_SPEC						=(7);
    static final int TEX_GLOSS						=(8);
    static final int TEX_SSS_MASK					=(9);
    static final int TEX_DEEP_SCATTER_COLOR			=(10);
    static final int TEX_SOURCE						=(11);
    static final int TEX_CURVATURE_LUT				=(12);
    static final int TEX_SHADOW_LUT					=(13);

    private static final int SAMP_POINT_CLAMP				=(0);
    private static final int SAMP_BILINEAR_CLAMP			=(1);
    private static final int SAMP_TRILINEAR_REPEAT			=(2);
    private static final int SAMP_TRILINEAR_REPEAT_ANISO	=(3);
    private static final int SAMP_PCF						=(4);

    ShaderProgram m_pPsCopy;
    ShaderProgram		m_pPsCreateVSM;
    ShaderProgram	m_pVsCurvature;
    ShaderProgram		m_pPsCurvature;
    ShaderProgram		m_pPsThickness;
    ShaderProgram		m_pPsGaussian;
    ShaderProgram		m_pPsHair;
    ShaderProgram	m_pVsScreen;
    ShaderProgram	m_pVsShadow;
    ShaderProgram	m_pVsSkybox;
    ShaderProgram		m_pPsSkybox;
    ShaderProgram	m_pVsTess;
    ShaderProgram		m_pHsTess;
    ShaderProgram	m_pDsTess;
    ShaderProgram	m_pVsWorld;

    int	m_pSsPointClamp;
    int	m_pSsBilinearClamp;
    int	m_pSsTrilinearRepeat;
    int	m_pSsTrilinearRepeatAniso;
    int	m_pSsPCF;

    Runnable		m_pInputLayout;

    BufferGL m_pCbufDebug;
    BufferGL m_pCbufFrame;
    BufferGL m_pCbufShader;

    final HashMap<Integer, ShaderProgram> m_mapSkinFeaturesToShader = new HashMap<>();
    final HashMap<Integer, ShaderProgram> m_mapEyeFeaturesToShader = new HashMap<>();
    private GLFuncProvider gl;
    private GLSLProgramPipeline m_programPipeline;

    private static ShaderProgram createShader(String filename, ShaderType type){
        ShaderProgram shaderProgram = new ShaderProgram();
        ShaderSourceItem item = new ShaderSourceItem();
        item.type = type;
        try {
            item.source = ShaderLoader.loadShaderFile("nvidia/FaceWorks/shaders/" + filename, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int dot = filename.indexOf('.');
        String debugName = filename.substring(0, dot);
        GLSLProgram.createFromString(item, shaderProgram, debugName);
        shaderProgram.setName(debugName);
        shaderProgram.printPrograminfo();
        return shaderProgram;
    }

    void Init(){
        gl= GLFuncProviderFactory.getGLFuncProvider();
        m_programPipeline = new GLSLProgramPipeline();
        m_pPsCopy = createShader("copy_ps.glsl", ShaderType.FRAGMENT);
        m_pPsCreateVSM = createShader("create_vsm_ps.glsl", ShaderType.FRAGMENT);
        m_pVsCurvature = createShader("curvature_vs.glsl", ShaderType.VERTEX);
        m_pPsCurvature = createShader("curvature_ps.glsl", ShaderType.FRAGMENT);
        m_pPsGaussian = createShader("gaussian_ps.glsl", ShaderType.FRAGMENT);
        m_pPsHair = createShader("hair_ps.glsl", ShaderType.FRAGMENT);
        m_pVsScreen = createShader("screen_vs.glsl", ShaderType.VERTEX);
        m_pVsShadow = createShader("shadow_vs.glsl", ShaderType.VERTEX);
        m_pVsSkybox = createShader("skybox_vs.glsl", ShaderType.VERTEX);
        m_pPsSkybox = createShader("skybox_ps.glsl", ShaderType.FRAGMENT);
        m_pVsTess = createShader("tess_vs.glsl", ShaderType.VERTEX);
        m_pHsTess = createShader("tess_hs.glsl", ShaderType.TESS_CONTROL);
        m_pDsTess = createShader("tess_ds.glsl", ShaderType.TESS_EVAL);
        m_pPsThickness = createShader("thickness_ps.glsl", ShaderType.FRAGMENT);
        m_pVsWorld = createShader("world_vs.glsl", ShaderType.VERTEX);

        SamplerDesc sampDesc = new SamplerDesc();
        sampDesc.minFilter = GLenum.GL_NEAREST_MIPMAP_NEAREST;
        sampDesc.magFilter = GLenum.GL_NEAREST;
        m_pSsPointClamp = SamplerUtils.createSampler(sampDesc);

        sampDesc.minFilter = GLenum.GL_LINEAR_MIPMAP_NEAREST;
        sampDesc.magFilter = GLenum.GL_LINEAR;
        m_pSsBilinearClamp = SamplerUtils.createSampler(sampDesc);

        sampDesc.minFilter = GLenum.GL_LINEAR_MIPMAP_LINEAR;
        sampDesc.magFilter = GLenum.GL_LINEAR;
        sampDesc.wrapR = sampDesc.wrapS = sampDesc.wrapT = GLenum.GL_REPEAT;
        m_pSsTrilinearRepeat = SamplerUtils.createSampler(sampDesc);

        sampDesc.anisotropic = 16;
        m_pSsTrilinearRepeatAniso = SamplerUtils.createSampler(sampDesc);

        sampDesc.minFilter = GLenum.GL_LINEAR_MIPMAP_NEAREST;
        sampDesc.magFilter = GLenum.GL_LINEAR;
        sampDesc.anisotropic =0;
        sampDesc.compareFunc = GLenum.GL_LESS;
        sampDesc.compareMode = GLenum.GL_COMPARE_REF_TO_TEXTURE;
        sampDesc.wrapR = sampDesc.wrapS = sampDesc.wrapT = GLenum.GL_CLAMP_TO_BORDER;
        sampDesc.borderColor = 0xFFFFFFFF;
        m_pSsPCF = SamplerUtils.createSampler(sampDesc);

        m_pInputLayout = this::inputLayout;
        m_pCbufDebug = new BufferGL();
        m_pCbufDebug.initlize(GLenum.GL_UNIFORM_BUFFER, Numeric.divideAndRoundUp(CbufDebug.SIZE, 16) * 16, null, GLenum.GL_STREAM_DRAW);

        m_pCbufFrame = new BufferGL();
        m_pCbufFrame.initlize(GLenum.GL_UNIFORM_BUFFER, Numeric.divideAndRoundUp(CbufFrame.SIZE, 16) * 16, null, GLenum.GL_STREAM_DRAW);

        m_pCbufShader = new BufferGL();
        m_pCbufShader.initlize(GLenum.GL_UNIFORM_BUFFER, 96, null, GLenum.GL_STREAM_DRAW);
    }

    void inputLayout(){
        gl.glEnableVertexAttribArray(0);
        gl.glVertexAttribPointer(0, 3, GLenum.GL_FLOAT, false, Vertex.SIZE, 0);

        gl.glEnableVertexAttribArray(1);
        gl.glVertexAttribPointer(1, 3, GLenum.GL_FLOAT, false, Vertex.SIZE, 12);

        gl.glEnableVertexAttribArray(2);
        gl.glVertexAttribPointer(2, 2, GLenum.GL_FLOAT, false, Vertex.SIZE, 24);

        gl.glEnableVertexAttribArray(3);
        gl.glVertexAttribPointer(3, 3, GLenum.GL_FLOAT, false, Vertex.SIZE, 32);

        gl.glEnableVertexAttribArray(4);
        gl.glVertexAttribPointer(4, 1, GLenum.GL_FLOAT, false, Vertex.SIZE, 44);
    }

    void InitFrame(
//            ID3D11DeviceContext * pCtx,
            CbufDebug pCbufDebug,
            CbufFrame pCbufFrame,
            TextureCube pSrvCubeDiffuse,
            TextureCube pSrvCubeSpec,
            Texture2D pSrvCurvatureLUT,
            Texture2D pSrvShadowLUT){
        // Set all the samplers
//        pCtx->PSSetSamplers(SAMP_POINT_CLAMP, 1, &m_pSsPointClamp);
//        pCtx->PSSetSamplers(SAMP_BILINEAR_CLAMP, 1, &m_pSsBilinearClamp);
//        pCtx->PSSetSamplers(SAMP_TRILINEAR_REPEAT, 1, &m_pSsTrilinearRepeat);
//        pCtx->PSSetSamplers(SAMP_TRILINEAR_REPEAT_ANISO, 1, &m_pSsTrilinearRepeatAniso);
//        pCtx->PSSetSamplers(SAMP_PCF, 1, &m_pSsPCF);

        // Set the input assembler state
//        pCtx->IASetInputLayout(m_pInputLayout);
//        pCtx->IASetPrimitiveTopology(D3D11_PRIMITIVE_TOPOLOGY_TRIANGLELIST);

        // Update the constant buffers

//        D3D11_MAPPED_SUBRESOURCE map = {};
//        V(pCtx->Map(m_pCbufDebug, 0, D3D11_MAP_WRITE_DISCARD, 0, &map));
//        memcpy(map.pData, pCbufDebug, sizeof(CbufDebug));
//        pCtx->Unmap(m_pCbufDebug, 0);
        ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(CbufDebug.SIZE);
        pCbufDebug.store(buffer).flip();
        m_pCbufDebug.update(0, buffer);

//        V(pCtx->Map(m_pCbufFrame, 0, D3D11_MAP_WRITE_DISCARD, 0, &map));
//        memcpy(map.pData, pCbufFrame, sizeof(CbufFrame));
//        pCtx->Unmap(m_pCbufFrame, 0);
        buffer = CacheBuffer.getCachedByteBuffer(CbufFrame.SIZE);
        pCbufFrame.store(buffer).flip();
        m_pCbufFrame.update(0, buffer);

        // Set the constant buffers to all shader stages
//        pCtx->VSSetConstantBuffers(CB_DEBUG, 1, &m_pCbufDebug);
//        pCtx->HSSetConstantBuffers(CB_DEBUG, 1, &m_pCbufDebug);
//        pCtx->DSSetConstantBuffers(CB_DEBUG, 1, &m_pCbufDebug);
//        pCtx->PSSetConstantBuffers(CB_DEBUG, 1, &m_pCbufDebug);
//        pCtx->VSSetConstantBuffers(CB_FRAME, 1, &m_pCbufFrame);
//        pCtx->HSSetConstantBuffers(CB_FRAME, 1, &m_pCbufFrame);
//        pCtx->DSSetConstantBuffers(CB_FRAME, 1, &m_pCbufFrame);
//        pCtx->PSSetConstantBuffers(CB_FRAME, 1, &m_pCbufFrame);
//        pCtx->VSSetConstantBuffers(CB_SHADER, 1, &m_pCbufShader);
//        pCtx->HSSetConstantBuffers(CB_SHADER, 1, &m_pCbufShader);
//        pCtx->DSSetConstantBuffers(CB_SHADER, 1, &m_pCbufShader);
//        pCtx->PSSetConstantBuffers(CB_SHADER, 1, &m_pCbufShader);
        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, CB_DEBUG, m_pCbufDebug.getBuffer());
        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, CB_FRAME, m_pCbufFrame.getBuffer());
        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, CB_SHADER, m_pCbufShader.getBuffer());

        // Set the textures that are kept for the whole frame
//        pCtx->PSSetShaderResources(TEX_CUBE_DIFFUSE, 1, &pSrvCubeDiffuse);
//        pCtx->PSSetShaderResources(TEX_CUBE_SPEC, 1, &pSrvCubeSpec);
//        pCtx->PSSetShaderResources(TEX_CURVATURE_LUT, 1, &pSrvCurvatureLUT);
//        pCtx->PSSetShaderResources(TEX_SHADOW_LUT, 1, &pSrvShadowLUT);
        gl.glActiveTexture(GLenum.GL_TEXTURE0 + TEX_CUBE_DIFFUSE);
        gl.glBindTexture(GLenum.GL_TEXTURE_CUBE_MAP, pSrvCubeDiffuse.getTexture());
        gl.glActiveTexture(GLenum.GL_TEXTURE0 + TEX_CUBE_SPEC);
        gl.glBindTexture(GLenum.GL_TEXTURE_CUBE_MAP, pSrvCubeSpec.getTexture());
        gl.glActiveTexture(GLenum.GL_TEXTURE0 + TEX_CURVATURE_LUT);
        gl.glBindTexture(pSrvCurvatureLUT.getTarget(), pSrvCurvatureLUT.getTexture());
        gl.glActiveTexture(GLenum.GL_TEXTURE0 + TEX_SHADOW_LUT);
        gl.glBindTexture(pSrvShadowLUT.getTarget(), pSrvShadowLUT.getTexture());

        gl.glUseProgram(0);
        GLCheck.checkError();
    }

    void BindShadowTextures(
//            ID3D11DeviceContext * pCtx,
            Texture2D pSrvShadowMap,
            Texture2D pSrvVSM){
//        pCtx->PSSetShaderResources(TEX_SHADOW_MAP, 1, &pSrvShadowMap);
//        pCtx->PSSetShaderResources(TEX_VSM, 1, &pSrvVSM);
        gl.glActiveTexture(GLenum.GL_TEXTURE0 + TEX_SHADOW_MAP);
        gl.glBindTexture(pSrvShadowMap.getTarget(), pSrvShadowMap.getTexture());
        gl.glActiveTexture(GLenum.GL_TEXTURE0 + TEX_VSM);
        gl.glBindTexture(pSrvVSM.getTarget(), pSrvVSM.getTexture());
    }

    ShaderProgram			GetSkinShader(int features){
        features &= SHDFEAT_PSMask;

        ShaderProgram i =
                m_mapSkinFeaturesToShader.get(features);

        if (i != null)
        {
            return i;
        }

        return CreateSkinShader(/*pDevice,*/ features);

    }

    ShaderProgram			GetEyeShader(int features){
        features &= SHDFEAT_PSMask;

        ShaderProgram i = m_mapEyeFeaturesToShader.get(features);

        if (i != null)
        {
            return i;
        }

        return CreateEyeShader(features);
    }

    void BindCopy(
//            ID3D11DeviceContext * pCtx,
            Texture2D pSrvSrc,
            Matrix4f matTransformColor){
//        pCtx->VSSetShader(m_pVsScreen, null, 0);
//        pCtx->PSSetShader(m_pPsCopy, null, 0);
//        pCtx->PSSetShaderResources(TEX_SOURCE, 1, &pSrvSrc);
        m_programPipeline.enable();
        m_programPipeline.setVS(m_pVsScreen);
        m_programPipeline.setPS(m_pPsCopy);
        m_programPipeline.setTC(null);
        m_programPipeline.setTE(null);
        gl.glActiveTexture(GLenum.GL_TEXTURE0+TEX_SOURCE);
        gl.glBindTexture(pSrvSrc.getTarget(), pSrvSrc.getTexture());

        // Update constant buffer
//        HRESULT hr;
//        D3D11_MAPPED_SUBRESOURCE map = {};
//        V(pCtx->Map(m_pCbufShader, 0, D3D11_MAP_WRITE_DISCARD, 0, &map));
//        memcpy(map.pData, &matTransformColor, sizeof(matTransformColor));
//        pCtx->Unmap(m_pCbufShader, 0);
        ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(Matrix4f.SIZE);
        matTransformColor.store(buffer).flip();
        m_pCbufShader.update(0, buffer);
    }
    void BindCreateVSM(
//            ID3D11DeviceContext * pCtx,
            Texture2D pSrvSrc){
//        pCtx->VSSetShader(m_pVsScreen, null, 0);
//        pCtx->PSSetShader(m_pPsCreateVSM, null, 0);
//        pCtx->PSSetShaderResources(TEX_SOURCE, 1, &pSrvSrc);
        m_programPipeline.enable();
        m_programPipeline.setVS(m_pVsScreen);
        m_programPipeline.setPS(m_pPsCreateVSM);
        m_programPipeline.setTC(null);
        m_programPipeline.setTE(null);
        gl.glActiveTexture(GLenum.GL_TEXTURE0+TEX_SOURCE);
        gl.glBindTexture(pSrvSrc.getTarget(), pSrvSrc.getTexture());
    }
    void BindCurvature(
//            ID3D11DeviceContext * pCtx,
            float curvatureScale,
            float curvatureBias){
//        pCtx->VSSetShader(m_pVsCurvature, null, 0);
//        pCtx->PSSetShader(m_pPsCurvature, null, 0);
        m_programPipeline.enable();
        m_programPipeline.setVS(m_pVsCurvature);
        m_programPipeline.setPS(m_pPsCurvature);
        m_programPipeline.setTC(null);
        m_programPipeline.setTE(null);

        // Update constant buffer
//        HRESULT hr;
//        D3D11_MAPPED_SUBRESOURCE map = {};
//        V(pCtx->Map(m_pCbufShader, 0, D3D11_MAP_WRITE_DISCARD, 0, &map));
//        static_cast<float *>(map.pData)[0] = curvatureScale;
//        static_cast<float *>(map.pData)[1] = curvatureBias;
//        pCtx->Unmap(m_pCbufShader, 0);
        ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(8);
        buffer.putFloat(curvatureScale);
        buffer.putFloat(curvatureBias);
        buffer.flip();
        m_pCbufShader.update(0, buffer);
    }

    void BindThickness(
//            ID3D11DeviceContext* pCtx,
            GFSDK_FaceWorks_CBData pFaceWorksCBData){
//        pCtx->VSSetShader(m_pVsWorld, null, 0);
//        pCtx->PSSetShader(m_pPsThickness, null, 0);
        m_programPipeline.enable();
        m_programPipeline.setVS(m_pVsWorld);
        m_programPipeline.setPS(m_pPsThickness);
        m_programPipeline.setTC(null);
        m_programPipeline.setTE(null);

        // Update constant buffer
//        HRESULT hr;
//        D3D11_MAPPED_SUBRESOURCE map = {};
//        V(pCtx->Map(m_pCbufShader, 0, D3D11_MAP_WRITE_DISCARD, 0, &map));
//        memcpy(map.pData, pFaceWorksCBData, sizeof(GFSDK_FaceWorks_CBData));
//        pCtx->Unmap(m_pCbufShader, 0);
        ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(GFSDK_FaceWorks_CBData.SIZE);
        pFaceWorksCBData.store(buffer).flip();
        m_pCbufShader.update(0, buffer);
    }

    void BindGaussian(
//            ID3D11DeviceContext * pCtx,
            Texture2D pSrvSrc,
            float blurX,
            float blurY){
//        pCtx->VSSetShader(m_pVsScreen, null, 0);
//        pCtx->PSSetShader(m_pPsGaussian, null, 0);
//        pCtx->PSSetShaderResources(TEX_SOURCE, 1, &pSrvSrc);
        m_programPipeline.enable();
        m_programPipeline.setVS(m_pVsScreen);
        m_programPipeline.setPS(m_pPsGaussian);
        m_programPipeline.setTC(null);
        m_programPipeline.setTE(null);

        // Update constant buffer
//        HRESULT hr;
//        D3D11_MAPPED_SUBRESOURCE map = {};
//        V(pCtx->Map(m_pCbufShader, 0, D3D11_MAP_WRITE_DISCARD, 0, &map));
//        static_cast<float *>(map.pData)[0] = blurX;
//        static_cast<float *>(map.pData)[1] = blurY;
//        pCtx->Unmap(m_pCbufShader, 0);
        ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(8);
        buffer.putFloat(blurX);
        buffer.putFloat(blurY);
        buffer.flip();
        m_pCbufShader.update(0, buffer);
    }
    void BindShadow(
//            ID3D11DeviceContext * pCtx,
            Matrix4f matWorldToClipShadow){
//        pCtx->VSSetShader(m_pVsShadow, null, 0);
//        pCtx->PSSetShader(null, null, 0);
        m_programPipeline.enable();
        m_programPipeline.setVS(m_pVsShadow);
        m_programPipeline.setPS(null);
        m_programPipeline.setTC(null);
        m_programPipeline.setTE(null);

        // Update constant buffer
//        HRESULT hr;
//        D3D11_MAPPED_SUBRESOURCE map = {};
//        V(pCtx->Map(m_pCbufShader, 0, D3D11_MAP_WRITE_DISCARD, 0, &map));
//        memcpy(map.pData, &matWorldToClipShadow, sizeof(matWorldToClipShadow));
//        pCtx->Unmap(m_pCbufShader, 0);
        ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(Matrix4f.SIZE);
        matWorldToClipShadow.store(buffer).flip();
        m_pCbufShader.update(0, buffer);
    }

    void BindSkybox(
//            ID3D11DeviceContext * pCtx,
            TextureCube pSrvSkybox,
            Matrix4f matClipToWorldAxes){
//        pCtx->VSSetShader(m_pVsSkybox, null, 0);
//        pCtx->PSSetShader(m_pPsSkybox, null, 0);
//        pCtx->PSSetShaderResources(TEX_SOURCE, 1, &pSrvSkybox);
        m_programPipeline.enable();
        m_programPipeline.setVS(m_pVsSkybox);
        m_programPipeline.setPS(m_pPsSkybox);
        m_programPipeline.setTC(null);
        m_programPipeline.setTE(null);
        gl.glActiveTexture(GLenum.GL_TEXTURE0+TEX_SOURCE);
        gl.glBindTexture(pSrvSkybox.getTarget(), pSrvSkybox.getTexture());

        // Update constant buffer
//        HRESULT hr;
//        D3D11_MAPPED_SUBRESOURCE map = {};
//        V(pCtx->Map(m_pCbufShader, 0, D3D11_MAP_WRITE_DISCARD, 0, &map));
//        memcpy(map.pData, &matClipToWorldAxes, sizeof(matClipToWorldAxes));
//        pCtx->Unmap(m_pCbufShader, 0);
        ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(Matrix4f.SIZE);
        matClipToWorldAxes.store(buffer).flip();
        m_pCbufShader.update(0, buffer);
    }

    void BindMaterial(
//            ID3D11DeviceContext * pCtx,
            int features,
            Material pMtl){
        // Determine which pixel shader to use
        ShaderProgram pPs;
        switch (pMtl.m_shader)
        {
            case Skin:	pPs = GetSkinShader(features); break;
            case Eye:	pPs = GetEyeShader(features); break;
            case Hair:	pPs = m_pPsHair; break;
            default:
                throw new IllegalArgumentException();
        }

//        pDevice.dispose();
        assert(pPs!=null);

//        pCtx->PSSetShader(pPs, null, 0);
        gl.glUseProgram(0);
        m_programPipeline.enable();
        m_programPipeline.setPS(pPs);

        // Determine which vertex/tess shaders to use
        if ((features & SHDFEAT_Tessellation)!=0)
        {
//            pCtx->VSSetShader(m_pVsTess, null, 0);
//            pCtx->HSSetShader(m_pHsTess, null, 0);
//            pCtx->DSSetShader(m_pDsTess, null, 0);
//            pCtx->IASetPrimitiveTopology(D3D11_PRIMITIVE_TOPOLOGY_3_CONTROL_POINT_PATCHLIST);
            m_programPipeline.setVS(m_pVsTess);
            m_programPipeline.setTC(m_pHsTess);
            m_programPipeline.setTE(m_pDsTess);
            gl.glPatchParameteri(GLenum.GL_PATCH_VERTICES, 3);
        }
        else
        {
//            pCtx->VSSetShader(m_pVsWorld, null, 0);
            m_programPipeline.setVS(m_pVsWorld);
        }

        // Bind textures
        for (int i = 0; i < pMtl.m_aSrv.length; ++i)
        {
            if (pMtl.m_aSrv[i] != null)
            {
                gl.glActiveTexture(GLenum.GL_TEXTURE0 + pMtl.m_textureSlots[i]);
                gl.glBindTexture(pMtl.m_aSrv[i].getTarget(), pMtl.m_aSrv[i].getTexture());
            }
//                pCtx->PSSetShaderResources(pMtl->m_textureSlots[i], 1, &pMtl->m_aSrv[i]);
        }

        // Update constant buffer
//        HRESULT hr;
//        D3D11_MAPPED_SUBRESOURCE map = {};
//        V(pCtx->Map(m_pCbufShader, 0, D3D11_MAP_WRITE_DISCARD, 0, &map));
//        memcpy(map.pData, pMtl->m_constants, sizeof(pMtl->m_constants));
//        pCtx->Unmap(m_pCbufShader, 0);
        FloatBuffer buffer = CacheBuffer.wrap(pMtl.m_constants);
        m_pCbufShader.update(0, buffer);

        GLCheck.checkError();
    }

    void UnbindTess(){}

    void DiscardRuntimeCompiledShaders(){
        for(ShaderProgram program : m_mapSkinFeaturesToShader.values()){
            program.dispose();
        }

        m_mapSkinFeaturesToShader.clear();

        for(ShaderProgram program : m_mapEyeFeaturesToShader.values()){
            program.dispose();
        }
        m_mapEyeFeaturesToShader.clear();

    }

    void Release(){
        if (m_pPsCopy != null)
        {
            m_pPsCopy.dispose();
            m_pPsCopy = null;
        }
        if (m_pPsCreateVSM!=null)
        {
            m_pPsCreateVSM.dispose();
            m_pPsCreateVSM = null;
        }
        if (m_pVsCurvature!=null)
        {
            m_pVsCurvature.dispose();
            m_pVsCurvature = null;
        }
        if (m_pPsCurvature!=null)
        {
            m_pPsCurvature.dispose();
            m_pPsCurvature = null;
        }
        if (m_pPsThickness!=null)
        {
            m_pPsThickness.dispose();
            m_pPsThickness = null;
        }
        if (m_pPsGaussian!=null)
        {
            m_pPsGaussian.dispose();
            m_pPsGaussian = null;
        }
        if (m_pPsHair!=null)
        {
            m_pPsHair.dispose();
            m_pPsHair = null;
        }
        if (m_pVsScreen!=null)
        {
            m_pVsScreen.dispose();
            m_pVsScreen = null;
        }
        if (m_pVsShadow!=null)
        {
            m_pVsShadow.dispose();
            m_pVsShadow = null;
        }
        if (m_pVsSkybox!=null)
        {
            m_pVsSkybox.dispose();
            m_pVsSkybox = null;
        }
        if (m_pPsSkybox!=null)
        {
            m_pPsSkybox.dispose();
            m_pPsSkybox = null;
        }
        if (m_pVsTess!=null)
        {
            m_pVsTess.dispose();
            m_pVsTess = null;
        }
        if (m_pHsTess!=null)
        {
            m_pHsTess.dispose();
            m_pHsTess = null;
        }
        if (m_pDsTess!=null)
        {
            m_pDsTess.dispose();
            m_pDsTess = null;
        }
        if (m_pVsWorld!=null)
        {
            m_pVsWorld.dispose();
            m_pVsWorld = null;
        }

        /*if (m_pSsPointClamp)
        {
            m_pSsPointClamp.dispose();
            m_pSsPointClamp = null;
        }
        if (m_pSsBilinearClamp)
        {
            m_pSsBilinearClamp.dispose();
            m_pSsBilinearClamp = null;
        }
        if (m_pSsTrilinearRepeat)
        {
            m_pSsTrilinearRepeat.dispose();
            m_pSsTrilinearRepeat = null;
        }
        if (m_pSsTrilinearRepeatAniso)
        {
            m_pSsTrilinearRepeatAniso.dispose();
            m_pSsTrilinearRepeatAniso = null;
        }
        if (m_pSsPCF)
        {
            m_pSsPCF.dispose();
            m_pSsPCF = null;
        }

        if (m_pInputLayout)
        {
            m_pInputLayout.dispose();
            m_pInputLayout = null;
        }*/

        if (m_pCbufDebug!=null)
        {
            m_pCbufDebug.dispose();
            m_pCbufDebug = null;
        }
        if (m_pCbufFrame!=null)
        {
            m_pCbufFrame.dispose();
            m_pCbufFrame = null;
        }
        if (m_pCbufShader!=null)
        {
            m_pCbufShader.dispose();
            m_pCbufShader = null;
        }

        DiscardRuntimeCompiledShaders();
    }

    private ShaderProgram CreateSkinShader(final int features){
        LogUtil.i(LogUtil.LogType.DEFAULT, String.format("Generating skin shader with feature mask %d", features));

        String skin_glsl = null;
        try {
            skin_glsl = ShaderLoader.loadShaderFile("nvidia/FaceWorks/shaders/skin.glsl", false).toString();
        } catch (IOException e) {
            e.printStackTrace();
        }

        final String pattern =
                "%s\n"+
                "in VertexThrough\n" +
                "{" +
                "float3 m_pos;\n" +
                "float3 m_normal;\n" +
                "float2 m_uv;\n" +
                "float3 m_tangent;\n" +
                "float m_curvature;\n" +
                "}_input;\n"+
                "in float3 o_vecCamera;\n" +
                "in float4 o_uvzwShadow;\n" +
                "layout(location=0) out vec3 Out_Color;\n"+
                "void main()\n"+
                "{\n"+
                    "Vertex i_vtx;\n" +
                    "i_vtx.m_pos = _input.m_pos;\n" +
                    "i_vtx.m_normal = _input.m_normal;\n" +
                    "i_vtx.m_uv = _input.m_uv;\n" +
                    "i_vtx.m_tangent = _input.m_tangent;\n" +
                    "i_vtx.m_curvature = _input.m_curvature;\n" +
                "	SkinMegashader(i_vtx, o_vecCamera, o_uvzwShadow, Out_Color, %s, %s);\n"+
                "}\n";

        String source = String.format(pattern, skin_glsl, (features & SHDFEAT_SSS)!=0 ? "true" : "false",
                (features & SHDFEAT_DeepScatter) !=0? "true" : "false");

        ShaderProgram shaderProgram = new ShaderProgram();
        ShaderSourceItem item = new ShaderSourceItem();
        item.type = ShaderType.FRAGMENT;
        item.source = source;
        GLSLProgram.createFromString(item, shaderProgram, "skin" + ((features & SHDFEAT_SSS)!=0 ? "_SSS" : "") + ((features & SHDFEAT_DeepScatter) !=0? "_DeepScatter" : ""));

        m_mapSkinFeaturesToShader.put(features, shaderProgram);
        return shaderProgram;
    }

    private ShaderProgram CreateEyeShader(final int features){
        LogUtil.i(LogUtil.LogType.DEFAULT, String.format("Generating eye shader with feature mask %d", features));
        assert((features & SHDFEAT_PSMask) == features);

        String eye_glsl = null;
        try {
            eye_glsl = ShaderLoader.loadShaderFile("nvidia/FaceWorks/shaders/eye.glsl", false).toString();
        } catch (IOException e) {
            e.printStackTrace();
        }

        final String pattern =
                "%s\n"+
                "in VertexThrough\n" +
                "{" +
                "float3 m_pos;\n" +
                "float3 m_normal;\n" +
                "float2 m_uv;\n" +
                "float3 m_tangent;\n" +
                "float m_curvature;\n" +
                "}_input;\n"+
                "in float3 o_vecCamera;\n" +
                "in float4 o_uvzwShadow;\n" +
                " out vec3 Out_Color;\n"+
                "void main()\n"+
                "{\n"+
                    "Vertex i_vtx;\n" +
                    "i_vtx.m_pos = _input.m_pos;\n" +
                    "i_vtx.m_normal = _input.m_normal;\n" +
                    "i_vtx.m_uv = _input.m_uv;\n" +
                    "i_vtx.m_tangent = _input.m_tangent;\n" +
                    "i_vtx.m_curvature = _input.m_curvature;\n" +
                "	EyeMegashader(i_vtx, o_vecCamera, o_uvzwShadow, Out_Color, %s, %s);\n"+
                "}\n";

        String source = String.format(pattern, eye_glsl, (features & SHDFEAT_SSS)!=0 ? "true" : "false",
                (features & SHDFEAT_DeepScatter) !=0? "true" : "false");

        ShaderProgram shaderProgram = new ShaderProgram();
        ShaderSourceItem item = new ShaderSourceItem();
        item.type = ShaderType.FRAGMENT;
        item.source = source;
        GLSLProgram.createFromString(item, shaderProgram, "eye" + ((features & SHDFEAT_SSS)!=0 ? "_SSS" : "") + ((features & SHDFEAT_DeepScatter) !=0? "_DeepScatter" : ""));

        m_mapEyeFeaturesToShader.put(features, shaderProgram);
        return shaderProgram;
    }
}
