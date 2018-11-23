package jet.opengl.demos.nvidia.volumelight;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.StackFloat;
import jet.opengl.postprocessing.util.StackInt;

public class TessellationMesh {

    static boolean DEBUG;

    private int      m_VB;
    private int      m_IB;
    private int      m_VAO;

    private int      m_VertexCount;
    private int      m_IndiceCount;

    private int type;
    private int resolution;

    private GLFuncProvider gl;

    public static void main(String[] args){
        DEBUG = true;

        new TessellationMesh(RenderVolumeDesc.MESHMODE_OMNI_VOLUME, 1);
    }

    public TessellationMesh(int type, int resolution){
        this.type = type;
        this.resolution = resolution;

        if(!DEBUG)
        gl = GLFuncProviderFactory.getGLFuncProvider();

        switch (type){
            case RenderVolumeDesc.MESHMODE_FRUSTUM_GRID:
                generateFrustumeGrid(resolution);
                break;
            case RenderVolumeDesc.MESHMODE_FRUSTUM_BASE:
                generateFrustumeGrid(resolution);  // As same as the grid.
                break;
            case RenderVolumeDesc.MESHMODE_FRUSTUM_CAP:
                generateFrustumeCap(resolution);
                break;
            case RenderVolumeDesc.MESHMODE_OMNI_VOLUME:
                generateOMNVolume(resolution);
                break;
        }
    }

    private void tessellatePlane(Vector2f low, Vector2f upper, int resolution, StackFloat out){

    }

    // Generate the quad mesh with the given resolution and CW order.
    private void generateFrustumeGrid(int resolution){
        Vector3f vClipPos = new Vector3f();

        m_VertexCount = (resolution + 1) * (resolution + 1);
        StackFloat verts = new StackFloat(m_VertexCount * 3);
        for(int j = 0; j < resolution + 1; j++){
            final float y = (float)j/resolution;
            for(int i = 0; i < resolution + 1; i++){
                final float x = (float)i/resolution;

                vClipPos.x = 2 * x - 1;
                vClipPos.y = 2 * y - 1;
                vClipPos.z = 1;

                verts.push(vClipPos);
            }
        }

        m_IndiceCount = resolution * resolution * 6;
        StackInt indices = new StackInt(m_IndiceCount);
        for(int j = 0; j < resolution; j++){
            for(int i = 0; i < resolution; i++) {
                int vertex_index = j * resolution + i;
                int vertex_index_right = vertex_index + 1;
                int vertex_index_top = vertex_index + resolution;
                int vertex_index_right_top = vertex_index_top + 1;

                indices.push(vertex_index);
                indices.push(vertex_index_top);
                indices.push(vertex_index_right);

                indices.push(vertex_index_top);
                indices.push(vertex_index_right_top);
                indices.push(vertex_index_right);
            }
        }

        if(!DEBUG) {
            // Create buffers
            m_VAO = gl.glGenVertexArray();
            gl.glBindVertexArray(m_VAO);

            m_VB = gl.glGenBuffer();
            gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, m_VB);
            gl.glBufferData(GLenum.GL_ARRAY_BUFFER, CacheBuffer.wrap(verts.getData(), 0, verts.size()), GLenum.GL_STATIC_DRAW);
            gl.glEnableVertexAttribArray(0);
            gl.glVertexAttribPointer(0, 3, GLenum.GL_FLOAT, false, 0, 0);

            m_IB = gl.glGenBuffer();
            gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, m_IB);
            gl.glBufferData(GLenum.GL_ELEMENT_ARRAY_BUFFER, CacheBuffer.wrap(indices.getData(), 0, indices.size()), GLenum.GL_STATIC_DRAW);

