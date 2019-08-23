package jet.opengl.demos.Unreal4;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector3i;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;
import java.util.Arrays;

import jet.opengl.postprocessing.util.CacheBuffer;

public class FForwardLightData implements Readable {
    public static final int SIZE = 544;

    public final Matrix4f DirectionalLightWorldToStaticShadow = new Matrix4f();
    public final Vector4f DirectionalLightStaticShadowBufferSize = new Vector4f();

    public final Matrix4f[] DirectionalLightWorldToShadowMatrix;
    public final Vector4f[] DirectionalLightShadowmapMinMax;
    public final Vector4f DirectionalLightShadowmapAtlasBufferSize= new Vector4f();

    public final Vector3f DirectionalLightColor = new Vector3f();
    public float DirectionalLightVolumetricScatteringIntensity;
    public final Vector3f DirectionalLightDirection = new Vector3f();
    public float DirectionalLightDepthBias;

    public final Vector3i CulledGridSize = new Vector3i();
    public int LightGridPixelSizeShift;

//    public final Vector4f[] ForwardLocalLightBuffer = new Vector4f[10];

    public boolean HasDirectionalLight;
    public boolean DirectionalLightUseStaticShadowing;
    public int NumDirectionalLightCascades;
    public int NumLocalLights;

    public final Vector3f LightGridZParams = new Vector3f();
    public final float[] CascadeEndDepths = new float[4];

    public int DirectionalLightShadowMapChannelMask;
    public int NumGridCells;
    public int MaxCulledLightsPerCell;
    public int NumReflectionCaptures;

    public final Vector2f DirectionalLightDistanceFadeMAD= new Vector2f();
    public final int size;

    public TextureBuffer ForwardLocalLightBuffer;
    public TextureBuffer NumCulledLightsGrid;
    public TextureBuffer CulledLightDataGrid;

    public FForwardLightData(int numCascade){
        this.NumDirectionalLightCascades = numCascade;

        DirectionalLightWorldToShadowMatrix = new Matrix4f[numCascade];
        DirectionalLightShadowmapMinMax = new Vector4f[numCascade];

        for(int i = 0; i < numCascade; i++) {
            DirectionalLightWorldToShadowMatrix[i] = new Matrix4f();
            DirectionalLightShadowmapMinMax[i] = new Vector4f();
        }

        size = Vector4f.SIZE * (numCascade+19 - 10)+Matrix4f.SIZE * (numCascade+1);

//        for(int i = 0; i < ForwardLocalLightBuffer.length; i++){
//            ForwardLocalLightBuffer[i] = new Vector4f();
//        }
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

        buf.putInt(HasDirectionalLight?1:0);
        buf.putInt(DirectionalLightUseStaticShadowing?1:0);
        buf.putInt(NumDirectionalLightCascades);
        buf.putInt(NumLocalLights);

        LightGridZParams.store(buf); buf.putInt(0);
        CacheBuffer.put(buf, CascadeEndDepths);

        buf.putInt(DirectionalLightShadowMapChannelMask);
        buf.putInt(NumGridCells);
        buf.putInt(MaxCulledLightsPerCell);
        buf.putInt(NumReflectionCaptures);

        DirectionalLightDistanceFadeMAD.store(buf);  buf.putLong(0);
        return buf;
    }

    public void reset(){
        DirectionalLightWorldToStaticShadow.setIdentity();
        DirectionalLightStaticShadowBufferSize.set(0,0,0,0);
        for(int i = 0; i < DirectionalLightWorldToShadowMatrix.length; i++)
            DirectionalLightWorldToShadowMatrix[i].setIdentity();

        for(int i = 0; i < DirectionalLightShadowmapMinMax.length;i++)
            DirectionalLightShadowmapMinMax[i].set(0,0,0,0);

        DirectionalLightShadowmapAtlasBufferSize.set(0,0,0,0);
        DirectionalLightColor.set(0,0,0);
        DirectionalLightVolumetricScatteringIntensity = 0;
        DirectionalLightDirection.set(0,0,0);
        DirectionalLightDepthBias = 0;

        CulledGridSize.set(0,0,0);
        LightGridPixelSizeShift = 0;

//    public final Vector4f[] ForwardLocalLightBuffer = new Vector4f[10];

        HasDirectionalLight = false;
        DirectionalLightUseStaticShadowing = false;
        NumDirectionalLightCascades = 0;
        NumLocalLights = 0;

        LightGridZParams.set(0,0,0);
        Arrays.fill(CascadeEndDepths, 0);

        DirectionalLightShadowMapChannelMask = 0;
        NumGridCells = 0;
        MaxCulledLightsPerCell = 0;
        NumReflectionCaptures = 0;

        DirectionalLightDistanceFadeMAD.set(0,0);
    }

    public final int sizeInBytes(){
        return SIZE;
    }
}
