package jet.opengl.demos.gpupro.volumetricfog;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector3i;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.util.CacheBuffer;

final class LocalLightData implements Readable {

    final Matrix4f DirectionalLightWorldToStaticShadow = new Matrix4f();
    final Vector4f DirectionalLightStaticShadowBufferSize = new Vector4f();

    final Matrix4f[] DirectionalLightWorldToShadowMatrix;
    final Vector4f[] DirectionalLightShadowmapMinMax;
    final Vector4f DirectionalLightShadowmapAtlasBufferSize= new Vector4f();

    final Vector3f DirectionalLightColor = new Vector3f();
    float DirectionalLightVolumetricScatteringIntensity;
    final Vector3f DirectionalLightDirection = new Vector3f();
    float DirectionalLightDepthBias;

    final Vector3i CulledGridSize = new Vector3i();
    int LightGridPixelSizeShift;

    final Vector4f[] ForwardLocalLightBuffer = new Vector4f[10];

    boolean HasDirectionalLight;
    boolean DirectionalLightUseStaticShadowing;
    final int NumDirectionalLightCascades;
    int NumLocalLights;

    final Vector3f LightGridZParams = new Vector3f();
    final float[] CascadeEndDepths = new float[4];

    final Vector2f DirectionalLightDistanceFadeMAD= new Vector2f();
    int DirectionalLightShadowMapChannelMask;
    final int size;

    LocalLightData(int numCascade){
        this.NumDirectionalLightCascades = numCascade;

        DirectionalLightWorldToShadowMatrix = new Matrix4f[numCascade];
        DirectionalLightShadowmapMinMax = new Vector4f[numCascade];

        for(int i = 0; i < numCascade; i++) {
            DirectionalLightWorldToShadowMatrix[i] = new Matrix4f();
            DirectionalLightShadowmapMinMax[i] = new Vector4f();
        }

        size = Vector4f.SIZE * (numCascade+19)+Matrix4f.SIZE * (numCascade+1);

        for(int i = 0; i < ForwardLocalLightBuffer.length; i++){
            ForwardLocalLightBuffer[i] = new Vector4f();
        }
    }

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        DirectionalLightWorldToStaticShadow.store(buf);
        DirectionalLightStaticShadowBufferSize.store(buf);
        CacheBuffer.put(buf, DirectionalLightWorldToShadowMatrix);
        CacheBuffer.put(buf, DirectionalLightShadowmapMinMax);
        DirectionalLightShadowmapAtlasBufferSize.store(buf);
        DirectionalLightColor.store(buf);
        buf.putFloat(DirectionalLightVolumetricScatteringIntensity);
        DirectionalLightDirection.store(buf);
        buf.putFloat(DirectionalLightDepthBias);
        CulledGridSize.store(buf);
        buf.putInt(LightGridPixelSizeShift);
        CacheBuffer.put(buf, ForwardLocalLightBuffer);
        buf.putInt(HasDirectionalLight?1:0);
        buf.putInt(DirectionalLightUseStaticShadowing?1:0);
        buf.putInt(NumDirectionalLightCascades);
        buf.putInt(NumLocalLights);

        LightGridZParams.store(buf);
        buf.putInt(0);
        CacheBuffer.put(buf, CascadeEndDepths);

        DirectionalLightDistanceFadeMAD.store(buf);
        buf.putInt(DirectionalLightShadowMapChannelMask);
        buf.putInt(size);
        return buf;
    }

    public final int size(){
        return size;
    }
}
