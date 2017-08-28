package jet.opengl.demos.nvidia.lightning;

import jet.opengl.postprocessing.buffer.VertexArrayObject;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;

/**
 * Created by mazhen'gui on 2017/8/28.
 */

final class ChainLightning extends LightningSeed{

    final ChainLightningProperties Properties = new ChainLightningProperties();

    private int  m_constants_chain_lightning;
    private VertexArrayObject m_subdivide_layout;

    ChainLightning(GLSLProgram first_pass, GLSLProgram subdivide, int pattern_mask, int subdivisions) {
        super(first_pass, subdivide, pattern_mask, subdivisions);
    }

    ChainLightning( int pattern_mask, int subdivisions){
        super(null, null, pattern_mask, subdivisions); // TODO
    }

    void SetChildConstants()
    {
//        m_constants_chain_lightning = Properties;  TODO
    }

    void RenderFirstPass()
    {
//        m_constants_chain_lightning = Properties; TODO

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