            gl.glBindVertexArray(0);
        }else {
            int vtx_count = 4 * resolution * resolution;
            for (int i = 0; i < vtx_count; i++) {
                testFrustumeGrid(i, resolution, vClipPos);
            }
        }
    }

    private void generateFrustumeCap(int resolution){
        Vector3f vClipPos = new Vector3f();

        if(!DEBUG) {

        }else{
            int vtx_count = 4*3*(resolution+1) + 6;
            for (int i = 0; i < vtx_count; i++) {
                testFrustumeCap(i, resolution, vClipPos);
            }
        }
    }

    // Generate a unit Cube.
    // Right->Top->Far->Left->Bottom->Near. In the Left hand coordinates.
    private void generateOMNVolume(int resolution){
        Vector3f vClipPos = new Vector3f();

        if(!DEBUG) {

        }else{
            int vtx_count = 6 * 4 * resolution * resolution;
            for (int i = 0; i < vtx_count; i++) {
                testOMNVolume(i, resolution, vClipPos);
            }
        }
    }

    public void drawMesh(){
        gl.glBindVertexArray(m_VAO);
        if(m_IndiceCount > 0){
            gl.glDrawElements(GLenum.GL_TRIANGLES, m_IndiceCount, GLenum.GL_UNSIGNED_INT, 0);
        }else{
            gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, m_VertexCount);
        }

        gl.glBindVertexArray(0);
    }

    private void testFrustumeGrid(int id, int resolution, Vector3f vClipPos){
        final float patch_size = 2.0f / resolution;
        int patch_idx = id / 4;
        int patch_row = patch_idx / resolution;
        int patch_col = patch_idx % resolution;
        vClipPos.x = patch_size*patch_col - 1.0f;
        vClipPos.y = patch_size*patch_row - 1.0f;

        int vtx_idx = id % 4;
        if(vtx_idx != 0)
        {
            vtx_idx = 4 - vtx_idx;
        }
        Vector2f vtx_offset = new Vector2f();
        if (vtx_idx == 0)
        {
            vtx_offset.set(0, 0);
        }
        else if (vtx_idx == 1)
        {
            vtx_offset.set(1, 0);
        }
        else if (vtx_idx == 2)
        {
            vtx_offset.set(1, 1);
        }
        else // if (vtx_idx == 3)
        {
            vtx_offset.set(0, 1);
        }
//        vClipPos.xy += patch_size * vtx_offset;
        vClipPos.x += patch_size * vtx_offset.x;
        vClipPos.y += patch_size * vtx_offset.y;

        vClipPos.z = 1.0f;
//        vClipPos.w = 1.0f;

        System.out.println(vClipPos.toString());
    }

    private void testFrustumeBase(int id, int resolution, Vector3f vClipPos){
        int vtx_idx = id % 3;
        if(vtx_idx != 0)
        {
            vtx_idx = 3 - vtx_idx;
        }

        vClipPos.x = (vtx_idx == 0) ? 1.0f : -1.0f;
        vClipPos.y = (vtx_idx == 2) ? -1.0f : 1.0f;
        vClipPos.x *= (id/3 == 0) ? 1.0f : -1.0f;
        vClipPos.y *= (id/3 == 0) ? 1.0f : -1.0f;
        vClipPos.z = 1.0f;

        System.out.println(vClipPos.toString());
    }

    private void testFrustumeCap(int id, int resolution, Vector3f vClipPos){
        int tris_per_face = resolution+1;
        int verts_per_face = 3*tris_per_face;
        int face_idx = id / verts_per_face;
        int vtx_idx = id % 3;
        if(vtx_idx != 0)
        {
            vtx_idx = 3 - vtx_idx;
        }

        if (face_idx < 4)
        {
            // Cap Side
            float patch_size = 2.0f / resolution;
            int split_point = (resolution+1)/2;
            Vector3f v = new Vector3f();
            int tri_idx = (id%verts_per_face)/3;
            if (tri_idx < resolution)
            {
                if (vtx_idx == 0)
                    v.x = (tri_idx >= split_point) ? 1 : -1;
                else if (vtx_idx == 1)
                    v.x = patch_size * tri_idx - 1;
                else // if (vtx_idx == 2)
                    v.x = patch_size * (tri_idx+1) - 1;
                v.y = (vtx_idx == 0) ? 0 : 1;
            }
            else
            {
                if (vtx_idx == 1)
                    v.x = patch_size*split_point-1;
                else
                    v.x = (vtx_idx == 0) ? -1 : 1;
                v.y = (vtx_idx == 1) ? 1 : 0;
            }
            v.z = 1;
            v.x *= (face_idx/2 == 0) ? 1 : -1;
            v.z *= (face_idx/2 == 0) ? 1 : -1;
//            vClipPos.xyz = (face_idx%2 == 0) ? v.zxy : v.xzy*float3(-1,1,1);
            if(face_idx%2 == 0){
                vClipPos.x = v.z;
                vClipPos.y = v.x;
                vClipPos.z = v.y;
            }else{
                vClipPos.x = -v.x;
                vClipPos.y = v.z;
                vClipPos.z = v.y;
            }
        }
        else
        {
            // Z=0
            int tri_idx = (id-4*verts_per_face)/3;
            vClipPos.x = (vtx_idx == 1) ? 1 : -1;
            vClipPos.y = (vtx_idx == 2) ? 1 : -1;
            vClipPos.x *= (tri_idx == 0) ? 1 : -1;
            vClipPos.y *= (tri_idx == 0) ? 1 : -1;
            vClipPos.z = 0.0f;
        }

        vClipPos.z =2* vClipPos.z - 1;  // remap dx depth to gl depth.
        System.out.println(vClipPos.toString());
    }

    private void testOMNVolume(int id, int resolution, Vector3f vClipPos){
        int verts_per_face = 4*resolution*resolution;
        int face_idx = id / verts_per_face;
        int face_vert_idx = id % verts_per_face;

        final float patch_size = 2.0f / (resolution);
        int patch_idx = face_vert_idx / 4;
        int patch_row = patch_idx / resolution;
        int patch_col = patch_idx % resolution;

        Vector3f P = new Vector3f();
        P.x = patch_size*patch_col - 1.0f;
        P.y = patch_size*patch_row - 1.0f;

        int vtx_idx = id % 4;
        Vector2f vtx_offset = new Vector2f();
        if (vtx_idx == 0)
        {
            vtx_offset.set(0, 0);
        }
        else if (vtx_idx == 1)
        {
            vtx_offset.set(1, 0);
        }
        else if (vtx_idx == 2)
        {
            vtx_offset.set(1, 1);
        }
        else // if (vtx_idx == 3)
        {
            vtx_offset.set(0, 1);
        }
        P.x += patch_size * vtx_offset.x;
        P.y += patch_size * vtx_offset.y;
        P.z = ((face_idx / 3) == 0) ? 1 : -1;
        if ((face_idx % 3) == 0) {
//            P.yzx = P.xyz * (((face_idx / 3) == 0) ? float3(1, 1, 1) : float3(-1, 1, 1));
            float z = P.z;
            if((face_idx / 3) == 0){
                P.z = P.y;
                P.y = P.x;
                P.x = z;
            }else{
                P.z = -P.y;
                P.y = P.x;
                P.x = z;
            }
        }
        else if ((face_idx % 3) == 1){
//            P.xzy = P.xyz * (((face_idx / 3) == 1) ? float3(1,1,1) : float3(-1,1,1));
            float z = P.z;
            if((face_idx / 3) == 1){
                P.z = P.y;
                P.y = z;
            }else{
                P.x *= -1;
                P.z = P.y;
                P.y = z;
            }
        }
        else //if ((face_idx % 3) == 2)
        {
//            P.xyz = P.xyz * (((face_idx / 3) == 0) ? float3(1, 1, 1) : float3(-1, 1, 1));
            if((face_idx / 3) == 0){
                // nothing need to do
            }else{
                P.x *= -1;
            }
        }
//        output.vClipPos = float4(normalize(P.xyz), 1);
        vClipPos.set(P);
//        vClipPos.normalise();

        System.out.println(vClipPos.toString());
    }
}
