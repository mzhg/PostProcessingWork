package com.nvidia.developer.opengl.demos.amdfx.dof;

/**
 * Created by mazhen'gui on 2017/6/24.
 */

public final class DepthOfFieldFX {

//    AMD_DEPTHOFFIELDFX_DLL_API DEPTHOFFIELDFX_RETURN_CODE DepthOfFieldFX_GetVersion(uint* major, uint* minor, uint* patch);
   public static DEPTHOFFIELDFX_RETURN_CODE DepthOfFieldFX_Initialize(DepthOfFieldFXDesc desc){
      desc.m_pOpaque = new DepthOfFieldFXOpaqueDesc();
      return  desc.m_pOpaque.initalize();
   }

   public static DEPTHOFFIELDFX_RETURN_CODE DepthOfFieldFX_Resize(DepthOfFieldFXDesc desc){
      return  desc.m_pOpaque.resize(desc);
   }

   public static DEPTHOFFIELDFX_RETURN_CODE DepthOfFieldFX_Render(DepthOfFieldFXDesc desc){
      return  desc.m_pOpaque.render(desc);
   }

   public static DEPTHOFFIELDFX_RETURN_CODE DepthOfFieldFX_RenderQuarterRes(DepthOfFieldFXDesc desc){
      return  desc.m_pOpaque.render_quarter_res(desc);
   }

   public static DEPTHOFFIELDFX_RETURN_CODE DepthOfFieldFX_RenderBox(DepthOfFieldFXDesc desc){
      return  desc.m_pOpaque.render_box(desc);
   }

   public static DEPTHOFFIELDFX_RETURN_CODE DepthOfFieldFX_Release(DepthOfFieldFXDesc desc){
      return  desc.m_pOpaque.release();
   }

    private DepthOfFieldFX(){}
}
