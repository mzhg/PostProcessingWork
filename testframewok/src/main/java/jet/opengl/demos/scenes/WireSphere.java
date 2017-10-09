package jet.opengl.demos.scenes;

import com.nvidia.developer.opengl.app.NvInputTransformer;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;
import java.nio.FloatBuffer;

import jet.opengl.demos.scene.BaseScene;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.util.BufferUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;
import jet.opengl.postprocessing.util.FileUtils;
import jet.opengl.postprocessing.util.Numeric;

/**
 * Created by mazhen'gui on 2017/10/9.
 */

public class WireSphere extends BaseScene {

    private static final String[] model_files = {"Scenes/WireSphere/models/loop","Scenes/WireSphere/models/triangle"};
    private static final float Z_NEAR = 0.4f;
    private static final float Z_FAR = 100.0f;
    private static final float FOV = 90;
    private static float color0[]={0.5f,0.5f,0.8f,1.0f};
    private static float color1[]={0.5f,0.9f,0.5f,1.0f};

    GLSLProgram m_ObjectProg;
    GLSLProgram m_LineProg;
    GLSLProgram m_FloorProg;
    int m_sphereVBO;
    int[] m_object = new int[2];
    int[] m_objectFileSize = new int[2];
    int m_floorVBO;
    float m_lineWidth = 2.0f;
    boolean m_autoSpin = true;

