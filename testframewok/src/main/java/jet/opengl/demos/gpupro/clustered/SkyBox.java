package jet.opengl.demos.gpupro.clustered;

import com.nvidia.developer.opengl.utils.HDRImage;

import jet.opengl.demos.nvidia.waves.samples.SkyBoxRender;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureCube;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.LogUtil;

final class SkyBox {
    SkyBoxRender m_renderer;

    //Has special depth testing requirements
    void draw(){
        // TODO setup the matrixs.
        m_renderer.setCubemap(skyBoxCubeMap.getTexture());
        m_renderer.draw();
    }

    //Setup functions for skybox mesh (VAO) and textures
    void setup(String skyboxName, boolean isHDR, int resolution){
        String skyBoxFolderPath = "../assets/skyboxes/";
        skyBoxFolderPath += skyboxName;
        String skyBoxFilePath = skyBoxFolderPath + "/" + skyboxName + ".hdr";

        this.resolution = resolution;

        HDRImage image = new HDRImage();
        if (!image.loadHDRIFromFile(skyBoxFilePath)) {
            LogUtil.e(LogUtil.LogType.NV_FRAMEWROK, String.format("Error loading image file '%s'\n", skyBoxFilePath));
        }
        if (!image.convertCrossToCubemap()) {
            LogUtil.e(LogUtil.LogType.NV_FRAMEWROK,"Error converting image to cubemap\n");
        }

        skyBoxCubeMap = createCubemapTexture(image, GLenum.GL_RGB, true);
    }

    TextureCube createCubemapTexture(HDRImage img, int internalformat, boolean filtering) {
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        int tex = gl.glGenTexture();
        gl.glBindTexture(GLenum.GL_TEXTURE_CUBE_MAP, tex);

        gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_MAG_FILTER, filtering ? GLenum.GL_LINEAR : GLenum.GL_NEAREST);
        gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_MIN_FILTER, filtering ? GLenum.GL_LINEAR_MIPMAP_LINEAR : GLenum.GL_NEAREST_MIPMAP_NEAREST);
        gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_WRAP_R, GLenum.GL_CLAMP_TO_EDGE);

        gl.glPixelStorei(GLenum.GL_UNPACK_ALIGNMENT, 1);

        GLCheck.checkError("creating cube map0");
        for(int i=0; i<6; i++) {
            gl.glTexImage2D(GLenum.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0,
                    GLenum.GL_RGB16F, img.getWidth(), img.getHeight(), 0,
                    GLenum.GL_RGB, GLenum.GL_FLOAT, CacheBuffer.wrap(img.getLevel(0, GLenum.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i)));
        }

        gl.glGenerateMipmap(GLenum.GL_TEXTURE_CUBE_MAP);
        gl.glPixelStorei(GLenum.GL_UNPACK_ALIGNMENT, 4);

        return TextureUtils.createTextureCube(GLenum.GL_TEXTURE_CUBE_MAP, tex);
    }

    //Transforming the equirecangular texture to a cubemap format for rendering
//    void fillCubeMapWithTexture(GLSLProgram buildCubeMapShader);

    int resolution;

    //Equirectangular map is not rendered, just an intermediate state
    Texture2D equirectangularMap;
    TextureCube skyBoxCubeMap;
}
