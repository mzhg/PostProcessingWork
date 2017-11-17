package jet.opengl.demos.intel.va;

/**
 * Created by mazhen'gui on 2017/11/17.
 */

public class VaAssetPackManager {
    private static VaAssetPackManager g_Instance;

    public static void CreateInstanceIfNot(){
        if(g_Instance == null )
            g_Instance = new VaAssetPackManager();
    }

    private  VaAssetPack                             m_defaultPack;

    /*protected:
    friend class vaAssetPack;*/
    protected TT_Tracker< VaAssetPack >                       m_assetPackTracker; // to track all vaAssetPacks

    private VaAssetPackManager( ){m_defaultPack = /*unique_ptr<vaAssetPack>*/( new VaAssetPack("default" ) );}

    public
    VaAssetPack                                        DefaultPack( )                              { return m_defaultPack; }

    public static VaAssetPackManager GetInstance() { return g_Instance;}

    /*protected:
    // Many assets have DirectX/etc. resource locks so make sure we're not holding any references
    friend class vaDirectXCore; // <- these should be reorganized so that this is not called from anything that is API-specific*/
    protected void                                                OnRenderingAPIAboutToShutdown( ){
        // notify all packs of this
        for( int i = 0; i < m_assetPackTracker.size(); i++ )
            m_assetPackTracker.get(i).OnRenderingAPIAboutToShutdown( );
    }

   /* protected String                                      IHO_GetInstanceInfo( ) const { return "Asset Pack Manager"; }
    protected void                                        IHO_Draw( );*/
}
