package com.nvidia.developer.opengl.models.sdkmesh;

import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;

import jet.opengl.postprocessing.util.Numeric;

final class SDKAnimationData {
	
	static final int SIZE = Vector3f.SIZE + Quaternion.SIZE + Vector3f.SIZE;

	final Vector3f translation = new Vector3f();
	final Quaternion orientation = new Quaternion();
	final Vector3f scaling = new Vector3f();
	
	int load(byte[] data, int position){
		translation.x = Numeric.getFloat(data, position); position += 4;
		translation.y = Numeric.getFloat(data, position); position += 4;
		translation.z = Numeric.getFloat(data, position); position += 4;
		
		orientation.x = Numeric.getFloat(data, position); position += 4;
		orientation.y = Numeric.getFloat(data, position); position += 4;
		orientation.z = Numeric.getFloat(data, position); position += 4;
		orientation.w = Numeric.getFloat(data, position); position += 4;
		
		scaling.x = Numeric.getFloat(data, position); position += 4;
		scaling.y = Numeric.getFloat(data, position); position += 4;
		scaling.z = Numeric.getFloat(data, position); position += 4;
		
		return position;
	}
	
	@Override
	public String toString() {
		return "SDKAnimationData [translation=" + translation + ", orientation=" + orientation + ", scaling=" + scaling
				+ "]";
	}
	
	
}
