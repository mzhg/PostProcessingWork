package jet.opengl.demos.nvidia.hbaoplus;

import org.lwjgl.util.vector.Matrix4f;

import jet.opengl.postprocessing.texture.Texture2D;
//---------------------------------------------------------------------------------------------------
// Input depth data.
//
// Requirements:
// 	* View-space depths (linear) are required to be non-multisample.
// 	* Hardware depths (non-linear) can be multisample or non-multisample.
// 	* The projection matrix must have the following form, with |P23| == 1.f:
//    	{ P00, 0.f, 0.f, 0.f }
//    	{ 0.f, P11, 0.f, 0.f }
//    	{ P20, P21, P22, P23 }
//    	{ 0.f, 0.f, P32, 0.f }
//
// Remarks:
// 	* MetersToViewSpaceUnits is used to convert the AO radius parameter from meters to view-space units,
//    as well as to convert the blur sharpness parameter from inverse meters to inverse view-space units.
//---------------------------------------------------------------------------------------------------
public class GFSDK_SSAO_InputDepthData {

	/** HARDWARE_DEPTHS, HARDWARE_DEPTHS_SUB_RANGE or VIEW_DEPTHS */
	public GFSDK_SSAO_DepthTextureType depthTextureType = GFSDK_SSAO_DepthTextureType.GFSDK_SSAO_HARDWARE_DEPTHS;
	
	/** 4x4 perspective matrix from the depth generation pass */
	public final Matrix4f projectionMatrix = new Matrix4f();
	
	/** DistanceInViewSpaceUnits = MetersToViewSpaceUnits * DistanceInMeters */
	public float metersToViewSpaceUnits = 1.0f;
	
	/** Viewport from the depth generation pass */
	public final GFSDK_SSAO_InputViewport viewport = new GFSDK_SSAO_InputViewport();
	
	/** Full-resolution depth texture */
	public Texture2D fullResDepthTexture;
}
