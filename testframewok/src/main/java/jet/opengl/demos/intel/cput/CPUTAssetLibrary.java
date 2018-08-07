package jet.opengl.demos.intel.cput;

import org.lwjgl.util.vector.Vector4f;

import java.io.IOException;
import java.util.Arrays;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.util.LogUtil;

/**
 * Created by mazhen'gui on 2017/11/14.
 */
public abstract class CPUTAssetLibrary implements Disposeable{
    protected static CPUTAssetLibrary mpAssetLibrary;

    // simple linked lists for now, but if we want to optimize or load blocks
    // we can change these to dynamically re-sizing arrays and then just do
    // memcopies into the structs.
    // Note: No camera, light, or NullNode directory names - they don't have payload files (i.e., they are defined completely in the .set file)
    protected String  mAssetSetDirectoryName;
    protected String  mModelDirectoryName;
    protected String  mMaterialDirectoryName;
    protected String  mTextureDirectoryName;
    protected String  mShaderDirectoryName;
    protected String  mFontDirectoryName;
    protected String  mSystemDirectoryName;

    public static CPUTAssetListEntry  mpAssetSetList;
    public static CPUTAssetListEntry  mpNullNodeList;
    public static CPUTAssetListEntry  mpModelList;
    public static CPUTAssetListEntry  mpCameraList;
    public static CPUTAssetListEntry  mpLightList;
    public static CPUTAssetListEntry  mpMaterialList;
    public static CPUTAssetListEntry  mpTextureList;
    public static CPUTAssetListEntry  mpBufferList;
    public static CPUTAssetListEntry  mpConstantBufferList;
    public static CPUTAssetListEntry  mpRenderStateBlockList;
    public static CPUTAssetListEntry  mpFontList;
    public static CPUTAssetEntry      mpMaterialEffectListEntry = new CPUTAssetEntry();
    public static CPUTAssetEntry      mpMaterialListEntry = new CPUTAssetEntry();

    public static void RebindTexturesAndBuffers(){
        CPUTAssetListEntry pMaterial = mpMaterialList;
        while( pMaterial != null)
        {
            ((CPUTMaterialDX11)pMaterial.pData).RebindTexturesAndBuffers(false);
            pMaterial = pMaterial.pNext;
        }
    }
    public static void ReleaseTexturesAndBuffers(){
        CPUTAssetListEntry pMaterial = mpMaterialList;
        while( pMaterial !=null )
        {
            ((CPUTMaterialDX11)pMaterial.pData).ReleaseTexturesAndBuffers(false);
            pMaterial = pMaterial.pNext;
        }
    }

    public static CPUTAssetLibrary GetAssetLibrary(){ return mpAssetLibrary; }

    static void DeleteAssetLibrary(){
        if(mpAssetLibrary != null)
            mpAssetLibrary.dispose();
        mpAssetLibrary = null;
    }

    static void CreateAssetLibrary(){
        if(mpAssetLibrary == null){
            mpAssetLibrary = new CPUTAssetLibraryDX11();
        }
    }

    public CPUTAssetLibrary() {}

    public void SetSystemDirectoryName(String directoryName ) { mSystemDirectoryName   = directoryName; }
    public String GetSystemDirectoryName()   { return mSystemDirectoryName; }

    // Add/get/delete items to specified library
    public Object FindAsset(String name, CPUTAssetListEntry pList, boolean nameIsFullPathAndFilename/*=false*/){
        /*String absolutePathAndFilename;
        CPUTOSServices *pServices = CPUTOSServices::GetOSServices();
        pServices->ResolveAbsolutePathAndFilename( nameIsFullPathAndFilename ? name : (mAssetSetDirectoryName + name), &absolutePathAndFilename);
        absolutePathAndFilename = nameIsFullPathAndFilename ? name : absolutePathAndFilename;*/
        String absolutePathAndFilename = nameIsFullPathAndFilename ? name : mAssetSetDirectoryName + name;
        absolutePathAndFilename = absolutePathAndFilename.toLowerCase();
//        UINT hash = CPUTComputeHash( absolutePathAndFilename );
        while(null!=pList)
        {
//            if( hash == pList->hash && (0 == _wcsicmp( absolutePathAndFilename.data(), pList->name.data() )) )
            if(absolutePathAndFilename.equals(pList.name))
            {
                return pList.pData;
            }
            pList = pList.pNext;
        }

        LogUtil.w(LogUtil.LogType.DEFAULT, "Couldn't found the asset by name: " + absolutePathAndFilename);
        return null;
    }

