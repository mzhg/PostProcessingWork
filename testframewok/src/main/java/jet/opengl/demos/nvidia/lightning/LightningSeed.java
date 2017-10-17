package jet.opengl.demos.nvidia.lightning;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.OpenGLProgram;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.ProgramLinkTask;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/8/28.
 */

abstract class LightningSeed {

    LightningStructure Structure;
    GLFuncProvider gl;
    int		m_pattern_mask;
    int m_subdivisions;

//    ID3D10Device*	m_device;

    //    ID3D10Effect*			m_effect;
    OpenGLProgram m_tech_first_pass;
    OpenGLProgram	m_tech_subdivide;

    BufferGL m_constants_lightning_structure;

    LightningSeed( OpenGLProgram first_pass, OpenGLProgram subdivide, int pattern_mask, int subdivisions)
    {
        gl = GLFuncProviderFactory.getGLFuncProvider();
        m_tech_first_pass = first_pass;
        m_tech_subdivide = subdivide;

//        m_constants_lightning_structure(effect,"LightningStructure"),
        m_constants_lightning_structure = new BufferGL();
        m_constants_lightning_structure.initlize(GLenum.GL_UNIFORM_BUFFER, LightningStructure.SIZE, null, GLenum.GL_STREAM_DRAW);
        m_constants_lightning_structure.unbind();

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

    abstract int GetNumVertices(int level);

    void SetChildConstants()
    {
    }

    void SetConstants()
    {
//        m_constants_lightning_structure.set(Structure);

        ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(m_constants_lightning_structure.getBufferSize());
        Structure.store(buffer).flip();
        if(buffer.remaining() != Structure.SIZE)
            throw new IllegalArgumentException();

        m_constants_lightning_structure.update(0, buffer);
        m_constants_lightning_structure.unbind();
        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, LightningRenderer.UNIFORM_LIGHT_STRUCTURE, m_constants_lightning_structure.getBuffer());

        SetChildConstants();
    }

    static void bindFeedback(int programid){
        final String[] varyings =
        {
                "Out_Start", "Out_End","Out_Up", "Out_Level",
        };

        GLFuncProviderFactory.getGLFuncProvider().glTransformFeedbackVaryings(programid, varyings, GLenum.GL_INTERLEAVED_ATTRIBS);
    }

    final void RenderFirstPass(){RenderFirstPass(true); }

    abstract void RenderFirstPass(boolean bindProgram);

    OpenGLProgram GetFirstPassTechnique()
    {
        return m_tech_first_pass;
    }

    OpenGLProgram	GetSubdivideTechnique()
    {
        return m_tech_subdivide;
    }

    int GetPatternMask()
    {
        return m_pattern_mask;
    }

    static GLSLProgram createProgram(String vertfile, String gemoFile, String fragFile, ProgramLinkTask task){
        final String path = "nvidia/lightning/shaders/";
        GLSLProgram program =  GLSLProgram.createProgram(path + vertfile, gemoFile != null? (path + gemoFile):null,
                                                          fragFile != null?(path + fragFile):null,
                                                           null);

        if(task != null){
            if(fragFile != null)
                throw new IllegalArgumentException();

            program.addLinkTask(task);
            program.relink();
        }

        return program;
    }

    static GLSLProgram createProgram(String vertfile, String gemoFile, String fragFile, ProgramLinkTask task, String debugName){
        GLSLProgram program = createProgram(vertfile, gemoFile, fragFile, task);
        program.setName(debugName);
        return program;
    }

//    LightningSeed():
//    m_constants_lightning_structure(0,"LightningStructure")
//    {
//    }
//    LightningSeed(ID3D10Effect* effect, ID3D10EffectTechnique* first_pass, ID3D10EffectTechnique* subdivide_pass,  int pattern_mask, unsigned int subdivisions);
//    virtual ~LightningSeed();
}
