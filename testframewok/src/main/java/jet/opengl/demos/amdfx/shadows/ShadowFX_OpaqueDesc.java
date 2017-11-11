package jet.opengl.demos.amdfx.shadows;

import org.lwjgl.util.vector.Vector4i;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.SamplerDesc;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.util.CommonUtil;

final class ShadowFX_OpaqueDesc implements ShadowFX_Constants{

	ShadowsData                                  m_ShadowsData;

//  ID3D11Buffer*                                m_cbShadowsData;
//  ID3D11VertexShader*                          m_vsFullscreen;

	RenderTechnique[][][][][][]                m_psShadowT2D = new RenderTechnique[SHADOWFX_FILTERING_COUNT][SHADOWFX_EXECUTION_COUNT][SHADOWFX_TEXTURE_FETCH_COUNT][SHADOWFX_TAP_TYPE_COUNT][SHADOWFX_NORMAL_OPTION_COUNT][SHADOWFX_FILTER_SIZE_COUNT];
	RenderTechnique[][][][][][]                m_psShadowT2DA = new RenderTechnique[SHADOWFX_FILTERING_COUNT][SHADOWFX_EXECUTION_COUNT][SHADOWFX_TEXTURE_FETCH_COUNT][SHADOWFX_TAP_TYPE_COUNT][SHADOWFX_NORMAL_OPTION_COUNT][SHADOWFX_FILTER_SIZE_COUNT];
  //ID3D11PixelShader*                         m_psShadowTC[SHADOWFX_FILTERING_COUNT][SHADOWFX_TEXTURE_FETCH_COUNT][SHADOWFX_TAP_TYPE_COUNT][SHADOWFX_NORMAL_OPTION_COUNT][SHADOWFX_FILTER_SIZE_COUNT];

  //ID3D11ComputeShader*                       m_csShadowT2D[SHADOWFX_FILTERING_COUNT][SHADOWFX_TEXTURE_FETCH_COUNT][SHADOWFX_TAP_TYPE_COUNT][SHADOWFX_NORMAL_OPTION_COUNT][SHADOWFX_FILTER_SIZE_COUNT];
  //ID3D11ComputeShader*                       m_csShadowT2DA[SHADOWFX_FILTERING_COUNT][SHADOWFX_TEXTURE_FETCH_COUNT][SHADOWFX_TAP_TYPE_COUNT][SHADOWFX_NORMAL_OPTION_COUNT][SHADOWFX_FILTER_SIZE_COUNT];
  //ID3D11ComputeShader*                       m_csShadowTC[SHADOWFX_FILTERING_COUNT][SHADOWFX_TEXTURE_FETCH_COUNT][SHADOWFX_TAP_TYPE_COUNT][SHADOWFX_NORMAL_OPTION_COUNT][SHADOWFX_FILTER_SIZE_COUNT];

	RenderTechnique[][]                        m_psShadowPointDebugT2D = new RenderTechnique[SHADOWFX_EXECUTION_COUNT][SHADOWFX_NORMAL_OPTION_COUNT];
	RenderTechnique[][]                        m_psShadowPointDebugT2DA = new RenderTechnique[SHADOWFX_EXECUTION_COUNT][SHADOWFX_NORMAL_OPTION_COUNT];
  //ID3D11PixelShader*                         m_psShadowPointDebugTC[SHADOWFX_NORMAL_OPTION_COUNT];

    int                          			   m_ssPointClamp;
    int                          			   m_ssLinearClamp;
    int                          			   m_scsPointBorder;
    int                          			   m_scsLinearBorder;
    int                          			   m_scsPointClamp;
    int                          			   m_scsLinearClamp;

//  ID3D11RasterizerState*                       m_rsNoCulling;
//  ID3D11DepthStencilState*                     m_dssEqualToRef;
//  ID3D11BlendState*                            m_bsOutputChannel[SHADOWFX_OUTPUT_CHANNEL_COUNT];
    
    int 									  m_framebuffer;
    private GLFuncProvider 					  gl;

  ShadowFX_OpaqueDesc(){}