    // Find an asset in a specific library
// ** Does not Addref() returned items **
// Asset library doesn't care if we're using absolute paths for names or not, it
// just adds/finds/deletes the matching string literal.
//-----------------------------------------------------------------------------
    Object FindAsset(String name, CPUTAssetListEntry pList, boolean nameIsFullPathAndFilename, Macro[] pShaderMacros, CPUTModel pModel, int meshIndex
    ){
        String absolutePathAndFilename;
        if( !nameIsFullPathAndFilename )
        {
//            CPUTFileSystem::ResolveAbsolutePathAndFilename( mAssetSetDirectoryName + name, &absolutePathAndFilename);
            absolutePathAndFilename = mAssetSetDirectoryName + name;
        }
        else
        {
            absolutePathAndFilename = name;
        }
//        UINT hash = CPUTComputeHash( absolutePathAndFilename );

        while(null!=pList)
        {
            //String szNameLower = pList->name;
            //std::transform(szNameLower.begin(), szNameLower.end(), szNameLower.begin(), tolow);
            //std::transform(absolutePathAndFilename.begin(), absolutePathAndFilename.end(), absolutePathAndFilename.begin(), tolow);

            if(    pModel    == pList.pModel
                    && meshIndex == pList.meshIndex
//                    && hash      == pList->hash
//fixme
/*#ifdef UNICODE
            && (0 == _wcsicmp( absolutePathAndFilename.data(), pList->name.data() ))
#endif
                && ShaderDefinesMatch( (char**)pShaderMacros, pList->pShaderMacros*/
                && pList.name.equalsIgnoreCase(absolutePathAndFilename)
        )
            {
                return pList.pData;
            }
            pList = pList.pNext;
        }
        return null;
    }

    public void ReleaseAllLibraryLists(){
        ReleaseList(mpAssetSetList);
        ReleaseList(mpMaterialList);
        ReleaseList(mpModelList);
        ReleaseList(mpLightList);
        ReleaseList(mpCameraList);
        ReleaseList(mpNullNodeList);
        ReleaseList(mpTextureList );
        ReleaseList(mpBufferList );
        ReleaseList(mpConstantBufferList );
        ReleaseList(mpRenderStateBlockList );
        ReleaseList(mpFontList);

        mpAssetSetList = null;
        mpMaterialList = null;
        mpModelList = null;
        mpLightList = null;
        mpCameraList = null;
        mpNullNodeList = null;
        mpTextureList = null;
        mpBufferList = null;
        mpConstantBufferList = null;
        mpRenderStateBlockList = null;
        mpFontList = null;
    }

    public void SetMediaDirectoryName( String directoryName)
    {
        mAssetSetDirectoryName = directoryName + "Asset\\";
        mModelDirectoryName    = directoryName + "Asset\\";
        mMaterialDirectoryName = directoryName + "Material\\";
        mTextureDirectoryName  = directoryName + "Texture\\";
        mShaderDirectoryName   = directoryName + "Shader\\";
        // mFontStateBlockDirectoryName   = directoryName + _L("Font\\");
    }
    public void SetAssetSetDirectoryName(        String directoryName) { mAssetSetDirectoryName  = directoryName; }
    public void SetModelDirectoryName(           String directoryName) { mModelDirectoryName     = directoryName; }
    public void SetMaterialDirectoryName(        String directoryName) { mMaterialDirectoryName  = directoryName; }
    public void SetTextureDirectoryName(         String directoryName) { mTextureDirectoryName   = directoryName; }
    public void SetShaderDirectoryName(          String directoryName) { mShaderDirectoryName    = directoryName; }
    public void SetFontDirectoryName(            String directoryName) { mFontDirectoryName      = directoryName; }

    public void SetAllAssetDirectoryNames(       String directoryName) {
        mAssetSetDirectoryName       = directoryName;
        mModelDirectoryName          = directoryName;
        mMaterialDirectoryName       = directoryName;
        mTextureDirectoryName        = directoryName;
        mShaderDirectoryName         = directoryName;
        mFontDirectoryName           = directoryName;
    };

    public String GetAssetSetDirectoryName() { return mAssetSetDirectoryName; }
    public String GetModelDirectory()        { return mModelDirectoryName; }
    public String GetMaterialDirectory()     { return mMaterialDirectoryName; }
    public String GetTextureDirectory()      { return mTextureDirectoryName; }
    public String GetShaderDirectory()       { return mShaderDirectoryName; }
    public String GetFontDirectory()         { return mFontDirectoryName; }

