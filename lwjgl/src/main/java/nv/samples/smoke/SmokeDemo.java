package nv.samples.smoke;

import com.nvidia.developer.opengl.app.NvSampleApp;

import jet.opengl.postprocessing.util.Numeric;

public class SmokeDemo extends NvSampleApp {
    // Some common globals shared across
    static int                                 g_Width;
    static int                                 g_Height;

    static long computeRowColsForFlat3DTexture( int depth/*, int *outCols, int *outRows*/ )
    {
        int outCols, outRows;
        // Compute # of rows and cols for a "flat 3D-texture" configuration
        // (in this configuration all the slices in the volume are spread in a single 2D texture)
        int rows =(int)Math.floor(Math.sqrt(depth));
        int cols = rows;
        while( rows * cols < depth ) {
            cols++;
        }
        assert( rows*cols >= depth );

        outCols = cols;
        outRows = rows;

        return Numeric.encode(outCols, outRows);
    }
}
