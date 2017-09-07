package com.nvidia.developer.opengl.models.obj;

import org.lwjgl.util.vector.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple class to hold values for a material, which are usually
 * read in from an OBJ .mtl file.  
 */
public class Material {

	public static final int 
		ColorOn_AmbientOff = 0,
	    ColorOn_AmbientOn = 1,
	    HighlightOn = 2,    // Specular highlights, so ambient+diffuse+specular
	    ReflectionOn_RayTraceOn = 3,
	    GlassOn_RayTraceOn = 4,
	    FresnelOn_RayTraceOn = 5,
	    RefractionOn_FresnelOff_RayTraceOn = 6,
	    RefractionOn_FresnelOn_RayTraceOn =7,
	    ReflectionOn_RayTraceOff = 8,
	    GlassOn_RayTraceOff = 9,
	    CastShadowsOntoInvisibleSurfaces = 10,
	    Force32Bit = 0x7FFFFFFF;
    
    /** Ambient surface color */
    public final Vector3f m_ambient = new Vector3f(1.0f, 1.0f, 1.0f);

    /** Diffuse surface color */
    public final Vector3f m_diffuse = new Vector3f(1.0f, 1.0f, 1.0f);

    /** Emissive surface color */
    public final Vector3f m_emissive = new Vector3f(0.0f, 0.0f, 0.0f);

    /// Object translucency
    public float m_alpha = 1.0f;

    /// Specular surface color
    public final Vector3f m_specular = new Vector3f(1.0f, 1.0f, 1.0f);

    /// Surface specular power
    public int m_shininess = 1;

    /// Surface optical density/refraction index
    public float m_opticalDensity = 1.0f;

    /// Surface color transmission filter
    public final Vector3f m_transmissionFilter = new Vector3f(1.0f, 1.0f, 1.0f);

    /// Index of ambient texture
    public final List<NvTextureDesc> m_ambientTextures = new ArrayList<>();

    /// Index of diffuse texture
    public final List<NvTextureDesc> m_diffuseTextures = new ArrayList<>();

    /// Index of specular texture
    public final List<NvTextureDesc> m_specularTextures = new ArrayList<>();

    /// Index of bump/normal map texture
    public final List<NvTextureDesc> m_bumpMapTextures = new ArrayList<>();

    /// Index of reflection texture
    public final List<NvTextureDesc> m_reflectionTextures = new ArrayList<>();

    /// Index of displacement map texture
    public final List<NvTextureDesc> m_displacementMapTextures = new ArrayList<>();

    /// Index of specular power texture
    public final List<NvTextureDesc> m_specularPowerTextures = new ArrayList<>();

    /// Index of translucency map texture
    public final List<NvTextureDesc> m_alphaMapTextures = new ArrayList<>();

    /// Index of decal texture
    public final List<NvTextureDesc> m_decalTextures = new ArrayList<>();
    
    public int m_illumModel = HighlightOn;
}
