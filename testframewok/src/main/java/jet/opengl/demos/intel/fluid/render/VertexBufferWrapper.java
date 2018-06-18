package jet.opengl.demos.intel.fluid.render;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lightweight wrapper around a generic vertex buffer, used to give marching cubes a place to output triangles.<p></p>
 * Created by Administrator on 2018/3/13 0013.
 */

public class VertexBufferWrapper {
    public float[]         positions   ;   /// Array of vertex positions extracted from grid.
    public float[]         normals     ;   /// Array of vertex normals extracted from grid.
    public final AtomicInteger count = new AtomicInteger();   /// Number of vertices written into vertices array.
    public int          capacity    ;   /// Maximum number of vertices that can fit into vertices array.
    public int          stride      ;   /// Number of bytes between adjacent vertices.
}
