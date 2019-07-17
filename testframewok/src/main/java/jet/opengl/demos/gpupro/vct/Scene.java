package jet.opengl.demos.gpupro.vct;

import com.nvidia.developer.opengl.app.NvInputTransformer;
import com.nvidia.developer.opengl.models.DrawMode;
import com.nvidia.developer.opengl.models.GLVAO;
import com.nvidia.developer.opengl.models.QuadricBuilder;
import com.nvidia.developer.opengl.models.QuadricMesh;
import com.nvidia.developer.opengl.models.QuadricSphere;
import com.nvidia.developer.opengl.models.obj.NvGLModel;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.FileLoader;
import jet.opengl.postprocessing.util.FileUtils;
import jet.opengl.postprocessing.util.StackInt;

abstract class Scene {

    final List<MeshRenderer> renderers = new ArrayList<>();
    final List<PointLight> pointLights = new ArrayList<>();

    final Matrix4f projection = new Matrix4f();
    final Matrix4f view = new Matrix4f();

    final NvInputTransformer transformer;

    Scene(NvInputTransformer transformer){
        this.transformer = transformer;
    }

    abstract void onActive();

    /// <summary> Updates the scene. Is called pre-render. </summary>
    abstract void update(float dt);

    void renderScene(GLSLProgram program){
        for (int i = 0; i < renderers.size(); ++i) {
            MeshRenderer mesh = renderers.get(i);
            if(mesh.enabled){
                if(mesh.materialSetting != null){
                    mesh.materialSetting.Upload(program, false);
                }

                mesh.render(program);
            }
        }
    }

    /// <summary> Initializes the scene. Is called after construction, but before update and render. </summary>
    void onResize(int viewportWidth, int viewportHeight){
        Matrix4f.perspective(60, (float)viewportWidth/viewportHeight, 0.1f, 100.f, projection);
    }

    static final float scale = 1;
    private static Mesh[] corner_vaos = new Mesh[6];
    private static StackInt corner_vbos = new StackInt(6);

    private static Mesh createMesh(Vector3f v0, Vector3f v1, Vector3f v2, Vector3f v3, Vector3f normal){
        final GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();

        int VAO = gl.glGenVertexArray();
        gl.glBindVertexArray(VAO);
        int VB = gl.glGenBuffer();
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, VB);

        FloatBuffer buffer = CacheBuffer.getCachedFloatBuffer(3 * 8);
        v0.store(buffer);normal.store(buffer);
        v1.store(buffer);normal.store(buffer);
        v2.store(buffer);normal.store(buffer);
        v3.store(buffer);normal.store(buffer);
        buffer.flip();

        gl.glBufferData(GLenum.GL_ARRAY_BUFFER, buffer,GLenum.GL_STATIC_DRAW);
        gl.glEnableVertexAttribArray(0);
        gl.glVertexAttribPointer(0, 3, GLenum.GL_FLOAT, false, Vector3f.SIZE*2, 0);
        gl.glEnableVertexAttribArray(1);
        gl.glVertexAttribPointer(1, 3, GLenum.GL_FLOAT, false, Vector3f.SIZE*2, Vector3f.SIZE);

        gl.glBindVertexArray(0);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);

        corner_vbos.push(VB);
        return new Mesh(VAO);
    }

    static Mesh[] buildConrer(){
        if(corner_vaos[0] == null){
            // six plane

            // front face  CCW
            final Vector3f v0 = new Vector3f(-scale,-scale,+scale);
            final Vector3f v1 = new Vector3f(+scale,-scale,+scale);
            final Vector3f v2 = new Vector3f(-scale,+scale,+scale);
            final Vector3f v3 = new Vector3f(+scale,+scale,+scale);

            // back face CCW
            final Vector3f v4 = new Vector3f(-scale,-scale,-scale);
            final Vector3f v5 = new Vector3f(+scale,-scale,-scale);
            final Vector3f v6 = new Vector3f(-scale,+scale,-scale);
            final Vector3f v7 = new Vector3f(+scale,+scale,-scale);

            // Generating the cube faces.
            corner_vaos[0] = createMesh(v0, v1, v2, v3, new Vector3f(0,0,-1));  // Front face
            corner_vaos[1] = createMesh(v4, v5, v6, v7, new Vector3f(0,0,+1));  // Back face
            corner_vaos[2] = createMesh(v0, v4, v2, v6, new Vector3f(+1,0,0));  // Left face
            corner_vaos[3] = createMesh(v5, v1, v7, v3, new Vector3f(-1,0,0));  // Right face
            corner_vaos[4] = createMesh(v3, v2, v7, v6, new Vector3f(0,-1,0));  // Top face
            corner_vaos[5] = createMesh(v0, v1, v4, v5, new Vector3f(0,+1,0));  // Bottom face
        }

        return corner_vaos;
    }

    static Mesh g_SphereMesh;

    static Mesh createSphere(){
        if(g_SphereMesh == null){
            QuadricBuilder builder = new QuadricBuilder();
            builder.setXSteps(30).setYSteps(30);
            builder.setDrawMode(DrawMode.FILL);
            builder.setCenterToOrigin(true);
            builder.setPostionLocation(0);
            builder.setNormalLocation(1);
            builder.setAutoGenTexCoord(false);
            builder.setGenTexCoord(false);

            GLVAO sphere = new QuadricMesh(builder, new QuadricSphere(1)).getModel().genVAO();
            g_SphereMesh = new Mesh(sphere);
        }

        return g_SphereMesh;
    }

    static final HashMap<String, Mesh> g_ObjMeshes = new HashMap<>();

    static Mesh loadObj(String filename){
        Mesh mesh = g_ObjMeshes.get(filename);
        if(mesh == null){
//            FileLoader old = FileUtils.g_IntenalFileLoader;
//            FileUtils.setIntenalFileLoader(FileLoader.g_DefaultFileLoader);

            NvGLModel model = new NvGLModel();
            model.loadModelFromFile(filename);
            model.initBuffers(false);

            mesh = new Mesh(model);
            g_ObjMeshes.put(filename, mesh);
//            FileUtils.setIntenalFileLoader(old);
        }

        return mesh;
    }

    static void release(){
        final GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        for(Mesh mesh : corner_vaos){
            if(mesh != null)
                mesh.dispose();
        }

        Arrays.fill(corner_vaos, null);

        for(int i = 0; i < corner_vbos.size(); i++){
            int VB = corner_vbos.get(i);
            if(VB != 0){
                gl.glDeleteBuffer(VB);
            }
        }

        corner_vbos.clear();

        for(Mesh mesh: g_ObjMeshes.values()){
            if(mesh != null)
                mesh.dispose();
        }

        g_ObjMeshes.clear();
    }
}
