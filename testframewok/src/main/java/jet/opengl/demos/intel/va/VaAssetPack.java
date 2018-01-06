package jet.opengl.demos.intel.va;

import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jet.opengl.postprocessing.util.LogUtil;

/**
 * Created by mazhen'gui on 2017/11/16.
 */

public class VaAssetPack extends VaImguiHierarchyObject {
    protected String m_name;
    protected Map< String, VaAsset> m_assetMap = new HashMap<>();

    private TT_Trackee< VaAssetPack > m_trackee;  // for tracking by vaAssetPackManager
    protected List<VaAsset> m_assetList;

    protected UIContext m_uiContext;

    public VaAssetPack( String name ){
        m_trackee = new TT_Trackee<>(VaAssetPackManager.GetInstance().m_assetPackTracker, this);
        m_name = name;
    }

    public String  FindSuitableAssetName(String nameSuggestion ){
//        String nameSuggestion = vaStringTools::ToLower( _nameSuggestion );
        nameSuggestion = nameSuggestion.toLowerCase();

        if( Find( nameSuggestion ) == null )
            return nameSuggestion;

        int index = 0;
        do
        {
            String newSuggestion = //vaStringTools::Format( "%s_%d", nameSuggestion.c_str(), index );
                    nameSuggestion + '_' + index;
            if( Find( newSuggestion ) == null )
                return newSuggestion;

            index++;
        } while ( true );
    }

    /*shared_ptr<vaAsset>*/ public VaAsset  Find( String name ){
        name = name.toLowerCase();
        return m_assetMap.get(name);
    }
    //shared_ptr<vaAsset>                                 FindByStoragePath( const wstring & _storagePath );
    public void Remove( VaAsset asset ){
        if( asset == null )
            return;

        if(m_assetList == null)
            return;

        assert( m_assetList.get(asset.m_parentPackStorageIndex) == asset );
        if( m_assetList.size( ) != ( asset.m_parentPackStorageIndex + 1 ) )
        {
            /*m_assetList[ asset->m_parentPackStorageIndex ] = m_assetList[ m_assetList.size( ) - 1 ];
            m_assetList[ asset->m_parentPackStorageIndex ]->m_parentPackStorageIndex = asset->m_parentPackStorageIndex;*/
            VaAsset last = m_assetList.get(m_assetList.size( ) - 1);
            last.m_parentPackStorageIndex = asset.m_parentPackStorageIndex;
            m_assetList.set(asset.m_parentPackStorageIndex, last);
        }
        asset.m_parentPackStorageIndex = -1;
//        m_assetList.pop_back();
        m_assetList.remove(m_assetList.size() - 1);

        {
            /*auto it = m_assetMap.find( vaStringTools::ToLower(asset->Name()) );
            // possible memory leak! does the asset belong to another asset pack?
            assert( it != m_assetMap.end() );
            if( it == m_assetMap.end() )
                return;

            m_assetMap.erase( it );*/
            m_assetMap.remove(asset.Name());
        }
    }
    public void RemoveAll( ){
        if(m_assetList != null) {
            m_assetList.clear();
            m_assetMap.clear();
        }
    }

    private static final int c_packFileVersion = 1;

