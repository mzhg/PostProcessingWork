package jet.opengl.demos.intel.va;

import java.io.IOException;
import java.util.UUID;

import jet.opengl.postprocessing.util.LogUtil;

/**
 * Created by mazhen'gui on 2017/11/17.
 */

public class VaAssetTexture extends VaAsset {

    protected VaAssetTexture(VaAssetPack pack, VaTexture texture, String name) {
        super(pack, VaAssetType.Texture, name, texture);
        Resource = texture;
    }

    @Override
    public boolean Save(VaStream outStream) {
        throw new UnsupportedOperationException();
    }

    public  VaTexture                           Resource;

    public  VaTexture           GetTexture( )                        { return Resource; }

    public static VaAssetTexture CreateAndLoad( VaAssetPack pack, String name, VaStream inStream ) throws IOException{
        /*vaGUID uid;
        VERIFY_TRUE_RETURN_ON_FALSE( inStream.ReadValue<vaGUID>( uid ) );*/
        long most = inStream.ReadLong();
        long leatest = inStream.ReadLong();
        UUID uid = new UUID(most, leatest);

        VaTexture newResource = //VA_RENDERING_MODULE_CREATE_PARAMS_SHARED( vaTexture, vaTextureConstructorParams( uid ) );
                VaRenderingModuleRegistrar.CreateModuleTyped("vaTexture", new VaTextureConstructorParams(uid));

        if( newResource == null )
            return null;

        if( !newResource.UIDObject_IsCorrectlyTracked() )
        {
            LogUtil.e(LogUtil.LogType.DEFAULT, "Error creating asset texture; uid already used" );
            return null;
        }

        if( newResource.Load( inStream ) )
        {
            return new VaAssetTexture( pack, newResource, name );
        }
        else
        {
            return null;
        }
    }

    public static VaAssetTexture               SafeCast( VaAsset asset ) {
        /*assert( asset->Type == vaAssetType::RenderMesh );
        return std::dynamic_pointer_cast<vaAssetTexture, vaAsset>( asset );*/
        try {
            return (VaAssetTexture) asset;
        } catch (Exception e) {
//            e.printStackTrace();
            LogUtil.e(LogUtil.LogType.DEFAULT, "The given asset type of " + asset.getClass() + " can't cast to VaAssetTexture.");
            return null;
        }
    }
}
