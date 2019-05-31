package jet.opengl.particles;

import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

import jet.opengl.postprocessing.util.Numeric;

public class RangeVector3f {
    private final Vector3f start = new Vector3f();
    private final Vector3f end = new Vector3f();

    public RangeVector3f(){}

    public RangeVector3f(ReadableVector3f start, ReadableVector3f end){
        this.start.set(start);
        this.end.set(end);
    }

    public void setStart(ReadableVector3f start){
        this.start.set(start);
    }

    public void setEnd(ReadableVector3f end){
        this.end.set(end);
    }

    public void eval(float time, Vector3f result){
        time = Numeric.clamp(time, 0, 1);
        Vector3f.mix(start,end, time, result);
    }
}
