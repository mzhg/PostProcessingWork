package com.nvidia.developer.opengl.models;

import jet.opengl.postprocessing.util.Numeric;

/** Utility class to generate the common shapes. */
public final class ModelGenerator {

	/** cube normals */
	static final float[] cube_normal = {
	        // Front
	        0.0f, 0.0f, 1.0f,
	        0.0f, 0.0f, 1.0f,
	        0.0f, 0.0f, 1.0f,
	        0.0f, 0.0f, 1.0f,
	        // Right
	        1.0f, 0.0f, 0.0f,
	        1.0f, 0.0f, 0.0f,
	        1.0f, 0.0f, 0.0f,
	        1.0f, 0.0f, 0.0f,
	        // Back
	        0.0f, 0.0f, -1.0f,
	        0.0f, 0.0f, -1.0f,
	        0.0f, 0.0f, -1.0f,
	        0.0f, 0.0f, -1.0f,
	        // Left
	        -1.0f, 0.0f, 0.0f,
	        -1.0f, 0.0f, 0.0f,
	        -1.0f, 0.0f, 0.0f,
	        -1.0f, 0.0f, 0.0f,
	        // Bottom
	        0.0f, -1.0f, 0.0f,
	        0.0f, -1.0f, 0.0f,
	        0.0f, -1.0f, 0.0f,
	        0.0f, -1.0f, 0.0f,
	        // Top
	        0.0f, 1.0f, 0.0f,
	        0.0f, 1.0f, 0.0f,
	        0.0f, 1.0f, 0.0f,
	        0.0f, 1.0f, 0.0f
	    };
	
	/** cube texcoords. */
	static final float[] cube_texcoords = {
	        // Front
	        0.0f, 0.0f,
	        1.0f, 0.0f,
	        1.0f, 1.0f,
	        0.0f, 1.0f,
	        // Right
	        0.0f, 0.0f,
	        1.0f, 0.0f,
	        1.0f, 1.0f,
	        0.0f, 1.0f,
	        // Back
	        0.0f, 0.0f,
	        1.0f, 0.0f,
	        1.0f, 1.0f,
	        0.0f, 1.0f,
	        // Left
	        0.0f, 0.0f,
	        1.0f, 0.0f,
	        1.0f, 1.0f,
	        0.0f, 1.0f,
	        // Bottom
	        0.0f, 0.0f,
	        1.0f, 0.0f,
	        1.0f, 1.0f,
	        0.0f, 1.0f,
	        // Top
	        0.0f, 0.0f,
	        1.0f, 0.0f,
	        1.0f, 1.0f,
	        0.0f, 1.0f
	    };
	
	/** Cube indices*/
	static final byte cube_indices[] = {
	        0,1,2,0,2,3,
	        4,5,6,4,6,7,
	        8,9,10,8,10,11,
	        12,13,14,12,14,15,
	        16,17,18,16,18,19,
	        20,21,22,20,22,23
	    };
	private ModelGenerator() {}
	
	/**
	 * Generate a cube model that aligned to the xyz axis and centered to the origin. All of the index of the attributes has binding to 0.
	 * @param size the length of the side of the cube.
	 * @param hasNormal
	 * @param hasTexCoord
	 * @param color
	 * @return 
	 */
	public static Model genCube(float size, boolean hasNormal, boolean hasTexCoord, boolean color){
		Model model = new Model();
		float side2 = size * 0.5f;
		int index = 0;
		float[] v = {
		   // Front
	       -side2, -side2, side2,
	        side2, -side2, side2,
	        side2,  side2, side2,
	       -side2,  side2, side2,
	       // Right
	        side2, -side2, side2,
	        side2, -side2, -side2,
	        side2,  side2, -side2,
	        side2,  side2, side2,
	       // Back
	       -side2, -side2, -side2,
	       -side2,  side2, -side2,
	        side2,  side2, -side2,
	        side2, -side2, -side2,
	       // Left
	       -side2, -side2, side2,
	       -side2,  side2, side2,
	       -side2,  side2, -side2,
	       -side2, -side2, -side2,
	       // Bottom
	       -side2, -side2, side2,
	       -side2, -side2, -side2,
	        side2, -side2, -side2,
	        side2, -side2, side2,
	       // Top
	       -side2,  side2, side2,
	        side2,  side2, side2,
	        side2,  side2, -side2,
	       -side2,  side2, -side2
	    };
		
		AttribFloatArray positions = new AttribFloatArray(3, v.length/3);
		System.arraycopy(v, 0, positions.getArray(), 0, v.length);
		positions.resize(v.length/3);
		
		model.addAttrib(positions, index++);
		if(hasNormal){
			AttribFloatArray normals = new AttribFloatArray(3, cube_normal.length/3);
			System.arraycopy(cube_normal, 0, normals.getArray(), 0, v.length);
			normals.resize(cube_normal.length/3);
			
			model.addAttrib(normals, index++);
		}
		
		if(hasTexCoord){
			AttribFloatArray texcoords = new AttribFloatArray(2, cube_texcoords.length/2);
			System.arraycopy(cube_texcoords, 0, texcoords.getArray(), 0, cube_texcoords.length);
			texcoords.resize(cube_texcoords.length/2);
			
			model.addAttrib(texcoords, index++);
		}
		
		if(color){
			AttribFloatArray colors = new AttribFloatArray(4, v.length/3);
			colors.add(1, 1, 1, 1);
			colors.add(1, 0, 0, 1);
			colors.add(0, 1, 0, 1);
			colors.add(0, 0, 1, 1);
			
			colors.add(1, 0, 0, 1);
			colors.add(1, 1, 0, 1);
			colors.add(0, 1, 1, 1);
			colors.add(0, 1, 0, 1);
			
			colors.add(0, 0, 0, 1);
			colors.add(1, 1, 0, 1);
			colors.add(0, 1, 1, 1);
			colors.add(1, 0, 1, 1);
			
			colors.add(0, 0, 0, 1);
			colors.add(1, 1, 1, 1);
			colors.add(0, 0, 1, 1);
			colors.add(1, 0, 1, 1);
			
			colors.add(0, 0, 1, 1);
			colors.add(0, 1, 0, 1);
			colors.add(0, 1, 1, 1);
			colors.add(1, 0, 1, 1);
			
			model.addAttrib(colors, index++);
		}
		
		AttribByteArray indices = new AttribByteArray(1, cube_indices.length);
		indices.resize(cube_indices.length);
		System.arraycopy(cube_indices, 0, indices.getArray(), 0, cube_indices.length);
		model.setElements(indices);
			
		return model;
	}
	
