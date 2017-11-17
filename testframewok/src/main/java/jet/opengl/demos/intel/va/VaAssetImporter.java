package jet.opengl.demos.intel.va;

import org.lwjgl.util.vector.Matrix4f;

import java.util.List;

/**
 * Created by mazhen'gui on 2017/11/17.
 */

public class VaAssetImporter {

    public static boolean LoadFileContents(String path, LoadingParameters parameters, /*LoadedContent * outContent = nullptr*/ List<VaAsset> outContent){
        throw new UnsupportedOperationException();
    }

    public static final class LoadingParameters{
        // pack to save assets into and to search dependencies to link to
        public VaAssetPack               AssetPack;

        public String                      NamePrefix;             // added to loaded asset resource names (can be used to create hierarchy - "importfilename\"
        public final Matrix4f BaseTransform = new Matrix4f();          // for conversion between coord systems, etc

        public boolean                        TextureOnlyLoadDDS;
        public boolean                        TextureTryLoadDDS;
        public boolean                        TextureAlphaMaskInColorAlpha;

        public boolean                        ForceGenerateNormals;
        public boolean                        GenerateNormalsIfNeeded;
        public boolean                        GenerateSmoothNormalsIfGenerating;
        public float                       GenerateSmoothNormalsSmoothingAngle;    // in degrees, see AI_CONFIG_PP_GSN_MAX_SMOOTHING_ANGLE for more info
        //bool                        RegenerateTangents;

        LoadingParameters( VaAssetPack assetPack, Matrix4f  baseTransform /*= vaMatrix4x4::Identity*/ ) /*: AssetPack( assetPack ), BaseTransform( baseTransform )*/
        {
            AssetPack = assetPack;
            BaseTransform.load(baseTransform);
            NamePrefix                          = "";
            ForceGenerateNormals                = false;
            GenerateNormalsIfNeeded             = true;
            GenerateSmoothNormalsIfGenerating   = false;
            //RegenerateTangents              = false;
            TextureTryLoadDDS                   = true;
            TextureOnlyLoadDDS                  = true;
            TextureAlphaMaskInColorAlpha        = true;
            GenerateSmoothNormalsSmoothingAngle = 88.0f;
        }
    }
}
