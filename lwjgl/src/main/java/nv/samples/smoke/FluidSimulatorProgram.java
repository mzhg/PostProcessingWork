package nv.samples.smoke;

import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.shader.ProgramLinkTask;
import jet.opengl.postprocessing.shader.ShaderLoader;
import jet.opengl.postprocessing.shader.ShaderSourceItem;
import jet.opengl.postprocessing.shader.ShaderType;

/**
 * Created by Administrator on 2018/6/30 0030.
 */

final class FluidSimulatorProgram  extends GLSLProgram{
    private int m_colorLoc = -1;
    private int m_vortConfinementScaleLoc = -1;
    private int m_textureWidthLoc = -1;
    private int m_decayLoc = -1;
    private int m_textureHeightLoc = -1;
    private int m_doVelocityAttenuationLoc = -1;
    private int m_drawTextureNumberLoc = -1;
    private int m_liquidHeightLoc = -1;
    private int m_maxDensityDecayLoc = -1;
    private int m_radiusLoc = -1;
    private int m_obstBoxVelocityLoc = -1;
    private int m_timestepLoc = -1;
    private int m_obstBoxLBDcornerLoc = -1;
    private int m_treatAsLiquidVelocityLoc = -1;
    private int m_centerLoc = -1;
    private int m_viscosityLoc = -1;
    private int m_obstBoxRTUcornerLoc = -1;
    private int m_fluidTypeLoc = -1;
    private int m_gravityLoc = -1;
    private int m_textureDepthLoc = -1;
    private int m_rhoLoc = -1;
    private int m_advectAsTemperatureLoc = -1;
    private int m_maxDensityAmountLoc = -1;
    private int m_temperatureLossLoc = -1;

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

    FluidSimulatorProgram(String vert, String frag){
        final String path = "nvidia/Smoke/shaders/";
        try {
            setSourceFromFiles(path + vert, path + frag);
        } catch (IOException e) {
            e.printStackTrace();
        }

        initUniforms();
    }

    FluidSimulatorProgram(String vertfile, String gemofile, String fragfile, Macro ... macros){
        this(vertfile, gemofile, fragfile, null, macros);
    }