  SHADOWFX_RETURN_CODE                         cbInitialize(ShadowFX_Desc desc){
	  SamplerDesc ssDesc = new SamplerDesc();
	  ssDesc.magFilter = GLenum.GL_NEAREST;
	  ssDesc.minFilter = GLenum.GL_NEAREST;
	  
	  m_ssPointClamp = SamplerUtils.createSampler(ssDesc);
	  
	  ssDesc.magFilter = GLenum.GL_LINEAR;
	  ssDesc.minFilter = GLenum.GL_LINEAR;
	  m_ssLinearClamp = SamplerUtils.createSampler(ssDesc);
	  
	  ssDesc.magFilter = GLenum.GL_NEAREST;
	  ssDesc.minFilter = GLenum.GL_NEAREST;
	  ssDesc.compareFunc = GLenum.GL_LEQUAL;
	  ssDesc.compareMode = GLenum.GL_COMPARE_REF_TO_TEXTURE;
	  m_scsPointClamp = SamplerUtils.createSampler(ssDesc);
	  
	  ssDesc.magFilter = GLenum.GL_LINEAR;
	  ssDesc.minFilter = GLenum.GL_LINEAR;
	  ssDesc.compareFunc = GLenum.GL_LEQUAL;
	  ssDesc.compareMode = GLenum.GL_COMPARE_REF_TO_TEXTURE;
	  m_scsLinearClamp = SamplerUtils.createSampler(ssDesc);
	  
	  ssDesc.magFilter = GLenum.GL_NEAREST;
	  ssDesc.minFilter = GLenum.GL_NEAREST;
	  ssDesc.compareFunc = GLenum.GL_LEQUAL;
	  ssDesc.compareMode = GLenum.GL_COMPARE_REF_TO_TEXTURE;
	  ssDesc.wrapR = GLenum.GL_CLAMP_TO_BORDER;
	  ssDesc.wrapS = GLenum.GL_CLAMP_TO_BORDER;
	  ssDesc.wrapT = GLenum.GL_CLAMP_TO_BORDER;
	  ssDesc.borderColor = 0xFFFFFFFF;  // White
	  m_scsPointBorder = SamplerUtils.createSampler(ssDesc);
	  
	  ssDesc.magFilter = GLenum.GL_LINEAR;
	  ssDesc.minFilter = GLenum.GL_LINEAR;
	  m_scsLinearBorder = SamplerUtils.createSampler(ssDesc);
	  
	  {
		  // Blend and cull face state setting...
//		  CD3D11_BLEND_DESC bsDesc(d3d11Default);
//		    m_bsOutputChannel[0] = NULL;
//		    for (int blend = SHADOWFX_OUTPUT_CHANNEL_R; blend < SHADOWFX_OUTPUT_CHANNEL_COUNT; blend++)
//		    {
//		        bsDesc.RenderTarget[0].RenderTargetWriteMask = (UINT8)blend;
//		        hr = desc.m_pDevice->CreateBlendState(&bsDesc, &m_bsOutputChannel[blend]);
//		        if (hr != S_OK) return SHADOWFX_RETURN_CODE_D3D11_CALL_FAILED;
//		    }
//
//		    CD3D11_RASTERIZER_DESC rsDesc(d3d11Default);
//		    rsDesc.CullMode = D3D11_CULL_NONE;
//		    hr = desc.m_pDevice->CreateRasterizerState(&rsDesc, &m_rsNoCulling);
//		    if (hr != S_OK) return SHADOWFX_RETURN_CODE_D3D11_CALL_FAILED;
	  }
	  
	  m_framebuffer = gl.glGenFramebuffer();
	  return SHADOWFX_RETURN_CODE.SHADOWFX_RETURN_CODE_SUCCESS;
  }
  
  SHADOWFX_RETURN_CODE                         createShaders(ShadowFX_Desc desc){
	  releaseShaders();
	  
	  return SHADOWFX_RETURN_CODE.SHADOWFX_RETURN_CODE_SUCCESS;
  }

