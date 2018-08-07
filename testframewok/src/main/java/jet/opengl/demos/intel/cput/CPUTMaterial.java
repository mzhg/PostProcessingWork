package jet.opengl.demos.intel.cput;

import org.lwjgl.util.vector.Vector4f;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.util.StringUtils;

/**
 * Created by mazhen'gui on 2017/11/13.
 */

public class CPUTMaterial implements Disposeable{
    public static final int
        CPUT_MATERIAL_MAX_TEXTURE_SLOTS         =8,  // TODO Eight is enough.
        CPUT_MATERIAL_MAX_BUFFER_SLOTS          =8,
        CPUT_MATERIAL_MAX_CONSTANT_BUFFER_SLOTS =8,
        CPUT_MATERIAL_MAX_SRV_SLOTS             =8,
            CPUT_MATERIAL_MAX_UAV_SLOTS         = 8;

    public static final CPUTConfigBlock mGlobalProperties = new CPUTConfigBlock();

    protected String               mMaterialName;
    protected CPUTModel            mpModel; // We use pointers to the model and mesh to distinguish instanced materials.
    protected int                  mMeshIndex;
    protected CPUTConfigBlock      mConfigBlock;
    protected CPUTRenderStateBlock mpRenderStateBlock;

    // A material can have multiple submaterials.  If it does, then that's all it does.  It just "branches" to other materials.
    // TODO: We could make that a special object, and derive them from material.  Not sure that's worth the effort.
    // The alternative we choose here is to simply comandeer this one, ignoring most of its state and functionality.
    protected int                   mMaterialEffectCount;
    protected CPUTMaterialEffect [] mpMaterialEffects;
    protected String              [] mpMaterialEffectNames;
    protected int                  mCurrentMaterialEffect;

    CPUTMaterial         mpMaterialNextClone;
    CPUTMaterial         mpMaterialLastClone;

    public static CPUTMaterial CreateMaterial(String absolutePathAndFilename, String modelSuffix, String meshSuffix )throws IOException{
        CPUTMaterialDX11 pMaterial = new CPUTMaterialDX11();

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
    )throws IOException{
        // Create the material and load it from file.
        //#ifdef CPUT_FOR_DX11
        //    CPUTMaterial *pMaterial = new CPUTMaterialDX11();
        //#elif (defined(CPUT_FOR_OGL) || defined(CPUT_FOR_OGLES))
        //    CPUTMaterial *pMaterial = new CPUTMaterialOGL();
        //#else
        //    #error You must supply a target graphics API (ex: #define CPUT_FOR_DX11), or implement the target API for this file.
        //#endif

        //    pMaterial->mpSubMaterials = NULL;
        CPUTMaterial pMaterial = new CPUTMaterial();

        pMaterial.LoadMaterial( absolutePathAndFilename, pModel, meshIndex, pShaderMacros, numSystemMaterials, pSystemMaterialNames, externalCount, pExternalName, pExternals, pExternalOffset, pExternalSize );
//        ASSERT( CPUTSUCCESS(result), _L("\nError - CPUTAssetLibrary::GetMaterial() - Error in material file: '")+absolutePathAndFilename+_L("'") );
//        UNREFERENCED_PARAMETER(result);

        // Add the material to the asset library.
        CPUTAssetLibrary.GetAssetLibrary().AddMaterial( absolutePathAndFilename, "", "", pMaterial, pShaderMacros, null,-1 );

        return pMaterial;
    }

