package jet.opengl.demos.nvidia.waves.samples;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/8/2.
 */

public class SkyRoundGenerator implements Disposeable{

    private int sky_vertexbuffer;
    private int sky_gridpoints;
    private GLFuncProvider gl;

    public void initlize(int sky_gridpoints, float sky_texture_angle, float terrain_far_range) {
        // creating sky vertex buffer
        float[] sky_vertexdata;
        int floatnum;
        sky_vertexdata = new float [sky_gridpoints*(sky_gridpoints+2)*2*6];
        float PI = (float) Math.PI;
        this.sky_gridpoints = sky_gridpoints;
        for(int j=0;j<sky_gridpoints;j++)
        {

            int i=0;
            floatnum=(j*(sky_gridpoints+2)*2)*6;
            sky_vertexdata[floatnum+0]=terrain_far_range*0.5f+4000.0f*cos(2.0f*PI*(float)i/(float)sky_gridpoints)*cos(-0.5f*PI+PI*(float)j/(float)sky_gridpoints);
            sky_vertexdata[floatnum+1]=4000.0f*sin(-0.5f*PI+PI*(float)(j)/(float)sky_gridpoints);
            sky_vertexdata[floatnum+2]=terrain_far_range*0.5f+4000.0f*sin(2.0f*PI*(float)i/(float)sky_gridpoints)*cos(-0.5f*PI+PI*(float)j/(float)sky_gridpoints);
            sky_vertexdata[floatnum+3]=1;
            sky_vertexdata[floatnum+4]=(sky_texture_angle+(float)i/(float)sky_gridpoints);
            sky_vertexdata[floatnum+5]=2.0f-2.0f*(float)j/(float)sky_gridpoints;
            floatnum+=6;
            for(i=0;i<sky_gridpoints+1;i++)
            {
                sky_vertexdata[floatnum+0]=terrain_far_range*0.5f+4000.0f*cos(2.0f*PI*(float)i/(float)sky_gridpoints)*cos(-0.5f*PI+PI*(float)j/(float)sky_gridpoints);
                sky_vertexdata[floatnum+1]=4000.0f*sin(-0.5f*PI+PI*(float)(j)/(float)sky_gridpoints);
                sky_vertexdata[floatnum+2]=terrain_far_range*0.5f+4000.0f*sin(2.0f*PI*(float)i/(float)sky_gridpoints)*cos(-0.5f*PI+PI*(float)j/(float)sky_gridpoints);
                sky_vertexdata[floatnum+3]=1;
                sky_vertexdata[floatnum+4]=(sky_texture_angle+(float)i/(float)sky_gridpoints);
                sky_vertexdata[floatnum+5]=2.0f-2.0f*(float)j/(float)sky_gridpoints;
                floatnum+=6;
                sky_vertexdata[floatnum+0]=terrain_far_range*0.5f+4000.0f*cos(2.0f*PI*(float)i/(float)sky_gridpoints)*cos(-0.5f*PI+PI*(float)(j+1)/(float)sky_gridpoints);
                sky_vertexdata[floatnum+1]=4000.0f*sin(-0.5f*PI+PI*(float)(j+1)/(float)sky_gridpoints);
                sky_vertexdata[floatnum+2]=terrain_far_range*0.5f+4000.0f*sin(2.0f*PI*(float)i/(float)sky_gridpoints)*cos(-0.5f*PI+PI*(float)(j+1)/(float)sky_gridpoints);
                sky_vertexdata[floatnum+3]=1;
                sky_vertexdata[floatnum+4]=(sky_texture_angle+(float)i/(float)sky_gridpoints);
                sky_vertexdata[floatnum+5]=2.0f-2.0f*(float)(j+1)/(float)sky_gridpoints;
                floatnum+=6;
            }
            i=sky_gridpoints;
            sky_vertexdata[floatnum+0]=terrain_far_range*0.5f+4000.0f*cos(2.0f*PI*(float)i/(float)sky_gridpoints)*cos(-0.5f*PI+PI*(float)(j+1)/(float)sky_gridpoints);
            sky_vertexdata[floatnum+1]=4000.0f*sin(-0.5f*PI+PI*(float)(j+1)/(float)sky_gridpoints);
            sky_vertexdata[floatnum+2]=terrain_far_range*0.5f+4000.0f*sin(2.0f*PI*(float)i/(float)sky_gridpoints)*cos(-0.5f*PI+PI*(float)(j+1)/(float)sky_gridpoints);
            sky_vertexdata[floatnum+3]=1;
            sky_vertexdata[floatnum+4]=(sky_texture_angle+(float)i/(float)sky_gridpoints);
            sky_vertexdata[floatnum+5]=2.0f-2.0f*(float)(j+1)/(float)sky_gridpoints;
            floatnum+=6;
        }

        if(gl == null)
            gl = GLFuncProviderFactory.getGLFuncProvider();

        sky_vertexbuffer = gl.glGenBuffer();
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, sky_vertexbuffer);
        gl.glBufferData(GLenum.GL_ARRAY_BUFFER, CacheBuffer.wrap(sky_vertexdata), GLenum.GL_STATIC_DRAW);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
    }

    public void draw(int posAttribLoc, int texAttribLoc){
        int stride = 4 * 6;
        gl.glBindVertexArray(0);
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, sky_vertexbuffer);
        gl.glEnableVertexAttribArray(posAttribLoc);
        gl.glEnableVertexAttribArray(texAttribLoc);
        gl.glVertexAttribPointer(posAttribLoc, 4, GLenum.GL_FLOAT, false, stride, 0);
        gl.glVertexAttribPointer(texAttribLoc, 2, GLenum.GL_FLOAT, false, stride, 4 * 4);

        gl.glDrawArrays(GLenum.GL_TRIANGLE_STRIP, 0, sky_gridpoints*(sky_gridpoints+2)*2);

        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
        gl.glDisableVertexAttribArray(posAttribLoc);
        gl.glDisableVertexAttribArray(texAttribLoc);

        GLCheck.checkError();
    }

    private static float sin(float angle) { return (float)Math.sin(angle);}
    private static float cos(float angle) { return (float)Math.cos(angle);}

    @Override
    public void dispose() {
        if(sky_vertexbuffer != 0){
            gl.glDeleteBuffer(sky_vertexbuffer);
            sky_vertexbuffer = 0;
        }
    }
}