    public void AddAssetSet(        String name, CPUTAssetSet         pAssetSet)        { mpAssetSetList = AddAsset( name, pAssetSet,         mpAssetSetList ); }
    public void AddNullNode(        String name, CPUTNullNode         pNullNode)        { mpNullNodeList = AddAsset( name, pNullNode,         mpNullNodeList ); }
    public void AddModel(           String name, CPUTModel            pModel)           { mpModelList =    AddAsset( name, pModel,            mpModelList ); }
    public void AddMaterial(        String name, CPUTMaterial         pMaterial)        { mpMaterialList = AddAsset( name, pMaterial,         mpMaterialList ); }
    public void AddMaterial(        String name, String prefixDecoration, String suffixDecoration, CPUTMaterial pMaterial, Macro[] pShaderMacros, CPUTModel pModel, int meshIndex){ AddAsset( name, prefixDecoration, suffixDecoration, pMaterial, /*&mpMaterialList,       &mpMaterialListTail*/mpMaterialListEntry,       pShaderMacros, pModel, meshIndex ); }
    public void AddMaterialEffect(  String name, String prefixDecoration, String suffixDecoration, CPUTMaterialEffect pMaterial, Macro[] pShaderMacros, CPUTModel pModel, int meshIndex){ AddAsset( name, prefixDecoration, suffixDecoration, pMaterial, /*&mpMaterialEffectList,       &mpMaterialEffectListTail*/ mpMaterialEffectListEntry,       pShaderMacros, pModel, meshIndex ); }
    public void AddLight(           String name, CPUTLight            pLight)           { mpLightList    = AddAsset( name, pLight,            mpLightList ); }
    public void AddCamera(          String name, CPUTCamera           pCamera)          { mpCameraList   = AddAsset( name, pCamera,           mpCameraList ); }
    public void AddTexture(         String name, CPUTTexture          pTexture)         { mpTextureList  = AddAsset( name, pTexture,          mpTextureList ); }
    public void AddBuffer(          String name, CPUTBuffer           pBuffer)          { mpBufferList   = AddAsset( name, pBuffer,           mpBufferList ); }
    public void AddConstantBuffer(  String name, CPUTBuffer           pBuffer)          { mpConstantBufferList = AddAsset( name, pBuffer,           mpConstantBufferList ); }
    public void AddRenderStateBlock(String name, CPUTRenderStateBlock pRenderStateBlock){ mpRenderStateBlockList = AddAsset( name, pRenderStateBlock, mpRenderStateBlockList ); }
//    public void AddFont(            String name, CPUTFont             pFont)            { AddAsset( name, pFont,             &mpFontList ); }

    public CPUTAssetSet FindAssetSet(String name, boolean nameIsFullPathAndFilename/*=false*/)       { return (CPUTAssetSet)FindAsset( name, mpAssetSetList, nameIsFullPathAndFilename ); }
    public CPUTNullNode FindNullNode(String name, boolean nameIsFullPathAndFilename/*=false*/)       { return (CPUTNullNode)FindAsset( name, mpNullNodeList, nameIsFullPathAndFilename ); }
    public CPUTModel    FindModel(String name, boolean nameIsFullPathAndFilename/*=false*/)          { return (CPUTModel)FindAsset(    name, mpModelList,    nameIsFullPathAndFilename ); }
    public CPUTMaterial FindMaterial(String name, boolean nameIsFullPathAndFilename/*=false*/)       { return (CPUTMaterial)FindAsset( name, mpMaterialList, nameIsFullPathAndFilename ); }
    public CPUTMaterialEffect FindMaterialEffect(String name, boolean nameIsFullPathAndFilename, Macro[] pShaderMacros, CPUTModel pModel, int meshIndex) { return (CPUTMaterialEffect)        FindAsset( name, mpMaterialEffectListEntry.pHead,         nameIsFullPathAndFilename, pShaderMacros, pModel, meshIndex ); }
    public CPUTLight    FindLight(String name, boolean nameIsFullPathAndFilename/*=false*/)          { return (CPUTLight)FindAsset(    name, mpLightList,    nameIsFullPathAndFilename ); }
    public CPUTCamera   FindCamera(String name, boolean nameIsFullPathAndFilename/*=false*/)         { return (CPUTCamera)FindAsset(   name, mpCameraList,   nameIsFullPathAndFilename ); }
    public CPUTTexture  FindTexture(String name, boolean nameIsFullPathAndFilename/*=false*/)        { return (CPUTTexture)FindAsset(  name, mpTextureList,  nameIsFullPathAndFilename ); }
    public CPUTBuffer   FindBuffer(String name, boolean nameIsFullPathAndFilename/*=false*/)         { return (CPUTBuffer)FindAsset(   name, mpBufferList,   nameIsFullPathAndFilename ); }
    public CPUTBuffer   FindConstantBuffer(String name, boolean nameIsFullPathAndFilename/*=false*/) { return (CPUTBuffer)FindAsset(   name, mpConstantBufferList, nameIsFullPathAndFilename ); }
    public CPUTRenderStateBlock FindRenderStateBlock(String name, boolean nameIsFullPathAndFilename/*=false*/ ) { return (CPUTRenderStateBlock)FindAsset( name, mpRenderStateBlockList, nameIsFullPathAndFilename ); }
//    CPUTFont     *FindFont(String name, bool nameIsFullPathAndFilename=false)           { return (CPUTFont*)FindAsset(     name, mpFontList,     nameIsFullPathAndFilename ); }