    private void    LoadMaterial(String fileName, CPUTModel pModel, int meshIndex/*=-1*/,
        Macro[] pShaderMacros, int systemMaterialCount, String []pSystemMaterialNames,
        int externalCount, String pExternalName,Vector4f []pExternals,
        int []pExternalOffset, int  []pExternalSize)throws IOException{
        mMaterialName = fileName;
//        mMaterialNameHash = CPUTComputeHash( mMaterialName );

        // Open/parse the file
        CPUTConfigFile file = new CPUTConfigFile();
        file.LoadFile(fileName);
//		const char * tempfilename = fileName.c_str();

        // Make a local copy of all the parameters
        CPUTConfigBlock pBlock = file.GetBlock(0);
//        ASSERT( pBlock, _L("Error getting parameter block") );
        if( pBlock == null)
        {
//            return CPUT_ERROR_PARAMETER_BLOCK_NOT_FOUND;
            throw new NullPointerException("Error getting parameter block");
        }
        mConfigBlock = pBlock;

        CPUTAssetLibrary pAssetLibrary = CPUTAssetLibrary.GetAssetLibrary();


        mMaterialEffectCount = 0;
        if( mConfigBlock.GetValueByName("MultiMaterial" ).ValueAsBool() )
        {
            // Count materials;
            for(;;)
            {
                CPUTConfigEntry pValue = mConfigBlock.GetValueByName( "Material" + mMaterialEffectCount );
                if( pValue.IsValid() )
                {
                    ++mMaterialEffectCount;
                } else
                {
                    break;
                }
            }
            assert mMaterialEffectCount != 0 : ("MultiMaterial specified, but no sub materials given.");

            // Reserve space for "authored" materials plus system materials
            mpMaterialEffectNames = new String[mMaterialEffectCount+systemMaterialCount];
            int ii;
            for( ii=0; ii<mMaterialEffectCount; ii++ )
            {
                CPUTConfigEntry pValue = mConfigBlock.GetValueByName( "Material" + ii );
                mpMaterialEffectNames[ii] = pAssetLibrary.GetMaterialEffectPath(pValue.ValueAsString(), false);
            }
        }
        else
        {
            mMaterialEffectCount = 1;
            mpMaterialEffectNames = new String[mMaterialEffectCount+systemMaterialCount];
            mpMaterialEffectNames[0] = fileName;
        }

        // The real material count includes the system material count
        mMaterialEffectCount += systemMaterialCount;
        for( int ii=0; ii<systemMaterialCount; ii++ )
        {

            // System materials "grow" from the end; the 1st system material is at the end of the list.
            mpMaterialEffectNames[mMaterialEffectCount-systemMaterialCount+ii] = pSystemMaterialNames[ii];
        }
        mpMaterialEffects = new CPUTMaterialEffect[mMaterialEffectCount+1];
        for( int ii=0; ii<mMaterialEffectCount; ii++ )
        {
            Macro [] pFinalShaderMacros = pShaderMacros;
            Macro []pUserSpecifiedMacros = null;
            int numUserSpecifiedMacros = 0;
            // Read additional macros from .mtl file
            String macroBlockName = "defines" + ii;
            CPUTConfigBlock pMacrosBlock = file.GetBlockByName(macroBlockName);
            if( pMacrosBlock != null)
            {
                List<Macro> userSpecifiedMacros = new ArrayList<>();
                List<Macro> finalShaderMacros = new ArrayList<>();
                ReadMacrosFromConfigBlock( pMacrosBlock, pShaderMacros, userSpecifiedMacros, /*&numUserSpecifiedMacros,*/ finalShaderMacros );

                pUserSpecifiedMacros = userSpecifiedMacros.toArray(new Macro[userSpecifiedMacros.size()]);
                pFinalShaderMacros = finalShaderMacros.toArray(new Macro[finalShaderMacros.size()]);
                numUserSpecifiedMacros = pUserSpecifiedMacros.length;
            }

            mpMaterialEffects[ii] = pAssetLibrary.GetMaterialEffect( mpMaterialEffectNames[ii], true, pModel, meshIndex, pFinalShaderMacros );
            for( int kk=0; kk<numUserSpecifiedMacros; kk++ )
            {
                // ReadMacrosFromConfigBlock allocates memory (ws2s does).  Delete it here.
//                SAFE_DELETE(pUserSpecifiedMacros[kk].Name);
//                SAFE_DELETE(pUserSpecifiedMacros[kk].Definition);
            }
//            SAFE_DELETE_ARRAY( pFinalShaderMacros );
//            SAFE_DELETE_ARRAY( pUserSpecifiedMacros );
        }
        mpMaterialEffects[mMaterialEffectCount] = null;
    }

    //-----------------------------------------------------------------------------
    void ReadMacrosFromConfigBlock(
            CPUTConfigBlock   pMacrosBlock,
            Macro[]  pShaderMacros,
            List<Macro> pUserSpecifiedMacros,
//            int               *pNumUserSpecifiedMacros,
            List<Macro> pFinalShaderMacros
    ){
        int pNumUserSpecifiedMacros = pMacrosBlock.ValueCount();

        // Count the number of macros passed in
        Macro[] pMacro = pShaderMacros;
        int numPassedInMacros = 0;
        if( pMacro != null && pMacro.length > 0)
        {
            while( !StringUtils.isBlank(pMacro[numPassedInMacros].key))
            {
                ++numPassedInMacros;
//                ++pMacro;
            }
        }

        // Allocate an array of macro pointer large enough to contain the passed-in macros plus those specified in the .mtl file.
//        *pFinalShaderMacros = new CPUT_SHADER_MACRO[*pNumUserSpecifiedMacros + numPassedInMacros + 1];

        // Copy the passed-in macro pointers to the final array
        int jj;
        for( jj=0; jj<numPassedInMacros; jj++ )
        {
//            (*pFinalShaderMacros)[jj] = *(CPUT_SHADER_MACRO*)&pShaderMacros[jj];
            pFinalShaderMacros.add(pShaderMacros[jj]);
        }

        // Create a CPUT_SHADER_MACRO for each of the macros specified in the .mtl file.
        // And, add their pointers to the final array
//        *pUserSpecifiedMacros = new CPUT_SHADER_MACRO[pNumUserSpecifiedMacros];
        for( int kk=0; kk<pNumUserSpecifiedMacros; kk++, jj++ )
        {
            CPUTConfigEntry pValue   = pMacrosBlock.GetValue(kk);
//            (*pUserSpecifiedMacros)[kk].Name       = ws2s(pValue->NameAsString());
//            (*pUserSpecifiedMacros)[kk].Definition = ws2s(pValue->ValueAsString());
//            (*pFinalShaderMacros)[jj] = (*pUserSpecifiedMacros)[kk];

            Macro macro = new Macro(pValue.NameAsString(), pValue.ValueAsString());
            pUserSpecifiedMacros.add(macro);
            pFinalShaderMacros.add(macro);
        }
//        (*pFinalShaderMacros)[jj].Name = NULL;
//        (*pFinalShaderMacros)[jj].Definition = NULL;
    }

    public void                  SetMaterialName(String MaterialName) { mMaterialName = MaterialName; }
    public String                  GetMaterialName()      {return mMaterialName; }

    public CPUTMaterialEffect    [] GetMaterialEffects() { return mpMaterialEffects; }
    public int                  GetMaterialEffectCount() { return mMaterialEffectCount; }
    public int  GetCurrentEffect() { return mCurrentMaterialEffect; }
    public void SetCurrentEffect(int CurrentMaterial) { mCurrentMaterialEffect = CurrentMaterial; }

    public CPUTMaterial         GetNextClone() { return mpMaterialNextClone; }

    @Override
    public void dispose() {
        for(int ii = 0; ii < mMaterialEffectCount; ii++)
        {
            SAFE_RELEASE(mpMaterialEffects[ii]);
        }

        mpMaterialEffects = null;
    }
}
