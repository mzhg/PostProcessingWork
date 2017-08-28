package jet.opengl.demos.nvidia.lightning;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;

/**
 * Created by mazhen'gui on 2017/8/28.
 */

class LightningSeed {
    final LightningStructure Structure = new LightningStructure();
    GLFuncProvider gl;

    LightningSeed( GLSLProgram first_pass, GLSLProgram subdivide, int pattern_mask, int subdivisions)
    {
        gl = GLFuncProviderFactory.getGLFuncProvider();
        m_tech_first_pass = first_pass;
        m_tech_subdivide = subdivide;

//        m_constants_lightning_structure(effect,"LightningStructure"),
        m_constants_lightning_structure = gl.glGenBuffer();
        gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, m_constants_lightning_structure);
        gl.glBufferData(GLenum.GL_UNIFORM_BUFFER, LightningStructure.SIZE, GLenum.GL_STREAM_DRAW);
        gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, 0);

        m_pattern_mask = pattern_mask;
        m_subdivisions = subdivisions;

    }

    int GetSubdivisions()
    {
        return m_subdivisions;
    }

    int GetMaxNumVertices()
    {

        return GetNumVertices(m_subdivisions);
    }

    int GetNumBoltVertices(int level)
    {
        int result = 1;
        for(int i = 0; i < level; ++i)
        {

            if((m_pattern_mask & ( 1 << i))!=0)
                result *= 3;
            else
                result *= 2;
        }
        return result;
    }

    int GetNumVertices(int level)
    {
        return 0;
    }

    void SetChildConstants()
    {
    }

    void SetConstants()
    {
        m_constants_lightning_structure.set(Structure);
        SetChildConstants();

    }

    void RenderFirstPass()
    {
//        ID3D10Buffer* zero = 0;
//        UINT nought = 0;
//        m_device->IASetVertexBuffers(0,1,&zero,&nought,&nought);
//        m_device->IASetPrimitiveTopology(D3D10_PRIMITIVE_TOPOLOGY_POINTLIST);
//        m_device->IASetInputLayout(0);
//
//        m_tech_first_pass->GetPassByIndex(0)->Apply(0);
//        m_device->Draw(GetNumVertices(0),0);
        throw new UnsupportedOperationException();
    }

    GLSLProgram GetFirstPassTechnique()
    {
        return m_tech_first_pass;
    }

    GLSLProgram	GetSubdivideTechnique()
    {
        return m_tech_subdivide;
    }

    int GetPatternMask()
    {
        return m_pattern_mask;
    }

    int		m_pattern_mask;
    int m_subdivisions;

//    ID3D10Device*	m_device;

//    ID3D10Effect*			m_effect;
    GLSLProgram	m_tech_first_pass;
    GLSLProgram	m_tech_subdivide;

    int  m_constants_lightning_structure;

//    LightningSeed():
//    m_constants_lightning_structure(0,"LightningStructure")
//    {
//    }
//    LightningSeed(ID3D10Effect* effect, ID3D10EffectTechnique* first_pass, ID3D10EffectTechnique* subdivide_pass,  int pattern_mask, unsigned int subdivisions);
//    virtual ~LightningSeed();
}
