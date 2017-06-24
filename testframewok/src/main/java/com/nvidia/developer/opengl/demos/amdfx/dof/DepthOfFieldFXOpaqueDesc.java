package com.nvidia.developer.opengl.demos.amdfx.dof;

import org.lwjgl.util.vector.Vector4i;

import java.io.IOException;
import java.nio.ByteBuffer;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.shader.ShaderLoader;
import jet.opengl.postprocessing.shader.ShaderSourceItem;
import jet.opengl.postprocessing.shader.ShaderType;
import jet.opengl.postprocessing.texture.SamplerDesc;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;

import static com.nvidia.developer.opengl.demos.amdfx.dof.DEPTHOFFIELDFX_RETURN_CODE.DEPTHOFFIELDFX_RETURN_CODE_INVALID_SURFACE;
import static com.nvidia.developer.opengl.demos.amdfx.dof.DEPTHOFFIELDFX_RETURN_CODE.DEPTHOFFIELDFX_RETURN_CODE_SUCCESS;

/**
 * Created by mazhen'gui on 2017/6/24.
 */

final class DepthOfFieldFXOpaqueDesc {

    static final int s_bartlettData[][] = {
        { -1, -1, 1, 0 }, { 0, -1, -2, 0 }, { 1, -1, 1, 0 }, { -1, 0, -2, 0 }, { 0, 0, 4, 0 }, { 1, 0, -2, 0 }, { -1, 1, 1, 0 }, { 0, 1, -2, 0 }, { 1, 1, 1, 0 },
    };

    static final int s_boxBartlettData[][] = {
        { -1, -1, 1, 0 }, { 1, -1, -1, 0 }, { -1, 1, -1, 0 }, { 1, 1, 1, 0 },
    };

    int m_padding;
    int m_bufferWidth;
    int m_bufferHeight;

    int              m_pIntermediateBuffer;       // texture buffer
    int              m_pIntermediateBufferTransposed;  // texture buffer
    int              m_pIntermediateUAV;          //
    int              m_pIntermediateTransposedUAV;
    int              m_pDofParamsCB;       // uniform buffer
    int              m_pPointSampler;      // texture sampler

    GLSLProgram      m_pFastFilterSetupCS;
    GLSLProgram      m_pFastFilterSetupQuarterResCS;
    GLSLProgram      m_pBoxFastFilterSetupCS;
    GLSLProgram      m_pReadFinalResultCS;
    GLSLProgram      m_pVerticalIntegrateCS;
    GLSLProgram      m_pDoubleVerticalIntegrateCS;

    private final DofParams m_DofParams = new DofParams();

    private GLFuncProvider gl;

