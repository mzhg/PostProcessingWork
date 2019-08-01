package jet.opengl.demos.scenes;

import com.nvidia.developer.opengl.app.GLEventListener;
import com.nvidia.developer.opengl.app.NvCameraMotionType;
import com.nvidia.developer.opengl.app.NvInputTransformer;
import com.nvidia.developer.opengl.app.NvKey;
import com.nvidia.developer.opengl.app.NvKeyActionType;
import com.nvidia.developer.opengl.app.NvSampleApp;
import com.nvidia.developer.opengl.models.GLVAO;
import com.nvidia.developer.opengl.ui.NvTweakBar;
import com.nvidia.developer.opengl.ui.NvTweakVarBase;
import com.nvidia.developer.opengl.utils.FieldControl;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;
import org.lwjgl.util.vector.WritableVector3f;

import java.io.IOException;
import java.nio.FloatBuffer;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.volumetricLighting.LightType;
import jet.opengl.postprocessing.shader.FullscreenProgram;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.shader.ShaderLoader;
import jet.opengl.postprocessing.shader.ShaderSourceItem;
import jet.opengl.postprocessing.shader.ShaderType;
import jet.opengl.postprocessing.shader.VisualDepthTextureProgram;
import jet.opengl.postprocessing.texture.SamplerDesc;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.Numeric;

public class Cube16 {

	public static final int ACCEL_STRUCT_NONE = 0;
	public static final int ACCEL_STRUCT_MIN_MAX_TREE = 1;
	public static final int ACCEL_STRUCT_BV_TREE = 2;
	public static final int INSCTR_INTGL_EVAL_METHOD_MY_LUT = 0;
	public static final int INSCTR_INTGL_EVAL_METHOD_SRNN05 = 1;
	public static final int INSCTR_INTGL_EVAL_METHOD_ANALYTIC = 2;
	public static final int LIGHT_SCTR_TECHNIQUE_EPIPOLAR_SAMPLING = 0;
	public static final int LIGHT_SCTR_TECHNIQUE_BRUTE_FORCE = 1;

	public static final int LIGHTMODE_DIRECTIONAL = 1;
	public static final int LIGHTMODE_SPOTLIGHT   = 2;
	public static final int LIGHTMODE_OMNI        = 3;

	float INSCATTERING_MULTIPLIER = 27.f/3.f;

	public static final int SHADOWMAP_RESOLUTION = 1024;
	public static final float LIGHT_RANGE = 50.0f;
	public static final float SPOTLIGHT_FALLOFF_ANGLE = Numeric.PI / 4.0f;
	public static final float SPOTLIGHT_FALLOFF_POWER = 1.0f;
	public static final float DISTANCE = 17.5f;
	
    float m_fLightIntensity = 12.f;
    
	private Texture2D m_pOffscreenRenderTarget;
	private Texture2D m_pOffscreenDepth;
	private Texture2D m_pShadowMap;
	private Texture2D m_pParaboloidShadowMap;

	private SceneController m_pScene;
	
	private int m_RenderTarget;
	private int m_DummyVAO;
	
	private DepthProgram m_DepthProgram;
	private DepthGSProgram m_DepthGSProgram;
	
	private SceneRenderProgram m_SceneDirectionalProgram;
	private SceneRenderProgram m_SceneSpotlightProgram;
	private SceneRenderProgram m_SceneOMNIProgram;
	private FullscreenProgram fullscreenProgram;
	
	private final ObjectCB m_ObjectCBStruct = new ObjectCB();
	private final ViewCB m_ViewCBStruct = new ViewCB();
	private final LightCB m_LightCBStruct = new LightCB();
	
	private boolean m_bPrintProgram = false;
	private boolean m_bVisualShadownMap;
	private int     m_shadowSlice;
	private VisualDepthTextureProgram m_visTexShader;
	private VisualDepthTextureProgram m_visTexShader_array;
	private NvTweakBar mTweakBar;
	private NvTweakVarBase mSaveSceneTweak;
	
	private final Matrix4f posCorrectMat = new Matrix4f();
	
	private NvInputTransformer m_Transformer;
	private NvSampleApp nvApp;
	
	private int m_psamComparison;
	private int m_psamDefault;
	private GLFuncProvider gl;
	
	public Cube16(NvSampleApp context){
		m_Transformer = context.getInputTransformer();
		nvApp = context;
	}
	
	public void initUI(NvTweakBar tweakBar){
		tweakBar.addValue("Visualize Depth", new FieldControl(this, "m_bVisualShadownMap"));
		tweakBar.addValue("Shadow Slice", new FieldControl(this, "m_shadowSlice"), 0, 1);

    	tweakBar.addValue("Animated: ", new FieldControl(m_pScene, "isPaused_"));
    	tweakBar.syncValues();
    	
    	mTweakBar = tweakBar;
	}

	public int getShadowMapResolution() { return SHADOWMAP_RESOLUTION;}
	
