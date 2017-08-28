package jet.opengl.demos.nvidia.lightning;

import java.nio.ByteBuffer;
import java.util.List;

import jet.opengl.postprocessing.buffer.AttribDesc;
import jet.opengl.postprocessing.buffer.BufferBinding;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.buffer.VertexArrayObject;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;

/**
 * Created by mazhen'gui on 2017/8/28.
 */

class PathLightning extends LightningSeed{
    private VertexArrayObject m_subdivide_layout;
    private BufferGL m_path_segments;
    private int m_num_vertices;

    PathLightning(List<LightningPathSegment> segments, int pattern_mask, int subdivisions) {
        super(null, null, pattern_mask, subdivisions);

        m_num_vertices = segments.size();
        int buffer_size = m_num_vertices * LightningPathSegment.SIZE;
        ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(buffer_size);
        for(LightningPathSegment segment : segments){
            segment.Start.store(buffer);
            segment.End.store(buffer);
            segment.Up.store(buffer);
        }
        buffer.flip();

        AttribDesc[] layout = null; // TODO Later implemente it.

        m_path_segments = new BufferGL();
        m_path_segments.initlize(GLenum.GL_ARRAY_BUFFER, buffer_size, buffer, GLenum.GL_STATIC_DRAW);

        m_subdivide_layout = new VertexArrayObject();
        m_subdivide_layout.initlize(CommonUtil.toArray(new BufferBinding(m_path_segments, layout)), null);
        m_subdivide_layout.unbind();
    }

    void RenderFirstPass()
    {
//        m_path_segments->BindToInputAssembler();
//        m_device->IASetPrimitiveTopology(D3D10_PRIMITIVE_TOPOLOGY_POINTLIST);
//        m_device->IASetInputLayout(m_subdivide_layout);
//
//        m_tech_first_pass->GetPassByIndex(0)->Apply(0);
//        m_device->Draw(GetNumVertices(0),0);
        m_subdivide_layout.bind();
        gl.glDrawArrays(GLenum.GL_POINTS, 0, m_num_vertices);
        m_subdivide_layout.unbind();
    }

    int GetNumVertices(int level)
    {
        return m_num_vertices * GetNumBoltVertices(level);
    }
}