    DEPTHOFFIELDFX_RETURN_CODE initalize(){
        gl = GLFuncProviderFactory.getGLFuncProvider();
        DEPTHOFFIELDFX_RETURN_CODE result = create_shaders(/*desc*/);
//        ID3D11Device*              pDev   = desc.m_pDevice;

        if (result == DEPTHOFFIELDFX_RETURN_CODE.DEPTHOFFIELDFX_RETURN_CODE_SUCCESS)
        {
            // initalize
//            D3D11_BUFFER_DESC bdesc = { 0 };
//            bdesc.BindFlags         = D3D11_BIND_CONSTANT_BUFFER;
//            bdesc.Usage             = D3D11_USAGE_DYNAMIC;
//            bdesc.CPUAccessFlags    = D3D11_CPU_ACCESS_WRITE;
//            bdesc.ByteWidth         = sizeof(dofParams);
//            _ASSERT(sizeof(dofParams) % 16 == 0);
//            HRESULT hr = pDev->CreateBuffer(&bdesc, nullptr, &m_pDofParamsCB);

            m_pDofParamsCB = gl.glGenBuffer();
            gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, m_pDofParamsCB);
            gl.glBufferData(GLenum.GL_UNIFORM_BUFFER, DofParams.SIZE, GLenum.GL_STREAM_DRAW);
            gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, 0);

//            result     = convert_result(hr);
        }

        if (result == DEPTHOFFIELDFX_RETURN_CODE.DEPTHOFFIELDFX_RETURN_CODE_SUCCESS)
        {
//            D3D11_SAMPLER_DESC sdesc = {};
//            sdesc.AddressU           = D3D11_TEXTURE_ADDRESS_CLAMP;
//            sdesc.AddressV           = D3D11_TEXTURE_ADDRESS_CLAMP;
//            sdesc.AddressW           = D3D11_TEXTURE_ADDRESS_CLAMP;
//            sdesc.ComparisonFunc     = D3D11_COMPARISON_ALWAYS;
//            sdesc.Filter             = D3D11_FILTER_MIN_LINEAR_MAG_MIP_POINT;
//            HRESULT hr               = pDev->CreateSamplerState(&sdesc, &m_pPointSampler);
            SamplerDesc sdesc = new SamplerDesc();
            sdesc.wrapR = GLenum.GL_CLAMP_TO_EDGE;
            sdesc.wrapS = GLenum.GL_CLAMP_TO_EDGE;
            sdesc.wrapT = GLenum.GL_CLAMP_TO_EDGE;
            sdesc.magFilter = GLenum.GL_NEAREST;
            sdesc.minFilter = GLenum.GL_LINEAR_MIPMAP_NEAREST;
            m_pPointSampler = SamplerUtils.createSampler(sdesc);
//            result                   = convert_result(hr);
        }

        return result;
    }

    DEPTHOFFIELDFX_RETURN_CODE resize(DepthOfFieldFXDesc desc){
        int width      = desc.m_screenSize.x;
        int height     = desc.m_screenSize.y;
        m_padding      = desc.m_maxBlurRadius + 2;
        m_bufferWidth  = width + 2 * m_padding;
        m_bufferHeight = height + 2 * m_padding;

//        SAFE_RELEASE(&m_pIntermediateUAV);
//        SAFE_RELEASE(&m_pIntermediateTransposedUAV);
//        SAFE_RELEASE(&m_pIntermediateBuffer);
//        SAFE_RELEASE(&m_pIntermediateBufferTransposed);

        if(m_pIntermediateUAV != 0){
            gl.glDeleteTexture(m_pIntermediateUAV);
            m_pIntermediateUAV = 0;
        }

        if(m_pIntermediateTransposedUAV != 0){
            gl.glDeleteTexture(m_pIntermediateTransposedUAV);
            m_pIntermediateTransposedUAV = 0;
        }

        if(m_pIntermediateBufferTransposed != 0){
            gl.glDeleteBuffer(m_pIntermediateBufferTransposed);
            m_pIntermediateBufferTransposed = 0;
        }

        if(m_pIntermediateBuffer != 0){
            gl.glDeleteBuffer(m_pIntermediateBuffer);
            m_pIntermediateBuffer = 0;
        }

        final int elementCount = m_bufferWidth * m_bufferHeight;

//        D3D11_BUFFER_DESC bdesc   = { 0 };
//        bdesc.BindFlags           = D3D11_BIND_SHADER_RESOURCE | D3D11_BIND_UNORDERED_ACCESS;
//        bdesc.Usage               = D3D11_USAGE_DEFAULT;
//        bdesc.ByteWidth           = elementCount * sizeof(uint4);
//        bdesc.StructureByteStride = sizeof(uint4);
//        bdesc.MiscFlags           = D3D11_RESOURCE_MISC_BUFFER_STRUCTURED;
//        D3D11_UNORDERED_ACCESS_VIEW_DESC uavDesc;
//        memset(&uavDesc, 0, sizeof(uavDesc));
//        uavDesc.ViewDimension       = D3D11_UAV_DIMENSION_BUFFER;
//        uavDesc.Format              = DXGI_FORMAT_UNKNOWN;
//        uavDesc.Buffer.FirstElement = 0;
//        uavDesc.Buffer.NumElements  = elementCount;
//        // uavDesc.Buffer.Flags = D3D11_BUFFER_UAV_FLAG_RAW;
//
//        result = pDev->CreateBuffer(&bdesc, nullptr, &m_pIntermediateBuffer);
//        pDev->CreateUnorderedAccessView(m_pIntermediateBuffer, &uavDesc, &m_pIntermediateUAV);
//        result = pDev->CreateBuffer(&bdesc, nullptr, &m_pIntermediateBufferTransposed);
//        pDev->CreateUnorderedAccessView(m_pIntermediateBufferTransposed, &uavDesc, &m_pIntermediateTransposedUAV);

        {
            m_pIntermediateBuffer = gl.glGenBuffer();
            gl.glBindBuffer(GLenum.GL_TEXTURE_BUFFER, m_pIntermediateBuffer);
            gl.glBufferData(GLenum.GL_TEXTURE_BUFFER, Vector4i.SIZE * elementCount, GLenum.GL_DYNAMIC_COPY);
            gl.glBindBuffer(GLenum.GL_TEXTURE_BUFFER, 0);

            m_pIntermediateUAV = gl.glGenTexture();
            gl.glBindTexture(GLenum.GL_TEXTURE_BUFFER, m_pIntermediateUAV);
            gl.glTexBuffer(GLenum.GL_TEXTURE_BUFFER, GLenum.GL_RGBA32UI, m_pIntermediateBuffer);
            gl.glBindTexture(GLenum.GL_TEXTURE_BUFFER, 0);
        }

        {
            m_pIntermediateBufferTransposed = gl.glGenBuffer();
            gl.glBindBuffer(GLenum.GL_TEXTURE_BUFFER, m_pIntermediateBufferTransposed);
            gl.glBufferData(GLenum.GL_TEXTURE_BUFFER, Vector4i.SIZE * elementCount, GLenum.GL_DYNAMIC_COPY);
            gl.glBindBuffer(GLenum.GL_TEXTURE_BUFFER, 0);

            m_pIntermediateTransposedUAV = gl.glGenTexture();
            gl.glBindTexture(GLenum.GL_TEXTURE_BUFFER, m_pIntermediateTransposedUAV);
            gl.glTexBuffer(GLenum.GL_TEXTURE_BUFFER, GLenum.GL_RGBA32UI, m_pIntermediateBufferTransposed);
            gl.glBindTexture(GLenum.GL_TEXTURE_BUFFER, 0);
        }

        return DEPTHOFFIELDFX_RETURN_CODE.DEPTHOFFIELDFX_RETURN_CODE_SUCCESS;
    }

    private final void dispatch(int x, int y){
        gl.glDispatchCompute(x, y, 1);
        gl.glMemoryBarrier(GLenum.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
    }

    DEPTHOFFIELDFX_RETURN_CODE render(DepthOfFieldFXDesc desc){
//        HRESULT result = S_OK;


        if (((desc.m_screenSize.x + desc.m_maxBlurRadius * 2) * (desc.m_screenSize.y + desc.m_maxBlurRadius * 2)) > (m_bufferWidth * m_bufferHeight))
        {
            return DEPTHOFFIELDFX_RETURN_CODE_INVALID_SURFACE;
        }

//        ID3D11DeviceContext* pCtx = desc.m_pDeviceContext;

        int pUAVs[] = { m_pIntermediateUAV, 0, desc.m_pResultUAV.getTexture() };
        Texture2D  pSRVs[] = { desc.m_pColorSRV, desc.m_pCircleOfConfusionSRV };
        int              pCBs  = m_pDofParamsCB;

//        pCtx->CSSetSamplers(0, 1, &m_pPointSampler);

        update_constant_buffer(desc, m_bufferWidth, m_bufferHeight);
//        pCtx->CSSetConstantBuffers(0, ELEMENTS_OF(pCBs), pCBs);
//        pCtx->CSSetShaderResources(0, ELEMENTS_OF(pSRVs), pSRVs);
        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(desc.m_pColorSRV.getTarget(), desc.m_pColorSRV.getTexture());
        gl.glActiveTexture(GLenum.GL_TEXTURE1);
        gl.glBindTexture(desc.m_pCircleOfConfusionSRV.getTarget(), desc.m_pCircleOfConfusionSRV.getTexture());
        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, pCBs);


        // clear intermediate buffer
//        UINT clearValues[4] = { 0 }; TODO
//        pCtx->ClearUnorderedAccessViewUint(m_pIntermediateUAV, clearValues);


        // Fast Filter Setup
//        pCtx->CSSetShader(m_pFastFilterSetupCS, nullptr, 0);
        m_pFastFilterSetupCS.enable();

        int tgX = (desc.m_screenSize.x + 7) / 8;
        int tgY = (desc.m_screenSize.y + 7) / 8;

        update_constant_buffer(desc, m_bufferWidth, m_bufferHeight);
        Bind_UAVs(desc, m_pIntermediateUAV, 0, 0);
//        pCtx->Dispatch(tgX, tgY, 1);
        gl.glDispatchCompute(tgX, tgY, 1);
        gl.glMemoryBarrier(GLenum.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);

        // do Vertical integration
        {
            update_constant_buffer(desc, m_bufferWidth, m_bufferHeight);
//            pCtx->CSSetShader(m_pDoubleVerticalIntegrateCS, nullptr, 0);
            m_pDoubleVerticalIntegrateCS.enable();
            Bind_UAVs(desc, m_pIntermediateUAV, m_pIntermediateTransposedUAV, 0);
//            pCtx->Dispatch((m_bufferWidth + 63) / 64, 1, 1);
            gl.glDispatchCompute((m_bufferWidth + 63) / 64, 1, 1);
            gl.glMemoryBarrier(GLenum.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        }

        // do vertical integration by transposing the image and doing horizontal integration again
        {
            update_constant_buffer(desc, m_bufferHeight, m_bufferWidth);
            Bind_UAVs(desc, m_pIntermediateTransposedUAV, m_pIntermediateUAV, 0);
//            pCtx->Dispatch((m_bufferHeight + 63) / 64, 1, 1);
            gl.glDispatchCompute((m_bufferHeight + 63) / 64, 1, 1);
            gl.glMemoryBarrier(GLenum.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        }

        // debug: Copy from intermediate results
        update_constant_buffer(desc, m_bufferWidth, m_bufferHeight);

//        pCtx->CSSetShader(m_pReadFinalResultCS, nullptr, 0);
        m_pReadFinalResultCS.enable();
        Bind_UAVs(desc, m_pIntermediateUAV, 0, desc.m_pResultUAV.getTexture());
//        pCtx->Dispatch((desc.m_screenSize.x + 7) / 8, (desc.m_screenSize.y + 7) / 8, 1);
        gl.glDispatchCompute((m_bufferHeight + 63) / 64, 1, 1);
        gl.glMemoryBarrier(GLenum.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);

//        memset(pUAVs, 0, sizeof(pUAVs));
//        memset(pSRVs, 0, sizeof(pSRVs));
//        memset(pCBs, 0, sizeof(pCBs));
//        pCtx->CSSetUnorderedAccessViews(0, ELEMENTS_OF(pUAVs), pUAVs, nullptr);
//        pCtx->CSSetShaderResources(0, ELEMENTS_OF(pSRVs), pSRVs);
//        pCtx->CSSetConstantBuffers(0, ELEMENTS_OF(pCBs), pCBs);
        Bind_UAVs(desc,0,0,0);  // unbind resources.
        return DEPTHOFFIELDFX_RETURN_CODE_SUCCESS;
    }

    DEPTHOFFIELDFX_RETURN_CODE render_quarter_res(DepthOfFieldFXDesc desc){
//        HRESULT result = S_OK;

//        ID3D11DeviceContext* pCtx = desc.m_pDeviceContext;

//        ID3D11UnorderedAccessView* pUAVs[] = { m_pIntermediateUAV, nullptr, desc.m_pResultUAV };
//        ID3D11ShaderResourceView*  pSRVs[] = { desc.m_pColorSRV, desc.m_pCircleOfConfusionSRV };
//        ID3D11Buffer*              pCBs[]  = { m_pDofParamsCB };

//        pCtx->CSSetSamplers(0, 1, &m_pPointSampler);

        update_constant_buffer(desc, m_bufferWidth, m_bufferHeight);
//        pCtx->CSSetConstantBuffers(0, ELEMENTS_OF(pCBs), pCBs);
//        pCtx->CSSetShaderResources(0, ELEMENTS_OF(pSRVs), pSRVs);
        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(desc.m_pColorSRV.getTarget(), desc.m_pColorSRV.getTexture());
        gl.glActiveTexture(GLenum.GL_TEXTURE1);
        gl.glBindTexture(desc.m_pCircleOfConfusionSRV.getTarget(), desc.m_pCircleOfConfusionSRV.getTexture());
        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, m_pDofParamsCB);

        // clear intermediate buffer
//        UINT clearValues[4] = { 0 };
//        pCtx->ClearUnorderedAccessViewUint(m_pIntermediateUAV, clearValues);

        // Fast Filter Setup
//        pCtx->CSSetShader(m_pFastFilterSetupQuarterResCS, nullptr, 0);
        m_pFastFilterSetupQuarterResCS.enable();

        int tgX = ((desc.m_screenSize.x / 2) + 7) / 8;
        int tgY = ((desc.m_screenSize.y / 2) + 7) / 8;

        update_constant_buffer(desc, m_bufferWidth, m_bufferHeight);
        Bind_UAVs(desc, m_pIntermediateUAV, 0, 0);
//        pCtx->Dispatch(tgX, tgY, 1);
        gl.glDispatchCompute(tgX, tgY, 1);
        gl.glMemoryBarrier(GLenum.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);

        // do Vertical integration
        {
            update_constant_buffer(desc, m_bufferWidth, m_bufferHeight);
//            pCtx->CSSetShader(m_pDoubleVerticalIntegrateCS, nullptr, 0);
            m_pDoubleVerticalIntegrateCS.enable();
            Bind_UAVs(desc, m_pIntermediateUAV, m_pIntermediateTransposedUAV, 0);
//            pCtx->Dispatch((m_bufferWidth + 63) / 64, 1, 1);
            gl.glDispatchCompute((m_bufferWidth + 63) / 64, 1, 1);
            gl.glMemoryBarrier(GLenum.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        }

        // do vertical integration by transposing the image and doing horizontal integration again
        {
            update_constant_buffer(desc, m_bufferHeight, m_bufferWidth);
            Bind_UAVs(desc, m_pIntermediateTransposedUAV, m_pIntermediateUAV, 0);
//            pCtx->Dispatch((m_bufferHeight + 63) / 64, 1, 1);
            gl.glDispatchCompute((m_bufferHeight + 63) / 64, 1, 1);
            gl.glMemoryBarrier(GLenum.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        }

        // debug: Copy from intermediate results
        update_constant_buffer(desc, m_bufferWidth, m_bufferHeight);

//        pCtx->CSSetShader(m_pReadFinalResultCS, nullptr, 0);
        m_pReadFinalResultCS.enable();
        Bind_UAVs(desc, m_pIntermediateUAV, 0, desc.m_pResultUAV.getTexture());
//        pCtx->Dispatch((desc.m_screenSize.x + 7) / 8, (desc.m_screenSize.y + 7) / 8, 1);
        gl.glDispatchCompute((desc.m_screenSize.x + 7) / 8, (desc.m_screenSize.y + 7) / 8, 1);
        gl.glMemoryBarrier(GLenum.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);

//        memset(pUAVs, 0, sizeof(pUAVs));
//        memset(pSRVs, 0, sizeof(pSRVs));
//        memset(pCBs, 0, sizeof(pCBs));
//        pCtx->CSSetUnorderedAccessViews(0, ELEMENTS_OF(pUAVs), pUAVs, nullptr);
//        pCtx->CSSetShaderResources(0, ELEMENTS_OF(pSRVs), pSRVs);
//        pCtx->CSSetConstantBuffers(0, ELEMENTS_OF(pCBs), pCBs);

        Bind_UAVs(desc,0,0,0);

        return DEPTHOFFIELDFX_RETURN_CODE_SUCCESS;
    }

    DEPTHOFFIELDFX_RETURN_CODE render_box(DepthOfFieldFXDesc desc){
//        HRESULT result = S_OK;

//        ID3D11DeviceContext* pCtx = desc.m_pDeviceContext;

//        ID3D11UnorderedAccessView* pUAVs[] = { m_pIntermediateUAV, nullptr, desc.m_pResultUAV };
//        ID3D11ShaderResourceView*  pSRVs[] = { desc.m_pColorSRV, desc.m_pCircleOfConfusionSRV };
//        ID3D11Buffer*              pCBs[]  = { m_pDofParamsCB };

//        pCtx->CSSetSamplers(0, 1, &m_pPointSampler);

        update_constant_buffer(desc, m_bufferWidth, m_bufferHeight);
//        pCtx->CSSetConstantBuffers(0, ELEMENTS_OF(pCBs), pCBs);
//        pCtx->CSSetShaderResources(0, ELEMENTS_OF(pSRVs), pSRVs);
        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(desc.m_pColorSRV.getTarget(), desc.m_pColorSRV.getTexture());
        gl.glActiveTexture(GLenum.GL_TEXTURE1);
        gl.glBindTexture(desc.m_pCircleOfConfusionSRV.getTarget(), desc.m_pCircleOfConfusionSRV.getTexture());
        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, m_pDofParamsCB);

        // clear intermediate buffer
//        UINT clearValues[4] = { 0 };
//        pCtx->ClearUnorderedAccessViewUint(m_pIntermediateUAV, clearValues);

        // Fast Filter Setup
//        pCtx->CSSetShader(m_pBoxFastFilterSetupCS, nullptr, 0);
        m_pBoxFastFilterSetupCS.enable();

        int tgX = (desc.m_screenSize.x + 7) / 8;
        int tgY = (desc.m_screenSize.y + 7) / 8;

        update_constant_buffer(desc, m_bufferWidth, m_bufferHeight);
        Bind_UAVs(desc, m_pIntermediateUAV, 0, 0);
//        pCtx->Dispatch(tgX, tgY, 1);
        dispatch(tgX, tgY);

        // do Vertical integration
        {
            update_constant_buffer(desc, m_bufferWidth, m_bufferHeight);
//            pCtx->CSSetShader(m_pVerticalIntegrateCS, nullptr, 0);
            m_pVerticalIntegrateCS.enable();
            Bind_UAVs(desc, m_pIntermediateUAV, m_pIntermediateTransposedUAV, 0);
//            pCtx->Dispatch((m_bufferWidth + 63) / 64, 1, 1);
            dispatch((m_bufferWidth + 63) / 64, 1);
        }

        // do vertical integration by transposing the image and doing horizontal integration again
        {
            update_constant_buffer(desc, m_bufferHeight, m_bufferWidth);
            Bind_UAVs(desc, m_pIntermediateTransposedUAV, m_pIntermediateUAV, 0);
//            pCtx->Dispatch((m_bufferHeight + 63) / 64, 1, 1);
            dispatch((m_bufferHeight + 63) / 64, 1);
        }

        // debug: Copy from intermediate results
        update_constant_buffer(desc, m_bufferWidth, m_bufferHeight);

//        pCtx->CSSetShader(m_pReadFinalResultCS, nullptr, 0);
        m_pReadFinalResultCS.enable();
        Bind_UAVs(desc, m_pIntermediateUAV, 0, desc.m_pResultUAV.getTexture());
//        pCtx->Dispatch((desc.m_screenSize.x + 7) / 8, (desc.m_screenSize.y + 7) / 8, 1);
        dispatch((desc.m_screenSize.x + 7) / 8, (desc.m_screenSize.y + 7) / 8);

//        memset(pUAVs, 0, sizeof(pUAVs));
//        memset(pSRVs, 0, sizeof(pSRVs));
//        memset(pCBs, 0, sizeof(pCBs));
//        pCtx->CSSetUnorderedAccessViews(0, ELEMENTS_OF(pUAVs), pUAVs, nullptr);
//        pCtx->CSSetShaderResources(0, ELEMENTS_OF(pSRVs), pSRVs);
//        pCtx->CSSetConstantBuffers(0, ELEMENTS_OF(pCBs), pCBs);
        Bind_UAVs(null,0,0,0);

        return DEPTHOFFIELDFX_RETURN_CODE_SUCCESS;
    }

    DEPTHOFFIELDFX_RETURN_CODE release(){
        if(m_pIntermediateUAV != 0){
            gl.glDeleteTexture(m_pIntermediateUAV);
            m_pIntermediateUAV = 0;
        }

        if(m_pIntermediateTransposedUAV != 0){
            gl.glDeleteTexture(m_pIntermediateTransposedUAV);
            m_pIntermediateTransposedUAV = 0;
        }

        if(m_pIntermediateBufferTransposed != 0){
            gl.glDeleteBuffer(m_pIntermediateBufferTransposed);
            m_pIntermediateBufferTransposed = 0;
        }

        if(m_pIntermediateBuffer != 0){
            gl.glDeleteBuffer(m_pIntermediateBuffer);
            m_pIntermediateBuffer = 0;
        }

        if(m_pDofParamsCB != 0){
            gl.glDeleteBuffer(m_pDofParamsCB);
            m_pDofParamsCB = 0;
        }

        if(m_pFastFilterSetupCS != null){
            m_pFastFilterSetupCS.dispose();
            m_pFastFilterSetupCS = null;
        }

        if(m_pFastFilterSetupQuarterResCS != null){
            m_pFastFilterSetupQuarterResCS.dispose();
            m_pFastFilterSetupQuarterResCS = null;
        }

        if(m_pBoxFastFilterSetupCS != null){
            m_pBoxFastFilterSetupCS.dispose();
            m_pBoxFastFilterSetupCS = null;
        }

        if(m_pReadFinalResultCS != null){
            m_pReadFinalResultCS.dispose();
            m_pReadFinalResultCS = null;
        }

        if(m_pVerticalIntegrateCS != null){
            m_pVerticalIntegrateCS.dispose();
            m_pVerticalIntegrateCS = null;
        }

        if(m_pDoubleVerticalIntegrateCS != null){
            m_pDoubleVerticalIntegrateCS.dispose();
            m_pDoubleVerticalIntegrateCS = null;
        }

        return DEPTHOFFIELDFX_RETURN_CODE_SUCCESS;
    }

    private static final GLSLProgram create(String filename, boolean doubled){
        try {
            CharSequence computeSrc = ShaderLoader.loadShaderFile("amdfx/DepthOfFieldFX/shaders/" + filename, false);
            ShaderSourceItem cs_item = new ShaderSourceItem(computeSrc, ShaderType.COMPUTE);
            cs_item.macros = CommonUtil.toArray(new Macro("OUT_FORMAT", "rgba8"), (doubled ? new Macro("DOUBLE_INTEGRATE", 1) : null ));
            return GLSLProgram.createFromShaderItems(cs_item);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    DEPTHOFFIELDFX_RETURN_CODE create_shaders(){
        m_pFastFilterSetupCS = create("FastFilterSetup.comp", false);
        m_pFastFilterSetupQuarterResCS = create("QuarterResFastFilterSetup.comp", false);
        m_pBoxFastFilterSetupCS = create("BoxFastFilterSetup.comp", false);
        m_pReadFinalResultCS = create("ReadFinalResult.comp", false);
        m_pVerticalIntegrateCS = create("VerticalIntegrate.comp", false);
        m_pDoubleVerticalIntegrateCS = create("VerticalIntegrate.comp", true);

        return DEPTHOFFIELDFX_RETURN_CODE_SUCCESS;
    }

//    static DEPTHOFFIELDFX_RETURN_CODE convert_result(HRESULT hResult);

    boolean update_constant_buffer(DepthOfFieldFXDesc desc, int padWidth, int padHeight){
//        ID3D11DeviceContext*     pCtx = desc.m_pDeviceContext;
//        D3D11_MAPPED_SUBRESOURCE data;
//        pCtx->Map(m_pDofParamsCB, 0, D3D11_MAP_WRITE_DISCARD, 0, &data);

        DofParams pParams              = m_DofParams;
        pParams.bufferResolution.x    = padWidth;
        pParams.bufferResolution.y    = padHeight;
        pParams.sourceResolution.x    = desc.m_screenSize.x;
        pParams.sourceResolution.y    = desc.m_screenSize.y;
        pParams.invSourceResolution.x = 1.0f / (desc.m_screenSize.x);
        pParams.invSourceResolution.y = 1.0f / (desc.m_screenSize.y);
//        pParams.padding               = desc.m_pOpaque.m_padding;
        pParams.scale_factor          = (float)(1 << desc.m_scaleFactor);
//        memcpy(pParams->bartlettData, s_bartlettData, sizeof(s_bartlettData));
//        memcpy(pParams->boxBartlettData, s_boxBartlettData, sizeof(s_boxBartlettData));

        ByteBuffer data = CacheBuffer.getCachedByteBuffer(DofParams.SIZE);
        pParams.store(data);
        data.flip();
//        pCtx->Unmap(m_pDofParamsCB, 0);
        gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, m_pDofParamsCB);
        gl.glBufferSubData(GLenum.GL_UNIFORM_BUFFER, 0, data);
        gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, 0);

        return true;
    }

    void Bind_UAVs(DepthOfFieldFXDesc desc, int pUAV0, int pUAV1, int pUAV2){
        gl.glBindImageTexture(0, pUAV0, 0, false, 0, GLenum.GL_READ_WRITE, GLenum.GL_RGBA32UI);
        gl.glBindImageTexture(1, pUAV1, 0, false, 0, GLenum.GL_READ_WRITE, GLenum.GL_RGBA32UI);
        gl.glBindImageTexture(2, pUAV2, 0, false, 0, GLenum.GL_READ_WRITE, GLenum.GL_RGBA32UI);
    }
}