    public CPUTAssetSet  GetAssetSet(String name) throws IOException{
        return GetAssetSet(name, false);
    }
    // If the asset exists, these 'Get' methods will addref and return it.  Otherwise,
    // they will create it and return it.
    public CPUTAssetSet  GetAssetSet(String name, boolean nameIsFullPathAndFilename/*=false*/ ) throws IOException{
        // Resolve the absolute path
        String absolutePathAndFilename;
        /*CPUTOSServices *pServices = CPUTOSServices::GetOSServices();
        pServices->ResolveAbsolutePathAndFilename( nameIsFullPathAndFilename ? name
                : (mAssetSetDirectoryName + name + _L(".set")), &absolutePathAndFilename );*/
        absolutePathAndFilename = nameIsFullPathAndFilename ? name : mAssetSetDirectoryName + name + ".set";
//        absolutePathAndFilename = nameIsFullPathAndFilename ? name : absolutePathAndFilename;

        CPUTAssetSet pAssetSet = FindAssetSet(absolutePathAndFilename, true);
        if(null == pAssetSet)
        {
            return CPUTAssetSet.CreateAssetSet( name, absolutePathAndFilename );
        }
        /*pAssetSet->AddRef();*/
        return pAssetSet;
    }

    public CPUTTexture          GetTexture(         String name, boolean nameIsFullPathAndFilename/*=false*/, boolean loadAsSRGB/*=true*/ ) throws IOException{
        String finalName;
        if( name.charAt(0) == '$' )
        {
            finalName = name;
        } else
        {
            // Resolve name to absolute path
            /*CPUTOSServices *pServices = CPUTOSServices::GetOSServices();
            pServices->ResolveAbsolutePathAndFilename( nameIsFullPathAndFilename? name : (mTextureDirectoryName + name), &finalName);*/
            finalName = nameIsFullPathAndFilename? name : (mTextureDirectoryName + name);
        }
        // If we already have one by this name, then return it
        CPUTTexture pTexture = FindTexture(finalName, true);
        if(null==pTexture)
        {
            return CPUTTexture.CreateTexture( name, finalName, loadAsSRGB);
        }
        /*pTexture->AddRef();*/
        return pTexture;
    }

