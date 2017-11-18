package jet.opengl.demos.intel.va;

import java.util.UUID;

/**
 * Created by mazhen'gui on 2017/11/17.
 */

final class VaAssetRenderMesh extends VaAsset {
    VaRenderMesh                        Resource;
    VaAssetRenderMesh( VaAssetPack pack, VaRenderMesh mesh, String name ){
        super(pack, VaAssetType.RenderMesh, name, mesh);

        Resource = mesh;
    }


    public VaRenderMesh        GetMesh( )                           { return Resource; }

    public static VaAssetRenderMesh                      CreateAndLoad( VaAssetPack pack, String name, VaStream inStream ){
        UUID uid;
        /*VERIFY_TRUE_RETURN_ON_FALSE( inStream.ReadValue<vaGUID>( uid ) );*/
        long mostSigBits = inStream.ReadLong();
        long leastSigBits = inStream.ReadLong();
        uid = new UUID(mostSigBits, leastSigBits);

        VaRenderMesh newResource = VaRenderMeshManager.GetInstance().CreateRenderMesh( uid );

        if( newResource == null )
            return null;

        if( newResource.Load( inStream ) )
        {
            return new VaAssetRenderMesh( pack, newResource, name );
        }
        else
        {
            return null;
        }
    }

//    static shared_ptr<>            SafeCast( const shared_ptr<vaAsset> & asset ) { assert( asset->Type == vaAssetType::RenderMesh ); return std::dynamic_pointer_cast<vaAssetRenderMesh, vaAsset>( asset ); }

    protected String IHO_GetInstanceInfo( )         { return /*vaStringTools::Format( "mesh: %s ", Name().c_str() );*/
                                                       "mesh: " + Name(); }

    @Override
    public boolean Save(VaStream outStream) {
        throw new UnsupportedOperationException();
    }
}
