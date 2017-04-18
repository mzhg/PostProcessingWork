package jet.opengl.postprocessing.common;

/**
 * Created by mazhen'gui on 2017/4/1.
 */

public class GLAPIVersion {
    public final boolean ES;
    public final int major;
    public final int minor;
    public final boolean coreProfiler;

    public GLAPIVersion(boolean ES, int major, int minor, boolean coreProfiler) {
        this.ES = ES;
        this.major = major;
        this.minor = minor;
        this.coreProfiler = coreProfiler;
    }

    public int toInt(){
        return major * 100 + minor * 10;
    }
}
