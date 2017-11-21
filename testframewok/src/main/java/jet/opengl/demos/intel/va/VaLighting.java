package jet.opengl.demos.intel.va;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

/**
 * Created by mazhen'gui on 2017/11/18.
 */

public abstract class VaLighting extends VaImguiHierarchyObject implements VaRenderingModule {
    private String m_renderingModuleTypeName;

    protected String  m_debugInfo;

    protected final Vector3f m_directionalLightDirection = new Vector3f();
    protected final Vector3f m_directionalLightIntensity = new Vector3f();
    protected final Vector3f m_ambientLightIntensity = new Vector3f();

    protected final Vector3f m_fogColor = new Vector3f();
    protected float m_fogDistanceMin;
    protected float m_fogDensity;

    @Override
    public String GetRenderingModuleTypeName() {
        return m_renderingModuleTypeName;
    }

    @Override
    public void InternalRenderingModuleSetTypeName(String name) {
        m_renderingModuleTypeName = name;
    }

    protected VaLighting( ){
        assert( VaRenderingCore.IsInitialized( ));
        m_debugInfo = "Lighting (uninitialized - forgot to call RenderTick?)";

        m_directionalLightDirection .set( 0.0f, 0.0f, -1.0f );
        m_directionalLightIntensity .set( 1.1f, 1.1f, 1.1f );
        m_ambientLightIntensity     .set( 0.15f, 0.15f, 0.15f );

        m_fogColor                  .set( 0.4f, 0.4f, 0.9f );
        m_fogDistanceMin = 100.0f;
        m_fogDensity = 0.0007f;
    }

    public void UpdateLightingGlobalConstants( VaDrawContext drawContext, LightingGlobalConstants consts ){
//        vaMatrix4x4 mat = drawContext.Camera.GetViewMatrix( ) * drawContext.Camera.GetProjMatrix( );

        consts.DirectionalLightWorldDirection       .set(m_directionalLightDirection);
        consts.DirectionalLightWorldDirection       .w = 0;
//        consts.DirectionalLightViewspaceDirection   = vaVector4( vaVector3::TransformNormal( m_directionalLightDirection, drawContext.Camera.GetViewMatrix( ) ), 1.0f );
        Matrix4f.transformNormal(drawContext.Camera.GetViewMatrix( ), m_directionalLightDirection, consts.DirectionalLightViewspaceDirection);
        consts.DirectionalLightViewspaceDirection   .w = 1.0f;

        consts.DirectionalLightIntensity            .set(m_directionalLightIntensity);
        consts.DirectionalLightIntensity            .w = 0;
        consts.AmbientLightIntensity                .set(m_ambientLightIntensity);
        consts.AmbientLightIntensity                .w = 0;

        consts.FogColor                             .set(m_fogColor);
        consts.FogDistanceMin                       = m_fogDistanceMin;
        consts.FogDensity                           = m_fogDensity;
//        consts.FogDummy0                            = 0.0f;
//        consts.FogDummy1                            = 0.0f;
    }

    public ReadableVector3f GetDirectionalLightDirection( )    { return m_directionalLightDirection;   }
    public ReadableVector3f GetDirectionalLightIntensity( )    { return m_directionalLightIntensity;   }
    public ReadableVector3f GetAmbientLightIntensity( )        { return m_ambientLightIntensity;       }

    public void SetDirectionalLightDirection( float x, float y, float z )                         { m_directionalLightDirection.set(x,y,z); }
    public void SetDirectionalLightDirection(  ReadableVector3f newValue )                         { m_directionalLightDirection.set(newValue); }
    public void SetDirectionalLightIntensity(  ReadableVector3f newValue )                         { m_directionalLightIntensity.set(newValue); }
    public void SetAmbientLightIntensity( ReadableVector3f newValue )                              { m_ambientLightIntensity    .set(newValue); }

    //void                                            GetFogParams
    public void SetFogParams(ReadableVector3f fogColor, float fogDistanceMin, float fogDensity )
                { m_fogColor.set(fogColor); m_fogDistanceMin = fogDistanceMin; m_fogDensity = fogDensity; }

    public abstract void ApplyDirectionalAmbientLighting( VaDrawContext drawContext, VaGBuffer GBuffer );

    public abstract void ApplyDynamicLighting( VaDrawContext drawContext, VaGBuffer GBuffer );

    //virtual void                                    ApplyTonemap( vaDrawContext & drawContext, vaGBuffer & GBuffer )                    = 0;

    protected String IHO_GetInstanceInfo( ) { return m_debugInfo; }
    protected void IHO_Draw( ){

    }
}
