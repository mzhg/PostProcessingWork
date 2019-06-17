package jet.opengl.particles;

import org.lwjgl.util.vector.ReadableVector4f;
import org.lwjgl.util.vector.Vector4f;

import jet.opengl.postprocessing.util.Numeric;

public class RangeVector4f {
    private final Vector4f start = new Vector4f();
    private final Vector4f end = new Vector4f();

    public RangeVector4f(){}

    public RangeVector4f(ReadableVector4f start, ReadableVector4f end){
        this.start.set(start);
        this.end.set(end);
    }

    public void eval(float time, Vector4f result){
        time = Numeric.clamp(time, 0, 1);
        Vector4f.mix(start,end, time, result);
    }

    public void setStart(ReadableVector4f start){
        this.start.set(start);
    }

    public void setEnd(ReadableVector4f end){
        this.end.set(end);
    }

    public void set(RangeVector4f ohs){
        this.start.set(ohs.start);
        this.end.set(ohs.end);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RangeVector4f that = (RangeVector4f) o;

        if (!start.equals(that.start)) return false;
        return end.equals(that.end);
    }

    @Override
    public int hashCode() {
        int result = start != null ? start.hashCode() : 0;
        result = 31 * result + (end != null ? end.hashCode() : 0);
        return result;
    }
}