    // save current contents
    public boolean Save( VaStream outStream ) {throw new UnsupportedOperationException();}
    // load contents (current contents are not deleted)
    public boolean Load( VaStream inStream ) throws IOException{
        long size = 0;
        size = inStream./*ReadValue<int64>*/ ReadLong( );

        int fileVersion = 0;
//        VERIFY_TRUE_RETURN_ON_FALSE( inStream.ReadValue<int32>( fileVersion ) );
        fileVersion = inStream.ReadInt();

        if( fileVersion != c_packFileVersion )
        {
            LogUtil.e(LogUtil.LogType.DEFAULT, "vaAssetPack::Load(): unsupported file version");
            return false;
        }

        m_name = inStream.ReadString(  );

        /*int32 numberOfAssets = 0;
        VERIFY_TRUE_RETURN_ON_FALSE( inStream.ReadValue<int32>( numberOfAssets ) );*/
        int numberOfAssets = inStream.ReadInt();

        List<VaAsset> loadedAssets = new ArrayList<>();

        for( int i = 0; i < numberOfAssets; i++  )
        {
            /*int64 subSize = 0;
            VERIFY_TRUE_RETURN_ON_FALSE( inStream.ReadValue<int64>( subSize ) );*/
            long subSize = inStream.ReadLong();

            // read type
            /*vaAssetType assetType;
            VERIFY_TRUE_RETURN_ON_FALSE( inStream.ReadValue<int32>( (int32&)assetType ) );*/
            int assetType = inStream.ReadInt();

            // read name
            /*string newAssetName;
            VERIFY_TRUE_RETURN_ON_FALSE( inStream.ReadString( newAssetName ) );*/
            String newAssetName = inStream.ReadString();

            if( Find( newAssetName ) != null )
            {
                LogUtil.e(LogUtil.LogType.DEFAULT, "vaAssetPack::Load(): duplicated asset name, stopping loading.");
                assert( false );
                return false;
            }

            VaAsset newAsset = null;

            switch( VaAssetType.values()[ assetType] )
            {
                case Texture:
                    newAsset = VaAssetTexture.CreateAndLoad(this, newAssetName, inStream ) ;
                    break;
                case RenderMesh:
                    newAsset = VaAssetRenderMesh.CreateAndLoad(this, newAssetName, inStream ) ;
                    break;
                case RenderMaterial:
                    newAsset = VaAssetRenderMaterial.CreateAndLoad(this, newAssetName, inStream );
                    break;
                default:
                    break;
            }


            if(newAsset == null)
                throw new IllegalStateException("asset is null");

            InsertAndTrackMe( newAsset );
            loadedAssets.add( newAsset );
        }

        for( int i = 0; i < loadedAssets.size(); i++ )
        {
            loadedAssets.get(i).ReconnectDependencies( );
        }

        return true;
    }

    /*shared_ptr<vaAssetTexture>*/ public VaAssetTexture  Add( /*const shared_ptr<vaTexture> &*/VaTexture texture, String name ){
//        string name = vaStringTools::ToLower( _name );
        name = name.toLowerCase();

        if( Find( name ) != null )
        {
            assert( false );
            LogUtil.e(LogUtil.LogType.DEFAULT, String.format("Unable to add asset '%s' to the asset pack '%s' because the name already exists", name, m_name));
            return null;
        }
//    assert( ( storagePath == L"" ) || ( FindByStoragePath( storagePath ) == nullptr ) );    // assets in packs must have unique names

        VaAssetTexture newItem = /*shared_ptr<vaAssetTexture>*/( new VaAssetTexture(this, texture, name ) );

        InsertAndTrackMe( newItem );

        return newItem;
    }
    public VaAssetRenderMesh                       Add( VaRenderMesh mesh, String name ){
//        string name = vaStringTools::ToLower( _name );
        name = name.toLowerCase();

        if( Find( name ) != null )
        {
            assert( false );
            LogUtil.e(LogUtil.LogType.DEFAULT, String.format("Unable to add asset '%s' to the asset pack '%s' because the name already exists", name, m_name));
            return null;
        }
//    assert( ( storagePath == L"" ) || ( FindByStoragePath( storagePath ) == nullptr ) );    // assets in packs must have unique names

        VaAssetRenderMesh newItem = /*shared_ptr<vaAssetRenderMesh>*/( new VaAssetRenderMesh(this, mesh, name ) );

        InsertAndTrackMe( newItem );

        return newItem;
    }

    public VaAssetRenderMaterial                   Add(VaRenderMaterial material, String name ){
        name = name.toLowerCase();

        if( Find( name ) != null )
        {
            assert( false );
            LogUtil.e(LogUtil.LogType.DEFAULT, String.format("Unable to add asset '%s' to the asset pack '%s' because the name already exists", name, m_name));
            return null;
        }
//    assert( ( storagePath == L"" ) || ( FindByStoragePath( storagePath ) == nullptr ) );    // assets in packs must have unique names

        VaAssetRenderMaterial newItem = /*shared_ptr<vaAssetRenderMesh>*/( new VaAssetRenderMaterial(this, material, name ) );

        InsertAndTrackMe( newItem );

        return newItem;
    }

