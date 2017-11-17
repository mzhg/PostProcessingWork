package jet.opengl.demos.intel.va;

import java.util.UUID;

/**
 * Created by mazhen'gui on 2017/11/16.
 */

public class VaAssetResource extends VaUIDObject{
    private VaAsset m_parentAsset;

    protected VaAssetResource( UUID uid ) /*: vaUIDObject( uid )*/          { super(uid); m_parentAsset = null; }
//    virtual ~vaAssetResource( )                                         { assert( m_parentAsset == null ); }

    public VaAsset GetParentAsset( )                               { return m_parentAsset; }

    /*private:
    friend struct vaAsset;*/
    void                SetParentAsset(VaAsset asset )
    {
        // there can be only one asset resource linked to one asset; this is one (of the) way to verify it:
        if( asset == null )
        {
            assert( m_parentAsset != null );
        }
        else
        {
            assert( m_parentAsset == null );
        }
        m_parentAsset = asset;
    }

    protected void        ReconnectDependencies( )                        { }
}
