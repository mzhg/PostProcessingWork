package jet.opengl.demos.labs.atmosphere;

import org.lwjgl.util.vector.Readable;

import java.nio.ByteBuffer;

final class DensityProfile implements Readable {
    static  final int SIZE = DensityProfileLayer.SIZE * 2;

    final DensityProfileLayer[] layers = new DensityProfileLayer[2];

    DensityProfile(){
        layers[0] = new DensityProfileLayer();
        layers[1] = new DensityProfileLayer();
    }

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        layers[0].store(buf);
        layers[1].store(buf);
        return buf;
    }
}
