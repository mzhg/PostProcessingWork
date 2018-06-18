package jet.opengl.demos.intel.fluid.render;

import org.lwjgl.util.vector.Vector3f;

/**
 * Lightweight wrapper around a generic rectilinear grid of scalar floating-point values.<p></p>
 * Created by Administrator on 2018/3/13 0013.
 */

public class GridWrapper {
    /** Values at grid points. */
    public float[] values;
    /** Number of grid points along each direction. */
    public final int[]  number = new int[3];
    /** Delta, in bytes, per index, between adjacent points in the grid. offset = ix * strides[0] + iy * strides[1] + iz * strides[2] */
    public final int[]  strides = new int[3];
    /** Location of first grid point, i.e. values[0] */
    public final Vector3f minPos = new Vector3f();
    /** Direction vectors corresponding to each index: position = ix * directions[0] + iy * directions[1] + iz * directions[2] + minPos */
    public final Vector3f[] directions = new Vector3f[ 3 ];

    public GridWrapper(){
        directions[0] = new Vector3f();
        directions[1] = new Vector3f();
        directions[2] = new Vector3f();
    }
}