    public String Name( )  { return m_name; }

    public boolean                                                Rename( VaAsset asset, String newName ){
//        string newName = vaStringTools::ToLower( _newName );
        newName = newName.toLowerCase();

        if( asset.m_parentPack != this )
        {
            LogUtil.e(LogUtil.LogType.DEFAULT, String.format("Unable to change asset name from '%s' to '%s' in asset pack '%s' - not correct parent pack!", asset.Name(), newName, m_name));
            return false;
        }
        if( newName == asset.Name() )
        {
            LogUtil.e(LogUtil.LogType.DEFAULT, String.format("Changing asset name from '%s' to '%s' in asset pack '%s' - same name requested? Nothing changed.", asset.Name(), newName, m_name));
            return true;
        }
        if( Find( newName ) != null )
        {
            LogUtil.e(LogUtil.LogType.DEFAULT, String.format("Unable to change asset name from '%s' to '%s' in asset pack '%s' - name already used by another asset!", asset.Name(), newName, m_name));
            return false;
        }

        {
//            auto it = m_assetMap.find( vaStringTools::ToLower( asset.Name() ) );
            VaAsset it = m_assetMap.remove(asset.Name().toLowerCase());

//            VaAsset assetSharedPtr;

            if( it != /*m_assetMap.end()*/null )
            {
                /*assetSharedPtr = it->second*/;
//                assert( assetSharedPtr.get() == &asset );
//                m_assetMap.remove()
            }
            else
            {
                LogUtil.e(LogUtil.LogType.DEFAULT, String.format("Error changing asset name from '%s' to '%s' in asset pack '%s' - original asset not found!", asset.Name(), newName, m_name));
                return false;
            }

            it.m_name = newName;

//            m_assetMap.insert( std::pair< string, shared_ptr<vaAsset> >( vaStringTools::ToLower( assetSharedPtr->Name() ), assetSharedPtr ) );
            m_assetMap.put(it.Name().toLowerCase(), it);
        }

        LogUtil.i(LogUtil.LogType.DEFAULT, String.format("Changing asset name from '%s' to '%s' in asset pack '%s' - success!", asset.Name(), newName, m_name));
        return true;
    }

    public int Count( ) {
        if(m_assetList == null){
            return 0;
        }

        assert( m_assetList.size() == m_assetMap.size() ); return m_assetList.size();
    }

//        const shared_ptr<vaAsset> &                         operator [] ( size_t index )            { return m_assetList[index]; }
    public VaAsset Get(int index)   { return m_assetList != null ? m_assetList.get(index) : null;}

