package jet.opengl.renderer.Unreal4;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.nio.ByteBuffer;

import jet.opengl.renderer.Unreal4.atmosphere.FAtmospherePrecomputeParameters;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.Recti;
import jet.opengl.renderer.Unreal4.scenes.FSceneViewState;

 class FViewInfo2 {
    private static final int FLAG_VIEW_BUFFER = 1;
    private static final int FLAG_VIEW_FORWARD_LIGHT = 2;

    public float FurthestReflectionCaptureDistance = 0;
    public float NearClippingDistance;
    public float FarClippingDistance;
    public int NumBoxReflectionCaptures;
    public int NumSphereReflectionCaptures;
    public FForwardLightingResources ForwardLightingResources;

    public BufferGL ViewBuffer;
    public FSceneViewState ViewState;

    private int mFlags;

    public final Matrix4f ViewToClip = new Matrix4f();
    public final Matrix4f ClipToView = new Matrix4f();
    public final Matrix4f TranslatedWorldToView = new Matrix4f();
    public final Matrix4f ViewToTranslatedWorld = new Matrix4f();

    public final Vector3f ViewOrigin = new Vector3f();

    public final Recti ViewRect = new Recti();

    public final FAtmospherePrecomputeParameters AtmospherePrecomputeParams = new FAtmospherePrecomputeParameters();

    public void updateViews(int viewWidth, int viewHeight, Matrix4f view, Matrix4f proj, float cameraFar, float cameraNear){
        ViewRect.set(0,0,viewWidth, viewHeight);
        NearClippingDistance = cameraNear;
        FarClippingDistance = cameraFar;

        TranslatedWorldToView.load(view);
        ViewToClip.load(proj);

        Matrix4f.invert(ViewToClip, ClipToView);
        Matrix4f.invert(TranslatedWorldToView, ViewToTranslatedWorld);

        Matrix4f.decompseRigidMatrix(TranslatedWorldToView, ViewOrigin, null, null);

        mFlags |= FLAG_VIEW_BUFFER;
        updateViewBuffer();
    }

    /** Call this method manually when necessary. */
    public void updateForwardLightData(){
        mFlags |= FLAG_VIEW_FORWARD_LIGHT;

        _updateForwardLightData();
    }

    public boolean IsPerspectiveProjection(){
        return true;
    }

    private void _updateForwardLightData(){
        if((mFlags & FLAG_VIEW_FORWARD_LIGHT) != 0){
            if(!GLFuncProviderFactory.isInitlized())
                return;

            if(ForwardLightingResources.ForwardLightDataUniformBuffer == null){
                ForwardLightingResources.ForwardLightDataUniformBuffer = new BufferGL();
                ForwardLightingResources.ForwardLightDataUniformBuffer.initlize(GLenum.GL_UNIFORM_BUFFER, FForwardLightData.SIZE, null, GLenum.GL_DYNAMIC_READ);
            }

            ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(FForwardLightData.SIZE);
            ForwardLightingResources.ForwardLightData.store(buffer);
            buffer.flip();

            if(buffer.remaining() != FForwardLightData.SIZE)
                throw new IllegalStateException("Inner error!");

            ForwardLightingResources.ForwardLightDataUniformBuffer.update(0, buffer);

            mFlags &= (~FLAG_VIEW_FORWARD_LIGHT);
        }
    }

    private void updateViewBuffer(){
        if((mFlags & FLAG_VIEW_BUFFER) != 0){
            if(!GLFuncProviderFactory.isInitlized())
                return;

            if(ViewBuffer == null){
                final int ViewBufferSize = 288;
                ViewBuffer = new BufferGL();
                ViewBuffer.initlize(GLenum.GL_UNIFORM_BUFFER, ViewBufferSize, null, GLenum.GL_DYNAMIC_READ);
            }

            ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(ViewBuffer.getBufferSize());
            ViewToClip.store(buffer);
            ClipToView.store(buffer);
            TranslatedWorldToView.store(buffer);
            ViewToTranslatedWorld.store(buffer);
            // Vieport
            buffer.putFloat(ViewRect.width).putFloat(ViewRect.height).putFloat(1.0f/ViewRect.width).putFloat(1.0f/ViewRect.height);
            // PreTranslate
            buffer.putFloat(0).putFloat(0).putFloat(0).putFloat(0);
            buffer.flip();

            if(buffer.remaining() != ViewBuffer.getBufferSize())
                throw new IllegalStateException("Inner error!");

            ViewBuffer.update(0, buffer);

            mFlags &= (~FLAG_VIEW_BUFFER);
        }
    }

    public void bindResources(){
        if(!GLFuncProviderFactory.isInitlized())
            return;

        final GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();

        updateViewBuffer();
        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 8, ViewBuffer.getBuffer());

        _updateForwardLightData();
        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 9, ForwardLightingResources.ForwardLightDataUniformBuffer!= null ? ForwardLightingResources.ForwardLightDataUniformBuffer.getBuffer() : 0);

        // TODO ReflectionCapture: later to do this
        gl.glBindTextureUnit(11, ForwardLightingResources.ForwardLocalLightBuffer != null ? ForwardLightingResources.ForwardLocalLightBuffer.getTexture() : 0);
        gl.glBindTextureUnit(12, ForwardLightingResources.NumCulledLightsGrid != null ? ForwardLightingResources.NumCulledLightsGrid.getTexture() : 0);
        gl.glBindTextureUnit(13, ForwardLightingResources.CulledLightDataGrid != null ? ForwardLightingResources.CulledLightDataGrid.getTexture() : 0);
    }

    private FViewInfo2(){
        ForwardLightingResources = new FForwardLightingResources();
    }

    private static FViewInfo2 g_Instance;

    public static FViewInfo2 getInstance(){
        if(g_Instance == null)
            g_Instance = new FViewInfo2();

        return g_Instance;
    }
}
