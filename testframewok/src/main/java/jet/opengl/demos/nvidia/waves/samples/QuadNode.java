package jet.opengl.demos.nvidia.waves.samples;

import com.nvidia.developer.opengl.utils.Pool;

import java.util.Arrays;

public class QuadNode implements Pool.Poolable{

	public float bottom_left_x;
	public float bottom_left_y;
	public float length;
	public int lod;
	
	public int[] sub_node;
	
	public QuadNode() {
		sub_node = new int[4];
		Arrays.fill(sub_node, -1);
	}
	
	public void set(float bottom_left_x, float bottom_left_y, float length, int lod){
		this.bottom_left_x = bottom_left_x;
		this.bottom_left_y = bottom_left_y;
		this.length = length;
		this.lod = lod;
	}

	@Override
	public void reset() {	Arrays.fill(sub_node, -1);}
}