    protected String                                      IHO_GetInstanceInfo( )                { //return vaStringTools::Format("Asset Pack '%s'", m_name.c_str() );
        return "Asset Pack " + m_name;
    }
    protected void                                        IHO_Draw( ){
        /*ImGui::PushItemWidth( 120.0f );

        // rename UI
        {
            bool renameJustOpened = false;
        const char * renameAssetPackPopup = "Rename asset pack";
            if( ImGui::Button( "Rename asset pack" ) )
            {
                ImGui::OpenPopup( renameAssetPackPopup );
                strcpy_s( m_uiContext.RenamingPopupNewNameBuff, sizeof(m_uiContext.RenamingPopupNewNameBuff), m_name.c_str() );
                renameJustOpened = true;
            }

            if( ImGui::BeginPopupModal( renameAssetPackPopup ) )
            {
                string newName = "";
                if( renameJustOpened )
                {
                    ImGui::SetKeyboardFocusHere();
                }

                //if(
                ImGui::InputText( "New name", m_uiContext.RenamingPopupNewNameBuff, sizeof( m_uiContext.RenamingPopupNewNameBuff ), ImGuiInputTextFlags_CharsNoBlank | ImGuiInputTextFlags_AutoSelectAll | ImGuiInputTextFlags_EnterReturnsTrue );// )

                newName = vaStringTools::ToLower( string( m_uiContext.RenamingPopupNewNameBuff ) );
                if( ImGui::Button( "Accept" ) )
                {
                    if( newName != "" )
                    {
                        m_name = newName;
                        ImGui::CloseCurrentPopup();
                    }
                }
                ImGui::SameLine();
                if( ImGui::Button( "Cancel" ) )
                {
                    ImGui::CloseCurrentPopup();
                }

                ImGui::EndPopup();
            }
        }

        ImGui::Separator();

#ifdef VA_ASSIMP_INTEGRATION_ENABLED
        // importing assets UI
        {
        const char * importAssetsPopup = "Import assets";
            if( ImGui::Button( importAssetsPopup ) )
            {
                wstring fileName = vaFileTools::OpenFileDialog( L"", vaCore::GetExecutableDirectory() );
                if( vaFileTools::FileExists( fileName ) )
                {
                    //strcpy_s( m_uiContext.ImportingModeNewFileNameBuff, sizeof(m_uiContext.ImportingModeNewFileNameBuff), "" );
                    m_uiContext.ImportingPopupSelectedFile          = fileName;
                    m_uiContext.ImportingPopupBaseTranslation       = vaVector3( 0.0f, 0.0f, 0.0f );
                    m_uiContext.ImportingPopupBaseScaling           = vaVector3( 1.0f, 1.0f, 1.0f );
                    m_uiContext.ImportingPopupRotateX90             = true;
                    m_uiContext.ImportingForceGenerateNormals       = false;
                    m_uiContext.ImportingGenerateNormalsIfNeeded    = true;
                    m_uiContext.ImportingGenerateSmoothNormals      = true;
                    //m_uiContext.ImportingRegenerateTangents     = false;
                    ImGui::OpenPopup("Import assets");
                }
            }

            if( ImGui::BeginPopupModal( importAssetsPopup ) )
            {
                string fileNameA = vaStringTools::SimpleNarrow( m_uiContext.ImportingPopupSelectedFile );
                ImGui::Text( "Importing file:" );
                ImGui::Text( "(idea: store these import settings default next to the importing file in .va_lastimport)" );
                ImGui::Text( " '%s'", fileNameA.c_str() );
                ImGui::Separator();
                ImGui::Text( "Base transformation (applied to everything):" );
                ImGui::Checkbox( "Base rotate around X axis by 90?,        &m_uiContext.ImportingPopupRotateX90 );
                ImGui::InputFloat3( "Base scaling",                         &m_uiContext.ImportingPopupBaseScaling.x );
                ImGui::InputFloat3( "Base translation",                     &m_uiContext.ImportingPopupBaseTranslation.x );
                ImGui::Separator();
                ImGui::Text( "Import options:" );
                ImGui::Checkbox( "Force generate normals",                  &m_uiContext.ImportingForceGenerateNormals );
                ImGui::Checkbox( "Generate normals (if missing)",           &m_uiContext.ImportingGenerateNormalsIfNeeded );
                ImGui::Checkbox( "Generate smooth normals (if generating)", &m_uiContext.ImportingGenerateSmoothNormals );
                //ImGui::Checkbox( "Regenerate tangents/bitangents",      &m_uiContext.ImportingRegenerateTangents );
                ImGui::Separator();
                if( ImGui::Button( "          Start importing (can take a while)          " ) )
                {
                    vaAssetImporter::LoadingParameters loadParams( *this );
                    loadParams.BaseTransform                       = vaMatrix4x4::Scaling( m_uiContext.ImportingPopupBaseScaling ) * vaMatrix4x4::RotationX( m_uiContext.ImportingPopupRotateX90?(VA_PIf * 0.5f):(0.0f) ) * vaMatrix4x4::Translation( m_uiContext.ImportingPopupBaseTranslation ) ;
                    loadParams.ForceGenerateNormals                = m_uiContext.ImportingForceGenerateNormals;
                    loadParams.GenerateNormalsIfNeeded             = m_uiContext.ImportingGenerateNormalsIfNeeded;
                    loadParams.GenerateSmoothNormalsIfGenerating   = m_uiContext.ImportingGenerateSmoothNormals;
                    //loadParams.RegenerateTangents  = m_uiContext.ImportingRegenerateTangents;
                    vaAssetImporter::LoadFileContents( m_uiContext.ImportingPopupSelectedFile, loadParams );

                    ImGui::CloseCurrentPopup();
                }
                ImGui::SameLine();
                if( ImGui::Button( "Cancel" ) )
                {
                    ImGui::CloseCurrentPopup();
                }

                ImGui::EndPopup();
            }
        }
#endif

        // Loading assets
        if( ImGui::Button( "Load .apack" ) )
        {
            wstring fileName = vaFileTools::OpenFileDialog( L"", vaCore::GetExecutableDirectory(), L".apack files\0*.apack\0\0" );
            if( vaFileTools::FileExists( fileName ) )
            {
                vaFileStream fileIn;
                if( fileIn.Open( fileName, FileCreationMode::Open ) )
                    Load( fileIn );
            }
        }

        // Saving assets
        if( ImGui::Button( "Save .apack" ) )
        {
            wstring fileName = vaFileTools::SaveFileDialog( L"", vaCore::GetExecutableDirectory(), L".apack files\0*.apack\0\0" );
            vaFileStream fileOut;
            if( fileOut.Open( fileName, FileCreationMode::Create ) )
                Save( fileOut );
        }

        ImGui::Separator();

        // remove all assets UI
        {
        const char * deleteAssetPackPopup = "Remove all assets";
            if( ImGui::Button( "Remove all assets" ) )
            {
                ImGui::OpenPopup( deleteAssetPackPopup );
            }

            if( ImGui::BeginPopupModal( deleteAssetPackPopup ) )
            {
                ImGui::Text( "Are you sure that you want to remove all assets?" );
                if( ImGui::Button( "Yes" ) )
                {
                    RemoveAll();
                    ImGui::CloseCurrentPopup();
                }
                ImGui::SameLine();
                if( ImGui::Button( "Cancel" ) )
                {
                    ImGui::CloseCurrentPopup();
                }
                ImGui::EndPopup();
            }
        }

        ImGui::Separator();

        // Show contents
        {
            ImGui::Text( "Contained assets (%d):", m_assetList.size() );
            ImGui::Indent();
            for( size_t i = 0; i < m_assetList.size(); i++ )
            {
                vaImguiHierarchyObject::DrawCollapsable( *m_assetList[i] );
            }
            ImGui::Unindent();
        }

        ImGui::PopItemWidth( );*/
    }