	public void onCreate() {
		gl = GLFuncProviderFactory.getGLFuncProvider();
		Texture2DDesc shadowMapDesc = new Texture2DDesc(SHADOWMAP_RESOLUTION, SHADOWMAP_RESOLUTION, GLenum.GL_DEPTH24_STENCIL8);
		m_pShadowMap = TextureUtils.createTexture2D(shadowMapDesc, null);
		shadowMapDesc.arraySize = 2;
		m_pParaboloidShadowMap = TextureUtils.createTexture2D(shadowMapDesc, null);
		fullscreenProgram = new FullscreenProgram();
		
//		m_pScene = new SceneVolume(m_transformer);
		
		m_RenderTarget = gl.glGenFramebuffer();
		m_DummyVAO     = gl.glGenVertexArray();
		
//		posCorrectMat.m22 = -1;
//		nvApp.setSwapInterval(0);
		
		SamplerDesc SamComparisonDesc = new SamplerDesc
		(
			GLenum.GL_NEAREST,
			GLenum.GL_NEAREST,
			GLenum.GL_CLAMP_TO_BORDER,   // TODO : GL ES doen't support GL_CLAMP_TO_BORDER
			GLenum.GL_CLAMP_TO_BORDER,
			GLenum.GL_CLAMP_TO_BORDER,
			-1,    // border color
			0,    // MaxAnisotropy
			GLenum.GL_LESS,    // ComparisonFunc
			GLenum.GL_COMPARE_REF_TO_TEXTURE   // ComparisonMode
		);
		
		m_psamComparison = SamplerUtils.createSampler(SamComparisonDesc);
		m_psamDefault = SamplerUtils.getDefaultSampler();

		m_pScene = new SceneController(m_Transformer);
	}

	public void onResize(int width, int height) {
		if(width == 0 || height == 0)
			return;
		
		if(m_pOffscreenRenderTarget != null){
			m_pOffscreenRenderTarget.dispose();
			m_pOffscreenRenderTarget = null;
		}
		
		if(m_pOffscreenDepth != null){
			m_pOffscreenDepth.dispose();
			m_pOffscreenDepth = null;
		}
		

		final int buffer_format = GLenum.GL_RGBA16F;
		Texture2DDesc offscreenDesc = new Texture2DDesc();
		offscreenDesc.width = width;
		offscreenDesc.height = height;
		offscreenDesc.arraySize =1;
		offscreenDesc.mipLevels = 1;
		offscreenDesc.format = buffer_format;
		offscreenDesc.sampleCount = 1;


		m_pOffscreenRenderTarget = TextureUtils.createTexture2D(offscreenDesc, null);
		offscreenDesc.format = GLenum.GL_DEPTH24_STENCIL8;
		m_pOffscreenDepth = TextureUtils.createTexture2D(offscreenDesc, null);

		GLCheck.checkError();
	}

	public void draw(){
		draw(true);
	}

	public void draw(boolean resovled) {
		gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
		gl.glViewport(0, 0, m_pOffscreenRenderTarget.getWidth(), m_pOffscreenRenderTarget.getHeight());
		gl.glClearColor(0.350f,  0.350f,  0.350f, 1.0f);
		gl.glClearDepthf(1.0f);
		gl.glClear(GLenum.GL_COLOR_BUFFER_BIT | GLenum.GL_DEPTH_BUFFER_BIT);
		
		m_pScene.animate(nvApp.getFrameDeltaTime());
		renderShadowMap();
		renderGemetry(true);
		
		if(m_bVisualShadownMap){
			showShadownMap();
			return;
		}

		if(resovled)
			renderTexture(m_pOffscreenRenderTarget);
		m_bPrintProgram = true;
	}

	public void resolve(){
		renderTexture(m_pOffscreenRenderTarget);
	}

	public Texture2D getSceneColor() { return m_pOffscreenRenderTarget;}
	public Texture2D getSceneDepth() { return m_pOffscreenDepth;}
	public boolean isVisualShadowMap() { return m_bVisualShadownMap;}

	void renderTexture(Texture2D texture){
		gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
		gl.glViewport(0, 0, texture.getWidth(), texture.getHeight());
		gl.glDisable(GLenum.GL_DEPTH_TEST);
		gl.glClearColor(0, 0, 0, 0);
		gl.glClear(GLenum.GL_COLOR_BUFFER_BIT);
		
//		texture.bind(0);
		gl.glActiveTexture(GLenum.GL_TEXTURE0);
		gl.glBindTexture(texture.getTarget(), texture.getTexture());
		fullscreenProgram.enable();

		gl.glBindVertexArray(m_DummyVAO);
		gl.glDrawArrays(GLenum.GL_TRIANGLE_STRIP, 0, 4);
		gl.glBindVertexArray(0);
		fullscreenProgram.disable();
	}
	
	public boolean handleKeyInput(int code, NvKeyActionType action) {
		if(action == NvKeyActionType.DOWN) {
			switch (code) {
				case NvKey.K_V:
					m_pScene.toggleViewpoint();
					return true;
				case NvKey.K_L:
					m_pScene.toggleLightMode();
					return true;
				default:
					return false;
			}
		}

		return false;
	}
	
	private void renderShadowMap(){
		GLCheck.checkError();
		m_pScene.setupLightViewCB(m_ViewCBStruct);
		
		gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, m_RenderTarget);
		gl.glViewport(0, 0, m_pShadowMap.getWidth(), m_pShadowMap.getHeight());
    	BaseProgram program;
    	if(m_pScene.getLightMode() == LightType.POINT){
			gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT0, GLenum.GL_TEXTURE_2D, 0, 0);
			gl.glFramebufferTexture(GLenum.GL_FRAMEBUFFER, GLenum.GL_DEPTH_STENCIL_ATTACHMENT, m_pParaboloidShadowMap.getTexture(), 0);
			gl.glDrawBuffers(GLenum.GL_NONE);

