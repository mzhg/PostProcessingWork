package jet.opengl.demos.nvidia.lightning;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/8/28.
 */

final class ChainLightning extends LightningSeed{

    final ChainLightningProperties Properties = new ChainLightningProperties();

    private BufferGL m_constants_chain_lightning;

    ChainLightning( int pattern_mask, int subdivisions){
        super(new SubdivideProgram(createProgram("ChainLightningVS.vert", "SubdivideGS.gemo", null, LightningSeed::bindFeedback,  "ChainLightning")),
                new SubdivideProgram(createProgram("SubdivideVS.vert", "SubdivideGS.gemo", null, LightningSeed::bindFeedback, "Subdivide")),
                pattern_mask, subdivisions);

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

        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, LightningRenderer.UNIFORM_LIGHT_CHAIN, m_constants_chain_lightning.getBuffer());
    }

    void RenderFirstPass(boolean bindPro)
    {
//        SetChildConstants();
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

        if(bindPro){
            m_tech_first_pass.enable();
        }

        gl.glBindVertexArray(0);
        gl.glDrawArrays(GLenum.GL_POINTS, 0, GetNumVertices(0));

        if(LightningDemo.canPrintLog()){
            m_tech_first_pass.printPrograminfo();
        }
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