    private void InsertAndTrackMe( /*shared_ptr<vaAsset>*/VaAsset newAsset ){
//        m_assetMap.insert( std::pair< string, shared_ptr<vaAsset> >( vaStringTools::ToLower( newAsset->Name() ), newAsset ) );
        m_assetMap.put(newAsset.Name().toLowerCase(), newAsset);
        //    if( storagePath != L"" )
        //        m_assetMapByStoragePath.insert( std::pair< wstring, shared_ptr<vaAsset> >( vaStringTools::ToLower(newAsset->storagePath), newAsset ) );

        if(m_assetList == null)
            m_assetList = new ArrayList<>();

        assert( newAsset.m_parentPackStorageIndex == -1 );
        m_assetList.add( newAsset );
        newAsset.m_parentPackStorageIndex = (m_assetList.size())-1;
    }

//    private:
    void OnRenderingAPIAboutToShutdown( )            { RemoveAll(); }

    private static final class UIContext
    {
//        char        RenamingPopupNewNameBuff[128];
        String  RenamingPopupNewNameBuff;

        String     ImportingPopupSelectedFile;
        Vector3f ImportingPopupBaseTranslation;
        Vector3f   ImportingPopupBaseScaling;
        boolean        ImportingPopupRotateX90;            // a.k.a. Z is up
        boolean        ImportingForceGenerateNormals;
        boolean        ImportingGenerateNormalsIfNeeded;
        boolean        ImportingGenerateSmoothNormals;
        //float       ImportingGenerateSmoothNormalsSmoothingAngle;
        //bool        ImportingRegenerateTangents;
    };
}
