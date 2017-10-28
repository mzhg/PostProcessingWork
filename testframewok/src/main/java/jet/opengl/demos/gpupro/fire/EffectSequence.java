package jet.opengl.demos.gpupro.fire;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.FileUtils;
import jet.opengl.postprocessing.util.Numeric;

/**
 * Created by mazhen'gui on 2017/10/28.
 */

final class EffectSequence implements Disposeable{
    private int d_layers;		// total number of slices of the flat 3D texture (Fig.2 in VFA)
    private int d_zx;			// number of slices of the flat 3D texture in every row
    // along the "horizontal edge" of this texture (Fig.2)
    private int dFirstSlice,	// reference number of the first slice in the flat 3D texture (Fig.2)
    // which contains some data
    dLastSlice;		// reference number of the last non-empty slice

    int d_no_of_frames;	// number of frames
    int[] d_skip_vert;		// points to the table of addresses of every frame
    int[] d_no_of_vert;		// points to the table of number of vertices within every frame
    private int d_VBO;				// reference to the vertex buffer object which holds the sequence
    private GLFuncProvider gl;

    int get_layers_no(){ return d_layers; }
    int get_slices_no_alongX(){ return d_zx; }
    int getFirstNonEmptySlice(){ return dFirstSlice; }
    int getLastNonEmptySlice(){ return dLastSlice; }

    public void dispose(){
        if(d_VBO != 0) gl.glDeleteBuffer(d_VBO);
    }

    boolean init(String filename){
        gl = GLFuncProviderFactory.getGLFuncProvider();
        byte[] data = null;
        byte[] no_of_vert = null;

        try (BufferedInputStream in = new BufferedInputStream(FileUtils.open(filename))){
            byte[] c = new byte[4];
            in.read(c);
            d_layers = c[0];
            d_zx = c[1];
            dFirstSlice = c[2];
            dLastSlice = c[3];

            in.read(c);
            d_no_of_frames = Numeric.getInt(c, 0);

            no_of_vert = new byte[d_no_of_frames * 4];
            in.read(no_of_vert);

            in.read(c);
            int all_vertices = Numeric.getInt(c, 0);
            int quant = all_vertices*(3 * 2+ 4);
            data = new byte[quant];
            in.read(data);
            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        d_skip_vert = new int[d_no_of_frames];
        d_no_of_vert = new int[d_no_of_frames];

        Numeric.toInts(no_of_vert, 0, d_no_of_vert, 0, d_no_of_frames);
        d_skip_vert[0] = 0;
        for(int i=0;i<d_no_of_frames-1;i++){
            d_skip_vert[i+1]=d_skip_vert[i]+d_no_of_vert[i];
        }

        d_VBO = gl.glGenBuffer();
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER_ARB, d_VBO);
        gl.glBufferData(GLenum.GL_ARRAY_BUFFER_ARB, CacheBuffer.wrap(data), GLenum.GL_STATIC_DRAW);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER_ARB, 0);

//        GL11.glVertexPointer(3, GL11. GL_SHORT,10,0);
//        GL11.glColorPointer(4,GL11.GL_UNSIGNED_BYTE,10,6);
        return true;
    }

    void enableDatVec(){
//        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
//        GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
//        ARBVertexBufferObject.glBindBufferARB(ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB,d_VBO);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER_ARB, d_VBO);
        gl.glEnableVertexAttribArray(0);
        gl.glVertexAttribPointer(0, 3, GLenum.GL_SHORT, true, 10, 0);
        gl.glEnableVertexAttribArray(1);
        gl.glVertexAttribPointer(1, 4, GLenum.GL_UNSIGNED_BYTE, true, 10, 6);
    }

    void disableDatVec(){
//        GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
//        GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER_ARB, 0);
        gl.glDisableVertexAttribArray(0);
        gl.glDisableVertexAttribArray(1);
    }
}
