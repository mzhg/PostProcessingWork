package jet.opengl.demos.nvidia.waves.ocean;

import java.io.IOException;

import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureCube;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.FileLoader;
import jet.opengl.postprocessing.util.FileUtils;
import jet.opengl.postprocessing.util.NvImage;

interface OceanConst {
    int MaxNumVessels =           1;
    int MaxNumSpotlights   =     11;

    boolean ENABLE_SHADOWS          = true;

    int ENABLE_GPU_SIMULATION   =1;
    int SPRAY_PARTICLE_SORTING  =1;

    int BitonicSortCSBlockSize  =512;

    int SPRAY_PARTICLE_COUNT    =(BitonicSortCSBlockSize * 256);

    int SprayParticlesCSBlocksSize =256;
    int SimulateSprayParticlesCSBlocksSize =256;

//#define ENABLE_SPRAY_PARTICLES  0

    float kSpotlightShadowResolution = 2048;

    int EmitParticlesCSBlocksSize =256;
    int SimulateParticlesCSBlocksSize =256;
    int PSMPropagationCSBlockSize =16;

    int TransposeCSBlockSize =16;

    String SHADER_PATH = "";

    int DXGI_FORMAT_R32G32B32A32_FLOAT = GLenum.GL_RGBA32F;
    int DXGI_FORMAT_R32G32_FLOAT = GLenum.GL_RG32F;
    int DXGI_FORMAT_R32G32B32_FLOAT = GLenum.GL_RGB32F;
    int DXGI_FORMAT_R32_FLOAT = GLenum.GL_R32F;
    int DXGI_FORMAT_R8G8B8A8_UNORM = GLenum.GL_RGBA8;
    int DXGI_FORMAT_R16G16_UNORM = GLenum.GL_RG16;
    int DXGI_FORMAT_D24_UNORM_S8_UINT = GLenum.GL_DEPTH24_STENCIL8;
    int DXGI_FORMAT_R32_UINT = GLenum.GL_R32UI;
    int DXGI_FORMAT_R8G8B8A8_UNORM_SRGB = GLenum.GL_SRGB8_ALPHA8;
    int DXGI_FORMAT_R16G16B16A16_FLOAT = GLenum.GL_RGBA16F;
    int DXGI_FORMAT_R16_FLOAT = GLenum.GL_R16F;

    // Ocean grid setting
    int BICOLOR_TEX_SIZE			= 256;

    int LOCAL_FOAMMAP_TEX_SIZE	    = 1024;

    float kParticleTTL = 7.5f; // particle lifetime
    float kGravity = 9.81f;
    float kParticleScaleRate = 0.02f;
    float kParticleDrag = 1.f;
    float kPSMOpacityMultiplier = 0.012f;
    float kFadeInTime = 0.4f;
    float kMaxSimulationTimeStep = 0.06f;

    boolean ENABLE_SPRAY_PARTICLES = true;
    String MEDIA_PATH = "E:\\SDK\\WaveWorks\\demo";

    static Texture2D CreateTexture2DFromFileSRGB(String filename){
        NvImage.loadAsSRGB(true);

        FileLoader old = FileUtils.g_IntenalFileLoader;
        try {
            FileUtils.setIntenalFileLoader(FileLoader.g_DefaultFileLoader);
            if(filename.startsWith(".")){
                filename = MEDIA_PATH + filename.substring(1);
            }

            int texture = NvImage.uploadTextureFromDDSFile(filename);
            return TextureUtils.createTexture2D(GLenum.GL_TEXTURE_2D, texture);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            FileUtils.setIntenalFileLoader(old);
        }

        return null;
    }

    static TextureCube CreateTextureCubeFromFileSRGB(String filename){
        NvImage.loadAsSRGB(true);

        FileLoader old = FileUtils.g_IntenalFileLoader;
        try {
            FileUtils.setIntenalFileLoader(FileLoader.g_DefaultFileLoader);
            if(filename.startsWith(".")){
                filename = MEDIA_PATH + filename.substring(1);
            }

            int texture = NvImage.uploadTextureFromDDSFile(filename);
            return TextureUtils.createTextureCube(GLenum.GL_TEXTURE_CUBE_MAP, texture);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            FileUtils.setIntenalFileLoader(old);
        }

        return null;
    }

    static Texture2D CreateTexture2DFromFile(String filename){
        NvImage.loadAsSRGB(false);

        FileLoader old = FileUtils.g_IntenalFileLoader;
        try {
            FileUtils.setIntenalFileLoader(FileLoader.g_DefaultFileLoader);
            if(filename.startsWith(".")){
                filename = MEDIA_PATH + filename.substring(1);
            }

            int texture = NvImage.uploadTextureFromDDSFile(filename);
            return TextureUtils.createTexture2D(GLenum.GL_TEXTURE_2D, texture);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            FileUtils.setIntenalFileLoader(old);
        }

        return null;
    }
}
