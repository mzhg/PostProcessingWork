package jet.opengl.demos.nvidia.waves;

/**
 * Created by Administrator on 2017/7/23 0023.
 */

public class GFSDK_WaveWorks_API_GUID {
    public int component1;
    public int component2;
    public int component3;
    public int component4;

    public GFSDK_WaveWorks_API_GUID(int component1, int component2, int component3, int component4) {
        this.component1 = component1;
        this.component2 = component2;
        this.component3 = component3;
        this.component4 = component4;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GFSDK_WaveWorks_API_GUID that = (GFSDK_WaveWorks_API_GUID) o;

        if (component1 != that.component1) return false;
        if (component2 != that.component2) return false;
        if (component3 != that.component3) return false;
        return component4 == that.component4;

    }

    @Override
    public int hashCode() {
        int result = component1;
        result = 31 * result + component2;
        result = 31 * result + component3;
        result = 31 * result + component4;
        return result;
    }
}
