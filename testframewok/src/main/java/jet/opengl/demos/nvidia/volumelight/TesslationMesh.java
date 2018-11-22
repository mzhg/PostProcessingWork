package jet.opengl.demos.nvidia.volumelight;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import jet.opengl.postprocessing.buffer.BufferGL;

public class TesslationMesh {

    private BufferGL m_VB;
    private BufferGL m_IB;
    private int      m_VAO;

    private int      m_VertexCount;
    private int      m_IndiceCount;

    private int type;
    private int resolution;

    public static void main(String[] args){
        new TesslationMesh(RenderVolumeDesc.MESHMODE_FRUSTUM_GRID, 2);
    }

    public TesslationMesh(int type, int resolution){
        this.type = type;
        this.resolution = resolution;

        switch (type){
            case RenderVolumeDesc.MESHMODE_FRUSTUM_GRID:
                generateFrustumeGrid(resolution);
                break;
        }
    }

    private void generateFrustumeGrid(int resolution){

        Vector3f vClipPos = new Vector3f();
        int vtx_count = 4 * resolution * resolution;
        for(int i = 0; i < vtx_count; i++){
            testFrustumeGrid(i, resolution, vClipPos);
        }
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
}