    // TODO: All of these Get() functions look very similar.
    public CPUTMaterial GetMaterial(
            String     name,
            boolean    nameIsFullPathAndFilename) throws IOException{
        return GetMaterial(name, nameIsFullPathAndFilename, null, 0, null,0,
                null, null, null, null, null,0);
    }
// Keep them all for their interface, but have them call a common function
//-----------------------------------------------------------------------------
    public CPUTMaterial GetMaterial(
            String     name,
            boolean    nameIsFullPathAndFilename,
            CPUTModel  pModel,
            int        meshIndex,
            Macro[]    pShaderMacros, // Note: this is honored only on first load.  Subsequent GetMaterial calls will return the material with shaders as compiled with original macros.
            int        numSystemMaterials,
            String[]   pSystemMaterialNames,  // Note: this is honored only on first load.  Subsequent GetMaterial calls will return the material with shaders as compiled with original macros.
            String     pExternalName,
            Vector4f[] pExternals, // list of values for the externals
            int[]      pExternalOffset, // list of offsets to each of the exernals (e.g., char offset of this external in the cbExternals constant buffer)
            int[]      pExternalSize,
            int        externalCount
    )throws IOException{
        // Resolve name to absolute path before searching
        String absolutePathAndFilename;
        if (name.charAt(0) == '%')
        {
            absolutePathAndFilename = mSystemDirectoryName + "Material/" + name.substring(1) + (".mtl");  // TODO: Instead of having the Material/directory hardcoded here it could be set like the normal material directory. But then there would need to be a bunch new variables like SetSystemMaterialDirectory
//            CPUTFileSystem::ResolveAbsolutePathAndFilename(absolutePathAndFilename, &absolutePathAndFilename); TODO
        } else if( !nameIsFullPathAndFilename )
        {
//            CPUTFileSystem::ResolveAbsolutePathAndFilename( mMaterialDirectoryName + name + _L(".mtl"), &absolutePathAndFilename);  TODO
            absolutePathAndFilename = mMaterialDirectoryName + name + ".mtl";
        } else
        {
            absolutePathAndFilename = name;
        }

        CPUTMaterial pMaterial= null;
        if( pModel != null)
        {
            pMaterial = pModel.GetMaterial( meshIndex );
            if( pMaterial != null)
            {
//                pMaterial->AddRef();
                return pMaterial;
            }
        }

        if(pMaterial == null && pShaderMacros == null && pExternals == null )
        {
            // Loading a non-instanced material (or, the master).
            // The caller supplied macros, so we assume they make the material unique.
            // TODO: Track macros and match if duplicate macros supplied?
            pMaterial = FindMaterial(absolutePathAndFilename, true);
        }

        // If the material has per-model properties, then we need a material clone
        if( pMaterial != null)
        {
//            pMaterial->AddRef();
        }
        else
        {
            pMaterial = CPUTMaterial.CreateMaterial(
                absolutePathAndFilename,
                pModel,
                meshIndex,
                pShaderMacros,
                numSystemMaterials,
                pSystemMaterialNames,
                externalCount,
                pExternalName,
                pExternals,
                pExternalOffset,
                pExternalSize
            );
//            ASSERT( pMaterial, _L("Failed creating material.") );
            if(pMaterial == null)
                throw new NullPointerException();
        }


        // Not looking for an instance, so return what we found
        return pMaterial;
    }

    public CPUTMaterial GetMaterial(String name, boolean nameIsFullPathAndFilename/*=false*/, String modelSuffix/*=_L("")*/,
                                            String meshSuffix/*=_L("")*/ ) throws IOException{
        // Resolve name to absolute path before searching
        /*CPUTOSServices *pServices = CPUTOSServices::GetOSServices();
        cString absolutePathAndFilename;
        pServices->ResolveAbsolutePathAndFilename( nameIsFullPathAndFilename? name : (mMaterialDirectoryName + name + _L(".mtl")), &absolutePathAndFilename);*/
        String absolutePathAndFilename = nameIsFullPathAndFilename ? name : (mMaterialDirectoryName + name + ".mtl");

        // If we already have one by this name, then return it
        CPUTMaterialDX11 pMaterial = (CPUTMaterialDX11) FindMaterial(absolutePathAndFilename, true);
        if(null==pMaterial)
        {
            // We don't already have it in the library, so create it.
            pMaterial = (CPUTMaterialDX11) CPUTMaterial.CreateMaterial( absolutePathAndFilename, modelSuffix, meshSuffix );
            return pMaterial;
        }
        else if( (0==modelSuffix.length()) && !pMaterial.MaterialRequiresPerModelPayload() )
        {
            // This material doesn't have per-model elements, so we don't need to clone it.
            /*pMaterial->AddRef();*/
            return pMaterial;
        }

        // We need to clone the material.  Do that by loading it again, but with a different name.
        // Add the model's suffix (address as string, plus model's material array index as string)
        CPUTMaterial pUniqueMaterial = FindMaterial(absolutePathAndFilename + modelSuffix + meshSuffix, true);
        /*ASSERT( NULL == pUniqueMaterial, _L("Unique material already not unique: ") + absolutePathAndFilename + modelSuffix + meshSuffix );*/
        if(pUniqueMaterial != null){
            throw new RuntimeException(("Unique material already not unique: ") + absolutePathAndFilename + modelSuffix + meshSuffix);
        }

        return pMaterial.CloneMaterial( absolutePathAndFilename, modelSuffix, meshSuffix );
    }

