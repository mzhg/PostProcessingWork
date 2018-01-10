package jet.opengl.demos.intel.va;

import org.lwjgl.util.vector.Vector4i;

/**
 * VaRenderDeviceContext is used to get/set current render targets and access rendering API stuff like contexts, etc.<p></p>
 * Created by mazhen'gui on 2017/11/20.
 */

public abstract class VaRenderDeviceContext extends VaRenderingModuleImpl {
    public static final int                 c_maxRTs = 8;
    public static final int                 c_maxUAVs = 8;

    public static final class OutputsState
    {
        public final VaViewport             Viewport = new VaViewport();
        public final Vector4i               ScissorRect = new Vector4i();
        public boolean                      ScissorRectEnabled;

        public final VaTexture[]            RenderTargets = new VaTexture[c_maxRTs];
        public final VaTexture[]            UAVs = new VaTexture[c_maxUAVs];
        public final int[]                  UAVInitialCounts = new int[c_maxUAVs];
        public VaTexture                    DepthStencil;

        public int                          RenderTargetCount;
        public int                          UAVsStartSlot;
        public int                          UAVCount;

        void Set(OutputsState ohs){
            Viewport.Set(ohs.Viewport);
            ScissorRect.set(ohs.ScissorRect);

            ScissorRectEnabled = ohs.ScissorRectEnabled;
            for(int i = 0; i < c_maxRTs; i++){
                RenderTargets[i] = ohs.RenderTargets[i];
                UAVs[i] = ohs.UAVs[i];
                UAVInitialCounts[i] = ohs.UAVInitialCounts[i];
            }

            DepthStencil = ohs.DepthStencil;
            RenderTargetCount = ohs.RenderTargetCount;
            UAVsStartSlot = ohs.UAVsStartSlot;
            UAVCount = ohs.UAVCount;
        }
    };

    public final OutputsState               m_outputsState = new OutputsState();

    protected VaRenderDeviceContext(){}

    public VaViewport GetViewport( )                 { return m_outputsState.Viewport;    }
    public void       SetViewport( VaViewport vp )   { m_outputsState.Viewport.Set(vp); m_outputsState.ScissorRect.set( 0, 0, 0, 0 ); m_outputsState.ScissorRectEnabled = false; UpdateViewport(); }

    public boolean    GetScissorRect( Vector4i outScissorRect/*, bool & outScissorRectEnabled*/ ) {
        outScissorRect = m_outputsState.ScissorRect;
        return m_outputsState.ScissorRectEnabled;
    }

    public void       SetViewportAndScissorRect( VaViewport vp, Vector4i scissorRect ) {
        m_outputsState.Viewport.Set(vp);
        m_outputsState.ScissorRect.set(scissorRect);
        m_outputsState.ScissorRectEnabled = true; UpdateViewport();
    }

    public VaTexture GetRenderTarget( ) { return m_outputsState.RenderTargets[0]; }
    public VaTexture GetDepthStencil( ) { return m_outputsState.DepthStencil; }
    public VaTexture[] GetRenderTargets( ) { return m_outputsState.RenderTargets; }
    public VaTexture[] GetUAVs( )       { return m_outputsState.RenderTargets; }

    public int  GetrenderTargetCount( )                       { return m_outputsState.RenderTargetCount; }
    public int  GetUAVsStartSlot( )                           { return m_outputsState.UAVsStartSlot; }
    public int  GetUAVCount( )                                { return m_outputsState.UAVCount; }
    public OutputsState GetOutputs( )                         { return m_outputsState; }
    public void  SetOutputs(OutputsState state )  { m_outputsState.Set(state); UpdateRenderTargetsDepthStencilUAVs(); UpdateViewport(); }

