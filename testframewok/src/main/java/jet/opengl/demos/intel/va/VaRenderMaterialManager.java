package jet.opengl.demos.intel.va;

import java.util.UUID;

import jet.opengl.postprocessing.util.LogUtil;

/**
 * Created by mazhen'gui on 2017/11/17.
 */

public class VaRenderMaterialManager extends VaImguiHierarchyObject implements VaRenderingModule {
    private String mRenderingModuleTypeName;
    protected final TT_Tracker< VaRenderMaterial >              m_renderMaterials;
    // map<wstring, shared_ptr<vaRenderMaterial>>  m_renderMaterialsMap;

    protected VaRenderMaterial                  m_defaultMaterial;
    protected boolean                                            m_isDestructing;
    protected boolean                                            m_texturingDisabled;

    private static VaRenderMaterialManager g_Instance;

    public static VaRenderMaterialManager GetInstance() { return g_Instance;}
    public static void CreateInstanceIfNot() {
        if(g_Instance == null){
            g_Instance = new VaRenderMaterialManager();
        }
    }

    protected VaRenderMaterialManager( ){
        m_isDestructing = false;

        m_renderMaterials = new TT_Tracker<>();
        m_renderMaterials.SetAddedCallback( /*std::bind( &vaRenderMaterialManager::RenderMaterialsTrackeeAddedCallback, this, std::placeholders::_1 )*/
               this:: RenderMaterialsTrackeeAddedCallback);
        m_renderMaterials.SetBeforeRemovedCallback(
                /*std::bind( &vaRenderMaterialManager::RenderMaterialsTrackeeBeforeRemovedCallback, this, std::placeholders::_1, std::placeholders::_2 )*/
                this::RenderMaterialsTrackeeBeforeRemovedCallback
                );

        m_defaultMaterial = //VA_RENDERING_MODULE_CREATE_PARAMS_SHARED( vaRenderMaterial, vaRenderMaterialConstructorParams( *this, vaCore::GUIDFromString( L"11523d65-09ea-4342-9bad-8dab7a4dc1e0" ) ) );
                            VaRenderingModuleRegistrar.CreateModuleTyped("vaRenderMaterial",
                                    new VaRenderMaterialConstructorParams(this, VaCore.GUIDFromString("11523d65-09ea-4342-9bad-8dab7a4dc1e0")));
        m_texturingDisabled = false;
    }
//    virtual ~vaRenderMaterialManager( );

    private void                                            RenderMaterialsTrackeeAddedCallback( int newTrackeeIndex ){}
    private void                                            RenderMaterialsTrackeeBeforeRemovedCallback( int removedTrackeeIndex, int replacedByTrackeeIndex ){}

    public VaRenderMaterial                    GetDefaultMaterial( )     { return m_defaultMaterial; }

    public VaRenderMaterial               CreateRenderMaterial(UUID uid /*= vaCore::GUIDCreate( )*/ ){
        VaRenderMaterial ret =  //VA_RENDERING_MODULE_CREATE_PARAMS_SHARED( vaRenderMaterial, vaRenderMaterialConstructorParams( *this, uid ) );
                VaRenderingModuleRegistrar.CreateModuleTyped("vaRenderMaterial",
                        new VaRenderMaterialConstructorParams(this, uid));

        if( !ret.UIDObject_IsCorrectlyTracked() )
        {
            LogUtil.e(LogUtil.LogType.DEFAULT, "Error creating render material; uid already used");
            return null;
        }

        return ret;
    }
    public TT_Tracker< VaRenderMaterial >             GetRenderMaterialTracker( ) { return m_renderMaterials; }

    public boolean                                         GetTexturingDisabled( )   { return m_texturingDisabled; }
    public void                                            SetTexturingDisabled( boolean texturingDisabled ){
        if( m_texturingDisabled == texturingDisabled )
            return;

        m_texturingDisabled = texturingDisabled;

        for( int i = 0; i < m_renderMaterials.size(); i++ )
        {
            m_renderMaterials.get(i).SetSettingsDirty();
        }
    }

    protected String                                IHO_GetInstanceInfo( )  { return String.format( "vaRenderMaterialManager (%d meshes)", m_renderMaterials.size( ) ); }
    protected void                                    IHO_Draw( ){

    }

    @Override
    public String GetRenderingModuleTypeName() {
        return mRenderingModuleTypeName;
    }

    @Override
    public void InternalRenderingModuleSetTypeName(String name) {
        mRenderingModuleTypeName = name;
    }
}
