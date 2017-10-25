package jet.opengl.demos.nvidia.lightning;

import com.nvidia.developer.opengl.utils.NvImage;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import jet.opengl.postprocessing.buffer.AttribDesc;
import jet.opengl.postprocessing.buffer.BufferBinding;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.buffer.VertexArrayObject;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;
import jet.opengl.postprocessing.util.FileUtils;

/**
 * Created by mazhen'gui on 2017/8/31.
 */
public final class XMesh implements Disposeable{
    private MeshSlice[] m_mesh_slices;
    private Texture2D[] m_textures;
    private GLFuncProvider gl;
    private HashMap<String, Texture2D> texCache = new HashMap<>();

    public XMesh(String token, int count, String[] textureNames){
        m_mesh_slices = new MeshSlice[count];
        m_textures = new Texture2D[count];
        gl = GLFuncProviderFactory.getGLFuncProvider();

        for(int i = 0; i < count; i++){
            String pattern = token + i + "_%s.dat";
            m_mesh_slices[i] = new MeshSlice();
            try {
                m_mesh_slices[i].initlize(pattern);
                if(textureNames[i] != null){
                    m_textures[i] = TextureUtils.createTexture2DFromFile(textureNames[i], true, true);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        texCache = null;  // clear the cache
    }

    public XMesh(String token, int count){
        m_mesh_slices = new MeshSlice[count];
        m_textures = new Texture2D[count];
        gl = GLFuncProviderFactory.getGLFuncProvider();

        String[] textureNames = null;
        int textureNameIdx = 0;
        int[] textureIndices = null;
        int textureIdx = 0;

        String materialFileName = token + "_Materials.txt";
        if(FileUtils.g_IntenalFileLoader.exists(materialFileName)){
            try (BufferedReader in = new BufferedReader(new InputStreamReader(FileUtils.open(materialFileName)))){
                String line;
                while ((line = in.readLine()) != null){
                    int dot = line.indexOf(':');
                    String key = line.substring(0, dot).trim();
                    String value = line.substring(dot+1).trim();

                    if(key.equals("MeshCount")){
                        int ivalue = Integer.parseInt(value);
                        textureIndices = new int[ivalue];
                    }else if(key.equals("materialIdx")){
                        textureIndices[textureIdx++] = Integer.parseInt(value);
                    }else if(key.equals("TextureCount")){
                        textureNames = new String[Integer.parseInt(value)];
                    }else if(key.equals("Texture")){
                        textureNames[textureNameIdx++] = value;
                    }
                }

                if(textureIdx != textureIndices.length)
                    throw  new IllegalArgumentException();
                if(textureNameIdx != textureNames.length)
                    throw  new IllegalArgumentException();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String parent = FileUtils.getParent(token) + "/";
        for(int i = 0; i < count; i++){
            String pattern = token + i + "_%s.dat";
            m_mesh_slices[i] = new MeshSlice();
            try {
                m_mesh_slices[i].initlize(pattern);
                int texIdx;
                if(textureIndices != null){
                    texIdx = textureIndices[i];
                }else{
                    texIdx = i;
                }

                if(textureNames != null && textureNames[texIdx] != null){
                    m_textures[i] = loadTexture2D(parent + textureNames[texIdx], true, true);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        texCache = null;
    }

    private Texture2D loadTexture2D(String filename, boolean flip, boolean genmipmap) throws IOException {
        Texture2D tex = texCache.get(filename);
        if(tex != null){
            return tex;
        }else{
            if(filename.endsWith("DDS") || filename.endsWith("dds")){
                int tex_id = NvImage.uploadTextureFromDDSFile(filename);
                tex = TextureUtils.createTexture2D(GLenum.GL_TEXTURE_2D, tex_id);
            }else{
                tex = TextureUtils.createTexture2DFromFile(filename, flip, genmipmap);
            }
            texCache.put(filename, tex);
            return tex;
        }
    }

    public void draw(){
        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindSampler(0,0);

        for(int i = 0; i < m_mesh_slices.length; i++){
            Texture2D tex = m_textures[i];
            if(tex != null){
                gl.glBindTexture(tex.getTarget(), tex.getTexture());
            }else{
                gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);
            }

            m_mesh_slices[i].draw();

            if(tex != null){
                gl.glBindTexture(tex.getTarget(), 0);
            }
        }
    }

    @Override
    public void dispose() {
        for(MeshSlice slice : m_mesh_slices){
            CommonUtil.safeRelease(slice);
        }

        for(Texture2D tex : m_textures){
            CommonUtil.safeRelease(tex);
        }

        Arrays.fill(m_mesh_slices, null);
        Arrays.fill(m_textures, null);
    }

    private final class MeshSlice implements Disposeable{
        VertexArrayObject m_vao;
        BufferGL m_position;
        BufferGL m_normal;
        BufferGL m_texcoord;
        BufferGL m_indices;

        int vertex_count;
        int indices_count;

        @Override
        public void dispose() {
            CommonUtil.safeRelease(m_vao);
            CommonUtil.safeRelease(m_position);
            CommonUtil.safeRelease(m_normal);
            CommonUtil.safeRelease(m_texcoord);
            CommonUtil.safeRelease(m_indices);
        }

        void initlize(String pattern) throws IOException{
            int stride = 0;
            int normal_offset = 0;
            int texcoord_offset = 0;
            String position_filename = String.format(pattern, "vertice");
            if(FileUtils.g_IntenalFileLoader.exists(position_filename)) {
                byte[] data = FileUtils.loadBytes(position_filename);
                m_position = new BufferGL();
                m_position.initlize(GLenum.GL_ARRAY_BUFFER, data.length, CacheBuffer.wrap(data), GLenum.GL_STATIC_DRAW);
                vertex_count = data.length/ Vector3f.SIZE;

                stride += Vector3f.SIZE;
            }

            String normal_filename = String.format(pattern, "normal");
            if(FileUtils.g_IntenalFileLoader.exists(normal_filename)) {
                byte[] data = FileUtils.loadBytes(normal_filename);
                m_normal = new BufferGL();
                m_normal.initlize(GLenum.GL_ARRAY_BUFFER, data.length, CacheBuffer.wrap(data), GLenum.GL_STATIC_DRAW);

                normal_offset = stride;
                stride += Vector3f.SIZE;
            }

            String texcoord_filename = String.format(pattern, "texcoord");
            if(FileUtils.g_IntenalFileLoader.exists(texcoord_filename)) {
                byte[] data = FileUtils.loadBytes(texcoord_filename);
                m_texcoord = new BufferGL();
                m_texcoord.initlize(GLenum.GL_ARRAY_BUFFER, data.length, CacheBuffer.wrap(data), GLenum.GL_STATIC_DRAW);

                texcoord_offset = stride;
                stride += Vector2f.SIZE;
            }

            String indices_filename = String.format(pattern, "indices");
            if(FileUtils.g_IntenalFileLoader.exists(indices_filename)) {
                byte[] data = FileUtils.loadBytes(indices_filename);
                m_indices = new BufferGL();
                m_indices.initlize(GLenum.GL_ELEMENT_ARRAY_BUFFER, data.length, CacheBuffer.wrap(data), GLenum.GL_STATIC_DRAW);

                indices_count = data.length/4;
            }

            m_vao = new VertexArrayObject();
            List<BufferBinding> bindings = new ArrayList<>(3);
            if(m_position != null){
                bindings.add(new BufferBinding(m_position, new AttribDesc(0, 3, GLenum.GL_FLOAT, false, 0, 0)));
            }

            if(m_normal!=null){
                bindings.add(new BufferBinding(m_normal, new AttribDesc(1, 3, GLenum.GL_FLOAT, false, 0, 0)));
            }

            if(m_texcoord!=null){
                bindings.add(new BufferBinding(m_texcoord, new AttribDesc(2, 2, GLenum.GL_FLOAT, false, 0, 0)));
            }

            m_vao.initlize(bindings.toArray(new BufferBinding[bindings.size()]), m_indices);
            m_vao.unbind();
        }

        void draw(){
            m_vao.bind();
            if(indices_count > 0){
                gl.glDrawElements(GLenum.GL_TRIANGLES, indices_count, GLenum.GL_UNSIGNED_INT, 0);
            }else{
                gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, vertex_count);
            }
            m_vao.unbind();
        }
    }
}
