package jet.opengl.demos.nvidia.lightning;

import java.nio.ByteBuffer;
import java.util.List;

import jet.opengl.postprocessing.buffer.AttribDesc;
import jet.opengl.postprocessing.buffer.BufferBinding;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.buffer.VertexArrayObject;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;

/**
 * Created by mazhen'gui on 2017/8/28.
 */

final class PathLightning extends LightningSeed{
    private VertexArrayObject m_subdivide_layout;
    private BufferGL m_path_segments;
    private int m_num_vertices;

    PathLightning(List<LightningPathSegment> segments, int pattern_mask, int subdivisions) {
        super(new SubdivideProgram(createProgram("SubdivideVS.vert", "SubdivideGS.gemo", null, LightningSeed::bindFeedback, "Subdivide")),
                new SubdivideProgram(createProgram("SubdivideVS.vert", "SubdivideGS.gemo", null, LightningSeed::bindFeedback, "Subdivide")),
                pattern_mask, subdivisions);

        m_num_vertices = segments.size();
        System.out.println("m_num_vertices = " + m_num_vertices);

        int buffer_size = m_num_vertices * SubdivideVertex.SIZE;
        ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(buffer_size);
        for(LightningPathSegment segment : segments){
            segment.Start.store(buffer);
            segment.End.store(buffer);
            segment.Up.store(buffer);
            buffer.putInt(0);
        }

        buffer.flip();

        final int stride = SubdivideVertex.SIZE;
        AttribDesc[] layout = {
          new AttribDesc(0, 3, GLenum.GL_FLOAT, false, stride, 0),
          new AttribDesc(1, 3, GLenum.GL_FLOAT, false, stride, 12),
          new AttribDesc(2, 3, GLenum.GL_FLOAT, false, stride, 24),
          new AttribDesc(3, 1, GLenum.GL_UNSIGNED_INT, false, stride, 36),
        };

        m_path_segments = new BufferGL();
        m_path_segments.initlize(GLenum.GL_ARRAY_BUFFER, buffer_size, buffer, GLenum.GL_STATIC_DRAW);

        m_subdivide_layout = new VertexArrayObject();
        m_subdivide_layout.initlize(CommonUtil.toArray(new BufferBinding(m_path_segments, layout)), null);
        m_subdivide_layout.unbind();
    }

    void RenderFirstPass(boolean bindPro)
    {
//        m_path_segments->BindToInputAssembler();
//        m_device->IASetPrimitiveTopology(D3D10_PRIMITIVE_TOPOLOGY_POINTLIST);
//        m_device->IASetInputLayout(m_subdivide_layout);
//
//        m_tech_first_pass->GetPassByIndex(0)->Apply(0);
//        m_device->Draw(GetNumVertices(0),0);
        GLCheck.checkError();

        if(bindPro)
            m_tech_first_pass.enable();
        m_subdivide_layout.bind();
        gl.glDrawArrays(GLenum.GL_POINTS, 0, m_num_vertices);
        m_subdivide_layout.unbind();

        if(LightningDemo.canPrintLog()){
            GLCheck.checkError();
            System.out.println("PathLightning::RenderFirstPass::");
            m_tech_first_pass.printPrograminfo();
        }
    }

    int GetNumVertices(int level)
    {
        return m_num_vertices * GetNumBoltVertices(level);
    }
}
