package assimp.common;

/** 
 * Defines alpha-blend flags.<p>
*
*  If you're familiar with OpenGL or D3D, these flags aren't new to you.
*  They define *how* the final color value of a pixel is computed, basing
*  on the previous color at that pixel and the new color value from the
*  material.<p>
*  The blend formula is:
*  <pre>
*    SourceColor * SourceBlend + DestColor * DestBlend
*  </pre>
*  where <DestColor> is the previous color in the framebuffer at this
*  position and <SourceColor> is the material colro before the transparency
*  calculation.<p>
*  This corresponds to the #AI_MATKEY_BLEND_FUNC property.
*/
public enum BlendMode {

	/** 
	 *  Formula:
	 *  <pre>
	 *  SourceColor*SourceAlpha + DestColor*(1-SourceAlpha)
	 *  </pre>
	 */
	aiBlendMode_Default /*= 0x0*/,

	/** Additive blending
	 *
	 *  Formula:
	 *  <pre>
	 *  SourceColor*1 + DestColor*1
	 *  </pre>
	 */
	aiBlendMode_Additive /*= 0x1*/,
}