    		gl.glViewportIndexedf(0, 0, 0, SHADOWMAP_RESOLUTION, SHADOWMAP_RESOLUTION);
			gl.glViewportIndexedf(1, 0, 0, SHADOWMAP_RESOLUTION, SHADOWMAP_RESOLUTION);

			GLCheck.checkError();
    		if(m_DepthGSProgram == null)
				m_DepthGSProgram  = new DepthGSProgram(null);
    		program = m_DepthGSProgram;
    	}else{
			gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT0, GLenum.GL_TEXTURE_2D, 0, 0);
    		gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_DEPTH_STENCIL_ATTACHMENT, m_pShadowMap.getTarget(), m_pShadowMap.getTexture(), 0);
    		gl.glDrawBuffers(GLenum.GL_NONE);

    		GLCheck.checkError();
    		if(m_DepthProgram == null)
    			m_DepthProgram = new DepthProgram(null);

    		program = m_DepthProgram;
    	}
    	
    	gl.glClearBufferfi(GLenum.GL_DEPTH_STENCIL, 0, 1.f, 0);
    	
    	program.enable();
    	renderScene(program);
    	program.disable();

		if(!m_bPrintProgram /*&& m_bEnableLightScattering*/){
			program.setName("ShadowRender");
			program.printPrograminfo();
//        	ProgramProperties props = GLSLUtil.getProperties(sceneRender.getProgram());
//        	System.out.println("SceneRender: ");
//        	System.out.println(props);
		}
    	
    	gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
		GLCheck.checkError();
	}
	
	private void showShadownMap() {
    	if(m_visTexShader == null){
			try {
				m_visTexShader = new VisualDepthTextureProgram(false);
                m_visTexShader_array = new VisualDepthTextureProgram(true);
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}

    	Texture2D shadowMap;
        VisualDepthTextureProgram visualProgram;
        float near, far;
        if(m_pScene.getLightMode() == LightType.POINT){
            visualProgram = m_visTexShader_array;
            shadowMap = m_pParaboloidShadowMap;
			near = 0.5f;
			far = 100.f;
        }else{
            visualProgram = m_visTexShader;
            shadowMap = m_pShadowMap;
			near = 0.5f;
			far = LIGHT_RANGE;
        }
    	
    	gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
		gl.glViewport(0, 0, nvApp.getGLContext().width(), nvApp.getGLContext().height());
		gl.glDisable(GLenum.GL_DEPTH_TEST);
		gl.glDisable(GLenum.GL_CULL_FACE);
		gl.glDisable(GLenum.GL_STENCIL_TEST);
		gl.glClearColor(0, 0, 0, 0);
		gl.glClear(GLenum.GL_COLOR_BUFFER_BIT);

        visualProgram.enable();
        visualProgram.setUniforms(near, far, m_shadowSlice, 1.0f);
		gl.glActiveTexture(GLenum.GL_TEXTURE0);
		gl.glBindTexture(shadowMap.getTarget(), shadowMap.getTexture());
		gl.glBindSampler(0, m_psamDefault);
        GLCheck.checkError();
		gl.glBindVertexArray(m_DummyVAO);
		gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
		gl.glBindVertexArray(0);

        m_visTexShader.disable();
	}
	
	private void renderGemetry(boolean renderToFBO){
		m_pScene.setupSceneCBs(m_ViewCBStruct, m_LightCBStruct/*, m_FrameAttribs.cameraAttribs, m_FrameAttribs.lightAttribs*/);
		gl.glViewport(0, 0, nvApp.getGLContext().width(), nvApp.getGLContext().height());
    	
        SceneRenderProgram sceneRender;
        switch (m_pScene.getLightMode()) {
        default:
		case DIRECTIONAL:
			if(m_SceneDirectionalProgram == null)
				m_SceneDirectionalProgram = new SceneRenderProgram(null, LIGHTMODE_DIRECTIONAL);
			
			sceneRender = m_SceneDirectionalProgram;
			break;
		case SPOT:
			if(m_SceneSpotlightProgram == null)
				m_SceneSpotlightProgram = new SceneRenderProgram(null, LIGHTMODE_SPOTLIGHT);
			
			sceneRender = m_SceneSpotlightProgram;
			break;
		case POINT:
			if(m_SceneOMNIProgram == null)
				m_SceneOMNIProgram = new SceneRenderProgram(null, LIGHTMODE_OMNI);
			
			sceneRender = m_SceneOMNIProgram;
			break;
		}
        
        sceneRender.enable();
        GLCheck.checkError();
//        rtManager.setTexture2DRenderTargets(pSceneRT_.getTexture(), pSceneDepth_.getTexture());
//        rtManager.clearRenderTarget(0, 0);
//        rtManager.clearDepthStencilTarget(1.0f, 0);
        
        if(renderToFBO){
	        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, m_RenderTarget);
			gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT0, m_pOffscreenRenderTarget.getTarget(), m_pOffscreenRenderTarget.getTexture(), 0);
			gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_DEPTH_STENCIL_ATTACHMENT, m_pOffscreenDepth.getTarget(), m_pOffscreenDepth.getTexture(), 0);
			gl.glDrawBuffers(GLenum.GL_COLOR_ATTACHMENT0);
			GLCheck.checkError();
	        
	        FloatBuffer colors = CacheBuffer.getCachedFloatBuffer(4);
	        colors.put(0).put(0).put(0).put(0).flip();
			gl.glClearBufferfv(GLenum.GL_COLOR, 0, colors);
			gl.glClearBufferfi(GLenum.GL_DEPTH_STENCIL, 0, 1.f, 0);
        }else{
			gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
			gl.glClearColor(0,0,0,0);
			gl.glClearDepthf(1.0f);
			gl.glClear(GLenum.GL_COLOR_BUFFER_BIT|GLenum.GL_DEPTH_BUFFER_BIT);
        }