  SHADOWFX_RETURN_CODE                         render(ShadowFX_Desc desc){
	  
	  if (desc == null || desc.m_DepthSize.x == 0 || 
		        		  desc.m_DepthSize.y == 0)
	    {
	        return SHADOWFX_RETURN_CODE.SHADOWFX_RETURN_CODE_INVALID_ARGUMENT;
	    }
	  
	  Vector4i FullscreenVP = new Vector4i(0, 0, (int)desc.m_DepthSize.x, (int)desc.m_DepthSize.y);
	  
	  m_ShadowsData.m_ActiveLightCount = desc.m_ActiveLightCount;
//	  memcpy(&m_ShadowsData.m_Size, &desc.m_DepthSize, sizeof(m_ShadowsData.m_Size));
//	  memcpy(&m_ShadowsData.m_Viewer, &desc.m_Viewer, sizeof(m_ShadowsData.m_Viewer));
	  m_ShadowsData.m_Size.set(desc.m_DepthSize);
	  m_ShadowsData.m_Viewer.set(desc.m_Viewer);
	  m_ShadowsData.m_SizeInv.x = 1.0f / desc.m_DepthSize.x;
	  m_ShadowsData.m_SizeInv.y = 1.0f / desc.m_DepthSize.y;
	  
	  for (int i = 0; i < desc.m_ActiveLightCount; i++)
	    {
//	        float2 shadowSizeInv ={1.0f / m_ShadowsData.m_Light[i].m_Size.x, 1.0f / m_ShadowsData.m_Light[i].m_Size.y};

//	        memcpy(&m_ShadowsData.m_Light[i].m_Camera, &desc.m_Light[i], sizeof(m_ShadowsData.m_Light[i].m_Camera));
		  m_ShadowsData.m_Light[i].m_Camera.set(desc.m_Light[i]);
//	        memcpy(&m_ShadowsData.m_Light[i].m_Size, &desc.m_ShadowSize[i], sizeof(m_ShadowsData.m_Light[i].m_Size));
		  m_ShadowsData.m_Light[i].m_Size.set(desc.m_ShadowSize[i]);
//	        memcpy(&m_ShadowsData.m_Light[i].m_SizeInv, &shadowSizeInv, sizeof(shadowSizeInv));
		  m_ShadowsData.m_Light[i].m_SizeInv.set(1.0f / m_ShadowsData.m_Light[i].m_Size.x, 1.0f / m_ShadowsData.m_Light[i].m_Size.y);
//	        memcpy(&m_ShadowsData.m_Light[i].m_Region, &desc.m_ShadowRegion[i], sizeof(m_ShadowsData.m_Light[i].m_Region));
		  m_ShadowsData.m_Light[i].m_Region.set(desc.m_ShadowRegion[i]);
//	        memcpy(&m_ShadowsData.m_Light[i].m_SunArea, &desc.m_SunArea[i], sizeof(m_ShadowsData.m_Light[i].m_SunArea));
		  m_ShadowsData.m_Light[i].m_SunArea = desc.m_SunArea[i];
//	        memcpy(&m_ShadowsData.m_Light[i].m_DepthTestOffset, &desc.m_DepthTestOffset[i], sizeof(m_ShadowsData.m_Light[i].m_DepthTestOffset));
		  m_ShadowsData.m_Light[i].m_DepthTestOffset = desc.m_DepthTestOffset[i];
//		  memcpy(&m_ShadowsData.m_Light[i].m_NormalOffsetScale, &desc.m_NormalOffsetScale[i], sizeof(m_ShadowsData.m_Light[i].m_NormalOffsetScale));
		  m_ShadowsData.m_Light[i].m_NormalOffsetScale = desc.m_NormalOffsetScale[i];
	      m_ShadowsData.m_Light[i].m_ArraySlice = desc.m_ArraySlice[i];
	      m_ShadowsData.m_Light[i].m_Weight.x = desc.m_Weight[i];
	    }
	  
//	  	D3D11_MAPPED_SUBRESOURCE MappedResource;
//	    desc.m_pContext->Map(m_cbShadowsData, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedResource);
//	    memcpy(MappedResource.pData, &m_ShadowsData, sizeof(m_ShadowsData));
//	    desc.m_pContext->Unmap(m_cbShadowsData, 0);
	  // TODO Don't forget to set the uniforms	
	  
	    int filterSize = 0;

	    switch (desc.m_FilterSize)
	    {
	    case SHADOWFX_FILTER_SIZE_7:  filterSize = 0; break;
	    case SHADOWFX_FILTER_SIZE_9:  filterSize = 1; break;
	    case SHADOWFX_FILTER_SIZE_11: filterSize = 2; break;
	    case SHADOWFX_FILTER_SIZE_13: filterSize = 3; break;
	    case SHADOWFX_FILTER_SIZE_15: filterSize = 4; break;
	    }

	    RenderTechnique psSelect = null;

	    switch (desc.m_TextureType)
	    {
	    case SHADOWFX_TEXTURE_2D:
	    {
	        if (desc.m_Filtering != SHADOWFX_FILTERING_DEBUG_POINT)
	            psSelect = m_psShadowT2D[desc.m_Filtering][desc.m_Execution][desc.m_TextureFetch][desc.m_TapType][desc.m_NormalOption][filterSize];
	        else
	            psSelect = m_psShadowPointDebugT2D[desc.m_Execution][desc.m_NormalOption];
	        break;
	    }

	    case SHADOWFX_TEXTURE_2D_ARRAY:
	    {
	        if (desc.m_Filtering != SHADOWFX_FILTERING_DEBUG_POINT)
	            psSelect = m_psShadowT2DA[desc.m_Filtering][desc.m_Execution][desc.m_TextureFetch][desc.m_TapType][desc.m_NormalOption][filterSize];
	        else
	            psSelect = m_psShadowPointDebugT2DA[desc.m_Execution][desc.m_NormalOption];
	        break;
	    }
	    }
//	    ID3D11Buffer* cb[] ={m_cbShadowsData};
//	    ID3D11SamplerState * ss[] ={m_ssPointClamp, m_ssLinearClamp, m_scsPointClamp, m_scsLinearClamp};
//	    ID3D11RenderTargetView* rtv[] ={desc.m_pOutputRTV};
//
//	    ID3D11BlendState * bsSelect = desc.m_pOutputBS != NULL ? desc.m_pOutputBS : m_bsOutputChannel[desc.m_OutputChannels];
//
//	    ID3D11ShaderResourceView* srv[] ={desc.m_pDepthSRV, desc.m_pNormalSRV, desc.m_pShadowSRV};
	    
	    // TODO Binding textures and samplers

	    /*HRESULT hr = AMD::*/RenderFullscreenPass(/*desc.m_pContext,*/
	        FullscreenVP, 
	        null, 
	        psSelect,
	        null, /*cb,*/ 1,
	        0, 0, // ss, AMD_ARRAY_SIZE(ss),
	        null, 0, // srv, AMD_ARRAY_SIZE(srv),
	        CommonUtil.toArray(desc.m_pOutputRTV), 1,
	        null, 0,
	        null, 0, 0,
	        desc.m_pOutputDSV,
	        0/*desc.m_pOutputDSS*/, desc.m_ReferenceDSS,
	        0/*bsSelect*/,
	        0/*m_rsNoCulling*/);

//	    return hr == S_OK ? SHADOWFX_RETURN_CODE_SUCCESS : SHADOWFX_RETURN_CODE_FAIL;
	  
	  return SHADOWFX_RETURN_CODE.SHADOWFX_RETURN_CODE_SUCCESS;
  }
  
