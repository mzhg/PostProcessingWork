package assimp.importer.mdl;

import java.nio.ByteBuffer;

import org.lwjgl.util.vector.Vector4f;

import assimp.common.AssUtil;

/** Data structure for a Material in a MDL7 file */
final class Material_MDL7 {
	static final int SIZE = AssUtil.SIZE_OF_VEC4 * 4 + AssUtil.SIZE_OF_FLOAT;
	//! Diffuse base color of the material
	final Vector4f	diffuse = new Vector4f();        

	//! Ambient base color of the material
	final Vector4f	ambient = new Vector4f();  

	//! Specular base color of the material
	final Vector4f	specular = new Vector4f();  

	//! Emissive base color of the material
	final Vector4f	emissive = new Vector4f(); 

	//! Phong power
    float			power;
    
    Material_MDL7 load(ByteBuffer buf){
    	diffuse.x = buf.getFloat();
    	diffuse.y = buf.getFloat();
    	diffuse.z = buf.getFloat();
    	diffuse.w = buf.getFloat();
    	
    	ambient.x = buf.getFloat();
    	ambient.y = buf.getFloat();
    	ambient.z = buf.getFloat();
    	ambient.w = buf.getFloat();
    	
    	specular.x = buf.getFloat();
    	specular.y = buf.getFloat();
    	specular.z = buf.getFloat();
    	specular.w = buf.getFloat();
    	
    	emissive.x = buf.getFloat();
    	emissive.y = buf.getFloat();
    	emissive.z = buf.getFloat();
    	emissive.w = buf.getFloat();
    	
    	power = buf.getFloat();
    	return this;
    }
}
