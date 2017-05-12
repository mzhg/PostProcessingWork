package jet.opengl.postprocessing.core.ssao;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

/**
 * Created by mazhen'gui on 2017/5/12.
 */

final class HBAOData {

    static final int AO_RANDOMTEX_SIZE = 4;
    static final int SIZE = (20 + 16 * 2 * 4) * 4 + Matrix4f.SIZE * 2;

    float   RadiusToScreen;        // radius
    float   R2;     // 1/radius
    float   NegInvR2;     // radius * radius
    float   NDotVBias;

    final Vector2f InvFullResolution = new Vector2f();
    final Vector2f InvQuarterResolution = new Vector2f();

    float   AOMultiplier;
    float   PowExponent;
    //		final Vector2f _pad0;
    float _pad0;
    float _pad1;

    final Vector4f projInfo = new Vector4f();
    final Vector2f projScale = new Vector2f();
    int projOrtho;
    int _pad2;

    final Vector4f[] float2Offsets = new Vector4f[AO_RANDOMTEX_SIZE*AO_RANDOMTEX_SIZE];
    final Vector4f[] jitters = new Vector4f[AO_RANDOMTEX_SIZE*AO_RANDOMTEX_SIZE];

    final Matrix4f projMat = new Matrix4f();
    final Matrix4f  viewProjInvMat = new Matrix4f();

    void store(ByteBuffer buf){
        buf.putFloat(RadiusToScreen);
        buf.putFloat(R2);
        buf.putFloat(NegInvR2);
        buf.putFloat(NDotVBias);
        InvFullResolution.store(buf);
        InvQuarterResolution.store(buf);
        buf.putFloat(AOMultiplier);
        buf.putFloat(PowExponent);
        buf.putFloat(_pad0);
        buf.putFloat(_pad1);
        projInfo.store(buf);
        projScale.store(buf);
        buf.putInt(projOrtho);
        buf.putInt(_pad2);

        for(Vector4f v : float2Offsets){
            v.store(buf);
        }

        for(Vector4f v : jitters){
            v.store(buf);
        }

        projMat.store(buf);
        viewProjInvMat.store(buf);
    }

    public HBAOData() {
//        Util.initArray(float2Offsets);
//        Util.initArray(jitters);
        for(int i = 0; i < float2Offsets.length; i++){
            float2Offsets[i] = new Vector4f();
        }

        for(int i = 0; i < jitters.length; i++){
            jitters[i] = new Vector4f();
        }
    }
}
