package jet.opengl.demos.nvidia.lightning;

import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

/**
 * Geometric properties of a single bolt
 * this one matches the constant buffer layout in the Lightning.fx file<p></p>
 * Created by mazhen'gui on 2017/8/28.
 */

final class LightningStructure implements Readable{
    static final int SIZE = Vector4f.SIZE * 7;

    // for ZigZag pattern
    final Vector2f ZigZagFraction = new Vector2f();
    final Vector2f ZigZagDeviationRight = new Vector2f();

    final Vector2f ZigZagDeviationUp = new Vector2f();
    float		ZigZagDeviationDecay;
//    float Dummy0;						// dummy to match HLSL padding

    // for Fork pattern
    final Vector2f ForkFraction = new Vector2f();
    final Vector2f ForkZigZagDeviationRight = new Vector2f();

    final Vector2f ForkZigZagDeviationUp = new Vector2f();
    float		ForkZigZagDeviationDecay;
//    float Dummy1;						// dummy to match HLSL padding

    final Vector2f ForkDeviationRight = new Vector2f();
    final Vector2f ForkDeviationUp = new Vector2f();

    final Vector2f ForkDeviationForward = new Vector2f();
    float		ForkDeviationDecay;
//    float Dummy2;						// dummy to match HLSL padding

    final Vector2f	ForkLength = new Vector2f();
    float		ForkLengthDecay;

    void set(LightningStructure other){
        ZigZagFraction.set(other.ZigZagFraction);
        ZigZagDeviationRight.set(other.ZigZagDeviationRight);

        ZigZagDeviationUp.set(other.ZigZagDeviationUp);
        ZigZagDeviationDecay = other.ZigZagDeviationDecay;

        ForkFraction.set(other.ForkFraction);
        ForkZigZagDeviationRight.set(other.ForkZigZagDeviationRight);

        ForkZigZagDeviationUp.set(other.ForkZigZagDeviationUp);
        ForkZigZagDeviationDecay = other.ForkZigZagDeviationDecay;

        ForkDeviationRight.set(other.ForkDeviationRight);
        ForkDeviationUp.set(other.ForkDeviationUp);

        ForkDeviationForward.set(other.ForkDeviationForward);
        ForkDeviationDecay = other.ForkDeviationDecay;

        ForkLength.set(other.ForkLength);
        ForkLengthDecay = other.ForkLengthDecay;
    }

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        ZigZagFraction.store(buf);
        ZigZagDeviationRight.store(buf);

        ZigZagDeviationUp.store(buf);
        buf.putFloat(ZigZagDeviationDecay);
        buf.putFloat(0);

        ForkFraction.store(buf);
        ForkZigZagDeviationRight.store(buf);

        ForkZigZagDeviationUp.store(buf);
        buf.putFloat(ForkZigZagDeviationDecay);
        buf.putFloat(0);

        ForkDeviationRight.store(buf);
        ForkDeviationUp.store(buf);

        ForkDeviationForward.store(buf);
        buf.putFloat(ForkDeviationDecay);
        buf.putFloat(0);

        ForkLength.store(buf);
        buf.putFloat(ForkLengthDecay);
        buf.putFloat(0);

        return buf;
    }
}
