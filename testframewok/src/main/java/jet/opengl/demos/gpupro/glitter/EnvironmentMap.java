package jet.opengl.demos.gpupro.glitter;

import java.awt.Image;
import java.io.IOException;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.ImageData;
import jet.opengl.postprocessing.texture.TextureUtils;

import static jet.opengl.postprocessing.common.GLenum.GL_TEXTURE_CUBE_MAP_POSITIVE_X;

final class EnvironmentMap {
    private int cubeMap;
    private GLFuncProvider gl;

    //generate environment map
    //takes a vector of paths to each face in the following order:
    //right, left, top, bottom, back, front
    EnvironmentMap(String[] faceTextures) {
        gl = GLFuncProviderFactory.getGLFuncProvider();
        //create cubemap
        cubeMap = gl.glGenTexture();
        gl.glBindTexture(GLenum.GL_TEXTURE_CUBE_MAP, cubeMap);

        //load environment map texture for each face
        for (int i = 0; i < 6; i++) {
            /*int width, height;
            unsigned char* image = NULL;
            image = SOIL_load_image(faceTextures[i].c_str(), &width, &height, 0, SOIL_LOAD_RGB);
            if (image == NULL) {
                std::cout << "EnvironmentMap could not load texture " << faceTextures[i] << std::endl;
            }*/

            try {
                ImageData data = gl.getNativeAPI().load(faceTextures[i], false);
                gl.glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, data.internalFormat, data.width, data.height, 0,
                        TextureUtils.measureFormat(data.internalFormat), TextureUtils.measureDataType(data.internalFormat), data.pixels);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //set cubemap settings
        gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);
        gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR);
        gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_WRAP_R, GLenum.GL_CLAMP_TO_EDGE);
        gl.glBindTexture(GLenum.GL_TEXTURE_CUBE_MAP, 0);
    }

    void BindBuffers(GLSLProgram envShader) {
        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glUniform1i(gl.glGetUniformLocation(envShader.getProgram(), "envMap"), 0);
        gl.glBindTexture(GLenum.GL_TEXTURE_CUBE_MAP, cubeMap);
    }

    void BindBuffers(GLSLProgram envShader, int textureUnit) {
        gl.glActiveTexture(GLenum.GL_TEXTURE0 + textureUnit);
        gl.glUniform1i(gl.glGetUniformLocation(envShader.getProgram(), "envMap"), textureUnit);
        gl.glBindTexture(GLenum.GL_TEXTURE_CUBE_MAP, cubeMap);
    }
}
