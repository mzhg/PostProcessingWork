package com.nvidia.developer.opengl.models;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

public class QuadricMesh {

	Model model;
	
	public QuadricMesh(QuadricBuilder builder, QuadricGenerator generator) {
		final int x_steps = builder.x_steps;
		final int y_steps = builder.y_steps;
		
		int vCount = x_steps * y_steps;
		AttribFloatArray positions = new AttribFloatArray(3, vCount);
		AttribFloatArray normals   = null;
		AttribFloatArray texCoords = null;
		AttribFloatArray colors    = null;
		
		Vector3f position = new Vector3f();
		Vector3f normal   = null;
		Vector2f texCoord = null;
		Vector4f color	  = null;
		
		Vector3f min      = null;
		Vector3f max      = null;
		
		if(builder.centerToOrigin){
			float v = Float.MAX_VALUE;
			min = new Vector3f(v,v,v);
			max = new Vector3f(-v,-v,-v);
		}
		
		if(builder.autoGenNormal || builder.hasNormal){
			normals = new AttribFloatArray(3, vCount);
			if(!builder.autoGenNormal)
				normal = new Vector3f();
		}
		
		if(builder.autoGenTexCoord || builder.hasTexCoord){
			texCoords = new AttribFloatArray(2, vCount);
			if(!builder.autoGenTexCoord)
				texCoord = new Vector2f();
		}
		
		if(builder.hasColor){
			colors = new AttribFloatArray(4, vCount);
			color = new Vector4f();
		}
		
		float s_step = 1.0f/(x_steps - 1);
		float t_step = 1.0f/(y_steps - 1);
		
		for(int i = 0; i < x_steps; i++){
			final float x = i * s_step;
			for(int j = 0; j < y_steps; j++){
				final float y = j * t_step;
				generator.genVertex(x, y, position, normal, texCoord, color);
				
				positions.add(position.x, position.y, position.z);
				if(builder.centerToOrigin){
					min.x = Math.min(min.x, position.x);
					min.y = Math.min(min.y, position.y);
					min.z = Math.min(min.z, position.z);
					
					max.x = Math.max(max.x, position.x);
					max.y = Math.max(max.y, position.y);
					max.z = Math.max(max.z, position.z);
				}
				
				if(builder.autoGenTexCoord){
					texCoords.add(x, y);
				}else if(texCoord != null){
					texCoords.add(texCoord.x, texCoord.y);
				}
				
				if(normal != null){
					normals.add(normal.x, normal.y, normal.z);
				}
				
				if(color != null){
					colors.add(color.x, color.y, color.z, color.w);
				}
			}
		}
		
		if(builder.centerToOrigin){
			Vector3f center = Vector3f.add(min, max, min);
			center.scale(0.5f);
			
			if(center.lengthSquared() > 1e-6){
				float[] data = positions.getArray();
				for(int i = 0; i < vCount; i++){
					int index = i * 3;
					data[index++] -= center.x;
					data[index++] -= center.y;
					data[index++] -= center.z;
				}
			}
		}
		
		if(builder.autoGenNormal){
			normals.resize(vCount);
			
			Vector3f v0 = new Vector3f();
			Vector3f v1 = new Vector3f();
			Vector3f v2 = new Vector3f();
			int x1, y1;
			int x2, y2;
			
			for(int i = 0; i < x_steps; i++){
				for(int j = 0; j < y_steps; j++){
					final int index = i * y_steps + j;
					float x = 0;
					float y = 0;
					float z = 0;
					
					positions.get(index, v0);
					
					if(builder.genNormalClamped){
						// right-up triangle
						x1 = i + 1;
						y1 = j;
						x2 = x1;
						y2 = j + 1;
						if(x1 < x_steps && y2 < y_steps){
							// right
							int index1 = x1 * y_steps + y1;
							positions.get(index1, v1);
							
							int index2 = x2 * y_steps + y2;
							positions.get(index2, v2);
							
							Vector3f.sub(v1, v0, v1);
							Vector3f.sub(v2, v0, v2);
							Vector3f.cross(v1, v2, v1);
							
	//						v1.store(tempArray, index);
							x += v1.x;
							y += v1.y;
							z += v1.z;
							
							// right-up
							index1 = index2;
							index2 = index + 1;
							positions.get(index1, v1);
							positions.get(index2, v2);
							Vector3f.sub(v1, v0, v1);
							Vector3f.sub(v2, v0, v2);
							Vector3f.cross(v1, v2, v1);
							
							x += v1.x;
							y += v1.y;
							z += v1.z;
						}
						
						// left-up triangle
						x1 = i;
						y1 = j + 1;
						x2 = i - 1;
						y2 = y1;
						if(y1 < y_steps && x2 >= 0){
							// up
							int index1 = x1 * y_steps + y1;
							positions.get(index1, v1);
							
							int index2 = x2 * y_steps + y2;
							positions.get(index2, v2);
							
							Vector3f.sub(v1, v0, v1);
							Vector3f.sub(v2, v0, v2);
							Vector3f.cross(v1, v2, v1);
							x += v1.x;
							y += v1.y;
							z += v1.z;
							
							// left-up
							index1 = index2;
							index2 = index - y_steps;
							
							positions.get(index1, v1);
							positions.get(index2, v2);
							Vector3f.sub(v1, v0, v1);
							Vector3f.sub(v2, v0, v2);
							Vector3f.cross(v1, v2, v1);
							
							x += v1.x;
							y += v1.y;
							z += v1.z;
						}
						
						
						// left-bottom
						x1 = i - 1;
						y1 = j;
						x2 = x1;
						y2 = y1 - 1;
						if(x1 >= 0 && y2 >=0){
							// left
							int index1 = x1 * y_steps + y1;
							positions.get(index1, v1);
							
							int index2 = x2 * y_steps + y2;
							positions.get(index2, v2);
							
							Vector3f.sub(v1, v0, v1);
							Vector3f.sub(v2, v0, v2);
							Vector3f.cross(v1, v2, v1);
							x += v1.x;
							y += v1.y;
							z += v1.z;
							
							// left-bottom
							index1 = index2;
							index2 = index - 1;
							
							positions.get(index1, v1);
							positions.get(index2, v2);
							Vector3f.sub(v1, v0, v1);
							Vector3f.sub(v2, v0, v2);
							Vector3f.cross(v1, v2, v1);
							
							x += v1.x;
							y += v1.y;
							z += v1.z;
						}
						
						// right-bottom
						x1 = i;
						y1 = j - 1;
						x2 = x1 + 1;
						y2 = y1;
						if(y1 >= 0 && x2 < x_steps){
							// bottom
							int index1 = x1 * y_steps + y1;
							positions.get(index1, v1);
							
							int index2 = x2 * y_steps + y2;
							positions.get(index2, v2);
							
							Vector3f.sub(v1, v0, v1);
							Vector3f.sub(v2, v0, v2);
							Vector3f.cross(v1, v2, v1);
							x += v1.x;
							y += v1.y;
							z += v1.z;
							
							// right-bottom
							index1 = index2;
							index2 = index + y_steps;
							
							positions.get(index1, v1);
							positions.get(index2, v2);
							Vector3f.sub(v1, v0, v1);
							Vector3f.sub(v2, v0, v2);
							Vector3f.cross(v1, v2, v1);
							
							x += v1.x;
							y += v1.y;
							z += v1.z;
						}
					}else{ // repeat.
						// right-up triangle
						x1 = (i + 1) % x_steps;
						y1 = j;
						x2 = x1;
						y2 = (j + 1) % y_steps;
						// right
						int index1 = x1 * y_steps + y1;
						positions.get(index1, v1);
						
						int index2 = x2 * y_steps + y2;
						positions.get(index2, v2);
						
						Vector3f.sub(v1, v0, v1);
						Vector3f.sub(v2, v0, v2);
						Vector3f.cross(v1, v2, v1);
						
//						v1.store(tempArray, index);
						x += v1.x;
						y += v1.y;
						z += v1.z;
						
						// right-up
						index1 = index2;
						index2 = index + 1;
						positions.get(index1, v1);
						positions.get(index2, v2);
						Vector3f.sub(v1, v0, v1);
						Vector3f.sub(v2, v0, v2);
						Vector3f.cross(v1, v2, v1);
						
						x += v1.x;
						y += v1.y;
						z += v1.z;
						
						// left-up triangle
						x1 = i;
						y1 = (j + 1) % y_steps;
						x2 = (i - 1);
						x2 = x2 < 0 ? x_steps - 1 : x2;
						y2 = y1;
						
						// up
						index1 = x1 * y_steps + y1;
						positions.get(index1, v1);
						
						index2 = x2 * y_steps + y2;
						positions.get(index2, v2);
						
						Vector3f.sub(v1, v0, v1);
						Vector3f.sub(v2, v0, v2);
						Vector3f.cross(v1, v2, v1);
						x += v1.x;
						y += v1.y;
						z += v1.z;
						
						// left-up
						index1 = index2;
						index2 = index - y_steps;
						
						positions.get(index1, v1);
						positions.get(index2, v2);
						Vector3f.sub(v1, v0, v1);
						Vector3f.sub(v2, v0, v2);
						Vector3f.cross(v1, v2, v1);
						
						x += v1.x;
						y += v1.y;
						z += v1.z;
						
						
						// left-bottom
						x1 = i - 1;
						x1 = x1 < 0 ? x_steps - 1 : x1;
						y1 = j;
						x2 = x1;
						y2 = y1 - 1;
						y2 = y2 < 0 ? y_steps - 1 : y2;
						// left
						index1 = x1 * y_steps + y1;
						positions.get(index1, v1);
						
						index2 = x2 * y_steps + y2;
						positions.get(index2, v2);
						
						Vector3f.sub(v1, v0, v1);
						Vector3f.sub(v2, v0, v2);
						Vector3f.cross(v1, v2, v1);
						x += v1.x;
						y += v1.y;
						z += v1.z;
						
						// left-bottom
						index1 = index2;
						index2 = index - 1;
						
						positions.get(index1, v1);
						positions.get(index2, v2);
						Vector3f.sub(v1, v0, v1);
						Vector3f.sub(v2, v0, v2);
						Vector3f.cross(v1, v2, v1);
						
						x += v1.x;
						y += v1.y;
						z += v1.z;
						
						// right-bottom
						x1 = i;
						y1 = j - 1;
						y1 = y1 < 0 ? y_steps - 1 : y1;
						x2 = (x1 + 1) % x_steps;
						y2 = y1;
						// bottom
						index1 = x1 * y_steps + y1;
						positions.get(index1, v1);
						
						index2 = x2 * y_steps + y2;
						positions.get(index2, v2);
						
						Vector3f.sub(v1, v0, v1);
						Vector3f.sub(v2, v0, v2);
						Vector3f.cross(v1, v2, v1);
						x += v1.x;
						y += v1.y;
						z += v1.z;
						
						// right-bottom
						index1 = index2;
						index2 = index + y_steps;
						
						positions.get(index1, v1);
						positions.get(index2, v2);
						Vector3f.sub(v1, v0, v1);
						Vector3f.sub(v2, v0, v2);
						Vector3f.cross(v1, v2, v1);
						
						x += v1.x;
						y += v1.y;
						z += v1.z;
					}
					
					v0.set(x, y, z);
					v0.normalise();
//					v0.store(tempArray, 3 * index);
					normals.set(index, v0.x, v0.y, v0.z);
				}
			}
		} // end auto generate normal
		
		AttribArray indices = genIndices(builder.drawMode, x_steps, y_steps);
		model = new Model();
		model.addAttrib(positions, builder.getPositionLocation());
		if(normals != null)
			model.addAttrib(normals, builder.getNormalLocation());
		if(texCoords != null)
			model.addAttrib(texCoords, builder.getTexCoordLocation());
		if(colors != null)
			model.addAttrib(colors, builder.getColorLocation());
		
		model.setElements(indices);
		model.setDrawMode(builder.drawMode);
	}
	
