package com.nvidia.developer.opengl.models.sdkmesh;

import org.lwjgl.util.vector.Vector4f;

import jet.opengl.postprocessing.util.Numeric;

final class SDKmeshMaterial {

//	char    Name[MAX_MATERIAL_NAME];
	String name;

    // Use MaterialInstancePath
//    char    MaterialInstancePath[MAX_MATERIAL_PATH];
	String materialInstancePath;

    // Or fall back to d3d8-type materials
//    char    DiffuseTexture[MAX_TEXTURE_NAME];
//    char    NormalTexture[MAX_TEXTURE_NAME];
//    char    SpecularTexture[MAX_TEXTURE_NAME];
	String diffuseTexture;
	String normalTexture;
	String specularTexture;
	
    final Vector4f diffuse = new Vector4f();
    final Vector4f ambient = new Vector4f();
    final Vector4f specular = new Vector4f();
    final Vector4f emissive = new Vector4f();
    float power;
    
    int pDiffuseTexture;
    int pNormalTexture;
    int pSpecularTexture;
    
    int load(byte[] data, int position){
    	name = new String(data, position, SDKmesh.MAX_MATERIAL_NAME).trim(); position+= SDKmesh.MAX_MATERIAL_NAME;
    	materialInstancePath = new String(data, position, SDKmesh.MAX_MATERIAL_PATH).trim(); position+= SDKmesh.MAX_MATERIAL_PATH;
    	
    	int maxLen = SDKmesh.MAX_TEXTURE_NAME;
    	diffuseTexture =  new String(data, position, maxLen).trim(); position += maxLen;
    	normalTexture =   new String(data, position, maxLen).trim(); position += maxLen;
    	specularTexture = new String(data, position, maxLen).trim(); position += maxLen;
    	
    	diffuse.x = Numeric.getFloat(data, position); position += 4;
    	diffuse.y = Numeric.getFloat(data, position); position += 4;
    	diffuse.z = Numeric.getFloat(data, position); position += 4;
    	diffuse.w = Numeric.getFloat(data, position); position += 4;
    	
    	ambient.x = Numeric.getFloat(data, position); position += 4;
    	ambient.y = Numeric.getFloat(data, position); position += 4;
    	ambient.z = Numeric.getFloat(data, position); position += 4;
    	ambient.w = Numeric.getFloat(data, position); position += 4;
    	
    	specular.x = Numeric.getFloat(data, position); position += 4;
    	specular.y = Numeric.getFloat(data, position); position += 4;
    	specular.z = Numeric.getFloat(data, position); position += 4;
    	specular.w = Numeric.getFloat(data, position); position += 4;
    	
    	emissive.x = Numeric.getFloat(data, position); position += 4;
    	emissive.y = Numeric.getFloat(data, position); position += 4;
    	emissive.z = Numeric.getFloat(data, position); position += 4;
    	emissive.w = Numeric.getFloat(data, position); position += 4;
    	
    	power = Numeric.getFloat(data, position); position += 4;
    	
    	position += 6 * 8;  // 6 * sizeof(uint64)
    	return position;
    }

	@Override
	public String toString() {
		return "SDKmeshMaterial [name=" + name + ", materialInstancePath=" + materialInstancePath + "\n, diffuseTexture="
				+ diffuseTexture + "\n, normalTexture=" + normalTexture + "\n, specularTexture=" + specularTexture
				+ "\n, diffuse=" + diffuse + "\n, ambient=" + ambient + "\n, specular=" + specular + "\n, emissive=" + emissive
				+ "\n, power=" + power + "]";
	}
}
