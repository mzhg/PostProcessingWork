package jet.opengl.demos.nvidia.face.sample;

import com.nvidia.developer.opengl.app.NvInputHandler_CameraFly;
import com.nvidia.developer.opengl.utils.NvImage;

import org.lwjgl.util.vector.Vector3f;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;

import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureDataDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.FileLoader;
import jet.opengl.postprocessing.util.FileUtils;

/**
 * Created by mazhen'gui on 2017/9/7.
 */

interface CScene {
    String MODEL_PATH = "E:\\SDK\\FaceWorks\\samples\\media\\";

    FileLoader g_SceneFileLoader = new FileLoader() {
        @Override
        public InputStream open(String file) throws FileNotFoundException {
            return new FileInputStream(file);
        }

        @Override
        public String getCanonicalPath(String file) throws IOException {
            return new File(file).getCanonicalPath();
        }

        @Override
        public boolean exists(String file) {
            return new File(file).exists();
        }

        @Override
        public String resolvePath(String file) {
            return file;
        }
    };

    void Init();
    void Release();

    NvInputHandler_CameraFly Camera();

    void GetBounds(Vector3f pPosMin, Vector3f pPosMax);
    void GetMeshesToDraw(List<MeshToDraw> pMeshesToDraw);

    static Texture2D loadDDSTexture(String name){
        int texture = 0;
        try {
            texture = NvImage.uploadTextureFromDDSFile(MODEL_PATH + name);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return TextureUtils.createTexture2D(GLenum.GL_TEXTURE_2D, texture);
    }

    static Texture2D loadCubeTexture(String name){
        int texture = 0;
        try {
            texture = NvImage.uploadTextureFromDDSFile(MODEL_PATH + name);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return TextureUtils.createTexture2D(GLenum.GL_TEXTURE_CUBE_MAP, texture);
    }

    static Texture2D loadTexture(String name){
        try {
            FileLoader old = FileUtils.g_IntenalFileLoader;
            FileUtils.setIntenalFileLoader(g_SceneFileLoader);
            Texture2D result =  TextureUtils.createTexture2DFromFile(MODEL_PATH + name, true);
            FileUtils.setIntenalFileLoader(old);  // reset to defualt.
            return result;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return  null;
    }

    static int GetTextureSize(Texture2D tex){
        return (tex.getWidth() + tex.getHeight())/2;
    }

    static Texture2D Create1x1Texture(float r, float g, float b, boolean linear){
        ByteBuffer pixels = CacheBuffer.getCachedByteBuffer(4);
        pixels.put((byte)((Math.min(Math.max(r, 0.0f), 1.0f) * 255.0f + 0.5f)));
        pixels.put((byte)((Math.min(Math.max(g, 0.0f), 1.0f) * 255.0f + 0.5f)));
        pixels.put((byte)((Math.min(Math.max(b, 0.0f), 1.0f) * 255.0f + 0.5f)));
        pixels.put((byte)(255));
        pixels.flip();

        TextureDataDesc initData = new TextureDataDesc(GLenum.GL_RGBA, linear ? GLenum.GL_UNSIGNED_BYTE : GLenum.GL_BYTE, pixels);
        Texture2DDesc texture2DDesc = new Texture2DDesc(1,1, linear ? GLenum.GL_RGBA8: GLenum.GL_SRGB8_ALPHA8);
        return TextureUtils.createTexture2D(texture2DDesc, initData);
    }

    static Texture2D Create1x1Texture(float r, float g, float b){
        return Create1x1Texture(r, g, b, false);
    }
}