    @Override
    public void onCreate(Object prevSavedData) {
        mNVApp.getInputTransformer().setTranslationVec(new Vector3f(0.0f, 0.0f, -3.0f));
        mNVApp.getInputTransformer().setRotationVec(new Vector3f(Numeric.PI * 0.15f, 0.0f, 0.0f));

        FloatBuffer buf = genLineSphere(40, 1);
        m_sphereVBO = gl.glGenBuffer();
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, m_sphereVBO);
        gl.glBufferData(GLenum.GL_ARRAY_BUFFER, buf, GLenum.GL_STATIC_DRAW);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);

        // make a quad for the floor
        float quad[/*12*/] = {	0.0f, -2.0f, 0.0f,
                1.0f, -2.0f, 0.0f,
                1.0f, -2.0f, -1.0f,
                0.0f, -2.0f, -1.0f };
        m_floorVBO = gl.glGenBuffer();
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, m_floorVBO);
        gl.glBufferData(GLenum.GL_ARRAY_BUFFER, CacheBuffer.wrap(quad), GLenum.GL_STATIC_DRAW);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);

        try {
            loadDataAndPrograms();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadDataAndPrograms() throws IOException {
        int i;
        byte[] pData;
        for (i=0;i<2;i++) {
            pData = FileUtils.loadBytes(model_files[i]);
            m_objectFileSize[i] = pData.length;
            m_object[i] = gl.glGenBuffer();
            gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, m_object[i]);
            gl.glBufferData(GLenum.GL_ARRAY_BUFFER, CacheBuffer.wrap(pData), GLenum.GL_STATIC_DRAW);
            gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
        }

        //init shaders
        m_LineProg = GLSLProgram.createFromFiles("Scenes/WireSphere/shaders/DrawLine.vert", "Scenes/WireSphere/shaders/DrawLine.frag");
        m_ObjectProg = GLSLProgram.createFromFiles("Scenes/WireSphere/shaders/Object.vert", "Scenes/WireSphere/shaders/Object.frag");
        m_FloorProg = GLSLProgram.createFromFiles("Scenes/WireSphere/shaders/Floor.vert", "Scenes/WireSphere/shaders/Floor.frag");

        mSceneData.near = Z_NEAR;
        mSceneData.far = Z_FAR;
        mSceneData.fov = FOV;
    }

    @Override
    public void update(float dt) {
        NvInputTransformer m_transformer = mNVApp.getInputTransformer();
        m_transformer.setRotationVel(0.0f, m_autoSpin ? (Numeric.PI * 0.05f) : 0.0f, 0.0f);
    }

    @Override
    public void onRender(boolean clearFBO) {
        if(clearFBO) {
            gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            gl.glClear(GLenum.GL_COLOR_BUFFER_BIT | GLenum.GL_DEPTH_BUFFER_BIT);
        }

        drawAll();
    }

    private void drawAll(){
        gl.glLineWidth(m_lineWidth);
        NvInputTransformer m_transformer = mNVApp.getInputTransformer();

        //draw lined sphere
//		nv::matrix4f vp = m_projection_matrix * m_transformer.getModelViewMat();
//		nv::rotationX(rotation, float(0.2));
//		mvp = m_projection_matrix * m_transformer.getModelViewMat() * rotation;
        /*Matrix4f.mul(m_projection_matrix, m_transformer.getModelViewMat(mvp), mvp);
        mvp.rotate(0.2f, VectorUtil.UNIT_X);*/
        mSceneData.setViewAndUpdateCamera(m_transformer.getModelViewMat(mSceneData.getViewMatrix()));
        Matrix4f mvp = mSceneData.getViewProjMatrix();
        mvp.rotate(0.2f, Vector3f.X_AXIS);
        drawSphere(mvp);

        //draw rings
//		model.make_identity();
//		model.set_scale(0.04);
//		nv::rotationX(rotation, float(0.1));
//		mvp = m_projection_matrix * m_transformer.getModelViewMat() * rotation* model;
        /*Matrix4f.mul(m_projection_matrix, m_transformer.getModelViewMat(mvp), mvp);
        mvp.rotate(0.1f, VectorUtil.UNIT_X);
        mvp.scale(vec3(0.04f));*/
        mSceneData.setViewAndUpdateCamera(m_transformer.getModelViewMat(mSceneData.getViewMatrix()));
        mvp.rotate(0.1f, Vector3f.X_AXIS);
        mvp.scale(0.04f);
        drawObjects(mvp, color0, 0);

//		model.make_identity();
//		model.set_scale(0.04);
//		nv::rotationX(rotation, float(NV_PI/2+0.1));
//		mvp = m_projection_matrix * m_transformer.getModelViewMat() * rotation * model;
        /*Matrix4f.mul(m_projection_matrix, m_transformer.getModelViewMat(mvp), mvp);
        mvp.rotate(MathUtil.PI /2+0.1f, VectorUtil.UNIT_X);
        mvp.scale(vec3(0.04f));*/
        mSceneData.setViewAndUpdateCamera(m_transformer.getModelViewMat(mSceneData.getViewMatrix()));
        mvp.rotate(Numeric.PI /2+0.1f, Vector3f.X_AXIS);
        mvp.scale(0.04f);
        drawObjects(mvp, color0, 0);

        //draw triangle
//		model.make_identity();
//		model.set_scale(0.03);
//		model.set_translate(nv::vec3f(0.0f, -0.3f, 0.0f));
//		mvp = m_projection_matrix * m_transformer.getModelViewMat() * model;
        /*Matrix4f.mul(m_projection_matrix, m_transformer.getModelViewMat(mvp), mvp);
        mvp.scale(vec3(0.03f));
        mvp.translate(vec3(0.0f, -0.3f, 0.0f));*/
        mSceneData.setViewAndUpdateCamera(m_transformer.getModelViewMat(mSceneData.getViewMatrix()));
        mvp.scale(0.03f);
        mvp.translate(0.0f, -0.3f, 0.0f);
        drawObjects(mvp, color1, 1);

        mSceneData.setViewAndUpdateCamera(m_transformer.getModelViewMat(mSceneData.getViewMatrix()));
        drawFloor(mvp);
    }

    private void drawObjects(Matrix4f mvp, float[] color, int id){
        gl.glEnable(GLenum.GL_DEPTH_TEST);
        gl.glEnable(GLenum.GL_CULL_FACE);
        gl.glFrontFace(GLenum.GL_CCW);
        gl.glCullFace(GLenum.GL_FRONT);

        /*gl.glUseProgram(m_ObjectProg.getProgram());
        m_ObjectProg.setUniformMatrix4("mvp", mvp, false);
        m_ObjectProg.setUniform4fv("color", color, 0, 4);*/
        m_ObjectProg.enable();
        int mvpIdx = m_ObjectProg.getUniformLocation("mvp");
        gl.glUniformMatrix4fv(mvpIdx, false, CacheBuffer.wrap(mvp));

        int colorIdx = m_ObjectProg.getUniformLocation("color");
        gl.glUniform4f(colorIdx, color[0], color[1], color[2], color[3]);


        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, m_object[id]);
        gl.glEnableVertexAttribArray(0);
        gl.glEnableVertexAttribArray(1);

        gl.glVertexAttribPointer(m_ObjectProg.getAttribLocation("aPosition"), 3, GLenum.GL_FLOAT, false, 32, 0);
        gl.glVertexAttribPointer(m_ObjectProg.getAttribLocation("aNormal"), 3, GLenum.GL_FLOAT, false, 32, 12);

        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, m_objectFileSize[id]/32);

        gl.glDisableVertexAttribArray(0);
        gl.glDisableVertexAttribArray(1);

        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
    }

    void drawSphere(Matrix4f mvp)
    {
        gl.glEnable(GLenum.GL_DEPTH_TEST);
        gl.glDisable(GLenum.GL_CULL_FACE);

        /*GL20.glUseProgram(m_LineProg.getProgram());
        m_LineProg.setUniformMatrix4("mvp",  mvp, false);
        m_LineProg.setUniform4f("color", 0.0f,0.0f,0.0f,1.0f);*/
        m_LineProg.enable();
        int mvpIdx = m_LineProg.getUniformLocation("mvp");
        gl.glUniformMatrix4fv(mvpIdx, false, CacheBuffer.wrap(mvp));

        int colorIdx = m_LineProg.getUniformLocation("color");
        gl.glUniform4f(colorIdx, 0, 0, 0, 1);

        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, m_sphereVBO);
        gl.glVertexAttribPointer(0, 3, GLenum.GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(0);
        gl.glDrawArrays(GLenum.GL_LINES, 0, 41 * 40 * 2);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
        gl.glDisable(GLenum.GL_DEPTH_TEST);
    }

    private void drawFloor(Matrix4f mvp)
    {
        gl.glEnable(GLenum.GL_DEPTH_TEST);
        gl.glDisable(GLenum.GL_CULL_FACE);

        final int instances = 5000;

        /*GL20.glUseProgram(m_FloorProg.getProgram());
        m_FloorProg.setUniformMatrix4("mvp", mvp);
        m_FloorProg.setUniform1i("instances", instances);*/
        m_FloorProg.enable();
        int mvpIdx = m_FloorProg.getUniformLocation("mvp");
        gl.glUniformMatrix4fv(mvpIdx, false, CacheBuffer.wrap(mvp));
        int instanceIdx = m_FloorProg.getUniformLocation("instances");
        gl.glUniform1i(instanceIdx, instances);

        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, m_floorVBO);
        gl.glVertexAttribPointer(0, 3, GLenum.GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(0);
        gl.glDrawArraysInstanced(GLenum.GL_QUADS, 0, 4, instances);

        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
        gl.glDisableVertexAttribArray(0);
    }

    @Override
    public void onResize(int width, int height) {
        super.onResize(width, height);

        if(width > 0 && height > 0){
            mSceneData.setProjection(FOV * 0.5f, (float)width/height, Z_NEAR, Z_FAR);
        }
    }

    @Override
    public void onDestroy() {
        CommonUtil.safeRelease(m_ObjectProg);
        CommonUtil.safeRelease(m_LineProg);
        CommonUtil.safeRelease(m_FloorProg);
        gl.glDeleteBuffer(m_sphereVBO);
        gl.glDeleteBuffer(m_floorVBO);
        gl.glDeleteBuffers(CacheBuffer.wrap(m_object));
    }

    private static FloatBuffer genLineSphere(int sphslices, float scale)
    {
        float rho, drho, theta, dtheta;
        float x;
        float y;
        float z;
        float s, t, ds, dt;
        int i, j;
        FloatBuffer buffer;

        int count=0;
        int slices = sphslices;
        int stacks = slices;
        buffer = BufferUtils.createFloatBuffer((slices+1)*stacks*3*2); // new GLfloat[(slices+1)*stacks*3*2];
//		if (bufferSize) *bufferSize = (slices+1)*stacks*3*2*4;
        ds = 1.0f / sphslices;//slices;
        dt = 1.0f / sphslices;//stacks;
        t = 1.0f;
        drho = Numeric.PI / stacks;
        dtheta = 2.0f * Numeric.PI / slices;

        for (i= 0; i < stacks; i++) {
            rho = i * drho;

            s = 0.0f;
            for (j = 0; j<=slices; j++) {
                theta = (j==slices) ? 0.0f : j * dtheta;
                x = (float) (-Math.sin(theta) * Math.sin(rho)*scale);
                z = (float) (Math.cos(theta) * Math.sin(rho)*scale);
                y = (float) (-Math.cos(rho)*scale);
                buffer.put(x).put(y).put(z);

                x = (float) (-Math.sin(theta) * Math.sin(rho+drho)*scale);
                z = (float) (Math.cos(theta) * Math.sin(rho+drho)*scale);
                y = (float) (-Math.cos(rho+drho)*scale);
                buffer.put(x).put(y).put(z);

                s += ds;
            }
            t -= dt;
        }
        buffer.flip();
        return buffer;
    }
}
