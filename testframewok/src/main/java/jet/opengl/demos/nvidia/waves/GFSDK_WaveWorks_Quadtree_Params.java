package jet.opengl.demos.nvidia.waves;

import org.lwjgl.util.vector.Vector2f;

/**
 * Quad-tree geometry generator<p></p>
 * Created by Administrator on 2017/7/23 0023.
 */

public class GFSDK_WaveWorks_Quadtree_Params {
    /** Dimension of a single square patch, default to 128x128 grids */
    public int mesh_dim;
    /** The size of the smallest permissible leaf quad in world space (i.e. the size of a lod zero patch) */
    public float min_patch_length;
    /** The coordinates of the min corner of patch (0,0,lod). This is only necessary if AllocPatch/FreePatch
    is being used to drive the quad-tree structure*/
    public final Vector2f patch_origin=new Vector2f();
    /** The lod of the root patch used for frustum culling and mesh lodding. This is only necessary if
     AllocPatch/FreePatch is *not* being used to drive the quad-tree structure. This determines the furthest
     coverage of the water surface from the eye point*/
    public int auto_root_lod;
    /** The upper limit of the pixel number a grid can cover in screen space*/
    public float upper_grid_coverage;
    /** The offset required to place the surface at sea level*/
    public float sea_level;
    /** The flag that defines if tessellation friendly topology and mesh must be generated*/
    public boolean use_tessellation;
    /** The tessellation LOD scale parameter*/
    public float tessellation_lod;
    /** The degree of geomorphing to use when tessellation is not available, in the range [0,1]*/
    public float geomorphing_degree;
    /** Controls the use of CPU timers to gather profiling data*/
    public boolean enable_CPU_timers;

    void set(GFSDK_WaveWorks_Quadtree_Params params){
        mesh_dim = params.mesh_dim;
        min_patch_length = params.min_patch_length;
        patch_origin.set(params.patch_origin);
        auto_root_lod = params.auto_root_lod;
        upper_grid_coverage = params.upper_grid_coverage;
        sea_level = params.sea_level;
        use_tessellation = params.use_tessellation;
        tessellation_lod = params.tessellation_lod;
        geomorphing_degree = params.geomorphing_degree;
        enable_CPU_timers = params.enable_CPU_timers;
    }
}
