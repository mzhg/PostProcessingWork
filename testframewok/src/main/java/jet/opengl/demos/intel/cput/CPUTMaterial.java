package jet.opengl.demos.intel.cput;

import java.io.IOException;

import jet.opengl.postprocessing.common.Disposeable;

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

    public void                  SetMaterialName(String MaterialName) { mMaterialName = MaterialName; }
    public String                  GetMaterialName()      {return mMaterialName; }
    public abstract void    LoadMaterial(String fileName, String modelSuffix, String meshSuffix)throws IOException;
    public abstract void          ReleaseTexturesAndBuffers( boolean recurseSubMaterials /*= false*/ );
    public abstract void          RebindTexturesAndBuffers( boolean recurseSubMaterials /*= false*/ );
    public void          SetRenderStates(CPUTRenderParameters renderParams) { if( mpRenderStateBlock !=null) { mpRenderStateBlock.SetRenderStates(renderParams); } }
    public abstract boolean          MaterialRequiresPerModelPayload();
    public abstract CPUTMaterial CloneMaterial( String absolutePathAndFilename, String modelSuffix, String meshSuffix );
}
