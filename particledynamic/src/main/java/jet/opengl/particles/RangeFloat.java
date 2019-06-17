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

    public void setStart(float start){
        this.start = start;
    }

    public void setEnd(float end){
        this.end = end;
    }

    public void set(RangeFloat ohs){
        this.start=(ohs.start);
        this.end = (ohs.end);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RangeFloat that = (RangeFloat) o;

        if (Float.compare(that.start, start) != 0) return false;
        return Float.compare(that.end, end) == 0;
    }

    @Override
    public int hashCode() {
        int result = (start != +0.0f ? Float.floatToIntBits(start) : 0);
        result = 31 * result + (end != +0.0f ? Float.floatToIntBits(end) : 0);
        return result;
    }
}
