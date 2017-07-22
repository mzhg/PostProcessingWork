package jet.opengl.demos.nvidia.waves;

/**
 * Created by mazhen'gui on 2017/7/22.
 */

final class GFSDK_WaveWorks_GFX_TimerQuery_Pool_Impl extends GFSDK_WaveWorks_GFX_Query_Pool_Impl<TimerQueryData>{
    GFSDK_WaveWorks_GFX_TimerQuery_Pool_Impl() {
        super(()-> new TimerQueryData());
    }
}