	static AttribArray genIndices(DrawMode mode, int width, int height){
		switch (mode) {
		case FILL:
		{
			int count =  (width - 1) * (height - 1) * 6;
			AttribIntArray indices = new AttribIntArray(1, count);
			indices.resize(count);
			int[] data = indices.getArray();
			int index = 0;
			for(int x = 0; x < width - 1; x++){
				for(int y = 0; y < height - 1; y++){
					int i0 = x * height + y;
					int i1 = (x + 1) * height + y;
					int i2 = i1 + 1;
					int i3 = i0 + 1;
					
					data[index++] = i0;
					data[index++] = i1;
					data[index++] = i2;
					
					data[index++] = i0;
					data[index++] = i2;
					data[index++] = i3;
				}
			}
			
			return indices;
		}
		case LINE:
		{
			int w = width - 1;
			int h = height - 1;
			int count =  ( (w * h) << 1 + w + h) << 1;
			
			AttribIntArray indices = new AttribIntArray(1, count);
			indices.resize(count);
			int[] data = indices.getArray();
			int index = 0;
			for(int x = 0; x < width; x++){
				for(int y = 0; y < height; y++){
					int i0 = x * height + y; // the current index.
					int x1 = x + 1;
					int y1 = y + 1;
					if(x1 < width){
						data[index++] = i0;
						data[index++] = i0 + height;
					}
					
					if(y1 < height){
						data[index++] = i0;
						data[index++] = i0 + 1;
					}
				}
			}
			
			return indices;
		}
		case POINT:
		{
			int count = width * height;
			AttribIntArray indices = new AttribIntArray(1, count);
			indices.resize(count);
			int[] data = indices.getArray();
			for(int i = 0; i < count; i++){
				data[i] = i;
			}
			
			return indices;
		}
		default:
			return null;
		}
	}
	
	public Model getModel(){ return model;}
	
	public static void main(String[] args) {
		System.out.println(-1 % 10);
	}
}
