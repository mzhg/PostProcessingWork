package jet.opengl.renderer.Unreal4.atmosphere;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Properties;

import jet.opengl.renderer.Unreal4.FScene;
import jet.opengl.renderer.Unreal4.FViewInfo;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.Texture3D;
import jet.opengl.postprocessing.texture.Texture3DDesc;
import jet.opengl.postprocessing.texture.TextureDataDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.DebugTools;

public class AtmosphereRendering {

    private static final int
    E_EnableAll = 0,
    E_DisableSunDisk = 1,
    E_DisableGroundScattering = 2,
    E_DisableLightShaft = 4, // Light Shaft shadow
    E_DisableSunAndGround = E_DisableSunDisk | E_DisableGroundScattering,
    E_DisableSunAndLightShaft = E_DisableSunDisk | E_DisableLightShaft,
    E_DisableGroundAndLightShaft = E_DisableGroundScattering | E_DisableLightShaft,
    E_DisableAll = E_DisableSunDisk | E_DisableGroundScattering | E_DisableLightShaft,
    E_RenderFlagMax = E_DisableAll + 1,
    E_LightShaftMask = (~E_DisableLightShaft);

    private static final String PRECOMPUTED_DATA_PATH = "E:/textures/AtmosphereRendering/";
    private static final String TRANSMITTANCE_PATH = PRECOMPUTED_DATA_PATH + "Transmittance.dat";
    private static final String IRRADIANCE_PATH = PRECOMPUTED_DATA_PATH + "Irradiance.dat";
    private static final String INSCATTER_PATH = PRECOMPUTED_DATA_PATH + "Inscatter.dat";
    private static final String CONFIG_PATH = PRECOMPUTED_DATA_PATH + "Config.txt";

    private final HashMap<Integer, GLSLProgram> mAtmosphereRenderingPrograms = new HashMap<>();
    private Texture2D AtmosphereTransmittance;
    private Texture2D AtmosphereIrradiance;
    private Texture3D AtmosphereInscatter;

    private boolean m_printOnce;

    public void RenderAtmosphere(FViewInfo view, FScene scene){
        if(!loadPrecomputedData()){
            FAtmosphericFogSceneInfo sceneInfo = new FAtmosphericFogSceneInfo(view.AtmospherePrecomputeParams);
            sceneInfo.PrecomputeTextures();
            savePrecomputedData(sceneInfo);

            AtmosphereTransmittance = (Texture2D) sceneInfo.AtmosphereTextures.AtmosphereTransmittance;
            AtmosphereIrradiance = (Texture2D) sceneInfo.AtmosphereTextures.AtmosphereIrradiance;
            AtmosphereInscatter = (Texture3D) sceneInfo.AtmosphereTextures.AtmosphereInscatter;
        }

//        GLSLProgram AtmosphereRenderingProgram =
        final GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        gl.glDisable(GLenum.GL_CULL_FACE);
        gl.glEnable(GLenum.GL_BLEND);
        // disable alpha writes in order to preserve scene depth values on PC
        gl.glColorMask(true, true, true, false);
        gl.glBlendEquation(GLenum.GL_FUNC_ADD);
        gl.glBlendFuncSeparate(GLenum.GL_ONE, GLenum.GL_SRC_ALPHA, GLenum.GL_ZERO, GLenum.GL_ONE);
        gl.glDisable(GLenum.GL_DEPTH_TEST);

        GLSLProgram AtmosphereRenderingProgram = getAtmosphereRenderingProgram(scene.RenderFlag);
        AtmosphereRenderingProgram.enable();

        // TODO binding uniforms.
        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3); // TODO

