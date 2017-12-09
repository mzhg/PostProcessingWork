package jet.opengl.demos.nvidia.hbaoplus;

import org.lwjgl.util.vector.Matrix4f;

import jet.opengl.postprocessing.texture.Texture2D;
//---------------------------------------------------------------------------------------------------
//	[Optional] Input normal data.
//
//	Requirements:
// 		* The normal texture is required to contain world-space normals in RGB.
// 		* The normal texture must have the same resolution and MSAA sample count as the input depth texture.
// 		* The view-space Y & Z axis are assumed to be pointing up & forward respectively (left-handed projection).
// 		* The WorldToView matrix is assumed to not contain any non-uniform scaling.
// 		* The WorldView matrix must have the following form:
//    		{ M00, M01, M02, 0.f }
//    		{ M10, M11, M12, 0.f }
//    		{ M20, M21, M22, 0.f }
//    		{ M30, M31, M32, M33 }
//
//	Remarks:
// 		* The actual view-space normal used for the AO rendering is:
//   	  N = normalize( mul( FetchedNormal.xyz * DecodeScale + DecodeBias, (float3x3)WorldToViewMatrix ) )
// 		* Using bent normals as input may result in false-occlusion (overdarkening) artifacts.
//   	  Such artifacts may be alleviated by increasing the AO Bias parameter.
//---------------------------------------------------------------------------------------------------
public class GFSDK_SSAO_InputNormalData {

	public boolean enable = false;
	public final Matrix4f worldToViewMatrix = new Matrix4f();
	public float decodeScale = 1.f;
	public float decodeBias = 0.f;
	
	/** Full-resolution world-space normal texture */
	public Texture2D fullResNormalTexture;
}
