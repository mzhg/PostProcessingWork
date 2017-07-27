package jet.opengl.demos.nvidia.waves.samples;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector4f;

public class Frustum {

	/**
	 * 0. left
	 * 1. right
	 * 2. bottom
	 * 3. top
	 * 4. near
	 * 5. far
	 */
	private final Vector4f[] planes = new Vector4f[4];
	private final Vector4f[] corner_verts = new Vector4f[4];
	private final Vector4f temp = new Vector4f();
	
	public Frustum() {
		
		for(int i = 0; i < planes.length; i++){
			planes[i] = new Vector4f();
			corner_verts[i] = new Vector4f();
		}
	}
	
	public void reset(Matrix4f proj){
//		Matrix4f.extractFrustumPlanes(proj, planes);  // need validate
		
		float fov_x = -(float) Math.atan(1.0f / proj.m00);
		Vector4f plane_left = planes[0];
		plane_left.set((float)Math.cos(fov_x), 0, (float)Math.sin(fov_x), 0);
		// Right plane
		Vector4f plane_right = planes[1];
		plane_right.set(-(float)Math.cos(fov_x), 0, (float)Math.sin(fov_x), 0);

		// Bottom plane
		float fov_y = -(float) Math.atan(1.0f / proj.m11);
		Vector4f plane_bottom = planes[2];
		plane_bottom.set(0, (float)Math.cos(fov_y), (float)Math.sin(fov_y), 0);
		// Top plane
		Vector4f plane_top = planes[3];
		plane_top.set(0, -(float)Math.cos(fov_y), (float)Math.sin(fov_y), 0);
	}
	
	public boolean contains(QuadNode quad_node, Matrix4f combine){
		corner_verts[0].set(quad_node.bottom_left_x, quad_node.bottom_left_y, 0, 1);
		corner_verts[1] = Vector4f.add(corner_verts[0], vec4(quad_node.length, 0, 0, 0), corner_verts[1]);
		corner_verts[2] = Vector4f.add(corner_verts[0], vec4(quad_node.length, quad_node.length, 0, 0), corner_verts[2]);
		corner_verts[3] = Vector4f.add(corner_verts[0], vec4(0, quad_node.length, 0, 0), corner_verts[3]);
		
		Matrix4f.transform(combine, corner_verts[0], corner_verts[0]);
		Matrix4f.transform(combine, corner_verts[1], corner_verts[1]);
		Matrix4f.transform(combine, corner_verts[2], corner_verts[2]);
		Matrix4f.transform(combine, corner_verts[3], corner_verts[3]);

		// Test against eye plane
		if (corner_verts[0].z > 0 && corner_verts[1].z > 0 && corner_verts[2].z > 0 && corner_verts[3].z > 0)
			return false;

		// Test against left plane
		Vector4f plane_left = planes[0];
		float dist_0 = Vector4f.dot(corner_verts[0], plane_left);
		float dist_1 = Vector4f.dot(corner_verts[1], plane_left);
		float dist_2 = Vector4f.dot(corner_verts[2], plane_left);
		float dist_3 = Vector4f.dot(corner_verts[3], plane_left);
		if (dist_0 < 0 && dist_1 < 0 && dist_2 < 0 && dist_3 < 0)
			return false;

		// Test against right plane
		Vector4f plane_right = planes[1];
		dist_0 = Vector4f.dot(corner_verts[0], plane_right);
		dist_1 = Vector4f.dot(corner_verts[1], plane_right);
		dist_2 = Vector4f.dot(corner_verts[2], plane_right);
		dist_3 = Vector4f.dot(corner_verts[3], plane_right);
		if (dist_0 < 0 && dist_1 < 0 && dist_2 < 0 && dist_3 < 0)
			return false;

		// Test against bottom plane
		Vector4f plane_bottom = planes[2];
		dist_0 = Vector4f.dot(corner_verts[0], plane_bottom);
		dist_1 = Vector4f.dot(corner_verts[1], plane_bottom);
		dist_2 = Vector4f.dot(corner_verts[2], plane_bottom);
		dist_3 = Vector4f.dot(corner_verts[3], plane_bottom);
		if (dist_0 < 0 && dist_1 < 0 && dist_2 < 0 && dist_3 < 0)
			return false;

		// Test against top plane
		Vector4f plane_top = planes[3];
		dist_0 = Vector4f.dot(corner_verts[0], plane_top);
		dist_1 = Vector4f.dot(corner_verts[1], plane_top);
		dist_2 = Vector4f.dot(corner_verts[2], plane_top);
		dist_3 = Vector4f.dot(corner_verts[3], plane_top);
		if (dist_0 < 0 && dist_1 < 0 && dist_2 < 0 && dist_3 < 0)
			return false;

		return true;
	}
	
	private Vector4f vec4(float x, float y, float z, float w) {
		temp.set(x, y, z, w);
		return temp;
	}
}
