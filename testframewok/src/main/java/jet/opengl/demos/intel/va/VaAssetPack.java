package jet.opengl.demos.intel.va;

import org.lwjgl.util.vector.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jet.opengl.postprocessing.util.LogUtil;

/**
 * Created by mazhen'gui on 2017/11/16.
 */

public class VaAssetPack /*extends ImguiHierarchyObject*/ {
    protected String                                              m_name;
    protected Map< String, VaAsset> m_assetMap = new HashMap<>();

    private TT_Trackee< VaAssetPack >                       m_trackee;  // for tracking by vaAssetPackManager
    protected List<VaAsset> m_assetList;

    protected UIContext                                           m_uiContext;

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

    /*shared_ptr<vaAsset>*/ public VaAsset  Find( String _name ){

    }
    //shared_ptr<vaAsset>                                 FindByStoragePath( const wstring & _storagePath );
    void                                                Remove( const shared_ptr<vaAsset> & asset );
    void                                                RemoveAll( );
    // save current contents
    bool                                                Save( vaStream & outStream );
    // load contents (current contents are not deleted)
    bool                                                Load( vaStream & inStream );

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
    shared_ptr<vaAssetRenderMesh>                       Add( const shared_ptr<vaRenderMesh> & mesh, const string & name );
    shared_ptr<vaAssetRenderMaterial>                   Add( const shared_ptr<vaRenderMaterial> & material, const string & name );

        const string &                                      Name( ) const                               { return m_name; }

    bool                                                Rename( vaAsset & asset, const string & newName );

    size_t                                              Count( ) const                          { assert( m_assetList.size() == m_assetMap.size() ); return m_assetList.size(); }

        const shared_ptr<vaAsset> &                         operator [] ( size_t index )            { return m_assetList[index]; }

    protected:
    virtual string                                      IHO_GetInstanceInfo( ) const                { return vaStringTools::Format("Asset Pack '%s'", m_name.c_str() ); }
    virtual void                                        IHO_Draw( );

    private void                                                InsertAndTrackMe( /*shared_ptr<vaAsset>*/VaAsset newAsset ){
//        m_assetMap.insert( std::pair< string, shared_ptr<vaAsset> >( vaStringTools::ToLower( newAsset->Name() ), newAsset ) );
        m_assetMap.put(newAsset.Name().toLowerCase(), newAsset);
        //    if( storagePath != L"" )
        //        m_assetMapByStoragePath.insert( std::pair< wstring, shared_ptr<vaAsset> >( vaStringTools::ToLower(newAsset->storagePath), newAsset ) );

        assert( newAsset.m_parentPackStorageIndex == -1 );
        m_assetList.add( newAsset );
        newAsset.m_parentPackStorageIndex = (m_assetList.size())-1;
    }

//    private:
    void                                                OnRenderingAPIAboutToShutdown( )            { RemoveAll(); }

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
