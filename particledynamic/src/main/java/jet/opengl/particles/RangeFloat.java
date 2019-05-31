package jet.opengl.particles;

import jet.opengl.postprocessing.util.Numeric;

public class RangeFloat {
    private float start;
    private float end;

    public RangeFloat(){}

    public RangeFloat(float start, float end){
        this.start = start;
        this.end = end;
    }

    public float eval(float time){
        time = Numeric.clamp(time, 0, 1);
        return Numeric.mix(start,end, time);
    }
}
