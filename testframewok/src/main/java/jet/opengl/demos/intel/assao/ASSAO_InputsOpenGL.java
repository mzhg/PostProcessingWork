package jet.opengl.demos.intel.assao;

import jet.opengl.postprocessing.texture.Texture2D;

final class ASSAO_InputsOpenGL extends ASSAO_Inputs{
	/**
	 * Hardware screen depths<p>
	 * <ul>
	 * <li> GL_R32F or GL_R24S8 texture formats are supported.
	 * <li> Multisampling not yet supported.
	 * <li> Decoded using provided ProjectionMatrix.
	 * <li> For custom decoding see PSPrepareDepths/PSPrepareDepthsAndNormals where they get converted to linear viewspace storage.
	 * </ul>
	 */
	Texture2D DepthSRV;

	/**
	 * Viewspace normals (optional) <p>
	 * <ul>
	 * <li> If null, normals are generated from the depth buffer, otherwise provided normals are used for AO. ASSAO is less
	 * costly when input normals are provided, and has a more defined effect. However, aliasing in normals can result in 
	 * aliasing/flickering in the effect so, in some rare cases, normals generated from the depth buffer can look better.
	 * <li> _FLOAT or _UNORM texture formats are supported.
	 * <li> Input normals are expected to be in viewspace, encoded in [0, 1] with "encodedNormal.xyz = (normal.xyz * 0.5 + 0.5)" 
	 * or similar. Decode is done in LoadNormal() function with "normal.xyz = (encodedNormal.xyz * 2.0 - 1.0)", which can be
	 * easily modified for any custom decoding.
	 * <li> Use SSAO_SMOOTHEN_NORMALS for additional normal smoothing to reduce aliasing/flickering. This, however, also reduces
	 * high detail AO and increases cost.
	 * </ul>
	 */
    Texture2D      NormalSRV;

    /**
     * If not NULL, instead writing into currently bound render target, Draw will use this. Current render target will be restored 
     * to what it was originally after the Draw call.
     */
    Texture2D        OverrideOutputRTV;
}
