package jet.opengl.demos.demos.nvidia.rain;

import java.io.IOException;
import java.nio.ByteBuffer;

import jet.opengl.postprocessing.buffer.AttribDesc;
import jet.opengl.postprocessing.buffer.BufferBinding;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.buffer.VertexArrayObject;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;
import jet.opengl.postprocessing.util.FileUtils;

/**
 * Created by mazhen'gui on 2017/7/3.
 */

final class XMesh implements Disposeable{
    private VertexArrayObject[] m_VAO;
    private BufferGL[] m_Position;
    private BufferGL[] m_Normal;
    private BufferGL[] m_Texcoord;
    private BufferGL[] m_Indices;
    private int[]      m_NumIndices;
    private int[]      m_IndiceTypes;

    XMesh(String filename, int count, int indiceType){
        m_VAO = new VertexArrayObject[count];
        m_Position = new BufferGL[count];
        m_Normal = new BufferGL[count];
        m_Texcoord = new BufferGL[count];
        m_Indices = new BufferGL[count];
        m_NumIndices = new int[count];
        m_IndiceTypes = new int[count];

        for(int i = 0; i < count; i++){
            GLFuncProviderFactory.getGLFuncProvider().glBindVertexArray(0);
            String vertice_filename = String.format("%s_%d_vertice.dat", filename, i);
            ByteBuffer vertice_buffer = load(vertice_filename);
            m_Position[i] = new BufferGL();
            m_Position[i].initlize(GLenum.GL_ARRAY_BUFFER, vertice_buffer.remaining(), vertice_buffer, GLenum.GL_STATIC_DRAW);

            String normal_filename = String.format("%s_%d_normal.dat", filename, i);
            ByteBuffer normal_buffer = load(normal_filename);
            m_Normal[i] = new BufferGL();
            m_Normal[i].initlize(GLenum.GL_ARRAY_BUFFER, normal_buffer.remaining(), normal_buffer, GLenum.GL_STATIC_DRAW);

            String texcoord_filename = String.format("%s_%d_texcoord.dat", filename, i);
            ByteBuffer texcoord_buffer = load(texcoord_filename);
            if(texcoord_buffer != null) {
                m_Texcoord[i] = new BufferGL();
                m_Texcoord[i].initlize(GLenum.GL_ARRAY_BUFFER, texcoord_buffer.remaining(), texcoord_buffer, GLenum.GL_STATIC_DRAW);
            }

            String indices_filename = String.format("%s_%d_indices.dat", filename, i);
            ByteBuffer indices_buffer = load(indices_filename);
            m_IndiceTypes[i] = indiceType;
            if(indiceType == GLenum.GL_UNSIGNED_INT){
                m_NumIndices[i] = indices_buffer.remaining() / 4;
            }else{  // GL_UNSIGNED_SHORT
                m_NumIndices[i] = indices_buffer.remaining() / 2;
            }

            m_Indices[i] = new BufferGL();
            m_Indices[i].initlize(GLenum.GL_ELEMENT_ARRAY_BUFFER, indices_buffer.remaining(), indices_buffer, GLenum.GL_STATIC_DRAW);

            m_VAO[i] = new VertexArrayObject();
            m_VAO[i].initlize(packBuffers(i), m_Indices[i]);
            m_VAO[i].unbind();

            GLCheck.checkError();
        }
    }

    private BufferBinding[] packBuffers(int index){
        BufferBinding[] bindings = new BufferBinding[m_Texcoord[index] != null ? 3 : 2];
        AttribDesc pos_desc = new AttribDesc();
        pos_desc.index = 0;
        pos_desc.size = 3;
        pos_desc.type = GLenum.GL_FLOAT;
        bindings[0] = new BufferBinding(m_Position[index], pos_desc);

        AttribDesc nor_desc = new AttribDesc();
        nor_desc.index = 1;
        nor_desc.size = 3;
        nor_desc.type = GLenum.GL_FLOAT;
        bindings[1] = new BufferBinding(m_Normal[index], nor_desc);

        if(bindings.length == 3){
            AttribDesc tex_desc = new AttribDesc();
            tex_desc.index = 1;
            tex_desc.size = 3;
            tex_desc.type = GLenum.GL_FLOAT;
            bindings[2] = new BufferBinding(m_Texcoord[index], tex_desc);
        }

        return bindings;
    }

    public void render(){
        for(int i = 0; i < m_VAO.length; i++){
            VertexArrayObject  vao = m_VAO[i];
            vao.bind();
            GLFuncProviderFactory.getGLFuncProvider().glDrawElements(GLenum.GL_TRIANGLES, m_NumIndices[i], m_IndiceTypes[i], 0);
            vao.unbind();
        }
    }

    private static ByteBuffer load(String filename){
        final String fullpath = "nvidia/Rain/models/" + filename;
        if(FileUtils.g_IntenalFileLoader.exists(fullpath)){
            try {
                byte[] bytes =  FileUtils.loadBytes(fullpath);
                return CacheBuffer.wrap(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    @Override
    public void dispose() {
        for(VertexArrayObject vao : m_VAO){
            CommonUtil.safeRelease(vao);
        }

        for(BufferGL vbo : m_Position){
            CommonUtil.safeRelease(vbo);
        }

        for(BufferGL vbo : m_Normal){
            CommonUtil.safeRelease(vbo);
        }

        for(BufferGL vbo : m_Texcoord){
            CommonUtil.safeRelease(vbo);
        }

        for(BufferGL ibo : m_Indices){
            CommonUtil.safeRelease(ibo);
        }
    }
}
