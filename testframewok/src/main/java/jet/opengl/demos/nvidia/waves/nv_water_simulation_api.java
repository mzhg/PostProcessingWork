package jet.opengl.demos.nvidia.waves;

/**
 * Created by mazhen'gui on 2017/7/22.
 */

interface nv_water_simulation_api {
    int nv_water_simulation_api_cuda = 0,
    nv_water_simulation_api_direct_compute = 1,
    nv_water_simulation_api_cpu = 2,
            nv_water_simulation_api_gpu_preferred = nv_water_simulation_api_direct_compute;

}