	/** Generate a rectangle model. The model just contain two attributes: position and texcoord */
	public static Model genRect(float left, float bottom, float right, float top, boolean hasTexCoord){
		Model model = new Model();
		int index = 0;
		AttribFloatArray positions = new AttribFloatArray(2, 4);
		positions.add(left, bottom);
		positions.add(right, bottom);
		positions.add(right, top);
		positions.add(left, top);
		model.addAttrib(positions, index++);
		
		if(hasTexCoord){
			AttribFloatArray texCoords = new AttribFloatArray(2, 4);
			texCoords.add(0, 0);
			texCoords.add(1, 0);
			texCoords.add(1, 1);
			texCoords.add(0, 1);
			model.addAttrib(texCoords, index++);
		}
		
		AttribByteArray indices = new AttribByteArray(1, 6);
		indices.add((byte)0);
		indices.add((byte)1);
		indices.add((byte)2);
		
		indices.add((byte)0);
		indices.add((byte)2);
		indices.add((byte)3);
		model.setElements(indices);
		return model;
	}

	/**
	 * renders a disk on the z = 0  plane.  The disk has a radius of
	 * outerRadius, and contains a concentric circular hole with a radius of
	 * innerRadius. If innerRadius is 0, then no hole is generated. The disk is
	 * subdivided around the z axis into slices (like pizza slices), and also
	 * about the z axis into rings (as specified by slices and loops,
	 * respectively).
	 *
	 * With respect to orientation, the +z side of the disk is considered to be
	 * "outside" (see glu.quadricOrientation).  This means that if the orientation
	 * is set to GLU.OUTSIDE, then any normals generated point along the +z axis.
	 * Otherwise, they point along the -z axis.
	 *
	 * If texturing is turned on (with hasTexCoord is true), texture coordinates are
	 * generated linearly such that where r=outerRadius, the value at (r, 0, 0) is
	 * (1, 0.5), at (0, r, 0) it is (0.5, 1), at (-r, 0, 0) it is (0, 0.5), and at
	 * (0, -r, 0) it is (0.5, 0).
	 */
	static Model genDisk(float innerRadius, float outerRadius, int slices, int loops, boolean hasNormal, boolean hasTexCoord){
		if(innerRadius < 0.0f){
			throw new IllegalArgumentException("The 'innerRadius' can't be less than 0. innerRadius = " + innerRadius);
		}

		if(outerRadius < 0.0f){
			throw new IllegalArgumentException("The 'outerRadius' can't be less than 0. outerRadius = " + outerRadius);
		}

		if(outerRadius < innerRadius){
			throw new IllegalArgumentException("The 'outerRadius' can't be less than the 'innerRadius'.");
		}

		if(slices < 3){
			throw new IllegalArgumentException("The 'slices' can't be less than 3. slices = " + slices);
		}

		Model model = new Model();
		int vertexCount = (2+Math.max(loops, 0)) * slices;
		AttribFloatArray positions = new AttribFloatArray(3, vertexCount);
		AttribFloatArray textures = null;
		AttribFloatArray normals = null;
		IndicesAttrib indices = null;

		if(hasNormal){
			normals = new AttribFloatArray(3, vertexCount);
		}

		if(hasTexCoord){
			textures = new AttribFloatArray(2, vertexCount);
		}

		if(vertexCount < Numeric.MAX_UBYTE){
//			indices = new AttribByteArray(1, )
		}

		return null;
	}
}