//        sceneRender.enable(shadowmap_srv.getTexture());
//        shadowmap_srv.bind(0, ss_shadowmap_);
//        m_pShadowMap.bind(0, m_psamComparison);
		gl.glActiveTexture(GLenum.GL_TEXTURE0);
		if(m_pScene.getLightMode() == LightType.POINT){
			gl.glBindTexture(m_pParaboloidShadowMap.getTarget(), m_pParaboloidShadowMap.getTexture());
		}else{
			gl.glBindTexture(m_pShadowMap.getTarget(), m_pShadowMap.getTexture());
		}

		gl.glBindSampler(0, m_psamComparison);
        renderScene(sceneRender);
//        m_pShadowMap.unbind();
		gl.glBindSampler(0, 0);
        
        if(!m_bPrintProgram /*&& m_bEnableLightScattering*/){
			sceneRender.setName("SceneRender");
        	sceneRender.printPrograminfo();
//        	ProgramProperties props = GLSLUtil.getProperties(sceneRender.getProgram());
//        	System.out.println("SceneRender: ");
//        	System.out.println(props);
        }
        sceneRender.disable();
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
	}
	
	final float[] scene_range = { -6, -3, 3, 6 };
	final Vector3f tempVec3 = new Vector3f();
	private void renderScene(BaseProgram program){
//		 NV_PERFEVENT(ctx, "Draw Scene Geometry");

		gl.glDisable(GLenum.GL_STENCIL_TEST);
		gl.glEnable(GLenum.GL_DEPTH_TEST);
		gl.glDepthFunc(GLenum.GL_LESS);
		gl.glDepthMask(true);
		gl.glBindVertexArray(m_DummyVAO);
		gl.glDisable(GLenum.GL_CULL_FACE);
		
		int idx = 0;
		for(float x : scene_range){
			for(float y : scene_range){
				for(float z: scene_range){
					Vector3f offset = tempVec3;
					offset.set(x, y, z);
					m_pScene.setupObjectCB(m_ObjectCBStruct, offset);
					setupUniforms(program);
					if(idx < 8){
						program.setCubeColor(1, 0, 0);
					}else{
						program.setCubeColor(1, 1, 1);
					}

					gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 36);
					
					idx ++;
				}
			}
		}

		gl.glBindVertexArray(0);
		gl.glDisable(GLenum.GL_DEPTH_TEST);
	}
	
	private void setupUniforms(BaseProgram program){
		// Modify the object transform to make the scene as same as directx.
		Matrix4f mat = m_ObjectCBStruct.mTransform;
		Matrix4f.mul(mat, posCorrectMat, mat);
		
		program.setupUniforms(m_LightCBStruct);
		program.setupUniforms(m_ObjectCBStruct);
		program.setupUniforms(m_ViewCBStruct);
	}
	
	public void onDestroy() {
		if(m_pShadowMap != null){
			m_pShadowMap.dispose();
			m_pShadowMap = null;
		}
		
		if(m_pOffscreenRenderTarget != null){
			m_pOffscreenRenderTarget.dispose();
			m_pOffscreenRenderTarget = null;
		}
		
		if(m_pOffscreenDepth != null){
			m_pOffscreenDepth.dispose();
			m_pOffscreenDepth = null;
		}
	}

	public float getSceneNearPlane(){ return m_ViewCBStruct.zNear;}
	public float getSceneFarPlane() { return m_ViewCBStruct.zFar;}
	public Matrix4f getViewMat()    { return m_ViewCBStruct.mView; }
	public Matrix4f getProjMat()    { return m_ViewCBStruct.mProj;}
	public Matrix4f getProjViewMat(){ return m_ViewCBStruct.mProjView;}
	public float getFovInRadian()   { return SPOTLIGHT_FALLOFF_ANGLE * 2.0f;}
	public Vector3f getLightDir()   { return m_LightCBStruct.vLightDirection;}
	public Vector3f getLightPos()   { return m_LightCBStruct.vLightPos;}
	public Matrix4f getLightProjMat() { return m_pScene.lightProj;}
	public Matrix4f getLightViewMat() { return m_LightCBStruct.mLightView;}
	public Matrix4f[] getCubeLightViewMats(){
		if(m_pScene.lightMode == LightType.POINT){
			Matrix4f[] vies = new Matrix4f[6];
			for(int i = 0; i < vies.length; i++) vies[i] = new Matrix4f();

			return vies;
		}

		return null;
	}
	public float getLightNearPlane(){ return m_LightCBStruct.zNear;}
	public float getLightFarlane()  { return m_LightCBStruct.zFar;}
	public LightType getLightMode() {	return m_pScene.getLightMode();}
	public void getLightIntensity(Vector3f out) { m_pScene.getLightIntensity(out);}

	public Texture2D getShadowMap() { return m_pScene.getLightMode() == LightType.POINT ? m_pParaboloidShadowMap : m_pShadowMap;}

	public void setLightType(LightType type) {m_pScene.lightMode = type;}
	final class SceneController {

		LightType lightMode = LightType.POINT;
		int lightPower_;
		int viewpoint_ = 0;

		final NvInputTransformer m_transformer;

		final Matrix4f sceneTransform_ = new Matrix4f();
		final Matrix4f lightTransform_ = new Matrix4f();
		final Matrix4f lightProj = new Matrix4f();

		final Vector3f[] LIGHT_POWER = {
				new Vector3f(1.00f, 0.95f, 0.90f),
				new Vector3f(0.50f, 0.475f, 0.45f),
				new Vector3f(1.50f, 0.95f * 1.5f, 0.90f * 1.5f),
				new Vector3f(1.00f, 0.95f, 0.90f),
				new Vector3f(1.00f, 0.75f, 0.50f),
				new Vector3f(0.75f, 1.00f, 0.75f),
				new Vector3f(0.50f, 0.75f, 1.00f),
		};

		final Vector3f[] VIEWPOINTS = {
				new Vector3f(0, 0, -DISTANCE),
				new Vector3f(0, DISTANCE, 0),
				new Vector3f(0, -DISTANCE, 0),
				new Vector3f(DISTANCE, 0, 0),
		};

		final ReadableVector3f[] VIEWPOINT_UP = {
				Vector3f.Y_AXIS,
				Vector3f.Z_AXIS_NEG,
				Vector3f.Z_AXIS_NEG,
				Vector3f.Y_AXIS,
		};

		private long time_elapsed_us = 0;
		private float total_angle;
		private boolean isPaused_ = false;

		SceneController(NvInputTransformer transformer) {
			m_transformer = transformer;
			m_transformer.setMotionMode(NvCameraMotionType.FIRST_PERSON);
			m_transformer.setTranslation(0, 0, DISTANCE);
			m_transformer.setMaxTranslationVel(20);
		}

		void setupSceneCBs(ViewCB pView, LightCB pLight){
			getViewerDesc(pView);
//			pView.mProj.load(viewAttribs.mViewProjT);
//			pView.vEyePos.set(viewAttribs.f4CameraPos);
//			pView.zNear = 0.50f;
//			pView.zFar = 50.0f;

//			getLightDesc(lightAttribs);
			getLightViewpoint(pLight.vLightPos, pLight.mLightView, pLight.mLightViewProj);

//			Vector4f vDirOnLight = lightAttribs.f4DirOnLight;
//        pLight.vLightDirection = (-pLight.vLightPos);
//        pLight.vLightDirection.normalize();
			Vector3f vLightDirection = pLight.vLightDirection;
			Vector3f.scale(pLight.vLightPos, -1, vLightDirection);
			vLightDirection.normalise();

			pLight.fLightFalloffCosTheta = (float) Math.cos(SPOTLIGHT_FALLOFF_ANGLE);
			pLight.fLightFalloffPower = SPOTLIGHT_FALLOFF_POWER;
			getLightIntensity(pLight.vLightColor);
			if (lightMode == LightType.SPOT)
			{
				pLight.vLightAttenuationFactors.set(1.0f, 2.0f, 1.0f, 0.0f);
				pLight.vSigmaExtinction.set(0.002096f, 0.0028239999f, 0.00481f);
//            pLight.vLightAttenuationFactors = Nv::NvVec4(lightDesc->Spotlight.fAttenuationFactors);
//				pLight.vLightAttenuationFactors.set(lightAttribs.f4AttenuationFactors);
//				Vector3f.sub(lightAttribs.f4LightWorldPos, viewAttribs.f4CameraPos,vDirOnLight);
			}
			else if (lightMode == LightType.POINT)
			{
				pLight.vLightAttenuationFactors.set(1.0f, 4.0f, 4.0f, 0.0f);
				pLight.vSigmaExtinction.set(0.002096f, 0.0028239999f, 0.00481f);

//            pLight.vLightAttenuationFactors = Nv::NvVec4(lightDesc->Omni.fAttenuationFactors);
//				pLight.vLightAttenuationFactors.set(lightAttribs.f4AttenuationFactors);
//				Vector3f.sub(lightAttribs.f4LightWorldPos, viewAttribs.f4CameraPos, vDirOnLight);
			}else {  // Direction Light
//				vDirOnLight.set(10, 15, 5);
			}
//			vDirOnLight.normalise();
			pLight.zNear = 0.50f;
			pLight.zFar = LIGHT_RANGE;

//			final MediumDesc mediumDesc = getMediumDesc();
////        pLight.vSigmaExtinction = *reinterpret_cast<const Nv::NvVec3 *>(&mediumDesc->vAbsorption);
//			pLight.vSigmaExtinction.set(mediumDesc.vAbsorption);
//			for (int t = 0; t < mediumDesc.uNumPhaseTerms; ++t)
//			{
////            pLight.vSigmaExtinction = pLight.vSigmaExtinction + *reinterpret_cast<const Nv::NvVec3 *>(&mediumDesc->PhaseTerms[t].vDensity);
//				Vector3f.add(pLight.vSigmaExtinction, mediumDesc.phaseTerms[t].vDensity, pLight.vSigmaExtinction);
//			}
		}

		void getViewerDesc(ViewCB attribs){
			final ReadableVector3f vOrigin = Vector3f.ZERO;

			final ReadableVector3f vEyePos = VIEWPOINTS[viewpoint_];
			final Matrix4f view      = attribs.mView;
			final Matrix4f mViewProj = attribs.mProjView;
			final Matrix4f mProj     = attribs.mProj;

			final float near = 0.5f;
			final float far  = 100.f;

			Matrix4f.lookAt(vEyePos, vOrigin, VIEWPOINT_UP[viewpoint_], view);
			Matrix4f.perspective((float)Math.toDegrees(SPOTLIGHT_FALLOFF_ANGLE * 2.0f), (float)m_pOffscreenRenderTarget.getWidth()/m_pOffscreenRenderTarget.getHeight(),
					near, far, mProj);
			Matrix4f.mul(mProj, view, mViewProj);

			attribs.vEyePos.set(vEyePos.getX(), vEyePos.getY(), vEyePos.getZ());
			attribs.zNear = near;
			attribs.zFar  = far;
		}

		void getLightViewpoint(Vector3f _vPos, Matrix4f view, Matrix4f mViewProj) {
			final ReadableVector3f vOrigin = Vector3f.ZERO;
			final ReadableVector3f vUp = Vector3f.Y_AXIS;

			final WritableVector3f vPos = (WritableVector3f) (_vPos);
			final ReadableVector3f rvPos = (ReadableVector3f) (_vPos);

			switch (lightMode) {
				case POINT: {
					vPos.set(15, 10, 0);
					Matrix4f.transformVector(lightTransform_, rvPos, vPos);
					mViewProj.setIdentity();
					mViewProj.m30 = -rvPos.getX();
					mViewProj.m31 = -rvPos.getY();
					mViewProj.m32 = -rvPos.getZ();

					view.load(mViewProj);
				}
				break;

				case SPOT: {
					vPos.set(10, 15, 5);
					Matrix4f.lookAt(rvPos, vOrigin, vUp, view);
					Matrix4f.perspective((float) Math.toDegrees(SPOTLIGHT_FALLOFF_ANGLE * 2.0f),
							1.0f,
							0.50f, LIGHT_RANGE, lightProj);
					Matrix4f.mul(lightProj, view, mViewProj);
				}
				break;

				default:
				case DIRECTIONAL: {
					vPos.set(10, 15, 5);
					Matrix4f.lookAt(rvPos, vOrigin, vUp, view);
					float min = -25.0f;
					float max = 25.0f;
					Matrix4f.ortho(min, max, min, max, 0.50f, LIGHT_RANGE, lightProj);

					Matrix4f.mul(lightProj, view, mViewProj);
				}
				break;
			}
		}

		void animate(float dt) {
			// Update the scene
			if (!isPaused_) {
				time_elapsed_us += (long) (dt * 1000000.0);
				if (lightMode != LightType.POINT) {
					float angle = dt * SPOTLIGHT_FALLOFF_ANGLE;
					total_angle += angle;
					sceneTransform_.setIdentity();
					sceneTransform_.rotate(total_angle, Vector3f.Y_AXIS);
				} else {
					sceneTransform_.setIdentity();
				}

				if (lightMode == LightType.POINT) {
					final long CYCLE_LENGTH = 60000000;
					float cyclePhase = (float) (time_elapsed_us % CYCLE_LENGTH) / (float) (CYCLE_LENGTH);
					lightTransform_.setIdentity();
					lightTransform_.m00 = (float) Math.cos(2.0 * Math.PI * 7.0 * cyclePhase);
					lightTransform_.m11 = (float) Math.cos(2.0 * Math.PI * 3.0 * cyclePhase);
				}
			}
		}

		void setupObjectCB(ObjectCB pObject, Vector3f offset) {
//    	pObject->mTransform = sceneTransform_*Nv::NvTransform(offset);
			pObject.mTransform.load(sceneTransform_);
			pObject.mTransform.translate(offset);

			pObject.vColor[0] = 1.0f;
			pObject.vColor[1] = 1.0f;
			pObject.vColor[2] = 1.0f;
		}

		void setupLightViewCB(ViewCB pView) {
			getLightViewpoint(pView.vEyePos, pView.mView,  pView.mProjView);
			pView.zNear = 0.50f;
			pView.zFar = LIGHT_RANGE;
		}

		void getLightIntensity(Vector3f intensity) {
			switch (getLightMode()) {
				case POINT:
					Vector3f.scale(LIGHT_POWER[lightPower_], 25000.0f, intensity);
					break;

				case SPOT:
					Vector3f.scale(LIGHT_POWER[lightPower_], 50000.0f, intensity);
					break;

				default:
				case DIRECTIONAL:
					Vector3f.scale(LIGHT_POWER[lightPower_], 250.0f, intensity);
					break;
			}
		}

		LightType getLightMode() {
			return lightMode;
		}

		void toggleViewpoint() {
			viewpoint_ = (viewpoint_ + 1) % 4;
		}
		void toggleLightMode() //{ lightMode = (lightMode + 1) % 3;}
		{
			int idx = lightMode.ordinal();
			idx = (idx + 1) % 3;
			lightMode = LightType.values()[idx];
		}
	}

	final class ObjectCB {
		public final Matrix4f mTransform = new Matrix4f();
		public final float[] vColor = new float[3];
	}

	final class ViewCB {
		public final Matrix4f mProj = new Matrix4f();
		public final Matrix4f mView = new Matrix4f();
		public final Matrix4f mProjView = new Matrix4f();
		public final Vector3f vEyePos = new Vector3f();
		public float zNear;
		public float zFar;
	}

	final class LightCB {
		public final Matrix4f mLightViewProj = new Matrix4f();
		public final Matrix4f mLightView = new Matrix4f();
//		public final Matrix4f mLightProj = new Matrix4f();
		public final Vector3f vLightDirection = new Vector3f();
		public float fLightFalloffCosTheta;
		public final Vector3f vLightPos = new Vector3f();
		public float fLightFalloffPower;
		public final Vector3f vLightColor = new Vector3f();
		public final Vector4f vLightAttenuationFactors = new Vector4f();
		public float zNear;
		public float zFar;
		public final Vector3f vSigmaExtinction = new Vector3f();
	}

	abstract class BaseProgram extends GLSLProgram{
		SceneController context;

		private int mEyePosLoc = -1;
		private int mObjectLoc = -1;
		private int mLightFalloffCosThetaLoc = -1;
		private int mLightViewProjLoc = -1;
		private int mLightDirectionLoc = -1;
		private int mViewProjLoc = -1;
		private int mObjectColorLoc = -1;
		private int mZFarLoc = -1;
		private int mLightAttenuationFactorsLoc = -1;
		private int mLightFalloffPowerLoc = -1;
		private int mLightPosLoc = -1;
		private int mZNearLoc = -1;
		private int mLightZNearLoc = -1;
		private int mLightZNFarLoc = -1;
		private int mSigmaExtinctionLoc = -1;
		private int mLightColorLoc = -1;
		private int mCubeColor = -1;

		protected BaseProgram(SceneController context) {
			this.context = context;
		}

		protected void compileProgram(String vertFile, String geomFile, String fragFile, Macro...macros) throws IOException{
			if(geomFile!=null){
				CharSequence vertSrc = ShaderLoader.loadShaderFile(vertFile, false);
				CharSequence geomSrc = ShaderLoader.loadShaderFile(geomFile, false);
				CharSequence fragSrc = ShaderLoader.loadShaderFile(fragFile, false);

				ShaderSourceItem vs_item = new ShaderSourceItem(vertSrc, ShaderType.VERTEX);
				ShaderSourceItem gs_item = new ShaderSourceItem(geomSrc, ShaderType.GEOMETRY);
				ShaderSourceItem ps_item = new ShaderSourceItem(fragSrc, ShaderType.FRAGMENT);

				vs_item.macros = macros;
				gs_item.macros = macros;
				ps_item.macros = macros;
				setSourceFromStrings(vs_item, gs_item, ps_item);
			}else{
				setSourceFromFiles(vertFile, fragFile, macros);
			}

			initUniformData();
			GLCheck.checkError();
		}

		protected void initUniformData(){
			int m_program = getProgram();
			mEyePosLoc = gl.glGetUniformLocation(m_program, "c_vEyePos");
			mObjectLoc = gl.glGetUniformLocation(m_program, "c_mObject");
			mLightFalloffCosThetaLoc = gl.glGetUniformLocation(m_program, "c_fLightFalloffCosTheta");
			mLightViewProjLoc = gl.glGetUniformLocation(m_program, "c_mLightViewProj");
			mLightDirectionLoc = gl.glGetUniformLocation(m_program, "c_vLightDirection");
			mViewProjLoc = gl.glGetUniformLocation(m_program, "c_mViewProj");
			mObjectColorLoc = gl.glGetUniformLocation(m_program, "c_vObjectColor");
			mZFarLoc = gl.glGetUniformLocation(m_program, "c_fZFar");
			mLightAttenuationFactorsLoc = gl.glGetUniformLocation(m_program, "c_vLightAttenuationFactors");
			mLightFalloffPowerLoc = gl.glGetUniformLocation(m_program, "c_fLightFalloffPower");
			mLightPosLoc = gl.glGetUniformLocation(m_program, "c_vLightPos");
			mZNearLoc = gl.glGetUniformLocation(m_program, "c_fZNear");
			mLightZNearLoc = gl.glGetUniformLocation(m_program, "c_fLightZNear");
			mLightZNFarLoc = gl.glGetUniformLocation(m_program, "c_fLightZNFar");
			mSigmaExtinctionLoc = gl.glGetUniformLocation(m_program, "c_vSigmaExtinction");
			mLightColorLoc = gl.glGetUniformLocation(m_program, "c_vLightColor");
			mCubeColor = gl.glGetUniformLocation(m_program, "g_CubeColor");
		}

		public void setupUniforms(ViewCB data){
			setViewProj(data.mProjView);
			setEyePos(data.vEyePos);
			setZNear(data.zNear);
			setZFar(data.zFar);
		}

		public void setupUniforms(LightCB data){
			setLightViewProj(data.mLightViewProj);
			setLightDirection(data.vLightDirection);
			setLightFalloffCosTheta(data.fLightFalloffCosTheta);
			setLightPos(data.vLightPos);
			setLightFalloffPower(data.fLightFalloffPower);
			setLightColor(data.vLightColor);
			setLightAttenuationFactors(data.vLightAttenuationFactors);
			setLightZNear(data.zNear);
			setLightZNFar(data.zFar);
			setSigmaExtinction(data.vSigmaExtinction);
		}

		public void setupUniforms(ObjectCB data){
			setObject(data.mTransform);
			setObjectColor(data.vColor);
		}

		private void setEyePos(Vector3f v) { if(mEyePosLoc >= 0)gl.glUniform3f(mEyePosLoc, v.x, v.y, v.z);}
		private void setObject(Matrix4f mat) { if(mObjectLoc >= 0)gl.glUniformMatrix4fv(mObjectLoc, false, CacheBuffer.wrap(mat));}
		private void setLightFalloffCosTheta(float f) { if(mLightFalloffCosThetaLoc >= 0)gl.glUniform1f(mLightFalloffCosThetaLoc, f);}
		private void setLightViewProj(Matrix4f mat) { if(mLightViewProjLoc >= 0)gl.glUniformMatrix4fv(mLightViewProjLoc, false, CacheBuffer.wrap(mat));}
		private void setLightDirection(Vector3f v) { if(mLightDirectionLoc >= 0)gl.glUniform3f(mLightDirectionLoc, v.x, v.y, v.z);}
		private void setViewProj(Matrix4f mat) { if(mViewProjLoc >= 0)gl.glUniformMatrix4fv(mViewProjLoc, false, CacheBuffer.wrap(mat));}
		private void setObjectColor(float[] v) { if(mObjectColorLoc >= 0)gl.glUniform3f(mObjectColorLoc, v[0], v[1], v[2]);}
		private void setZFar(float f) { if(mZFarLoc >= 0)gl.glUniform1f(mZFarLoc, f);}
		private void setLightAttenuationFactors(Vector4f v) { if(mLightAttenuationFactorsLoc >= 0)gl.glUniform4f(mLightAttenuationFactorsLoc, v.x, v.y, v.z, v.w);}
		private void setLightFalloffPower(float f) { if(mLightFalloffPowerLoc >= 0)gl.glUniform1f(mLightFalloffPowerLoc, f);}
		private void setLightPos(Vector3f v) { if(mLightPosLoc >= 0)gl.glUniform3f(mLightPosLoc, v.x, v.y, v.z);}
		private void setZNear(float f) { if(mZNearLoc >= 0)gl.glUniform1f(mZNearLoc, f);}
		private void setLightZNear(float f) { if(mLightZNearLoc >= 0)gl.glUniform1f(mLightZNearLoc, f);}
		private void setLightZNFar(float f) { if(mLightZNFarLoc >= 0)gl.glUniform1f(mLightZNFarLoc, f);}
		private void setSigmaExtinction(Vector3f v) { if(mSigmaExtinctionLoc >= 0)gl.glUniform3f(mSigmaExtinctionLoc, v.x, v.y, v.z);}
		private void setLightColor(Vector3f v) { if(mLightColorLoc >= 0)gl.glUniform3f(mLightColorLoc, v.x, v.y, v.z);}
		public void setCubeColor(float r, float g, float b) { if(mCubeColor >= 0)gl.glUniform4f(mCubeColor, r, g, b, 1);}
	}

	final class SceneRenderProgram extends BaseProgram{
		public SceneRenderProgram(SceneController context, int method) {
			super(context);

			Macro[] psMacros = {
					new Macro("LIGHTMODE_DIRECTIONAL", LIGHTMODE_DIRECTIONAL),
					new Macro("LIGHTMODE_SPOTLIGHT", LIGHTMODE_SPOTLIGHT),
					new Macro("LIGHTMODE_OMNI", LIGHTMODE_OMNI),
					new Macro("LIGHTMODE", method),
			};

			try {
				compileProgram("Scenes/Cube16/shaders/scene_VS.vert", null, "Scenes/Cube16/shaders/scene_PS.frag", psMacros);
			} catch (IOException e) {
				e.printStackTrace();
			}

			enable();
			setTextureUniform("tShadowmap", 0);
			setTextureUniform("tShadowmapArray", 0);
		}
	}

	final class DepthProgram extends BaseProgram{

		public DepthProgram(SceneController context) {
			super(context);

			try {
				compileProgram("Scenes/Cube16/shaders/scene_VS.vert", null, null);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	final class DepthGSProgram extends BaseProgram{
		public DepthGSProgram(SceneController context) {
			super(context);

			try {
				compileProgram("Scenes/Cube16/shaders/scene_VS.vert", "Scenes/Cube16/shaders/scene_GS.frag", "Scenes/Cube16/shaders/Dummy_PS.frag");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
