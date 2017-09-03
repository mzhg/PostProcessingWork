package assimp.importer.nff;

import org.lwjgl.util.vector.Vector3f;

import assimp.common.TextureMapping;

/** describes face material properties */
final class ShadingInfo {

	final Vector3f color = new Vector3f(0.6f,0.6f,0.6f);
	final Vector3f diffuse = new Vector3f(1.f,1.f,1.f);
	final Vector3f specular = new Vector3f(1.f,1.f,1.f);
	final Vector3f ambient = new Vector3f();
	final Vector3f emissive = new Vector3f();
	float refracti = 1.f;

	String texFile;

	// For NFF2
	boolean twoSided;
	boolean shaded = true;
	float opacity = 1.f, shininess;

	String name = "";

	// texture mapping to be generated for the mesh - uv is the default
	// it means: use UV if there, nothing otherwise. This property is
	// used for locked meshes.
	TextureMapping mapping = TextureMapping.aiTextureMapping_UV;
	
	void set(ShadingInfo o){
//		shader.ambient   = mat.ambient;
//		shader.diffuse   = mat.diffuse;
//		shader.emissive  = mat.emissive;
//		shader.opacity   = mat.opacity;
//		shader.specular  = mat.specular;
//		shader.shininess = mat.shininess;
		
		diffuse.set(o.diffuse);
		specular.set(o.specular);
		ambient.set(o.ambient);
		emissive.set(o.emissive);
		opacity = o.opacity;
		shininess = o.shininess;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		ShadingInfo other = (ShadingInfo) obj;
		if (!ambient.equals(other.ambient))
			return false;
		if (!color.equals(other.color))
			return false;
		if (!diffuse.equals(other.diffuse))
			return false;
		if (Float.floatToIntBits(refracti) != Float.floatToIntBits(other.refracti))
			return false;
		if (shaded != other.shaded)
			return false;
		if (!specular.equals(other.specular))
			return false;
		if (texFile == null) {
			if (other.texFile != null)
				return false;
		} else if (!texFile.equals(other.texFile))
			return false;
		if (twoSided != other.twoSided)
			return false;
		return true;
	}
	
	
}
