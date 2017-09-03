package assimp.importer.obj;

import org.lwjgl.util.vector.Vector3f;

/** Data structure to store all material specific data */
/*public*/ final class ObjMaterial {
	
	/*public*/ static final int 
		TextureDiffuseType = 0,
		TextureSpecularType = 1,
		TextureAmbientType = 2,
		TextureEmissiveType = 3,
		TextureBumpType = 4,
		TextureNormalType = 5,
		TextureSpecularityType = 6,
		TextureOpacityType = 7,
		TextureDispType = 8,
		TextureTypeCount = 9;
	
	//!	Name of material description
		String materialName;

		//!	Texture names
		String texture;
		String textureSpecular;
		String textureAmbient;
		String textureEmissive;
		String textureBump;
		String textureNormal;
		String textureSpecularity;
		String textureOpacity;
		String textureDisp;
		
		boolean[] clamp = new boolean[TextureTypeCount];

		//!	Ambient color 
		final Vector3f ambient = new Vector3f();
		//!	Diffuse color
		final Vector3f diffuse = new Vector3f(0.6f,0.6f,0.6f);
		//!	Specular color
		final Vector3f specular = new Vector3f();
		//!	Emissive color
		final Vector3f emissive = new Vector3f();
		//!	Alpha value
		float alpha = 1.0f;
		//!	Shineness factor
		float shineness;
		//!	Illumination model 
		int illumination_model = 1;
		//! Index of refraction
		float ior = 1;
}
