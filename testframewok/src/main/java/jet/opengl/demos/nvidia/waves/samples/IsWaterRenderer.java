package jet.opengl.demos.nvidia.waves.samples;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.util.CommonUtil;

/**
 * Created by mazhen'gui on 2017/8/2.
 */

final class IsWaterRenderer implements Disposeable{
    private IsWaterRenderProgram m_waterRenderProgram;
    private CTerrainGenerator m_waterVB;
    private TextureSampler[] water_textures;


    void initlize(CTerrainGenerator m_terrainVB, Texture2D depthmap_texture){
        m_waterVB = m_terrainVB;
        m_waterRenderProgram = new IsWaterRenderProgram("nvidia/WaveWorks/shaders/");

        water_textures = new TextureSampler[7];
        water_textures[0] = new TextureSampler(0, 0);  // g_HeightfieldTexture;
        water_textures[1] = new TextureSampler(0, IsSamplers.g_SamplerDepthAnisotropic);  // g_DepthTexture;
        water_textures[2] = new TextureSampler(0, IsSamplers.g_SamplerLinearWrap);  // g_WaterBumpTexture;
        water_textures[3] = new TextureSampler(0, IsSamplers.g_SamplerLinearClamp);  // g_RefractionDepthTextureResolved;
        water_textures[4] = new TextureSampler(0, IsSamplers.g_SamplerLinearClamp);  // g_ReflectionTexture;
        water_textures[5] = new TextureSampler(0, IsSamplers.g_SamplerLinearClamp);  // g_RefractionTexture;
        water_textures[6] = new TextureSampler(depthmap_texture.getTexture(), IsSamplers.g_SamplerLinearWrap);  // g_DepthMapTexture;
    }

    void render(IsParameters params){
        m_waterRenderProgram.enable(params, water_textures);
        if(params.g_Wireframe)
            m_waterRenderProgram.setupColorPass();
        else
            m_waterRenderProgram.setupWaterPatchPass();

        m_waterVB.draw(0, true);

        // reset the state.
        m_waterRenderProgram.disable();
    }

    void setWaterBump(Texture2D waterBump)  { water_textures[2].textureID = waterBump.getTexture();}
    void setShadowMap(Texture2D shadowMap) { water_textures[1].textureID = shadowMap.getTexture();}
    void setRefractionDepth(Texture2D refractionDepth) {water_textures[3].textureID = refractionDepth.getTexture();}
    void setRefractionColor(Texture2D refractionColor) {water_textures[5].textureID = refractionColor.getTexture();}
    void setReflectionColor(Texture2D reflectionnColor) {water_textures[4].textureID = reflectionnColor.getTexture();}

    @Override
    public void dispose() {
        CommonUtil.safeRelease(m_waterRenderProgram);
    }
}
