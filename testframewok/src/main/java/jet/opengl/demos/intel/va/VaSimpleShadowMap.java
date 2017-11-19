package jet.opengl.demos.intel.va;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;

import jet.opengl.postprocessing.util.Numeric;

/**
 * Created by Administrator on 2017/11/19 0019.
 */

public abstract class VaSimpleShadowMap extends VaRenderingModuleImpl implements VaSimpleShadowMapAPIInternalCallbacks {
    private int                             m_resolution;
    private final VaOrientedBoundingBox           m_volume = new VaOrientedBoundingBox();

    private VaTexture      m_shadowMap;

    private final Matrix4f m_view = new Matrix4f();
    private final Matrix4f m_proj = new Matrix4f();
    private final Matrix4f m_viewProj = new Matrix4f();
    private final Vector2f m_texelSize = new Vector2f();

    private VaSimpleVolumeShadowMapPlugin m_volumeShadowMapPlugin;

    protected VaSimpleShadowMap(){

    }

    public void Initialize( int resolution ){
        SetResolution(resolution);
    }

    public void SetVolumeShadowMapPlugin( VaSimpleVolumeShadowMapPlugin vsmp ) { m_volumeShadowMapPlugin = vsmp; }
    public VaSimpleVolumeShadowMapPlugin GetVolumeShadowMapPlugin( ) { return m_volumeShadowMapPlugin; }

    public void UpdateArea( VaOrientedBoundingBox volume ){
        m_volume.set(volume);

        /*m_view                  = vaMatrix4x4( volume.Axis.Transpose() );
        m_view.r3.x             = -vaVector3::Dot( volume.Axis.r0, volume.Center );
        m_view.r3.y             = -vaVector3::Dot( volume.Axis.r1, volume.Center );
        m_view.r3.z             = -vaVector3::Dot( volume.Axis.r2, volume.Center );*/
        volume.ToAABBAndTransform(null, m_view);
        Matrix4f.invertRigid(m_view, m_view);

//        m_view.r3.AsVec3()      += vaVector3( 0.0f, 0.0f, 1.0f ) * volume.Extents.z * 1.0f;
        m_view.m32 += volume.Extents.z;

//        m_proj                  = vaMatrix4x4::OrthoLH( volume.Extents.x*2.0f, volume.Extents.y*2.0f, 0.0f, volume.Extents.z * 2.0f );
        Matrix4f.ortho(volume.Extents.x*2.0f, volume.Extents.y*2.0f, 0.0f, volume.Extents.z * 2.0f, m_proj);

//        m_viewProj              = m_view * m_proj;
        Matrix4f.mul(m_proj, m_view, m_viewProj);

//        vaVector2 newTexelSize;
        float newTexelSizeX          = volume.Extents.x * 2.0f / (float)m_resolution;
        float newTexelSizeY          = volume.Extents.y * 2.0f / (float)m_resolution;

//        if( !vaVector2::CloseEnough( newTexelSize, m_texelSize ) )
        if(Math.abs(m_texelSize.lengthSquared() - Vector2f.lengthSquare(newTexelSizeX, newTexelSizeY)) > Numeric.EPSILON)
        {
            InternalResolutionOrTexelWorldSizeChanged( );
//            m_texelSize         = newTexelSize;
            m_texelSize.set(newTexelSizeX, newTexelSizeY);
        }
    }

    public VaTexture  GetShadowMapTexture( )   { return m_shadowMap; }
    public Matrix4f   GetViewMatrix( )         { return m_view;     }
    public Matrix4f   GetProjMatrix( )         { return m_proj;     }
    public Matrix4f   GetViewProjMatrix( )     { return m_viewProj; }

    public int        GetResolution( )         { return m_resolution; }
    public VaOrientedBoundingBox  GetVolume( ) { return m_volume; }
    public Vector2f   GetTexelSize( )          { return m_texelSize; }

    public void StartGenerating( VaDrawContext context )        { assert( (context.PassType == VaRenderPassType.GenerateShadowmap) || (context.PassType == VaRenderPassType.GenerateVolumeShadowmap) ); InternalStartGenerating( context ); assert( context.SimpleShadowMap == null ); context.SimpleShadowMap = this; }
    public void StopGenerating( VaDrawContext context )         { assert( (context.PassType == VaRenderPassType.GenerateShadowmap) || (context.PassType == VaRenderPassType.GenerateVolumeShadowmap) ); InternalStopGenerating(  context ); assert( context.SimpleShadowMap == this ); context.SimpleShadowMap = null; }
    public void StartUsing( VaDrawContext context )             { assert( context.PassType != VaRenderPassType.GenerateShadowmap ); InternalStartUsing(      context ); assert( context.SimpleShadowMap == null ); context.SimpleShadowMap = this; }
    public void StopUsing( VaDrawContext context )              { assert( context.PassType != VaRenderPassType.GenerateShadowmap ); InternalStopUsing(       context ); assert( context.SimpleShadowMap == this ); context.SimpleShadowMap = null; }
    private void SetResolution( int resolution ){
        if( m_resolution != resolution )
        {
            m_resolution = resolution;

            m_shadowMap = VaTexture.Create2D( VaTexture.R16_TYPELESS, resolution, resolution, 1, 1, 1, 
                           VaTexture.BSF_DepthStencil | VaTexture.BSF_ShaderResource, /*vaTextureAccessFlags::None*/0, null/*,
                            0, VaTexture.R16_UNORM, VaTexture.Unknown, VaTexture.D16_UNORM*/);

            InternalResolutionOrTexelWorldSizeChanged( );
        }
    }
}
