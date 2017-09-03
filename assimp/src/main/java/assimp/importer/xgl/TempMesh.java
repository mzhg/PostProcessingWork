package assimp.importer.xgl;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import assimp.common.DeadlyImportError;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

final class TempMesh {

	final Int2IntOpenHashMap points = new Int2IntOpenHashMap();
	final Int2IntOpenHashMap normals = new Int2IntOpenHashMap();
	final Int2IntOpenHashMap uvs = new Int2IntOpenHashMap();
	
	final FloatArrayList float3 = new FloatArrayList();
	final FloatArrayList float2 = new FloatArrayList();
	
	public TempMesh() {
		points.defaultReturnValue(-1);
		normals.defaultReturnValue(-1);
		uvs.defaultReturnValue(-1);
	}
	
	void getPoint(int id, Vector3f out){
		int index = points.get(id);
		if(index == -1)
			throw new DeadlyImportError("point index out of range");
		
		index = index * 3;
		out.x = float3.getFloat(index++);
		out.y = float3.getFloat(index++);
		out.z = float3.getFloat(index++);
	}
	
	void getNormal(int id, Vector3f out){
		int index = normals.get(id);
		if(index == -1)
			throw new DeadlyImportError("normal index out of range");
		
		index = index * 3;
		out.x = float3.getFloat(index++);
		out.y = float3.getFloat(index++);
		out.z = float3.getFloat(index++);
	}
	
	void getUV(int id, Vector2f out){
		int index = uvs.get(id);
		if(index == -1)
			throw new DeadlyImportError("uv index out of range");
		
		index = index * 2;
		out.x = float2.getFloat(index++);
		out.y = float2.getFloat(index++);
	}
}
