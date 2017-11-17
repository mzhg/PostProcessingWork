package jet.opengl.demos.intel.va;

/**
 * Created by mazhen'gui on 2017/11/17.
 */

final class VaAssetRenderMesh extends VaAsset {
    VaAssetRenderMesh( VaAssetPack pack, VaRenderMesh mesh, String name ); {
        super(pack, type, name, resourceBasePtr);
    }

    VaRenderMesh                        Resource;

    public VaRenderMesh        GetMesh( )                           { return Resource; }

    static vaAssetRenderMesh *                      CreateAndLoad( vaAssetPack & pack, const string & name, vaStream & inStream );

    static shared_ptr<vaAssetRenderMesh>            SafeCast( const shared_ptr<vaAsset> & asset ) { assert( asset->Type == vaAssetType::RenderMesh ); return std::dynamic_pointer_cast<vaAssetRenderMesh, vaAsset>( asset ); }

    virtual string                                  IHO_GetInstanceInfo( ) const        { return vaStringTools::Format( "mesh: %s ", Name().c_str() ); }

    @Override
    public boolean Save(VaStream outStream) {
        throw new UnsupportedOperationException();
    }
}