        if(!m_printOnce)
            AtmosphereRenderingProgram.printPrograminfo();
    }

    private GLSLProgram getAtmosphereRenderingProgram(Integer renderFlag){
        GLSLProgram result = mAtmosphereRenderingPrograms.get(renderFlag);
        if(result == null){
            final int RenderFlag = renderFlag.intValue();
            Macro[] macros = {
                new Macro("ATMOSPHERIC_NO_SUN_DISK", (RenderFlag & E_DisableSunDisk)),
                new Macro("ATMOSPHERIC_NO_GROUND_SCATTERING", (RenderFlag & E_DisableGroundScattering)),
                new Macro("ATMOSPHERIC_NO_LIGHT_SHAFT", (RenderFlag & E_DisableLightShaft)),
            };
            result = GLSLProgram.createProgram(FAtmosphericFogSceneInfo.SHADER_PATH + "AtmosphericVertexMain.vert",
                                               FAtmosphericFogSceneInfo.SHADER_PATH + "AtmosphericPixelMain.frag", macros);
            mAtmosphereRenderingPrograms.put(renderFlag, result);
        }

        return result;
    }

    private void savePrecomputedData(FAtmosphericFogSceneInfo sceneInfo){
        Properties properties = new Properties();
        properties.setProperty("transmittance", AtmosphereTransmittance.getWidth() + "," + AtmosphereTransmittance.getHeight());
        properties.setProperty("irradiance", AtmosphereIrradiance.getWidth() + "," + AtmosphereIrradiance.getHeight());
        properties.setProperty("inscatter", AtmosphereInscatter.getWidth() + "," + AtmosphereInscatter.getHeight());

        try {
            File file = new File(CONFIG_PATH);
            file.createNewFile();

            properties.store(new FileWriter(file), null);
        } catch (IOException e) {
            e.printStackTrace();
        }

        DebugTools.saveBinary(sceneInfo.PrecomputeTransmittance, TRANSMITTANCE_PATH);
        DebugTools.saveBinary(sceneInfo.PrecomputeInscatter, INSCATTER_PATH);
        DebugTools.saveBinary(sceneInfo.PrecomputeIrradiance, IRRADIANCE_PATH);
    }

    private boolean loadPrecomputedData(){
        ByteBuffer transmittanceData = DebugTools.loadBinary(TRANSMITTANCE_PATH);
        ByteBuffer irradianceData = DebugTools.loadBinary(IRRADIANCE_PATH);
        ByteBuffer inscatter = DebugTools.loadBinary(INSCATTER_PATH);
        CharSequence condifSources =  DebugTools.loadText(CONFIG_PATH);

        if(transmittanceData == null || irradianceData == null|| inscatter == null || condifSources == null)
            return false;

        Properties properties = new Properties();
        try {
            properties.load(new StringReader(condifSources.toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        {
            int width, height;
            String value = properties.getProperty("transmittance");
            int dot = value.indexOf(',');
            if(dot < 0)
                throw new IllegalStateException("Inner error!");

            width = Integer.parseInt(value.substring(0, dot).trim());
            height = Integer.parseInt(value.substring(dot+1).trim());

            Texture2DDesc desc = new Texture2DDesc(width, height, GLenum.GL_RGBA8);
            TextureDataDesc data = new TextureDataDesc(GLenum.GL_RGBA, GLenum.GL_UNSIGNED_BYTE, transmittanceData);

            AtmosphereTransmittance = TextureUtils.createTexture2D(desc, data);
        }

        {
            int width, height;
            String value = properties.getProperty("irradiance");
            int dot = value.indexOf(',');
            if(dot < 0)
                throw new IllegalStateException("Inner error!");

            width = Integer.parseInt(value.substring(0, dot).trim());
            height = Integer.parseInt(value.substring(dot+1).trim());

            Texture2DDesc desc = new Texture2DDesc(width, height, GLenum.GL_RGBA8);
            TextureDataDesc data = new TextureDataDesc(GLenum.GL_RGBA, GLenum.GL_UNSIGNED_BYTE, irradianceData);

            AtmosphereIrradiance = TextureUtils.createTexture2D(desc, data);
        }

        {
            int width, height, depth;
            String value = properties.getProperty("inscatter");
            int dot0 = value.indexOf(',');
            int dot1 = value.lastIndexOf(',');
            if(dot0 < 0 || dot1 < 0 || dot0 >= dot1)
                throw new IllegalStateException("Inner error!");

            width = Integer.parseInt(value.substring(0, dot0).trim());
            height = Integer.parseInt(value.substring(dot0+1, dot1).trim());
            depth = Integer.parseInt(value.substring(dot1+1).trim());

            Texture3DDesc desc = new Texture3DDesc(width, height, depth, 1, GLenum.GL_RGBA16F);
            TextureDataDesc data = new TextureDataDesc(GLenum.GL_RGBA, GLenum.GL_HALF_FLOAT, inscatter);
            AtmosphereInscatter = TextureUtils.createTexture3D(desc, data);
        }
        return true;
    }
}
