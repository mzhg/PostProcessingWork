package jet.opengl.demos.nvidia.sparkles;

import com.nvidia.developer.opengl.models.DrawMode;
import com.nvidia.developer.opengl.models.GLVAO;
import com.nvidia.developer.opengl.models.QuadricBuilder;
import com.nvidia.developer.opengl.models.QuadricMesh;
import com.nvidia.developer.opengl.models.QuadricSphere;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.io.IOException;

import jet.opengl.demos.scene.BaseScene;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.AttribBinder;
import jet.opengl.postprocessing.shader.AttribBindingTask;
import jet.opengl.postprocessing.util.CommonUtil;

/**
 * Created by mazhen'gui on 2017/10/21.
 */

final class SphereScene extends BaseScene {
    private static final int POSITION_LOC = 0;
    private static final int NORMAL_LOC = 1;
    private static final int TEXTURE_LOC = 2;

    private SimpleLightProgram lightProgram;
    private final SimpleLightProgram.LightParams lightParams = new SimpleLightProgram.LightParams();
    private final Vector4f light_pos = new Vector4f(5,5,10,1);
    private final Vector3f lightDiff = new Vector3f(1,1,1);
    private final Vector3f lightSpec = new Vector3f(0.9f,0.9f,0.9f);
    final Matrix4f model = new Matrix4f();
    final Matrix4f mvp = new Matrix4f();
    private final float shinefact = 50;
    private float alpha;
    private float beta;

    private GLVAO m_sphere;

    @Override
    protected void onCreate(Object prevSavedData) {
        try {
            lightProgram = new SimpleLightProgram(true, new AttribBindingTask(
                    new AttribBinder(SimpleLightProgram.POSITION_ATTRIB_NAME, POSITION_LOC),
                    new AttribBinder(SimpleLightProgram.TEXTURE_ATTRIB_NAME, TEXTURE_LOC),
                    new AttribBinder(SimpleLightProgram.NORMAL_ATTRIB_NAME, NORMAL_LOC)));
        } catch (IOException e) {
            e.printStackTrace();
        }

        lightParams.lightPos.set(light_pos);
        lightParams.lightDiffuse.set(lightDiff);
        lightParams.lightSpecular.set(lightSpec);
        lightParams.materialSpecular.set(lightSpec);
        lightParams.materialSpecular.w = shinefact;
        lightParams.materialDiffuse.set(lightDiff);
        lightParams.eyePos.set(0,0,0);

        QuadricBuilder builder = new QuadricBuilder();
        builder.setXSteps(30).setYSteps(30);
        builder.setDrawMode(DrawMode.FILL);
        builder.setCenterToOrigin(true);
        builder.setPostionLocation(POSITION_LOC);
        builder.setNormalLocation(NORMAL_LOC);
        builder.setTexCoordLocation(TEXTURE_LOC);

        m_sphere = new QuadricMesh(builder, new QuadricSphere(1)).getModel().genVAO();
        GLCheck.checkError();
    }

    @Override
    protected void update(float dt) {
        alpha += (0.6 * dt);
        beta += (0.2234 * dt);
    }

    private void updateTransform(){
        mSceneData.setViewAndUpdateCamera(mNVApp.getInputTransformer().getModelViewMat(mSceneData.getViewMatrix()));
        Matrix4f.mul(mSceneData.getViewProjMatrix(), model, mvp);
    }

    public void getViews(Matrix4f model, boolean bigSphere){
        model.setIdentity();
        model.m32 = -3;
        model.rotate(alpha, 0,1,0);
        model.rotate(beta, 1,0,0);

        if(!bigSphere){
            model.translate(1.5f,0,0);
            model.scale(0.5f);
        }
    }

    public GLVAO getSphere() {return m_sphere;}

    @Override
    protected void onRender(boolean clearFBO) {
        if(clearFBO){
            gl.glClearColor(.25f, .25f, .25f, 1);
            gl.glClearDepthf(1.0f);
            gl.glClear(GLenum.GL_COLOR_BUFFER_BIT|GLenum.GL_DEPTH_BUFFER_BIT);
        }

        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);  // no texture binding.
        gl.glEnable(GLenum.GL_DEPTH_TEST);
        gl.glDepthFunc(GLenum.GL_LESS);
        gl.glDepthMask(true);
        gl.glDisable(GLenum.GL_BLEND);
        gl.glDisable(GLenum.GL_CULL_FACE);

        { // Draw the big red sphere.
            model.setIdentity();
            model.m32 = -3;
            model.rotate(alpha, 0,1,0);
            model.rotate(beta, 1,0,0);

            updateTransform();
            Matrix4f.decompseRigidMatrix(mSceneData.getViewMatrix(), lightParams.eyePos,null,null);

            lightProgram.enable();
            lightParams.model.load(model);
            lightParams.modelViewProj.load(mvp);

            lightParams.color.set(0.5f,0.0f,0.2f, 1.0f);
            lightProgram.setLightParams(lightParams);
            m_sphere.bind();
            m_sphere.draw(GLenum.GL_TRIANGLES);
            m_sphere.unbind();
        }

        { // Draw the small blue sphere.
            model.translate(1.5f,0,0);
            model.scale(0.5f);
            updateTransform();
            lightParams.model.load(model);
            lightParams.modelViewProj.load(mvp);
            lightParams.color.set(0.0f,0.2f,0.5f, 1.0f);
            lightProgram.setLightParams(lightParams);

            m_sphere.bind();
            m_sphere.draw(GLenum.GL_TRIANGLES);
            m_sphere.unbind();
        }
    }

    @Override
    public void onResize(int width, int height) {
        super.onResize(width, height);

        if(width > 0 && height > 0){
            mSceneData.setProjection(60, (float)width/height, 0.1f, 100.0f);
        }
    }

    @Override
    protected void onDestroy() {
        CommonUtil.safeRelease(lightProgram);
        CommonUtil.safeRelease(m_sphere);
    }
}
