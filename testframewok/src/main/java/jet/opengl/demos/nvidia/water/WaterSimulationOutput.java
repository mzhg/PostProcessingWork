package jet.opengl.demos.nvidia.water;

/**
 * Created by mazhen'gui on 2017/8/22.
 */

public enum WaterSimulationOutput {
    /** Render the water mesh to the given textures(if null, output to the default framebuffer.) */
    RENDER,
    /** Only output the simulated vertices of the water mesh into the vertex array buffers.*/
    VERTEX,
    /** For the deffered rendering, not supported so far. */
    DEFERRED
}
