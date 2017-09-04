package jet.opengl.demos.nvidia.face.libs;

/**
 * Parameters for building curvature lookup texture (LUT) for SSS.<p></p>
 * Created by mazhen'gui on 2017/9/4.
 */

public class GFSDK_FaceWorks_CurvatureLUTConfig {
    /** Diffusion radius, in world units (= 2.7mm for human skin) */
    public float		m_diffusionRadius;
    /** Width of curvature LUT (typically 512) */
    public int			m_texWidth;		
    /** Height of curvature LUT (typically 512)*/
    public int			m_texHeight;	
    /** Min radius of curvature used to build the LUT (typically ~0.1 cm min)*/
    public float		m_curvatureRadiusMin;
    /** Max radius of curvature used to build the LUT (typically ~10.0 cm max) */
    public float		m_curvatureRadiusMax; 
}
