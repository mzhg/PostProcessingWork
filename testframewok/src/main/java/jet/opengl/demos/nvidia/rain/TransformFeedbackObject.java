package jet.opengl.demos.nvidia.rain;

import java.util.Arrays;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CachaRes;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/7/3.
 */

public final class TransformFeedbackObject implements Disposeable{
    private int transformFeedback;
//    private int primitve_count;

    private int m_CurrentMode;
    private final int[] stream_vaos;
    private final int[] stream_vbos;
    private final int[] stream_quey;
    private final int[] buffer_sizes;
    private GLFuncProvider gl;

    @CachaRes
    @Override
    public void dispose() {
        gl.glDeleteTransformFeedback(transformFeedback);
        gl.glDeleteQueries(CacheBuffer.wrap(stream_quey));
        gl.glDeleteVertexArrays(CacheBuffer.wrap(stream_vaos));
        gl.glDeleteBuffers(CacheBuffer.wrap(stream_vbos));
    }

    public TransformFeedbackObject(int bufferSize, Runnable vertex_binding){
        this(new int[]{bufferSize}, new Runnable[]{vertex_binding});
    }

    public TransformFeedbackObject(int[] bufferSize, Runnable[] vertex_binding) {
        gl = GLFuncProviderFactory.getGLFuncProvider();

        buffer_sizes = Arrays.copyOf(bufferSize, bufferSize.length);
        stream_vaos = new int[bufferSize.length];
        stream_vbos = new int[bufferSize.length];
        stream_quey = new int[bufferSize.length];

        // make sure there is no VAO binding.
        gl.glBindVertexArray(0);
        for(int i = 0; i < bufferSize.length; i++){
            int vbo = gl.glGenBuffer();
            gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, vbo);
            gl.glBufferData(GLenum.GL_ARRAY_BUFFER, bufferSize[i], GLenum.GL_STREAM_DRAW);

            int vao = gl.glGenVertexArray();
            gl.glBindVertexArray(vao);
            gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, vbo);
            vertex_binding[i].run();
            gl.glBindVertexArray(0);
            gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);

            stream_vbos[i] = vbo;
            stream_vaos[i] = vao;

            stream_quey[i] = gl.glGenQuery();
        }

        transformFeedback = gl.glGenTransformFeedback();
        gl.glBindTransformFeedback(GLenum.GL_TRANSFORM_FEEDBACK, transformFeedback);
        for(int i = 0; i < bufferSize.length; i++) {
            gl.glBindBufferBase(GLenum.GL_TRANSFORM_FEEDBACK_BUFFER, i, stream_vbos[i]);
        }
        gl.glBindTransformFeedback(GLenum.GL_TRANSFORM_FEEDBACK, 0);
    }

    public void beginRecord(int primitveType){
        gl.glEnable(GLenum.GL_RASTERIZER_DISCARD);
        gl.glBindTransformFeedback(GLenum.GL_TRANSFORM_FEEDBACK, transformFeedback);
        if(stream_quey.length == 1){
            gl.glBeginQuery(GLenum.GL_TRANSFORM_FEEDBACK_PRIMITIVES_WRITTEN, stream_quey[0]);
        }else{
            for(int i = 0; i < stream_quey.length; i++){
                gl.glBeginQueryIndexed(GLenum.GL_TRANSFORM_FEEDBACK_PRIMITIVES_WRITTEN, i, stream_quey[i]);
            }
        }
        gl.glBeginTransformFeedback(primitveType);
        m_CurrentMode = primitveType;

//        try {
//            GLError.checkError();
//        } catch (Exception e) {
//            System.out.println(particleSystem.count);
//            e.printStackTrace();
//        }
//
//        if(debug){
//            current_prog = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
//            if(current_prog == 0)
//                throw new IllegalArgumentException("No program binded.");
//        }
    }

    public int getBufferSize(int index){ return buffer_sizes[index];}

    public void endRecord(){
//        if(debug){
//            int prog = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
//            if(current_prog != prog){
//                throw new IllegalArgumentException("The program doesn't match!");
//            }
//        }

        gl.glEndTransformFeedback();
        if(stream_quey.length == 1){
            gl.glEndQuery(GLenum.GL_TRANSFORM_FEEDBACK_PRIMITIVES_WRITTEN);
        }else{
            for(int i = 0; i < stream_quey.length; i++){
                gl.glEndQueryIndexed(GLenum.GL_TRANSFORM_FEEDBACK_PRIMITIVES_WRITTEN, i);
            }
        }

        gl.glBindTransformFeedback(GLenum.GL_TRANSFORM_FEEDBACK, 0);
        gl.glDisable(GLenum.GL_RASTERIZER_DISCARD);

//        if(debug){
//            primitve_count = GL15.glGetQueryObjectui(stream_quey[0], GL15.GL_QUERY_RESULT);
//            int nebula_count = GL15.glGetQueryObjectui(stream_quey[1], GL15.GL_QUERY_RESULT);
//            if(particleSystem.count % 60 == 0)
//                System.out.println("primitive_count = " + primitve_count + ", nebula_count = " + nebula_count);
//
//            if(particleSystem.count == 60 || particleSystem.count == 61){
//                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, stream_vbos[1]);
//                ByteBuffer buf = GL15.glMapBuffer(GL15.GL_ARRAY_BUFFER, GL15.GL_READ_ONLY);
//                System.out.println("nebula_count = " + nebula_count);
//                Particle particle = new Particle();
//                for(int i = 0; i < nebula_count; i++){
//                    particle.load(buf);
//                    System.out.println(particle);
//                }
//                GL15.glUnmapBuffer(GL15.GL_ARRAY_BUFFER);
//                System.out.println("----------------------");
//            }
//        }
    }

//		public void bindVAO(){	GL30.glBindVertexArray(vao);}

    public void drawStream(int index){
        gl.glBindVertexArray(stream_vaos[index]);
//			GL40.glDrawTransformFeedback(GL11.GL_POINTS,transformFeedback);
        gl.glDrawTransformFeedbackStream(m_CurrentMode, transformFeedback, index);
        gl.glBindVertexArray(0);
    }

    public void drawArrays(int index, int primive_count){
        gl.glBindVertexArray(stream_vaos[index]);
//			GL40.glDrawTransformFeedback(GL11.GL_POINTS,transformFeedback);
//        gl.glDrawTransformFeedbackStream(m_CurrentMode, transformFeedback, index);
        gl.glDrawArrays(m_CurrentMode, 0, primive_count);
        gl.glBindVertexArray(0);
    }


}