    // TODO: All of these Get() functions look very similar.
// Keep them all for their interface, but have them call a common function
//-----------------------------------------------------------------------------
    public CPUTMaterialEffect GetMaterialEffect(String name, boolean nameIsFullPathAndFilename, CPUTModel pModel, int meshIndex,
            Macro[] pShaderMacros // Note: this is honored only on first load.  Subsequent GetMaterial calls will return the material with shaders as compiled with original macros.
    ){
        // Resolve name to absolute path before searching
        String absolutePathAndFilename;
        if (name.charAt(0) == '%')
        {
            absolutePathAndFilename = mSystemDirectoryName + "Material/" + name.substring(1) + ".mtl";  // TODO: Instead of having the Material/directory hardcoded here it could be set like the normal material directory. But then there would need to be a bunch new variables like SetSystemMaterialDirectory
//            CPUTFileSystem::ResolveAbsolutePathAndFilename(absolutePathAndFilename, &absolutePathAndFilename);
        } else if( !nameIsFullPathAndFilename )
        {
//            CPUTFileSystem::ResolveAbsolutePathAndFilename( mMaterialDirectoryName + name + _L(".mtl"), &absolutePathAndFilename);
            absolutePathAndFilename = mMaterialDirectoryName + name + (".mtl");
        } else
        {
            absolutePathAndFilename = name;
        }

        CPUTMaterialEffect pMaterial=null;

        if( pMaterial == null && pShaderMacros == null )
        {
            // Loading a non-instanced material (or, the master)
            pMaterial = FindMaterialEffect(absolutePathAndFilename, true);
        }

        // If the material has per-model properties, then we need a material clone
        if( pMaterial )
        {
            pMaterial->AddRef();
        }
        else
        {
            pMaterial = CPUTMaterialEffect::CreateMaterialEffect( absolutePathAndFilename, pModel, meshIndex, pShaderMacros );
            ASSERT( pMaterial, _L("Failed creating material Effect.") );
        }
        return pMaterial;
    }

    public CPUTModel            GetModel(           String name, boolean nameIsFullPathAndFilename/*=false*/  ) throws IOException{
        // Resolve name to absolute path before searching
        String absolutePathAndFilename;
        /*CPUTOSServices *pServices = CPUTOSServices::GetOSServices();
        pServices->ResolveAbsolutePathAndFilename( nameIsFullPathAndFilename? name : (mModelDirectoryName + name + _L(".mdl")), &absolutePathAndFilename);*/
        absolutePathAndFilename = nameIsFullPathAndFilename ? name : (mModelDirectoryName + name + ".mdl");

        // If we already have one by this name, then return it
        CPUTModel pModel = FindModel(absolutePathAndFilename, true);
        if(null!=pModel)
        {
            /*pModel->AddRef();*/
            return pModel;
        }

        // Looks like no one calls GetModel().  Or, they never call it for missing models.
/*#if TODO // delegate
                // Model was not in the library, so create and load a new model
                pModel = new CPUTModel();
        pModel->LoadModelPayload(absolutePathAndFilename);
        AddModel(name, pModel);

        return CPUTModel::CreateMode( absolutePathAndFilename, aboslutePathAndFilename );
#endif*/

        return pModel;
    }

    public CPUTRenderStateBlock GetRenderStateBlock(String name, boolean nameIsFullPathAndFilename/*=false*/){
        // Resolve name to absolute path before searching
        String finalName;
        if( name.charAt(0) == '$' )
        {
            finalName = name;
        } else
        {
            /*// Resolve name to absolute path
            CPUTOSServices *pServices = CPUTOSServices::GetOSServices();
            pServices->ResolveAbsolutePathAndFilename( nameIsFullPathAndFilename? name : (mShaderDirectoryName + name), &finalName);*/
            finalName = nameIsFullPathAndFilename ? name : (mShaderDirectoryName + name);
        }

        // see if the render state block is already in the library
        CPUTRenderStateBlock pRenderStateBlock = FindRenderStateBlock(finalName, true);
        if(null==pRenderStateBlock)
        {
            return CPUTRenderStateBlock.CreateRenderStateBlock( name, finalName );
        }

        /*pRenderStateBlock->AddRef();*/
        return pRenderStateBlock;
    }
    public CPUTBuffer           GetBuffer(          String name ){
        // If we already have one by this name, then return it
        CPUTBuffer pBuffer = FindBuffer(name, true);
        /*ASSERT( pBuffer, _L("Can't find buffer ") + name );
        pBuffer->AddRef();*/
        if(pBuffer == null){
            LogUtil.e(LogUtil.LogType.DEFAULT, "Can't find buffer " + name);
        }

        return pBuffer;
    }
    public CPUTBuffer           GetConstantBuffer(  String name ){
        // If we already have one by this name, then return it
        CPUTBuffer pBuffer = FindConstantBuffer(name, true);
        /*ASSERT( pBuffer, _L("Can't find constant buffer ") + name );
        pBuffer->AddRef();*/
        if(pBuffer == null){
            LogUtil.e(LogUtil.LogType.DEFAULT, "Can't find constant buffer " + name);
        }
        return pBuffer;
    }