    FluidSimulatorProgram(String vertfile, String gemofile, String fragfile, ProgramLinkTask task, Macro ... macros){
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

    private void initUniforms(){
        // TODO Don't forget initlize the program here.
        m_colorLoc = getUniformLocation( "color");
        m_vortConfinementScaleLoc = getUniformLocation( "vortConfinementScale");
        m_textureWidthLoc = getUniformLocation( "textureWidth");
        m_decayLoc = getUniformLocation( "decay");
        m_textureHeightLoc = getUniformLocation( "textureHeight");
        m_doVelocityAttenuationLoc = getUniformLocation( "doVelocityAttenuation");
        m_drawTextureNumberLoc = getUniformLocation( "drawTextureNumber");
        m_liquidHeightLoc = getUniformLocation( "liquidHeight");
        m_maxDensityDecayLoc = getUniformLocation( "maxDensityDecay");
        m_radiusLoc = getUniformLocation( "radius");
        m_obstBoxVelocityLoc = getUniformLocation( "obstBoxVelocity");
        m_timestepLoc = getUniformLocation( "timestep");
        m_obstBoxLBDcornerLoc = getUniformLocation( "obstBoxLBDcorner");
        m_treatAsLiquidVelocityLoc = getUniformLocation( "treatAsLiquidVelocity");
        m_centerLoc = getUniformLocation( "center");
        m_viscosityLoc = getUniformLocation( "viscosity");
        m_obstBoxRTUcornerLoc = getUniformLocation( "obstBoxRTUcorner");
        m_fluidTypeLoc = getUniformLocation( "fluidType");
        m_gravityLoc = getUniformLocation( "gravity");
        m_textureDepthLoc = getUniformLocation( "textureDepth");
        m_rhoLoc = getUniformLocation( "rho");
        m_advectAsTemperatureLoc = getUniformLocation( "advectAsTemperature");
        m_maxDensityAmountLoc = getUniformLocation( "maxDensityAmount");
        m_temperatureLossLoc = getUniformLocation( "temperatureLoss");
    }
    
    public void setColor(Vector4f v) { if(m_colorLoc >=0)gl.glUniform4f(m_colorLoc, v.x, v.y, v.z, v.w);}
    public void setVortConfinementScale(float f) { if(m_vortConfinementScaleLoc >=0)gl.glUniform1f(m_vortConfinementScaleLoc, f);}
    public void setTextureWidth(float f) { if(m_textureWidthLoc >=0)gl.glUniform1f(m_textureWidthLoc, f);}
    public void setDecay(float f) { if(m_decayLoc >=0)gl.glUniform1f(m_decayLoc, f);}
    public void setTextureHeight(float f) { if(m_textureHeightLoc >=0)gl.glUniform1f(m_textureHeightLoc, f);}
    public void setVelocityAttenuation(boolean b) { if(m_doVelocityAttenuationLoc >=0)gl.glUniform1i(m_doVelocityAttenuationLoc, b ? 1 : 0);}
    public void setDrawTextureNumber(int i) { if(m_drawTextureNumberLoc >=0)gl.glUniform1i(m_drawTextureNumberLoc, i);}
    public void setM_liquidHeight(float f) { if(m_liquidHeightLoc >=0)gl.glUniform1f(m_liquidHeightLoc, f);}
    public void setM_maxDensityDecay(float f) { if(m_maxDensityDecayLoc >=0)gl.glUniform1f(m_maxDensityDecayLoc, f);}
    public void setRadius(float f) { if(m_radiusLoc >=0)gl.glUniform1f(m_radiusLoc, f);}
    public void setObstBoxVelocity(Vector4f v) { if(m_obstBoxVelocityLoc >=0)gl.glUniform4f(m_obstBoxVelocityLoc, v.x, v.y, v.z, v.w);}
    public void setTimestep(float f) { if(m_timestepLoc >=0)gl.glUniform1f(m_timestepLoc, f);}
    public void setObstBoxLBDcorner(Vector3f v) { if(m_obstBoxLBDcornerLoc >=0)gl.glUniform3f(m_obstBoxLBDcornerLoc, v.x, v.y, v.z);}
    public void setTreatAsLiquidVelocity(boolean b) { if(m_treatAsLiquidVelocityLoc >=0)gl.glUniform1i(m_treatAsLiquidVelocityLoc, b ? 1 : 0);}
    public void setCenter(Vector3f v) { if(m_centerLoc >=0)gl.glUniform3f(m_centerLoc, v.x, v.y, v.z);}
    public void setViscosity(float f) { if(m_viscosityLoc >=0)gl.glUniform1f(m_viscosityLoc, f);}
    public void setObstBoxRTUcorner(Vector3f v) { if(m_obstBoxRTUcornerLoc >=0)gl.glUniform3f(m_obstBoxRTUcornerLoc, v.x, v.y, v.z);}
    public void setFluidType(int i) { if(m_fluidTypeLoc >=0)gl.glUniform1i(m_fluidTypeLoc, i);}
    public void setGravity(Vector3f v) { if(m_gravityLoc >=0)gl.glUniform3f(m_gravityLoc, v.x, v.y, v.z);}
    public void setTextureDepth(float f) { if(m_textureDepthLoc >=0)gl.glUniform1f(m_textureDepthLoc, f);}
    public void setRho(float f) { if(m_rhoLoc >=0)gl.glUniform1f(m_rhoLoc, f);}
    public void setAdvectAsTemperature(boolean b) { if(m_advectAsTemperatureLoc >=0)gl.glUniform1i(m_advectAsTemperatureLoc, b ? 1 : 0);}
    public void setMaxDensityAmount(float f) { if(m_maxDensityAmountLoc >=0)gl.glUniform1f(m_maxDensityAmountLoc, f);}
    public void setTemperatureLoss(float f) { if(m_temperatureLossLoc >=0)gl.glUniform1f(m_temperatureLossLoc, f);}
}
