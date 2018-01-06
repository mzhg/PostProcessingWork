package jet.opengl.demos.intel.va;

import java.util.Objects;
import java.util.UUID;

import jet.opengl.postprocessing.util.LogUtil;

/**
 * Created by mazhen'gui on 2017/11/17.
 */

public abstract class VaRenderMeshManager extends VaImguiHierarchyObject implements VaRenderingModule {
    private static  VaRenderMeshManager g_Instance;

    private String __name;
    protected final TT_Tracker< VaRenderMesh >  m_renderMeshes = new TT_Tracker<>();
    protected boolean  m_isDestructing;

    public static VaRenderMeshManager GetInstance() {
        if(g_Instance == null){
            VaRenderingModuleRegistrar.CreateModule("vaRenderMeshManager", null);
        }

        Objects.requireNonNull(g_Instance);
        return g_Instance;
    }

    protected VaRenderMeshManager(){
        if(g_Instance != null)
            throw new Error("This is a single-ton class!");

        m_isDestructing = false;
        m_renderMeshes.SetAddedCallback( /*std::bind( &vaRenderMeshManager::RenderMeshesTrackeeAddedCallback, this, std::placeholders::_1 )*/ this::RenderMeshesTrackeeAddedCallback );
        m_renderMeshes.SetBeforeRemovedCallback( /*std::bind( &vaRenderMeshManager::RenderMeshesTrackeeBeforeRemovedCallback, this, std::placeholders::_1, std::placeholders::_2 )*/
                this::RenderMeshesTrackeeBeforeRemovedCallback);

        g_Instance = this;
    }

    @Override
    public String GetRenderingModuleTypeName() {
        return __name;
    }

    @Override
    public void InternalRenderingModuleSetTypeName(String name) {
        __name = name;
    }

    protected void RenderMeshesTrackeeAddedCallback( int newTrackeeIndex ){}
    protected void RenderMeshesTrackeeBeforeRemovedCallback( int removedTrackeeIndex, int replacedByTrackeeIndex ){}

    public abstract void Draw( VaDrawContext drawContext, VaRenderMeshDrawList list );

    public TT_Tracker< VaRenderMesh> GetRenderMeshTracker( ) { return m_renderMeshes; }

    public VaRenderMesh CreateRenderMesh(){
        return CreateRenderMesh(UUID.randomUUID());
    }

    public VaRenderMesh CreateRenderMesh(UUID uid /*= vaCore::GUIDCreate()*/ ){
//        shared_ptr<vaRenderMesh> ret = shared_ptr<vaRenderMesh>( new vaRenderMesh( *this, uid ) );
        VaRenderMesh ret = new VaRenderMesh(this, uid);

        if( !ret.UIDObject_IsCorrectlyTracked() )
        {
            LogUtil.e(LogUtil.LogType.DEFAULT, "Error creating mesh; uid already used");
            return null;
        }

        return ret;
    }

    protected String IHO_GetInstanceInfo( )                                                 { return String.format("vaRenderMeshManager (%d meshes)", m_renderMeshes.size() ); }
    protected void                                    IHO_Draw( ){
        /*static int selected = 0;
        ImGui::BeginChild( "left pane", ImVec2( 150, 0 ), true );
        for( int i = 0; i < 7; i++ )
        {
            char label[128];
            sprintf_s( label, _countof( label ), "MyObject %d", i );
            if( ImGui::Selectable( label, selected == i ) )
            selected = i;
        }
        ImGui::EndChild( );
        ImGui::SameLine( );

        // right
        ImGui::BeginGroup( );
        ImGui::BeginChild( "item view", ImVec2( 0, -ImGui::GetItemsLineHeightWithSpacing( ) ) ); // Leave room for 1 line below us
        ImGui::Text( "MyObject: %d", selected );
        ImGui::Separator( );
        ImGui::TextWrapped( "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. " );
        ImGui::EndChild( );
        ImGui::BeginChild( "buttons" );
        if( ImGui::Button( "Revert" ) ) { }
        ImGui::SameLine( );
        if( ImGui::Button( "Save" ) ) { }
        ImGui::EndChild( );
        ImGui::EndGroup( );*/
    }
}
