package nv.samples.smoke;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.shader.ProgramLinkTask;
import jet.opengl.postprocessing.shader.ShaderLoader;
import jet.opengl.postprocessing.shader.ShaderSourceItem;
import jet.opengl.postprocessing.shader.ShaderType;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by Administrator on 2018/7/8 0008.
 */

final class VoxelizerProgram extends GLSLProgram{
    private int m_WorldViewProjection = -1;
    private int m_projSpacePixDim = -1;
    private int m_gridDim = -1;
    private int m_recTimeStep= -1;
    private int m_sliceIdx = -1;
    private int m_sliceZ = -1;
    private int m_velocityMultiplier = -1;

    private Runnable m_BlendState;
    private Runnable m_RasterizerState;
    private Runnable m_DepthStencilState;

    public void setBlendState(Runnable blendState) { m_BlendState = blendState;}
    public void setRasterizerState(Runnable rasterizerState) { m_RasterizerState = rasterizerState;}
    public void setDepthStencilState(Runnable depthStencilState) {m_DepthStencilState = depthStencilState;}

    @Override
    public void enable() {
        super.enable();

        m_BlendState.run();
        m_RasterizerState.run();
        m_DepthStencilState.run();
    }

    VoxelizerProgram(String vert, String frag){
        final String path = "nvidia/Smoke/shaders/";
        try {
            setSourceFromFiles(path + vert, path + frag);
        } catch (IOException e) {
            e.printStackTrace();
        }

        initUniforms();
    }

    VoxelizerProgram(String vertfile, String gemofile, String fragfile, Macro... macros){
        this(vertfile, gemofile, fragfile, null, macros);
    }

    VoxelizerProgram(String vertfile, String gemofile, String fragfile, ProgramLinkTask task, Macro ... macros){
        ShaderSourceItem vs_item = new ShaderSourceItem();
        ShaderSourceItem gs_item = null;
        ShaderSourceItem ps_item = null;

        try {
            final String folder = "nvidia/Smoke/shaders/";
            vs_item.source = ShaderLoader.loadShaderFile(folder + vertfile, false);
            vs_item.type = ShaderType.VERTEX;
            vs_item.macros = macros;

            if(gemofile != null){
                gs_item = new ShaderSourceItem();
                gs_item.source = ShaderLoader.loadShaderFile(folder + gemofile, false);
                gs_item.type = ShaderType.GEOMETRY;
                gs_item.macros = macros;
            }

            if(fragfile != null){
                ps_item = new ShaderSourceItem();
                ps_item.source = ShaderLoader.loadShaderFile(folder + fragfile, false);
                ps_item.type = ShaderType.FRAGMENT;
                ps_item.macros = macros;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(task != null){
            addLinkTask(task);
        }

        setSourceFromStrings(vs_item, gs_item, ps_item);
        initUniforms();
    }

    private void setWorldViewProjection(Matrix4f mvp){
        if(m_WorldViewProjection >= 0){
            gl.glUniformMatrix4fv(m_WorldViewProjection, false, CacheBuffer.wrap(mvp));
        }
    }

    private void setProjSpacePixDim(Vector2f v){
        if(m_projSpacePixDim >=0){
            gl.glUniform2f(m_projSpacePixDim, v.x, v.y);
        }
    }

    private void setGridDim(Vector3f v){
        if(m_gridDim >=0){
            gl.glUniform3f(m_gridDim, v.x, v.y, v.z);
        }
    }

    private void setRecTimeStep(float f){
        if(m_recTimeStep >=0)
            gl.glUniform1f(m_recTimeStep, f);
    }

    private void setSliceIdx(int idx){
        if(m_sliceIdx >=0)
            gl.glUniform1i(m_sliceIdx, idx);
    }

    private void setSliceZ(float f){
        if(m_sliceZ >=0)
            gl.glUniform1f(m_sliceZ, f);
    }

    private void setVelocityMultiplier(float f){
        if(m_velocityMultiplier >=0)
            gl.glUniform1f(m_velocityMultiplier, f);
    }

    private void initUniforms() {
        m_WorldViewProjection = getUniformLocation("WorldViewProjection");
        m_projSpacePixDim = getUniformLocation("projSpacePixDim");
        m_gridDim = getUniformLocation("gridDim");
        m_recTimeStep = getUniformLocation("recTimeStep");
        m_sliceIdx = getUniformLocation("sliceIdx");
        m_sliceZ = getUniformLocation("sliceZ");
        m_velocityMultiplier = getUniformLocation("velocityMultiplier");
    }


}
