package assimp.importer.b3d;

import org.lwjgl.util.vector.Vector3f;


final class B3DVertex {

	float x,y,z;
	float nx,ny,nz;
	float tx,ty,tz;
	final byte[] bones = new byte[4];
	final float[] weights = new float[4];
	
	void setVertex(Vector3f v){ x = v.x; y = v.y; z = v.z;}
	void setNormal(Vector3f v){ nx = v.x; ny = v.y; nz = v.z;}
	void setTexCoord(Vector3f v){ tx = v.x; ty = v.y; tz = v.z;}
}