  void RenderFullscreenPass(
//	        ID3D11DeviceContext*        pDeviceContext,
		  Vector4i              Viewport,
		  RenderTechnique         pVS,
		  RenderTechnique          pPS,
		  Vector4i                 pScissor, int uNumSR,
		  int              ppCB, int uNumCBs,
		  int[]        ppSamplers, int uNumSamplers,
		  Texture2D[]  ppSRVs, int uNumSRVs,
		  Texture2D[]    ppRTVs, int uNumRTVs,
		  Texture2D[] ppUAVs, int uStartUAV, int uNumUAVs,
		  Texture2D     pDSV,
		  int    pOutputDSS, int uStencilRef,
		  int    pOutputBS,
		  int    pOutputRS)
	    {
	        RenderFullscreenInstancedPass(//pDeviceContext,
	                                             Viewport,
	                                             pVS, null, pPS,
	                                             pScissor, uNumSR,
	                                             ppCB, uNumCBs,
	                                             ppSamplers, uNumSamplers,
	                                             ppSRVs, uNumSRVs,
	                                             ppRTVs, uNumRTVs,
	                                             ppUAVs, uStartUAV, uNumUAVs,
	                                             pDSV, pOutputDSS, uStencilRef,
	                                             pOutputBS, pOutputRS, 1);
	    }