    public CPUTCamera           GetCamera(          String name ){
        // TODO: Should we prefix camera names with a path anyway?  To keek them unique?
        // If we already have one by this name, then return it
        CPUTCamera pCamera = FindCamera(name, true);
        if(null!=pCamera)
        {
            /*pCamera->AddRef();*/
            return pCamera;
        }
        return null;
    }

    public CPUTLight            GetLight(           String name ){
        // If we already have one by this name, then return it
        CPUTLight pLight = FindLight(name, true);
        if(null!=pLight)
        {
//            pLight->AddRef();
            return pLight;
        }
        return null;
    }
//    CPUTFont             *GetFont(            String name );

    // helper functions
    protected void ReleaseList(CPUTAssetListEntry pLibraryRoot){
        CPUTAssetListEntry pNext;
        for( CPUTAssetListEntry pNodeEntry = pLibraryRoot; null != pNodeEntry; pNodeEntry = pNext )
        {
            pNext = pNodeEntry.pNext;
            /*CPUTRefCount *pRefCountedNode = (CPUTRefCount*)pNodeEntry->pData;
            pRefCountedNode->Release();
            HEAPCHECK;  TODO
            delete pNodeEntry;*/
            if(pNodeEntry.pData instanceof Disposeable){
                ((Disposeable)pNodeEntry.pData).dispose();
            }
        }
    }

    private final static class CPUTAssetEntry{
        CPUTAssetListEntry pHead;
        CPUTAssetListEntry pTail;
    }

    protected CPUTAssetListEntry AddAsset( String name, Object pAsset,  CPUTAssetListEntry pHead ){
        // convert string to lowercase
        /*cString lowercaseName = name;
        std::transform(lowercaseName.begin(), lowercaseName.end(), lowercaseName.begin(), ::tolower);*/
        name = name.toLowerCase();

        // Do we already have one by this name?
        CPUTAssetListEntry pTail = pHead;
        // TODO:  Save explicit tail pointer instead of iterating to find the null.
        for( CPUTAssetListEntry pCur=pHead; null!=pCur; pCur=pCur.pNext )
        {
            // Assert that we haven't added one with this name
//            ASSERT( 0 != _wcsicmp( pCur->name.data(), name.data() ), _L("Warning: asset ")+name+_L(" already exists") );
            if(pCur.name.equals(name)){
                LogUtil.e(LogUtil.LogType.DEFAULT, "Warning: asset " + name + " already exists");
            }

            pTail = pCur;
        }

        /*CPUTAssetListEntry **pDest = pTail ? &pTail->pNext : pHead;*/
        CPUTAssetListEntry pDest = new CPUTAssetListEntry();
        pDest.name = name;
        pDest.pData = pAsset;
        pDest.pNext = null;

        // TODO: Our assets are not yet all derived from CPUTRenderNode.
        // TODO: For now, rely on caller performing the AddRef() as it knows the assets type.
        /*((CPUTRefCount*)pAsset)->AddRef();*/
        if(pHead == null){
            return pDest;
        }else{
            pTail.pNext = pDest;
            return pHead;
        }
    }

