package jet.opengl.demos.nvidia.face.libs;

/**
 * Parameters for building shadow lookup texture (LUT) for SSS.<p></p>
 * Created by mazhen'gui on 2017/9/4.
 */

public class GFSDK_FaceWorks_ShadowLUTConfig {
    /** Diffusion radius, in world units (= 2.7mm for human skin) */
    public float		m_diffusionRadius;
    /** Width of curvature LUT (typically 512) */
    public int			m_texWidth;
    /** Height of curvature LUT (typically 512) */
    public int			m_texHeight;
    /** Min world-space penumbra width used to build the LUT (typically ~0.8 cm) */
    public float		m_shadowWidthMin;
    /** Max world-space penumbra width used to build the LUT (typically ~10.0 cm) */
    public float		m_shadowWidthMax;
    /** Ratio by which output shadow is sharpened (adjust to taste; typically 3.0 to 10.0) */
    public float		m_shadowSharpening;
}
