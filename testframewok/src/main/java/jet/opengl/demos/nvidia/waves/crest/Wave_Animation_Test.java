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

    private Technique _flowMaterial;
    private Technique _displacementMaterial;
    private Technique _dampDynWavesMaterial;

//     [Range(0, 1000), SerializeField]
    float _amplitude = 20;
//        [Range(0, 1000), SerializeField]
    float _radius = 80f;
//        [Range(0, 1000), SerializeField]
    float _eyeRadius = 1f;
//        [Range(0, 1000), SerializeField]
    float _maxSpeed = 10f;

    private final Matrix4f mProj = new Matrix4f();
    private final Matrix4f mView = new Matrix4f();

    private GLFuncProvider gl;

    protected void initRendering(){
        getGLContext().setSwapInterval(0);

        m_Clipmap_Params.sea_level = 5;
        m_Clipmap_Params.lodDataResolution = 256;
        m_Clipmap_Params.geometryDownSampleFactor = 2;

        mCDClipmap = new Wave_CDClipmap();
        mCDClipmap.init(m_Clipmap_Params);

        m_Simulation_Params.shape_combine_pass_pingpong = true;
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
        m_Simulation_Params.gravityMultiplier = 12;

        m_Simulation_Params.create_flow = true;

        mAnimation = new Wave_Simulation();
        mAnimation.init(mCDClipmap, m_Simulation_Params, Wave_Demo_Animation.Whirlpool);

        AttribDesc attribute_descs[] =
        {
                new AttribDesc(0, 3, GLenum.GL_FLOAT, false, 20, 0),	// vPos
                new AttribDesc(1, 2, GLenum.GL_FLOAT, false, 20, 12)	// UV
        };

//        Vector3f[] verts = {
//                new Vector3f(-1, 0, 1), new Vector3f(1,0,1), new Vector3f(-1, 0, -1), new Vector3f(1,0,-1)
//        };

        float[] _verts = {
           -1,0,1, 0, 0,
           1,0,1, 1, 0,
           -1,0,-1, 1, 1,
           1,0,-1, 0, 1,
        };

        int[] indices = {0,1,3, 0, 3,2};

        BufferGL vertexBuffer = new BufferGL();
        vertexBuffer.initlize(GLenum.GL_ARRAY_BUFFER, 4*_verts.length, CacheBuffer.wrap(_verts), GLenum.GL_STATIC_DRAW);

        BufferGL indexBuffer = new BufferGL();
        indexBuffer.initlize(GLenum.GL_ELEMENT_ARRAY_BUFFER, 4*indices.length, CacheBuffer.wrap(indices), GLenum.GL_STATIC_DRAW);

        Wave_Mesh mesh = new Wave_Mesh(attribute_descs, vertexBuffer, indexBuffer, 0);
        mesh.setIndice(GLenum.GL_TRIANGLES, indices.length);

        // Add the wave particle
        {

            Technique waveParticleShader = ShaderManager.getInstance().getProgram("Crest/Inputs/Animated Waves/Wave Particle");
            waveParticleShader.setName("Wave Particle");
            waveParticleShader.setStateEnabled(false);

            Wave_Simulation_Animation_Input waveParticles = new Wave_Simulation_Animation_Input(waveParticleShader, null,mAnimation, mCDClipmap);

            waveParticles.setWaveLegnth(11);
            waveParticles.getTransform().setPosition(-25, 0, -25);

            mAnimation._lodDataAnimWaves.addLodDataInput(waveParticles);
        }

        // Add the ripple
        if(m_Simulation_Params.create_dynamic_wave /*&& !m_Simulation_Params.create_flow*/){
            Technique rippleShader = ShaderManager.getInstance().getProgram("Crest/Inputs/Dynamic Waves/Add Bump");
            rippleShader.setName("Ripple");

            Wave_Simulation_Common_Input rippleInput = new Wave_Simulation_Common_Input(rippleShader, mesh);
            rippleInput.getTransform().setPosition(25, 0, 25);
            mAnimation._lodDataDynWaves.addLodDataInput(rippleInput);

            m_RippleShader = rippleShader;
            m_RippleInput = rippleInput;
            m_RippleShader.setStateEnabled(false);
            GLSLUtil.setFloat(m_RippleShader, "_Amplitude", -1333);
            GLSLUtil.setFloat(m_RippleShader, "_Radius", 1f);
        }

        // Add the whirlpool
        if(m_Simulation_Params.create_flow){

            final Vector3f whirlPoolLocation = new Vector3f();
            {
                _displacementMaterial = ShaderManager.getInstance().getProgram("Crest/Inputs/Animated Waves/Whirlpool");
                _displacementMaterial.setName("Displacement");
                _displacementMaterial.setStateEnabled(false);

                Wave_Simulation_Animation_Input displacementInput = new Wave_Simulation_Animation_Input(_displacementMaterial, null,mAnimation, mCDClipmap);
                displacementInput.getTransform().setPosition(whirlPoolLocation);
                mAnimation._lodDataAnimWaves.addLodDataInput(displacementInput);
            }

            {
                _flowMaterial = ShaderManager.getInstance().getProgram("Crest/Inputs/Flow/Whirlpool");
                _flowMaterial.setName("Flow");
                _flowMaterial.setStateEnabled(false);

                Wave_Simulation_Common_Input flowInput = new Wave_Simulation_Common_Input(_flowMaterial, null);
                flowInput.getTransform().setPosition(whirlPoolLocation);
                flowInput.getTransform().setScale(_radius, 1, _radius);
                mAnimation._lodDataFlow.addLodDataInput(flowInput);
            }

            {
                _dampDynWavesMaterial = ShaderManager.getInstance().getProgram("Crest/Inputs/Dynamic Waves/Dampen Circle");
                _dampDynWavesMaterial.setName("Dampen Circle");
                Wave_Simulation_Common_Input dampDynWavesInput = new Wave_Simulation_Common_Input(_dampDynWavesMaterial, mesh);
                dampDynWavesInput.getTransform().setPosition(whirlPoolLocation);
                mAnimation._lodDataDynWaves.addLodDataInput(dampDynWavesInput);
            }
        }

        m_Renderer = new Wave_Renderer();
        m_Renderer.init(mCDClipmap, mAnimation);

        gl = GLFuncProviderFactory.getGLFuncProvider();

        m_transformer.setMotionMode(NvCameraMotionType.FIRST_PERSON);
        m_transformer.setTranslation(0.567f, -10.1f, 0.6f);
        m_transformer.setMaxTranslationVel(3 * m_transformer.getMaxTranslationVel(0));
    }

    @Override
    public void display() {
        GLStateTracker tracker = GLStateTracker.getInstance();
        tracker.saveStates();

        Wave_Simulation_Technique.g_CrestTime = getTotalTime();

        updateRipple();
        UpdateMaterials();

        m_transformer.getModelViewMat(mView);
        mCDClipmap.updateWave(mView);
        mAnimation.update(getFrameDeltaTime());

        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
        gl.glViewport(0,0, getGLContext().width(), getGLContext().height());
        m_Renderer.waveShading(mProj, mView, true);

        tracker.restoreStates();
    }

    private void updateRipple(){
        if(!(m_Simulation_Params.create_dynamic_wave /*&& !m_Simulation_Params.create_flow*/))
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

    private void UpdateMaterials()
    {
//        _flowMaterial.SetFloat("_EyeRadiusProportion", _eyeRadius / _radius);
//        _flowMaterial.SetFloat("_MaxSpeed", _maxSpeed);

        if(m_Simulation_Params.create_flow) {

            mCDClipmap.ReportMaxDisplacementFromShape(0, _amplitude, 0);

            if(_flowMaterial != null) {
                GLSLUtil.setFloat(_flowMaterial, "_EyeRadiusProportion", _eyeRadius / _radius);
                GLSLUtil.setFloat(_flowMaterial, "_MaxSpeed", _maxSpeed);
            }

//        _displacementMaterial.SetFloat("_Radius", _radius * 0.25f);
//        _displacementMaterial.SetFloat("_Amplitude", _amplitude);

            if(_displacementMaterial!= null) {
                GLSLUtil.setFloat(_displacementMaterial, "_Radius", _radius * 0.25f);
                GLSLUtil.setFloat(_displacementMaterial, "_Amplitude", _amplitude);
            }
        }
    }

    @Override
    protected void reshape(int width, int height) {
        if(width <= 0 || height <= 0)
            return;

        gl.glViewport(0,0, width, height);
        Matrix4f.perspective(60, (float)width/height, 0.1f, 1000, mProj);
    }
}
