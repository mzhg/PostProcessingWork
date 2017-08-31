package jet.opengl.demos.nvidia.lightning;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.buffer.VertexArrayObject;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/8/28.
 */

final class ChainLightning extends LightningSeed{

    final ChainLightningProperties Properties = new ChainLightningProperties();

    private BufferGL m_constants_chain_lightning;
    private VertexArrayObject m_subdivide_layout;

    ChainLightning( int pattern_mask, int subdivisions){
        super(createProgram("ChainLightningVS.vert", "SubdivideGS.gemo"),
              createProgram("SubdivideVS.vert", "SubdivideGS.gemo"), pattern_mask, subdivisions);

        m_constants_chain_lightning = new BufferGL();
        m_constants_chain_lightning.initlize(GLenum.GL_UNIFORM_BUFFER, ChainLightningProperties.SIZE, null, GLenum.GL_STREAM_DRAW);
        m_constants_chain_lightning.unbind();
    }

    void SetChildConstants()
    {
        ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(ChainLightningProperties.SIZE);
        Properties.store(buffer).flip();
        m_constants_chain_lightning.update(0, buffer);
        m_constants_chain_lightning.unbind();
    }

    void RenderFirstPass()
    {
        SetChildConstants();
        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, m_constants_chain_lightning.getBuffer());
//        m_device->IASetPrimitiveTopology(D3D10_PRIMITIVE_TOPOLOGY_POINTLIST);
//
//        ID3D10Buffer* zero = 0;
//        UINT nought = 0;
//        m_device->IASetVertexBuffers(0,1,&zero,&nought,&nought);
//        m_device->IASetInputLayout(0);
//
//        m_tech_first_pass->GetPassByIndex(0)->Apply(0);
//        m_device->Draw(GetNumVertices(0),0);
//
//
//        m_device->IASetInputLayout(m_subdivide_layout);

        m_subdivide_layout.bind();
        gl.glDrawArrays(GLenum.GL_POINTS, 0, GetNumVertices(0));
        m_subdivide_layout.unbind();
    }

    int GetMaxNumVertices()
    {

        return  ChainLightningProperties.MaxTargets * GetNumBoltVertices(m_subdivisions);
    }

    int GetNumVertices(int level)
    {

        return Properties.NumTargets * GetNumBoltVertices(level);
    }


}
