package jet.opengl.demos.nvidia.volumelight;

import org.lwjgl.util.vector.Vector2f;

/** Parameters for Volume Generation */
public class VolumeDesc {

	/** Target minimum ray width in pixels */
	public float fTargetRayResolution;
	/** Maximum geometric resolution of the mesh. Accounts for requested tessellation quality. */
	public int uMaxMeshResolution;
	/** Amount to bias ray geometry depth */
	public float fDepthBias;
	/** Quality level of tessellation to use */
    public TessellationQuality eTessQuality;
    
    public static void main(String[] args) {
//		Vector2f tex = new Vector2f();
//		Vector2f pos = new Vector2f();
//    	for(int i = 0; i < 4; i++){
//			mainVS(i, tex, pos);
//			
//			System.out.println(i + ": " +tex + ", " + pos);
//		}
    	
    	float amount = 1000f/38.21f;
    	float ss = amount * 39.31f - 1000f;
    	System.out.println(ss);
	}
    
    static void mainVS(int id, Vector2f tex, Vector2f pos){
    	tex.set((id << 1) & 2, id & 2);
    	
    	Vector2f scale = new Vector2f(2, -2);
    	Vector2f bias = new Vector2f(-1, 1);
    	
    	Vector2f.scale(tex, scale, pos);
    	Vector2f.add(pos, bias, pos);
    }
}
