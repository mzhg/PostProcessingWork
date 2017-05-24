package jet.opengl.render.volumeline;

import org.lwjgl.util.vector.Matrix4f;

import java.io.IOException;

import jet.opengl.postprocessing.buffer.AttribDesc;
import jet.opengl.postprocessing.buffer.BufferBinding;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.buffer.VertexArrayObject;
import jet.opengl.postprocessing.common.DepthStencilState;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLStateTracker;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.SimpleUniformColorProgram;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Simple line renderer implementation.<p></p>
 * Created by mazhen'gui on 2017/5/24.
 */

final class SimpleLineRenderer  extends VolumeLinesRenderer{
    SimpleUniformColorProgram gpupLine;   // program id

    BufferGL          m_lineVBO;
    VertexArrayObject m_lineVAO;
    final int maxLineCount ;
    int lineCount;
    final float[] lineVertex/* = new float[maxLineCount * 6]*/;
    GLStateTracker m_StateTracker;
    DepthStencilState m_dsstate;

    public SimpleLineRenderer() throws  IOException{
        this(1024);
    }

    public SimpleLineRenderer(int maxLineCount) throws IOException{
        this.maxLineCount = maxLineCount;
        lineVertex = new float[maxLineCount * 6];
        gpupLine = new SimpleUniformColorProgram();
        m_StateTracker = GLStateTracker.getInstance();

        m_lineVBO = new BufferGL();
        m_lineVBO.initlize(GLenum.GL_ARRAY_BUFFER, maxLineCount*2*3*4, null, GLenum.GL_STREAM_DRAW);


        AttribDesc desc = new AttribDesc();
        desc.index = gpupLine.getPositionAttribLocation();
        desc.size = 3;
        desc.type = GLenum.GL_FLOAT;
        desc.normalized = false;
        desc.stride = 0;
        desc.offset = 0;
        desc.divisor = 0;
        BufferBinding binding = new BufferBinding(m_lineVBO, desc);
        m_lineVAO = new VertexArrayObject();
        m_lineVAO.initlize(new BufferBinding[]{binding}, null);
        if(GLCheck.CHECK)
            GLCheck.checkError();

        m_dsstate = new DepthStencilState();
        m_dsstate.depthEnable = true;
    }

    @Override
    public void begin(float radius, Matrix4f mvpMat, Matrix4f mvMat,
                      Matrix4f pMat, Texture2D texture, float screenRatio) {
        gpupLine.enable();
        gpupLine.setUniformColor(1, 1, 1, 1);
        gpupLine.setMVP(mvpMat);

        GLFuncProviderFactory.getGLFuncProvider().glLineWidth(radius);
        lineCount = 0;
    }

    @Override
    public void line(float x0, float y0, float z0, float x1, float y1, float z1) {
        int index = lineCount * 6;
        lineVertex[index++] = x0;
        lineVertex[index++] = y0;
        lineVertex[index++] = z0;
        lineVertex[index++] = x1;
        lineVertex[index++] = y1;
        lineVertex[index++] = z1;

        lineCount ++;
    }

    @Override
    public void lines(int _nbLines, float[] pLines, int offset) {
        int actualLines = _nbLines;
        int src_index = offset;
        int dst_index = lineCount * 6;
        System.arraycopy(pLines, src_index, lineVertex, dst_index, actualLines * 6);
        lineCount += actualLines;
    }

    @Override
    public void end() {
//        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, lineVBO);
////			GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER,0,nbLines*2*3*4,pLines);		//send data to the graphic card TODO
//        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, GLUtil.wrap(lineVertex, 0, lineCount * 2 * 3));
//        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
//
//        GL30.glBindVertexArray(lineVA);
//        GL31.glDrawArraysInstanced(GL11.GL_LINES, 0, lineCount*2, 1);				//render lines as usual
//        GL30.glBindVertexArray(0);
//        gpupLine.disable();

        m_lineVBO.update(0, CacheBuffer.wrap(lineVertex, 0, lineCount * 2 * 3));
        m_lineVBO.unbind();

        try{
            m_StateTracker.saveStates();
            m_StateTracker.clearFlags(true);

//            m_StateTracker.setCurrentFramebuffer();
//            m_StateTracker.setCurrentViewport();
            m_StateTracker.setVAO(m_lineVAO);
            m_StateTracker.setProgram(gpupLine);

            m_StateTracker.setRasterizerState(null);  // TODO line width
            m_StateTracker.setDepthStencilState(m_dsstate);
            m_StateTracker.setBlendState(null);   // TODO make the line smooth??

            GLFuncProviderFactory.getGLFuncProvider().glDrawArraysInstanced(GLenum.GL_LINES, 0, lineCount*2, 1); //render lines as usual
        }finally {
            m_StateTracker.restoreStates();
            m_StateTracker.reset();
        }
    }

    @Override
    public void dispose() {
        if(m_lineVBO != null){
            m_lineVBO.dispose();
            m_lineVBO = null;
        }

        if(m_lineVAO != null){
            m_lineVAO.dispose();
            m_lineVAO = null;
        }

        if(gpupLine != null){
            gpupLine.dispose();
            gpupLine = null;
        }
    }

    public int getLineCount() { return lineCount; }
    public int getMaxLineCount() { return maxLineCount;}
}
