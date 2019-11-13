package jet.opengl.demos.nvidia.waves.crest;

import com.nvidia.developer.opengl.app.NvCameraMotionType;
import com.nvidia.developer.opengl.app.NvSampleApp;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import jet.opengl.demos.nvidia.waves.ocean.Technique;
import jet.opengl.postprocessing.buffer.AttribDesc;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLStateTracker;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CacheBuffer;

public class Wave_Animation_Test extends NvSampleApp {

    private Wave_CDClipmap_Params m_Clipmap_Params = new Wave_CDClipmap_Params();
    private Wave_CDClipmap mCDClipmap;

    private Wave_Simulation_Params m_Simulation_Params = new Wave_Simulation_Params();
    private Wave_Simulation mAnimation;

    private Wave_Renderer m_Renderer;

    private final Matrix4f mProj = new Matrix4f();
    private final Matrix4f mView = new Matrix4f();

    private GLFuncProvider gl;
    protected void initRendering(){
        getGLContext().setSwapInterval(0);

        m_Clipmap_Params.sea_level = 5;
        mCDClipmap = new Wave_CDClipmap();
        mCDClipmap.init(m_Clipmap_Params);

        m_Simulation_Params.shape_combine_pass_pingpong = true;
        m_Simulation_Params.random_seed = 1000000;
        m_Simulation_Params.direct_towards_Point = false;
        m_Simulation_Params.max_displacement_Vertical = 5.5f;
        mAnimation = new Wave_Simulation();
        mAnimation.init(mCDClipmap, m_Simulation_Params, Wave_Demo_Animation.BoatScene);

        {
            Technique waveParticleShader = ShaderManager.getInstance().getProgram("Crest/Inputs/Animated Waves/Wave Particle");
            waveParticleShader.setName("Wave Particle");
            AttribDesc attribute_descs[] =
            {
                    new AttribDesc(0, 3, GLenum.GL_FLOAT, false, 0, 0)	// vPos
            };

            Vector3f[] verts = {
                    new Vector3f(-1, 0, 1), new Vector3f(1,0,1), new Vector3f(-1, 0, -1), new Vector3f(1,0,-1)
            };

            int[] indices = {0,1,3, 0, 3,2};

            BufferGL vertexBuffer = new BufferGL();
            vertexBuffer.initlize(GLenum.GL_ARRAY_BUFFER, Vector3f.SIZE*verts.length, CacheBuffer.wrap(verts), GLenum.GL_STATIC_DRAW);

            BufferGL indexBuffer = new BufferGL();
            indexBuffer.initlize(GLenum.GL_ELEMENT_ARRAY_BUFFER, 4*indices.length, CacheBuffer.wrap(indices), GLenum.GL_STATIC_DRAW);

            Wave_Mesh mesh = new Wave_Mesh(attribute_descs, vertexBuffer, indexBuffer, 0);
            mesh.setIndice(GLenum.GL_TRIANGLES, indices.length);

            Wave_Simulation_Animation_Input waveParticles = new Wave_Simulation_Animation_Input(waveParticleShader, mesh,mAnimation, mCDClipmap);

            waveParticles.setWaveLegnth(11);
            waveParticles.getTransform().setPosition(5, 0, 5);

            mAnimation._lodDataAnimWaves.addLodDataInput(waveParticles);
        }

        m_Renderer = new Wave_Renderer();
        m_Renderer.init(mCDClipmap, mAnimation);

        gl = GLFuncProviderFactory.getGLFuncProvider();

        m_transformer.setMotionMode(NvCameraMotionType.FIRST_PERSON);
        m_transformer.setTranslation(0.567f, -10.1f, 0.6f);
    }

    @Override
    public void display() {
        GLStateTracker tracker = GLStateTracker.getInstance();
        tracker.saveStates();

        m_transformer.getModelViewMat(mView);
        mCDClipmap.updateWave(mView);
        mAnimation.update(getFrameDeltaTime());

        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
        gl.glViewport(0,0, getGLContext().width(), getGLContext().height());
        m_Renderer.waveShading(mProj, mView, true);

        tracker.restoreStates();
    }

    @Override
    protected void reshape(int width, int height) {
        if(width <= 0 || height <= 0)
            return;

        gl.glViewport(0,0, width, height);
        Matrix4f.perspective(60, (float)width/height, 0.1f, 1000, mProj);
    }
}
