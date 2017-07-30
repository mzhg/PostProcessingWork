package jet.opengl.desktop.lwjgl;

import com.nvidia.developer.opengl.app.NvAppBase;
import com.nvidia.developer.opengl.app.NvEGLConfiguration;
import com.nvidia.developer.opengl.utils.NvGfxAPIVersion;
import com.nvidia.developer.opengl.utils.NvImage;

import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AIFace;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AIVector3D;
import org.lwjgl.assimp.Assimp;
import org.lwjgl.opengl.NVCommandList;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import jet.opengl.demos.nvidia.waves.samples.OceanCSDemo;
import jet.opengl.postprocessing.util.BufferUtils;
import jet.opengl.postprocessing.util.DebugTools;
import jet.opengl.postprocessing.util.FileLoader;
import jet.opengl.postprocessing.util.FileUtils;

/**
 * Created by mazhen'gui on 2017/4/12.
 */

public class DemoApp {

    static {
        System.setProperty("jet.opengl.postprocessing.debug", "true");
    }

    public static void run(NvAppBase app){
        NvEGLConfiguration config = new NvEGLConfiguration();
        app.configurationCallback(config);

        LwjglApp baseApp = new LwjglApp();
        GLContextConfig glconfig = baseApp.getGLContextConfig();
        glconfig.alphaBits = config.alphaBits;
        glconfig.depthBits = config.depthBits;
        glconfig.stencilBits = config.stencilBits;

        glconfig.redBits = config.redBits;
        glconfig.greenBits = config.greenBits;
        glconfig.blueBits = config.blueBits;
        glconfig.debugContext = config.debugContext;
        glconfig.multiSamplers = config.multiSamplers;
        baseApp.registerGLEventListener(app);
        baseApp.registerGLFWListener(new InputAdapter(app, app, app));
        app.setGLContext(baseApp);
        baseApp.start();
    }

    public static void main(String[] args) {
//        LogUtil.setLoggerLevel(LogUtil.LogType.NV_FRAMEWROK, Level.OFF);
//        LogUtil.setLoggerLevel(LogUtil.LogType.DEFAULT, Level.OFF);

        final String path = "app\\src\\main\\assets\\";
        FileUtils.setIntenalFileLoader(new FileLoader() {
            @Override
            public InputStream open(String file) throws FileNotFoundException {
                if(file.contains(path)){  // Not safe
                    return new FileInputStream(file);
                }
                return new FileInputStream(path + file);
            }

            @Override
            public String getParent(String file) {
                if(file.contains(path)){
                    return new File(file).getParent();
                }
                return new File(path + file).getParent();
            }

            @Override
            public String getCanonicalPath(String file) throws IOException {
                if(file.contains(path)){
                    return new File(file).getCanonicalPath();
                }
                return new File(path + file).getCanonicalPath();
            }

            @Override
            public boolean exists(String file) {
                if(file.contains(path)){
                    return new File(file).exists();
                }

                return new File(path + file).exists();
            }
        });

        NvImage.setAPIVersion(NvGfxAPIVersion.GL4_4);
        run(new OceanCSDemo());

        if(true) return;

        File file = new File("E:\\SDK\\Rain\\Direct3D\\Media\\Bridge\\Bridge.x");
        if(file.exists() == false)
            throw new IllegalArgumentException();
        AIScene scene = Assimp.aiImportFile(file.getAbsolutePath(), 0);
        if (scene == null) {
            throw new IllegalStateException(Assimp.aiGetErrorString());
        }
        int numMesh = scene.mNumMeshes();
        PointerBuffer meshesBuffer  =  scene.mMeshes();

        for(int i = 0; i < numMesh; i++){
            saveMeshData(AIMesh.create(meshesBuffer.get(i)), i);
        }

        Assimp.aiReleaseImport(scene);
    }

    static void saveMeshData(AIMesh mesh, int index){
        AIVector3D.Buffer vertices = mesh.mVertices();
        ByteBuffer verticesBuffer = MemoryUtil.memByteBuffer(vertices.address(), AIVector3D.SIZEOF * vertices.remaining());
        try {
            DebugTools.write(verticesBuffer, "E:\\SDK\\Rain\\Direct3D\\Media\\Bridge\\Bridge_" + index + "_vertice.dat", false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Vertice sizeof = " + AIVector3D.SIZEOF * vertices.remaining());

        AIVector3D.Buffer normals = mesh.mNormals();
        ByteBuffer normalsBuffer = MemoryUtil.memByteBuffer(normals.address(), AIVector3D.SIZEOF * normals.remaining());
        try {
            DebugTools.write(normalsBuffer, "E:\\SDK\\Rain\\Direct3D\\Media\\Bridge\\Bridge_" + index + "_normal.dat", false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        AIVector3D.Buffer tangents = mesh.mTangents();
        if(tangents != null) {
            ByteBuffer tangentsBuffer = MemoryUtil.memByteBuffer(tangents.address(), AIVector3D.SIZEOF * tangents.remaining());
            try {
                DebugTools.write(tangentsBuffer, "E:\\SDK\\Rain\\Direct3D\\Media\\Bridge\\Bridge_" + index + "_tangent.dat", false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        AIVector3D.Buffer texcoords = mesh.mTextureCoords(0);
        if(texcoords != null){
            ByteBuffer texcoordsBuffer = MemoryUtil.memByteBuffer(texcoords.address(), AIVector3D.SIZEOF * texcoords.remaining());
            try {
                DebugTools.write(texcoordsBuffer, "E:\\SDK\\Rain\\Direct3D\\Media\\Bridge\\Bridge_" + index + "_texcoord.dat", false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        int faceCount = mesh.mNumFaces();
        if(faceCount == 0)
            return;

        int elementCount = faceCount * 3;
        int[] elementArrayBufferData = new int[elementCount];

        AIFace.Buffer facesBuffer = mesh.mFaces();
        for (int i = 0; i < faceCount; ++i) {
            AIFace face = facesBuffer.get(i);
            if (face.mNumIndices() != 3) {
                throw new IllegalStateException("AIFace.mNumIndices() != 3");
            }
//            elementArrayBufferData.put(face.mIndices());
            IntBuffer indices = face.mIndices();
            elementArrayBufferData[i * 3 + 0] = indices.get();
            elementArrayBufferData[i * 3 + 1] = indices.get();
            elementArrayBufferData[i * 3 + 2] = indices.get();
        }

        try {
            ByteBuffer bytes = BufferUtils.createByteBuffer(elementArrayBufferData.length * 4);
            IntBuffer shorts = bytes.asIntBuffer();
            shorts.put(elementArrayBufferData).flip();
            DebugTools.write(bytes, "E:\\SDK\\Rain\\Direct3D\\Media\\Bridge\\Bridge_" + index + "_indices.dat", false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
