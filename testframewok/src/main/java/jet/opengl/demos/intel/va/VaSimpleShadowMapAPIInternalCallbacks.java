package jet.opengl.demos.intel.va;

/**
 * Created by Administrator on 2017/11/19 0019.
 */

public interface VaSimpleShadowMapAPIInternalCallbacks {
    void                    InternalResolutionOrTexelWorldSizeChanged( );
    void                    InternalStartGenerating( VaDrawContext context );
    void                    InternalStopGenerating( VaDrawContext context );
    void                    InternalStartUsing( VaDrawContext context );
    void                    InternalStopUsing( VaDrawContext context );
}
