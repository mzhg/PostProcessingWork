package jet.opengl.demos.nvidia.waves.wavework;

/**
 * Created by Administrator on 2017/7/23 0023.
 */

public class GFSDK_WaveWorks_Quadtree_Stats {
    public int num_patches_drawn;

    /** CPU time spent on quadtree update, measured in milliseconds (1e-3 sec) */
    public float CPU_quadtree_update_time;

    public void set(GFSDK_WaveWorks_Quadtree_Stats other){
        num_patches_drawn = other.num_patches_drawn;
        CPU_quadtree_update_time = other.CPU_quadtree_update_time;
    }
}