    void AddAsset(String name, String prefixDecoration, String suffixDecoration, Object pAsset, /*CPUTAssetListEntry pHead,
                CPUTAssetListEntry pTail*/ CPUTAssetEntry entry, Macro[] pShaderMacros, CPUTModel pModel, int meshIndex
    ){
        // convert string to lowercase
        String lowercaseName = name.toLowerCase();
//        std::transform(lowercaseName.begin(), lowercaseName.end(), lowercaseName.begin(), tolow);

        // Do we already have one by this name?
        for( CPUTAssetListEntry pCur=entry.pHead; null!=pCur; pCur=pCur.pNext )
        {
//            if ( ((0 == _wcsicmp( pCur->name.data(), name.data() )) && (pCur->pModel == pModel) && (pCur->meshIndex == meshIndex) ) ) {
            if(pCur.name.equals(name) && pCur.pModel == pModel && pCur.meshIndex == meshIndex){
                throw new IllegalArgumentException("WARNING: Adding asset with duplicate name: " + name);
            }
        }
        String fileName;
//FIXME string
/*#ifdef UNICODE
#define FORWARD_SLASH L"/"
#define BACK_SLASH L"\\"
#else
#define FORWARD_SLASH "/"
#define BACK_SLASH "\\"
#endif*/
        final char FORWARD_SLASH = '/';
        final char BACK_SLASH = '\\';
        // find final part of filename to use as short name when when don't care about duplicates
        // move this to reusable location
        int index = lowercaseName.indexOf(FORWARD_SLASH);
        if  (index == /*cString::npos*/-1) {
            index = lowercaseName.indexOf(BACK_SLASH);
        }
        if  (index != /*cString::npos*/ -1) {
            fileName = lowercaseName.substring(index + 1); // add one to skip the forward or backward slash
        }
        else {
            fileName = name;
        }

        CPUTAssetListEntry value = new CPUTAssetListEntry(); // TODO: init via constructor

//        (*pTail)->hash       = CPUTComputeHash(prefixDecoration + name + suffixDecoration);
        value.name       = prefixDecoration + name + suffixDecoration;
        value.pData      = pAsset;
        value.pModel     = pModel;
        value.meshIndex  = meshIndex;
        value.pNext      = null;
        value.fileName   = fileName;

        // Copy the shader macros.  If two assets (shaders only?) have different macros, then they're different assets.
        // Fix me: add support for shader macros

        /*char **p1=(char**)pShaderMacros;
        int count = 0;
        if( p1 )
        {
            // Count the macros
            while( *p1 )
            {
                ++count;
                ++p1;
            }
            (*pTail)->pShaderMacros = new char *[count+2]; // Add 2 for NULL terminator (1 for macro's name and 1 for its value)
            p1 = (char**)pShaderMacros;
            char **p2 = (*pTail)->pShaderMacros;
            while( *p1 && *p2 )
            {
                size_t len = strlen(*p1);
            *p2 = new char[len+1]; // +1 for NULL terminator
#ifdef CPUT_OS_WINDOWS
                strncpy_s( *p2, len+1, *p1, len+1 );
#else
                strcpy(*p2, *p1);
#endif
                    ++p1;
                ++p2;
            }
            // Set the macro's name and value to NULL
        *(p2++) = NULL;
        *p2 = NULL;
        } else
        {
            (*pTail)->pShaderMacros = NULL;
        }*/

        if(pShaderMacros != null){
            entry.pTail.pShaderMacros = Arrays.copyOf(pShaderMacros, pShaderMacros.length);
        }else{
            entry.pTail.pShaderMacros = null;
        }

//        pTail = &(*pTail)->pNext;

        if(entry.pHead == null){
            entry.pHead = entry.pTail = value;
        }else if(entry.pHead == entry.pTail){
            entry.pTail = value;
            entry.pHead.pNext = entry.pTail;
        }else{
            entry.pTail.pNext = value;
            entry.pTail = value;
        }

        // TODO: Our assets are not yet all derived from CPUTRenderNode.
        // TODO: For now, rely on caller performing the AddRef() as it knows the assets type.
//        ((CPUTRefCount*)pAsset)->AddRef();
    }

    @Override
    public void dispose() {

    }

    public String GetMaterialEffectPath(String   name,boolean       nameIsFullPathAndFilename)
    {
        // Resolve name to absolute path before searching
        /*String absolutePathAndFilename;
        if (name.charAt(0) == '%')
        {
            absolutePathAndFilename = mSystemDirectoryName + "Material/" + name.substring(1) + (".mtl");  // TODO: Instead of having the Material/directory hardcoded here it could be set like the normal material directory. But then there would need to be a bunch new variables like SetSystemMaterialDirectory
            CPUTFileSystem::ResolveAbsolutePathAndFilename(absolutePathAndFilename, &absolutePathAndFilename);
        } else if( !nameIsFullPathAndFilename )
        {
            CPUTFileSystem::ResolveAbsolutePathAndFilename( mMaterialDirectoryName + name + _L(".mtl"), &absolutePathAndFilename);
        } else
        {
            absolutePathAndFilename = name;
        }*/

        String absolutePathAndFilename;
        if (name.charAt(0) == '%')
        {
            absolutePathAndFilename = mSystemDirectoryName + "Material/" + name.substring(1) + (".mtl");  // TODO: Instead of having the Material/directory hardcoded here it could be set like the normal material directory. But then there would need to be a bunch new variables like SetSystemMaterialDirectory
//            CPUTFileSystem::ResolveAbsolutePathAndFilename(absolutePathAndFilename, &absolutePathAndFilename); TODO
        } else if( !nameIsFullPathAndFilename )
        {
//            CPUTFileSystem::ResolveAbsolutePathAndFilename( mMaterialDirectoryName + name + _L(".mtl"), &absolutePathAndFilename);  TODO
            absolutePathAndFilename = mMaterialDirectoryName + name + ".mtl";
        } else
        {
            absolutePathAndFilename = name;
        }

        return absolutePathAndFilename;
    }
}
