package jet.opengl.demos.intel.cput;

import org.lwjgl.util.vector.Vector4f;

import java.io.IOException;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.shader.Macro;

/**
 * Created by mazhen'gui on 2017/11/13.
 */

public abstract class CPUTMaterial implements Disposeable{
    public static final int
        CPUT_MATERIAL_MAX_TEXTURE_SLOTS         =8,  // TODO Eight is enough.
        CPUT_MATERIAL_MAX_BUFFER_SLOTS          =8,
        CPUT_MATERIAL_MAX_CONSTANT_BUFFER_SLOTS =8,
        CPUT_MATERIAL_MAX_SRV_SLOTS             =8,
            CPUT_MATERIAL_MAX_UAV_SLOTS         = 8;

    public static final CPUTConfigBlock mGlobalProperties = new CPUTConfigBlock();

    protected String               mMaterialName;
    protected CPUTConfigBlock       mConfigBlock;
    protected CPUTRenderStateBlock mpRenderStateBlock;
    protected int                  mBufferCount;

    public static CPUTMaterial CreateMaterial(String absolutePathAndFilename, String modelSuffix, String meshSuffix )throws IOException{
        CPUTMaterial pMaterial = new CPUTMaterialDX11();

        pMaterial.LoadMaterial(absolutePathAndFilename, modelSuffix, meshSuffix);
//        ASSERT( CPUTSUCCESS(result), _L("\nError - CPUTAssetLibrary::GetMaterial() - Error in material file: '")+absolutePathAndFilename+_L("'") );

        // add material to material library list
        // cString finalName = pMaterial->MaterialRequiresPerModelPayload() ? absolutePathAndFilename +  modelSuffix + meshSuffix : absolutePathAndFilename;
        // CPUTAssetLibrary::GetAssetLibrary()->AddMaterial( finalName, pMaterial );
//        CPUTAssetLibrary::GetAssetLibrary()->AddMaterial( absolutePathAndFilename, pMaterial );  TODO

        return pMaterial;
    }

    public static CPUTMaterial CreateMaterial(
            String   absolutePathAndFilename,
            CPUTModel pModel,
            int        meshIndex,
            Macro[] pShaderMacros, // Note: this is honored only on first load.  Subsequent GetMaterial calls will return the material with shaders as compiled with original macros.
            int        numSystemMaterials,
            String[]   pSystemMaterialNames,
            int        externalCount,
            String     pExternalName,
            Vector4f[] pExternals,
            int[]      pExternalOffset,
            int[]      pExternalSize
    ){
        // Create the material and load it from file.
        //#ifdef CPUT_FOR_DX11
        //    CPUTMaterial *pMaterial = new CPUTMaterialDX11();
        //#elif (defined(CPUT_FOR_OGL) || defined(CPUT_FOR_OGLES))
        //    CPUTMaterial *pMaterial = new CPUTMaterialOGL();
        //#else
        //    #error You must supply a target graphics API (ex: #define CPUT_FOR_DX11), or implement the target API for this file.
        //#endif

        //    pMaterial->mpSubMaterials = NULL;
        CPUTMaterial pMaterial = new CPUTMaterialDX11();

        CPUTResult result = pMaterial.LoadMaterial( absolutePathAndFilename, pModel, meshIndex, pShaderMacros, numSystemMaterials, pSystemMaterialNames, externalCount, pExternalName, pExternals, pExternalOffset, pExternalSize );
        ASSERT( CPUTSUCCESS(result), _L("\nError - CPUTAssetLibrary::GetMaterial() - Error in material file: '")+absolutePathAndFilename+_L("'") );
        UNREFERENCED_PARAMETER(result);

        // Add the material to the asset library.
        CPUTAssetLibrary.GetAssetLibrary().AddMaterial( absolutePathAndFilename, _L(""), _L(""), pMaterial, pShaderMacros );

        return pMaterial;
    }

    public void                  SetMaterialName(String MaterialName) { mMaterialName = MaterialName; }
    public String                  GetMaterialName()      {return mMaterialName; }
    public abstract void    LoadMaterial(String fileName, String modelSuffix, String meshSuffix)throws IOException;
    public abstract void          ReleaseTexturesAndBuffers( boolean recurseSubMaterials /*= false*/ );
    public abstract void          RebindTexturesAndBuffers( boolean recurseSubMaterials /*= false*/ );
    public void          SetRenderStates(CPUTRenderParameters renderParams) { if( mpRenderStateBlock !=null) { mpRenderStateBlock.SetRenderStates(renderParams); } }
    public abstract boolean          MaterialRequiresPerModelPayload();
    public abstract CPUTMaterial CloneMaterial( String absolutePathAndFilename, String modelSuffix, String meshSuffix );
}
