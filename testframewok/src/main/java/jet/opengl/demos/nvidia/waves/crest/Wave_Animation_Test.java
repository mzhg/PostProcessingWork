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
import jet.opengl.postprocessing.shader.GLSLUtil;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.postprocessing.util.Rectf;

public class Wave_Animation_Test extends NvSampleApp {

    private Wave_CDClipmap_Params m_Clipmap_Params = new Wave_CDClipmap_Params();
    private Wave_CDClipmap mCDClipmap;

    private Wave_Simulation_Params m_Simulation_Params = new Wave_Simulation_Params();
    private Wave_Simulation mAnimation;

    private Wave_Renderer m_Renderer;
    private boolean m_RippleEnabled = true;
    private Technique m_RippleShader;
    private Wave_Simulation_Common_Input m_RippleInput;

    private final Matrix4f mProj = new Matrix4f();
    private final Matrix4f mView = new Matrix4f();

    private GLFuncProvider gl;
    protected void initRendering(){
        getGLContext().setSwapInterval(0);

        m_Clipmap_Params.sea_level = 5;
        mCDClipmap = new Wave_CDClipmap();
        mCDClipmap.init(m_Clipmap_Params);

        m_Simulation_Params.shape_combine_pass_pingpong = false;
        m_Simulation_Params.random_seed = 1000000;
        m_Simulation_Params.direct_towards_Point = false;
        m_Simulation_Params.max_displacement_Vertical = 5.5f;
        m_Simulation_Params.create_dynamic_wave = true;
        m_Simulation_Params.create_foam = false;

        m_Simulation_Params.damping = 0.25f;
        m_Simulation_Params.courant_number = 1f;
        m_Simulation_Params.max_simsteps_perframe = 3;
        m_Simulation_Params.horiz_displace = 6;
        m_Simulation_Params.displace_clamp = 0.3f;
        m_Simulation_Params.gravityMultiplier = 12f;

        mAnimation = new Wave_Simulation();
        mAnimation.init(mCDClipmap, m_Simulation_Params, Wave_Demo_Animation.BoatScene);

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

        // Add the wave particle
        {

            Technique waveParticleShader = ShaderManager.getInstance().getProgram("Crest/Inputs/Animated Waves/Wave Particle");
            waveParticleShader.setName("Wave Particle");

            Wave_Simulation_Animation_Input waveParticles = new Wave_Simulation_Animation_Input(waveParticleShader, mesh,mAnimation, mCDClipmap);

            waveParticles.setWaveLegnth(11);
            waveParticles.getTransform().setPosition(-5, 0, -5);

            mAnimation._lodDataAnimWaves.addLodDataInput(waveParticles);
        }

        // Add the ripple
        if(m_Simulation_Params.create_dynamic_wave){
            Technique rippleShader = ShaderManager.getInstance().getProgram("Crest/Inputs/Dynamic Waves/Add Bump");
            rippleShader.setName("Ripple");

            Wave_Simulation_Common_Input rippleInput = new Wave_Simulation_Common_Input(rippleShader, mesh);
            rippleInput.getTransform().setPosition(5, 0, 5);
            mAnimation._lodDataDynWaves.addLodDataInput(rippleInput);

            m_RippleShader = rippleShader;
            m_RippleInput = rippleInput;

            GLSLUtil.setFloat(m_RippleShader, "_Amplitude", -1333);
            GLSLUtil.setFloat(m_RippleShader, "_Radius", 1f);
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

        updateRipple();

        m_transformer.getModelViewMat(mView);
        mCDClipmap.updateWave(mView);
        mAnimation.update(getFrameDeltaTime());

        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
        gl.glViewport(0,0, getGLContext().width(), getGLContext().height());
        m_Renderer.waveShading(mProj, mView, true);

        tracker.restoreStates();
    }

    private void updateRipple(){
        if(!m_Simulation_Params.create_dynamic_wave)
            return;

        if(!m_RippleEnabled)
            return;

        boolean _animate = true;
        float _warmUp = 1f;
        float _onTime = 0.3f;
        float _period = 6f;
        m_RippleInput.setEnabled(false);

        if(_animate)
        {
            float t = getTotalTime();
            if (t < _warmUp)
                return;
            t -= _warmUp;
            t = Numeric.fmod(t, _period);
//            _rdwi.enabled = t < _onTime;
            if(t >= _onTime)
                return;
        }

        // which lod is this object in (roughly)?
        Rectf thisRect = new Rectf(m_RippleInput.getTransform().getPositionX(),m_RippleInput.getTransform().getPositionZ(), 0,0);
        int minLod = mAnimation._lodDataAnimWaves.SuggestDataLOD(thisRect);
        if (minLod == -1)
        {
            // outside all lods, nothing to update!
            return;
        }

        // how many active wave sims currently apply to this object - ideally this would eliminate sims that are too
        // low res, by providing a max grid size param
        int simsPresent, simsActive;
        long value = mAnimation._lodDataDynWaves.CountWaveSims(minLod/*, out simsPresent, out simsActive*/);
        simsPresent = Numeric.decodeFirst(value);
        simsActive = Numeric.decodeSecond(value);
        if (simsPresent == 0)
        {
            m_RippleEnabled = false;
            return;
        }

        m_RippleInput.setEnabled(true);

        if (simsActive > 0)
        {
//            _mat.SetFloat("_SimCount", simsActive);
            GLSLUtil.setFloat(m_RippleShader, "_SimCount", simsActive);
        }

//        float dt; int steps;
        value = mAnimation._lodDataDynWaves.GetSimSubstepData(getFrameDeltaTime()/*, out steps, out dt*/);
        float dt = Float.intBitsToFloat(Numeric.decodeSecond(value));
//        _mat.SetFloat("_SimDeltaTime", dt);
        GLSLUtil.setFloat(m_RippleShader, "_SimDeltaTime", dt);
    }

    @Override
    protected void reshape(int width, int height) {
        if(width <= 0 || height <= 0)
            return;

        gl.glViewport(0,0, width, height);
        Matrix4f.perspective(60, (float)width/height, 0.1f, 1000, mProj);
    }
}
