package jet.opengl.demos.labs.atmosphere;

/**
 * An atmosphere layer of width 'width' (in m), and whose density is defined as
 * 'exp_term' * exp('exp_scale' * h) + 'linear_term' * h + 'constant_term',
 * clamped to [0,1], and where h is the altitude (in m). 'exp_term' and
 * 'constant_term' are unitless, while 'exp_scale' and 'linear_term' are in
 * m^-1.
 */
final class DensityProfileLayer {
    DensityProfileLayer() {this(0.0, 0.0, 0.0, 0.0, 0.0);}
    DensityProfileLayer(double width, double exp_term, double exp_scale,
                        double linear_term, double constant_term)
    {
        this.width = width;
        this.exp_term = exp_term;
        this.exp_scale = exp_scale;
        this.linear_term = linear_term;
        this.constant_term = constant_term;
    }
    double width;
    double exp_term;
    double exp_scale;
    double linear_term;
    double constant_term;
}
