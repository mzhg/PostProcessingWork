package jet.opengl.demos.labs.atmosphere;

import org.lwjgl.util.vector.Readable;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.util.Numeric;

/**
 * An atmosphere layer of width 'width' (in m), and whose density is defined as
 * 'exp_term' * exp('exp_scale' * h) + 'linear_term' * h + 'constant_term',
 * clamped to [0,1], and where h is the altitude (in m). 'exp_term' and
 * 'constant_term' are unitless, while 'exp_scale' and 'linear_term' are in
 * m^-1.
 */
final class DensityProfileLayer implements Readable {
    static final int SIZE = 16;

    DensityProfileLayer() {this(0.0, 0.0, 0.0, 0.0, 0.0);}
    DensityProfileLayer(double width, double exp_term, double exp_scale,
                        double linear_term, double constant_term)
    {
        this.width = (float)width;
        this.exp_term = (float)exp_term;
        this.exp_scale = (float)exp_scale;

        short linear =  Numeric.convertFloatToHFloat((float)linear_term);
        short constant =  Numeric.convertFloatToHFloat((float)constant_term);
        linear_and_constant_term = Numeric.encode(linear, constant);
    }
    float width;
    float exp_term;
    float exp_scale;
    int linear_and_constant_term;

    void set(DensityProfileLayer o, float length_unit_in_meters){
        this.width = o.width / length_unit_in_meters;
        this.exp_term = o.exp_term;
        this.exp_scale = o.exp_scale * length_unit_in_meters;

        if(o.linear_and_constant_term != 0){
            short linear = (short)Numeric.decodeFirst(o.linear_and_constant_term);
            short constant = (short)Numeric.decodeSecond(o.linear_and_constant_term);

            float linearf = Numeric.convertHFloatToFloat(linear) * length_unit_in_meters;
            linear = Numeric.convertFloatToHFloat(linearf);
            this.linear_and_constant_term = Numeric.encode(linear, constant);
        }else{
            this.linear_and_constant_term = o.linear_and_constant_term;
        }

    }

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        buf.putFloat(width);
        buf.putFloat(exp_term);
        buf.putFloat(exp_scale);
        buf.putInt(linear_and_constant_term);
        return buf;
    }
}