	    void RenderFullscreenInstancedPass(
//	        ID3D11DeviceContext*        pDeviceContext,
	        Vector4i              Viewport,
	        RenderTechnique         pVS,
	        RenderTechnique       pGS,
	        RenderTechnique          pPS,
	        Vector4i                 pScissor,   int uNumSR,
	        int              ppCB,       		 int uNumCBs,
	        int[]        ppSamplers, 			 int uNumSamplers,
	        Texture2D[]  ppSRVs,     			 int uNumSRVs,
	        Texture2D[]  ppRTVs,     			 int uNumRTVs,
	        Texture2D[]  ppUAVs,    		     int uStartUAV,  int uNumUAVs,
	        Texture2D    pDSV,
	        int    pOutputDSS, 					 int uStencilRef,
	        int    pOutputBS,
	        int    pOutputRS,
	        int                instanceCount)
	    {
	        float white[] = {1.0f, 1.0f, 1.0f, 1.0f};
//	        ID3D11ShaderResourceView*  pNullSRV[8]    = { NULL };
//	        ID3D11RenderTargetView*    pNullRTV[8]    = { NULL };
//	        ID3D11UnorderedAccessView* pNullUAV[8]    = { NULL };
//	        ID3D11Buffer*              pNullBuffer[8] = { NULL };
//	        uint NullStride[8] = { 0 };
//	        uint NullOffset[8] = { 0 };

	        if ((/*pDeviceContext == NULL ||*/ (pVS == null && pPS == null) || (ppRTVs == null && pDSV == null && ppUAVs == null)))
	        {
//	            AMD_OUTPUT_DEBUG_STRING("Invalid pointer argument in function %s\n", AMD_FUNCTION_NAME);
//	            return E_POINTER;
	        	// TODO
	        }

			gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, m_framebuffer);
	        dssEqualToRef(0xFF);

//	        pDeviceContext->OMSetDepthStencilState( pOutputDSS, uStencilRef );
//	        if (ppUAVs == NULL)
//	        {
//	            pDeviceContext->OMSetRenderTargets( uNumRTVs, (ID3D11RenderTargetView*const*)ppRTVs, pDSV );
//	        }
//	        else
//	        {
//	            pDeviceContext->OMSetRenderTargetsAndUnorderedAccessViews( uNumRTVs, (ID3D11RenderTargetView*const*)ppRTVs, pDSV, uStartUAV, uNumUAVs, ppUAVs, NULL);
//	        }
//	        pDeviceContext->OMSetBlendState(pOutputBS, white, 0xFFFFFFFF);
			gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT0, ppRTVs[0].getTarget(), ppRTVs[0].getTarget(), 0);

//	        pDeviceContext->RSSetViewports( 1, &Viewport );
//	        pDeviceContext->RSSetScissorRects(uNumSR, pScissor);
//	        pDeviceContext->RSSetState( pOutputRS );
			gl.glViewport(Viewport.x, Viewport.y, Viewport.z, Viewport.w);

//	        pDeviceContext->PSSetConstantBuffers( 0, uNumCBs, ppCB);
//	        pDeviceContext->PSSetShaderResources( 0, uNumSRVs, ppSRVs );
//	        pDeviceContext->PSSetSamplers( 0, uNumSamplers, ppSamplers );
//
//	        pDeviceContext->IASetInputLayout( NULL );
//	        pDeviceContext->IASetVertexBuffers( 0, AMD_ARRAY_SIZE(pNullBuffer), pNullBuffer, NullStride, NullOffset );
//	        pDeviceContext->IASetPrimitiveTopology( D3D11_PRIMITIVE_TOPOLOGY_TRIANGLELIST );
//
//	        pDeviceContext->VSSetShader( pVS, NULL, 0 );
//	        pDeviceContext->GSSetShader( pGS, NULL, 0 );
//	        pDeviceContext->PSSetShader(pPS, NULL, 0);
//
//	        pDeviceContext->Draw( 3 * instanceCount, 0 );
	        //TODO
	        pPS.enable();
			gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);

	        // Unbind RTVs and SRVs back to NULL (otherwise D3D will throw warnings)
//	        if (ppUAVs == NULL)
//	        {
//	            pDeviceContext->OMSetRenderTargets( AMD_ARRAY_SIZE(pNullRTV), pNullRTV, NULL );
//	        }
//	        else
//	        {
//	            pDeviceContext->OMSetRenderTargetsAndUnorderedAccessViews( uNumRTVs, pNullRTV, NULL, uStartUAV, uNumUAVs, pNullUAV, NULL);
//	        }
//
//	        pDeviceContext->PSSetShaderResources( 0, AMD_ARRAY_SIZE(pNullSRV), pNullSRV );
//
//	        return S_OK;
	    }

  void                                         release(){
	  
  }
  
  void                                         releaseShaders(){
	  
  }
  
  private void dssEqualToRef(int ref){
	  gl.glDisable(GLenum.GL_DEPTH_TEST);
	  gl.glEnable(GLenum.GL_STENCIL_TEST);
	  gl.glStencilOp(GLenum.GL_ZERO, GLenum.GL_ZERO, GLenum.GL_ZERO);
	  gl.glStencilFunc(GLenum.GL_EQUAL, ref, 0xFF);
  }
}
