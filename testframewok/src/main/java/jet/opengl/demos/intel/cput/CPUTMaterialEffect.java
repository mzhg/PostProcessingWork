package jet.opengl.demos.intel.cput;

import org.lwjgl.util.vector.Vector4f;

import java.io.IOException;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.shader.Macro;

public abstract class CPUTMaterialEffect implements Disposeable{

    protected String               mMaterialName;
    protected CPUTModel            mpModel; // We use pointers to the model and mesh to distinguish instanced materials.
    protected int                  mMeshIndex = -1;
    protected CPUTConfigBlock      mConfigBlock;
    protected CPUTRenderStateBlock mpRenderStateBlock;

    // TODO: We could make that a special object, and derive them from material.  Not sure that's worth the effort.
    // The alternative we choose here is to simply comandeer this one, ignoring most of its state and functionality.

    protected CPUTMaterialEffect         mpMaterialNextClone;
    protected CPUTMaterialEffect         mpMaterialLastClone;

    public CPUTLayerType		  mLayer = CPUTLayerType.CPUT_LAYER_SOLID;
    public static CPUTMaterialEffect CreateMaterialEffect(
            String   absolutePathAndFilename,
            CPUTModel pModel/*=NULL*/,
            int        meshIndex/*=-1*/,
            Macro[] pShaderMacros/*=NULL*/,
            int        externalCount/*=0*/,
            String   pExternalName/*=NULL*/,
//            Vector4f[]    pExternals=NULL,
            int       []pExternalOffset/*=NULL*/,
            int       []pExternalSize/*=NULL*/
    ) throws IOException{
        CPUTMaterialEffect pMaterialEffect = new CPUTMaterialEffectDX11();

        pMaterialEffect.LoadMaterialEffect( absolutePathAndFilename, pModel, meshIndex, pShaderMacros,
                0,null, null, null, null );
//        ASSERT( CPUTSUCCESS(result), _L("\nError - CPUTAssetLibrary::GetMaterial() - Error in material file: '")+absolutePathAndFilename+_L("'") );
//        UNREFERENCED_PARAMETER(result);

        // Add the material to the asset library.
        if( pModel != null && pMaterialEffect.MaterialRequiresPerModelPayload() )
        {
            CPUTAssetLibrary.GetAssetLibrary().AddMaterialEffect( absolutePathAndFilename, "", "", pMaterialEffect, pShaderMacros, pModel, meshIndex );
        } else
        {
            CPUTAssetLibrary.GetAssetLibrary().AddMaterialEffect( absolutePathAndFilename, "", "", pMaterialEffect, pShaderMacros, null, -1 );
        }

        return pMaterialEffect;
    }

    public int GetNameHash() { return mMaterialName.hashCode(); }

    public void SetMaterialName(String materialName) { mMaterialName = materialName; /*mMaterialNameHash = CPUTComputeHash(materialName);*/ }
    public String  GetMaterialName() { return mMaterialName; }
    public abstract void          ReleaseTexturesAndBuffers();
    public abstract void          RebindTexturesAndBuffers();
    public void          SetRenderStates(CPUTRenderParameters renderParams) { if( mpRenderStateBlock !=null)
                    { mpRenderStateBlock.SetRenderStates(renderParams); } }
    public abstract boolean          MaterialRequiresPerModelPayload();
    public abstract CPUTMaterialEffect CloneMaterialEffect(  CPUTModel  pModel/*=NULL*/, int meshIndex/*=-1*/ );
    public CPUTMaterialEffect    GetNextClone() { return mpMaterialNextClone; }
    public CPUTModel      GetModel() { return mpModel; }
    public int                   GetMeshIndex() { return mMeshIndex; }
    public abstract void  LoadMaterialEffect(
        String   fileName, CPUTModel pModel/*=NULL*/,
        int        meshIndex/*=-1*/,
        Macro[] pShaderMacros/*=NULL*/,
        int        externalCount/*=0*/,
        String     pExternalName,
        Vector4f[] pExternals,
        int[]      pExternalOffses,
        int[]      pExternalSize
    ) throws IOException;
}