    public void SetRenderTarget(VaTexture renderTarget, VaTexture depthStencil, boolean updateViewport ){
        for( int i = 0; i < c_maxRTs; i++ )     m_outputsState.RenderTargets[i]      = null;
        for( int i = 0; i < c_maxUAVs; i++ )    m_outputsState.UAVs[i]               = null;
        for( int i = 0; i < c_maxUAVs; i++ )    m_outputsState.UAVInitialCounts[i]   = -1;
        m_outputsState.RenderTargets[0]     = renderTarget;
        m_outputsState.DepthStencil         = depthStencil;
        m_outputsState.RenderTargetCount    = 1;
        m_outputsState.UAVsStartSlot        = 0;
        m_outputsState.UAVCount             = 0;
        m_outputsState.ScissorRect          .set( 0, 0, 0, 0 );
        m_outputsState.ScissorRectEnabled   = false;

        VaTexture anyRT = ( renderTarget != null ) ? ( renderTarget ) : ( depthStencil );

        VaViewport vp = m_outputsState.Viewport;

        if( anyRT != null )
        {
            assert( ( anyRT.GetType( ) ==VaTextureType.Texture2D ) || (anyRT.GetType( ) == VaTextureType.Texture2DMS ) );   // others not supported yet
            vp.X = 0;
            vp.Y = 0;
            vp.Width = anyRT.GetViewedSliceSizeX( );
            vp.Height = anyRT.GetViewedSliceSizeY( );
        }

        if( renderTarget != null )
        {
            assert( ( renderTarget.GetBindSupportFlags( ) & VaTexture.BSF_RenderTarget ) != 0 );
        }
        if( depthStencil != null )
        {
            assert( ( depthStencil.GetBindSupportFlags( ) & VaTexture.BSF_DepthStencil ) != 0 );
        }

        UpdateRenderTargetsDepthStencilUAVs( );

        if( updateViewport )
            SetViewport( vp );
    }

    public void SetRenderTargets( int numRTs, VaTexture[] renderTargets, VaTexture depthStencil, boolean updateViewport ){
        SetRenderTargetsAndUnorderedAccessViews(numRTs, renderTargets, depthStencil,
                                                0,0,null, updateViewport, null);
    }

    public void SetRenderTargetsAndUnorderedAccessViews(int numRTs, VaTexture[] renderTargets, VaTexture depthStencil,
                                                        int UAVStartSlot, int numUAVs, VaTexture[] UAVs, boolean updateViewport,
                                                        int[] UAVInitialCounts /*= nullptr*/ ){
        assert( numRTs <= c_maxRTs );
        assert( numUAVs <= c_maxUAVs );
        m_outputsState.RenderTargetCount = numRTs  = Math.min( numRTs , c_maxRTs  );
        m_outputsState.UAVCount          = numUAVs = Math.min( numUAVs, c_maxUAVs );

        for( int i = 0; i < c_maxRTs; i++ )
            m_outputsState.RenderTargets[i]      = (i < m_outputsState.RenderTargetCount)?(renderTargets[i]):(null);

        for( int i = 0; i < c_maxUAVs; i++ )
        {
            m_outputsState.UAVs[i]               = (i < m_outputsState.UAVCount)?(UAVs[i]):(null);
            m_outputsState.UAVInitialCounts[i]   = ( (i < m_outputsState.UAVCount) && (UAVInitialCounts != null) )?( UAVInitialCounts[i] ):( -1 );
        }
        m_outputsState.DepthStencil = depthStencil;
        m_outputsState.UAVsStartSlot = UAVStartSlot;

        VaTexture anyRT = ( m_outputsState.RenderTargets[0] != null ) ? ( m_outputsState.RenderTargets[0] ) : ( depthStencil );

        VaViewport vp = m_outputsState.Viewport;

        if( anyRT != null )
        {
            assert( ( anyRT.GetType( ) == VaTextureType.Texture2D ) || ( anyRT.GetType( ) == VaTextureType.Texture2DMS ) ||
                    ( anyRT.GetType( ) == VaTextureType.Texture2DArray ) || ( anyRT.GetType( ) == VaTextureType.Texture2DMSArray ) );   // others not supported yet
            vp.X = 0;
            vp.Y = 0;
            vp.Width = anyRT.GetViewedSliceSizeX( );
            vp.Height = anyRT.GetViewedSliceSizeY( );
        }

        for( int i = 0; i < m_outputsState.RenderTargetCount; i++ )
        {
            if( m_outputsState.RenderTargets[i] != null )
            {
                assert( ( m_outputsState.RenderTargets[i].GetBindSupportFlags( ) & VaTexture.BSF_RenderTarget ) != 0 );
            }
        }
        for( int i = 0; i < m_outputsState.UAVCount; i++ )
        {
            if( m_outputsState.UAVs[i] != null )
            {
                assert( ( m_outputsState.UAVs[i].GetBindSupportFlags( ) & VaTexture.BSF_UnorderedAccess ) != 0 );
            }
        }
        if( depthStencil != null )
        {
            assert( ( depthStencil.GetBindSupportFlags( ) & VaTexture.BSF_DepthStencil ) != 0 );
        }

        UpdateRenderTargetsDepthStencilUAVs( );

        if( updateViewport )
        {
            // can't update viewport if no RTs
            assert(anyRT != null);
            if( anyRT != null )
                SetViewport( vp );
        }
    }

    protected abstract void                        UpdateViewport( );
    protected abstract void                        UpdateRenderTargetsDepthStencilUAVs( );

    public abstract Object                      GetAPIImmediateContextPtr( );
}
