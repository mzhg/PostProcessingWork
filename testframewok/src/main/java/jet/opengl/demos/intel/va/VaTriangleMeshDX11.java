package jet.opengl.demos.intel.va;

import jet.opengl.postprocessing.common.GLenum;

/**
 * Created by mazhen'gui on 2017/11/20.
 */
public class VaTriangleMeshDX11 extends VaTriangleMesh implements VaDirectXNotifyTarget {
    private boolean                                m_dirty = true;

    private VaDirectXIndexBuffer/*< uint32 >*/      m_indexBuffer = new VaDirectXIndexBuffer(4);
    private VaDirectXVertexBuffer/*< VertexType >*/ m_vertexBuffer;
    private final int m_vertexStride;

    protected VaTriangleMeshDX11(VaConstructorParamsBase params ){
        m_vertexStride = ((VaTriangleMeshConstructParams)params).VertexStride;
        m_vertexBuffer = new VaDirectXVertexBuffer(m_vertexStride);

        VaDirectXCore.helperInitlize(this);
    }

    void UpdateAndSetToD3DContext(/* ID3D11DeviceContext * context*/ ){
        if( m_dirty )
        {
            m_indexBuffer.dispose( );
            m_vertexBuffer.dispose( );

            if( Indices.size( ) > 0 )
            {
                Indices.trimToSize();
                Vertices.trimToSize();

                m_indexBuffer.Create( Indices.size( ), Indices.getData(), GLenum.GL_STATIC_DRAW);
                m_vertexBuffer.Create( Vertices.size( ), Vertices.getData(), GLenum.GL_STATIC_DRAW );
            }
            m_dirty = false;
        }

        m_indexBuffer.SetToD3DContext( /*context*/ );
        m_vertexBuffer.SetToD3DContext( /*context*/ );
    }

    public VaDirectXIndexBuffer     GetIndexBuffer( )      { return m_indexBuffer; }
    public VaDirectXVertexBuffer GetVertexBuffer( )     { return m_vertexBuffer; }

    public int GetIndexCount( )       { return Indices.size( ); }
    public int GetVertexCount( )      { return Vertices.size( )/m_vertexStride; }

    // vaDirectXNotifyTarget
    public void                                    OnDeviceCreated( /*ID3D11Device* device, IDXGISwapChain* swapChain*/ )
    {
        m_dirty = true;
    }

    public void                                    OnDeviceDestroyed( )
    {
        m_indexBuffer.dispose( );
        m_vertexBuffer.dispose( );
        m_dirty = true;
    }

    public void                                    SetDataDirty( )
    {
        m_indexBuffer.dispose( );
        m_vertexBuffer.dispose( );
        m_dirty = true;
    }
}
