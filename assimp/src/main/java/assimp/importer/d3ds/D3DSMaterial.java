package assimp.importer.d3ds;

import org.lwjgl.util.vector.Vector3f;

/** Helper structure representing a 3ds material */
public class D3DSMaterial {

	static int iCnt = 0;
	/** Name of the material */
	public String mName;
	/** Diffuse color of the material */
	public final Vector3f mDiffuse = new Vector3f(0.6f,0.6f,0.6f);
	/** Specular exponent*/
	public float mSpecularExponent;
	/** Shininess strength, in percent*/
	public float mShininessStrength = 1.0f;
	/** Specular color of the material*/
	public final Vector3f mSpecular = new Vector3f();
	/** Ambient color of the material*/
	public final Vector3f mAmbient = new Vector3f();
	/** Shading type to be used*/
	public int mShading = D3DSHelper.Gouraud;
	/** Opacity of the material*/
	public float mTransparency = 1.0f;
	/** Diffuse texture channel*/
	public final D3DSTexture sTexDiffuse = new D3DSTexture();
	/** Opacity texture channel*/
	public final D3DSTexture sTexOpacity = new D3DSTexture();
	/** Specular texture channel*/
	public final D3DSTexture sTexSpecular = new D3DSTexture();
	/** Reflective texture channel*/
	public final D3DSTexture sTexReflective = new D3DSTexture();
	/** Bump texture channel*/
	public final D3DSTexture sTexBump = new D3DSTexture();
	/** Emissive texture channel*/
	public final D3DSTexture sTexEmissive = new D3DSTexture();
	/** Shininess texture channel*/
	public final D3DSTexture sTexShininess = new D3DSTexture();
	/** Scaling factor for the bump values*/
	public float mBumpHeight = 1.0f;
	/** Emissive color*/
	public final Vector3f mEmissive = new Vector3f();
	/** Ambient texture channel
	 * (used by the ASE format)*/
	public final D3DSTexture sTexAmbient = new D3DSTexture();
	/** True if the material must be rendered from two sides*/
	public boolean mTwoSided;
	
	public D3DSMaterial() {
		mName = String.format("UNNAMED_%i",iCnt++);
	}
	
}
