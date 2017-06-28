package com.nvidia.developer.opengl.models.sdkmesh;

import org.lwjgl.util.vector.Vector4f;

import jet.opengl.postprocessing.util.Numeric;

final class SDKmeshMaterial {
	
	public static final int SIZE = 1256;

//	char    Name[MAX_MATERIAL_NAME];
	String name;  // 100

    // Use MaterialInstancePath
//    char    MaterialInstancePath[MAX_MATERIAL_PATH];
	String materialInstancePath;  // 260

    // Or fall back to d3d8-type materials
//    char    DiffuseTexture[MAX_TEXTURE_NAME];
//    char    NormalTexture[MAX_TEXTURE_NAME];
//    char    SpecularTexture[MAX_TEXTURE_NAME];
	String diffuseTexture;  // 260
	String normalTexture;   // 260
	String specularTexture; // 260
	
    final Vector4f diffuse = new Vector4f();
    final Vector4f ambient = new Vector4f();
    final Vector4f specular = new Vector4f();
    final Vector4f emissive = new Vector4f();
    float power;
    
    int pDiffuseTexture11;  // aligin 8
    int pNormalTexture11;
    int pSpecularTexture11;
    int pDiffuseRV11;
    int pNormalRV11;
    int pSpecularRV11;

	public void toString(StringBuilder out, int index){
		out.append("SDKmeshMaterial").append(index).append(":------------------------------\n");
		out.append("name = ").append(name).append('\n');
		out.append("materialInstancePath = ").append(materialInstancePath).append('\n');
		out.append("diffuseTexture = ").append(diffuseTexture).append('\n');
		out.append("normalTexture = ").append(normalTexture).append('\n');
		out.append("specularTexture = ").append(specularTexture).append('\n');
		out.append("diffuse = ").append(diffuse).append('\n');
		out.append("ambient = ").append(ambient).append('\n');
		out.append("specular = ").append(specular).append('\n');
		out.append("emissive = ").append(emissive).append('\n');
		out.append("power = ").append(power).append('\n');
		out.append("------------------------------------\n");
	}
    
    int load(byte[] data, int position){
    	name = SDKmesh.getString(data, position, SDKmesh.MAX_MATERIAL_NAME); position+= SDKmesh.MAX_MATERIAL_NAME;
    	int maxLen = SDKmesh.MAX_TEXTURE_NAME;
    	materialInstancePath = SDKmesh.getString(data, position, maxLen); position += maxLen;
    	diffuseTexture 		 = SDKmesh.getString(data, position, maxLen); position += maxLen;
    	normalTexture 		 = SDKmesh.getString(data, position, maxLen); position += maxLen;
    	specularTexture 	 = SDKmesh.getString(data, position, maxLen); position += maxLen;
    	
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
    	
    	pDiffuseTexture11 = Numeric.getInt(data, position); position += 8;
    	pNormalTexture11 = Numeric.getInt(data, position); position += 8;
    	pSpecularTexture11 = Numeric.getInt(data, position); position += 8;
    	pDiffuseRV11 = Numeric.getInt(data, position); position += 8;
    	pNormalRV11 = Numeric.getInt(data, position); position += 8;
    	pSpecularRV11 = Numeric.getInt(data, position); position += 8;
    	
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
