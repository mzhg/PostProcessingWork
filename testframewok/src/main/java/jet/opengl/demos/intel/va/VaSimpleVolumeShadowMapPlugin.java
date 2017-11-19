package jet.opengl.demos.intel.va;

import jet.opengl.postprocessing.shader.Macro;

/**
 * Created by Administrator on 2017/11/19 0019.
 */

public interface VaSimpleVolumeShadowMapPlugin {
    Macro[] GetShaderMacros( );

    void StartGenerating( VaDrawContext context, VaSimpleShadowMap  ssm );
    void StopGenerating( VaDrawContext context, VaSimpleShadowMap  ssm );
    void StartUsing( VaDrawContext context, VaSimpleShadowMap  ssm );
    void StopUsing( VaDrawContext context, VaSimpleShadowMap  ssm ) ;

    void SetDebugTexelLocation( int x, int y );
    int  GetResolution( );
    boolean GetSupported( );    // supported on current hardware?
}
