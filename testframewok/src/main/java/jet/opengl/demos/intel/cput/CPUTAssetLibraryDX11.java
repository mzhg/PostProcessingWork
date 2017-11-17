package jet.opengl.demos.intel.cput;

import java.io.IOException;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.ShaderProgram;
import jet.opengl.postprocessing.shader.ShaderType;

/**
 * Created by mazhen'gui on 2017/11/15.
 */

public final class CPUTAssetLibraryDX11 extends CPUTAssetLibrary{
    private static CPUTAssetListEntry  mpPixelShaderList;
    private static CPUTAssetListEntry  mpComputeShaderList;
    private static CPUTAssetListEntry  mpVertexShaderList;
    private static CPUTAssetListEntry  mpGeometryShaderList;
    private static CPUTAssetListEntry  mpHullShaderList;
    private static CPUTAssetListEntry  mpDomainShaderList;

    @Override
    public void dispose() {
        ReleaseAllLibraryLists();
    }

    public void ReleaseAllLibraryLists(){
        // TODO: we really need to wrap the DX assets so we don't need to distinguish their IUnknown type.
        ReleaseList(mpPixelShaderList);
        ReleaseList(mpComputeShaderList);
        ReleaseList(mpVertexShaderList);
        ReleaseList(mpGeometryShaderList);
        ReleaseList(mpHullShaderList);
        ReleaseList(mpDomainShaderList);

        mpPixelShaderList = null;
        mpComputeShaderList = null;
        mpVertexShaderList = null;
        mpGeometryShaderList = null;
        mpHullShaderList = null;
        mpDomainShaderList = null;
        // Call base class implementation to clean up the non-DX object lists
        super.ReleaseAllLibraryLists();
    }
    void ReleaseIunknownList( CPUTAssetListEntry pList ){
        CPUTAssetListEntry pNode = pList;
        CPUTAssetListEntry pOldNode = null;

        while( null!=pNode )
        {
            // release the object using the DirectX IUnknown interface
            if(pNode.pData != null && pNode.pData instanceof Disposeable){
                ((Disposeable)(pNode.pData)).dispose();
            }

            pOldNode = pNode;
            pNode = pNode.pNext;
//            delete pOldNode;
        }
    }

    private static ShaderProgram check(ShaderProgram program, ShaderType type){
        if(program.getTarget() != type.shader){
            throw new IllegalArgumentException("The program is not the type of " + type);
        }

        return program;
    }

    void AddPixelShader(String name, ShaderProgram    pShader) { check(pShader, ShaderType.FRAGMENT); AddAsset( name, pShader, mpPixelShaderList ); }
    void AddComputeShader(  String name, ShaderProgram  pShader) { check(pShader, ShaderType.COMPUTE);AddAsset( name, pShader, mpComputeShaderList ); }
    void AddVertexShader(   String name, ShaderProgram   pShader) { check(pShader, ShaderType.VERTEX); AddAsset( name, pShader,mpVertexShaderList ); }
    void AddGeometryShader( String name, ShaderProgram pShader) { check(pShader, ShaderType.GEOMETRY); AddAsset( name, pShader, mpGeometryShaderList ); }
    void AddHullShader(     String name, ShaderProgram     pShader) {check(pShader, ShaderType.TESS_CONTROL); AddAsset( name, pShader, mpHullShaderList ); }
    void AddDomainShader(   String name, ShaderProgram   pShader) { check(pShader, ShaderType.TESS_EVAL);AddAsset( name, pShader, mpDomainShaderList ); }

    ShaderProgram    FindPixelShader(    String name, boolean nameIsFullPathAndFilename/*=false*/ ) { return    (ShaderProgram) FindAsset( name, mpPixelShaderList,    nameIsFullPathAndFilename ); }
    ShaderProgram  FindComputeShader(  String name, boolean nameIsFullPathAndFilename/*=false*/ ) { return  (ShaderProgram) FindAsset( name, mpComputeShaderList,  nameIsFullPathAndFilename ); }
    ShaderProgram   FindVertexShader(   String name, boolean nameIsFullPathAndFilename/*=false*/ ) { return   (ShaderProgram) FindAsset( name, mpVertexShaderList,   nameIsFullPathAndFilename ); }
    ShaderProgram FindGeometryShader( String name, boolean nameIsFullPathAndFilename/*=false*/ ) { return (ShaderProgram) FindAsset( name, mpGeometryShaderList, nameIsFullPathAndFilename ); }
    ShaderProgram     FindHullShader(     String name, boolean nameIsFullPathAndFilename/*=false*/ ) { return     (ShaderProgram) FindAsset( name, mpHullShaderList,     nameIsFullPathAndFilename ); }
    ShaderProgram   FindDomainShader(   String name, boolean nameIsFullPathAndFilename/*=false*/ ) { return   (ShaderProgram) FindAsset( name, mpDomainShaderList,   nameIsFullPathAndFilename ); }

    // shaders - vertex, pixel
    ShaderProgram GetPixelShader(     String name, /*ID3D11Device *pD3dDevice,*/ String shaderMain, String shaderProfile, /*CPUTPixelShaderDX11    **ppShader,*/
                                      boolean nameIsFullPathAndFilename/*=false*/) throws IOException{
//        CPUTResult result = CPUT_SUCCESS;
        String finalName;
        if( name.charAt(0) == '$' )
        {
            finalName = name;
        } else
        {
            // Resolve name to absolute path
            /*CPUTOSServices *pServices = CPUTOSServices::GetOSServices();
            pServices->ResolveAbsolutePathAndFilename( nameIsFullPathAndFilename? name : (mShaderDirectoryName + name), &finalName);*/
            finalName = nameIsFullPathAndFilename? name : (mShaderDirectoryName + name);
        }

        // see if the shader is already in the library
        ShaderProgram pShader = FindPixelShader(finalName + shaderMain + shaderProfile, true);
        if(null!=pShader)
        {
            /**ppPixelShader = (CPUTPixelShaderDX11*) pShader;
            (*ppPixelShader)->AddRef();
            return result;*/
            return pShader;
        }
        /**ppPixelShader = CPUTPixelShaderDX11::CreatePixelShader( finalName, pD3dDevice, shaderMain, shaderProfile );
        return result;*/
        ShaderProgram shader =  GLSLProgram.createShaderProgramFromFile(finalName, ShaderType.FRAGMENT);
        AddPixelShader(finalName + shaderMain + shaderProfile, shader);
        return shader;
    }

    ShaderProgram GetComputeShader(   String name, /*ID3D11Device *pD3dDevice,*/ String shaderMain, String shaderProfile, /*CPUTComputeShaderDX11  **ppShader,*/
                                      boolean nameIsFullPathAndFilename/*=false*/)throws IOException{
        String finalName;
        if( name.charAt(0) == '$' )
        {
            finalName = name;
        } else
        {
            // Resolve name to absolute path
            /*CPUTOSServices *pServices = CPUTOSServices::GetOSServices();
            pServices->ResolveAbsolutePathAndFilename( nameIsFullPathAndFilename? name : (mShaderDirectoryName + name), &finalName);*/
            finalName = nameIsFullPathAndFilename? name : (mShaderDirectoryName + name);
        }

        // see if the shader is already in the library
        ShaderProgram pShader = FindComputeShader(finalName + shaderMain + shaderProfile, true);
        if(null!=pShader)
        {
            /**ppPixelShader = (CPUTPixelShaderDX11*) pShader;
             (*ppPixelShader)->AddRef();
             return result;*/
            return pShader;
        }
        /**ppPixelShader = CPUTPixelShaderDX11::CreatePixelShader( finalName, pD3dDevice, shaderMain, shaderProfile );
         return result;*/
        ShaderProgram shader =  GLSLProgram.createShaderProgramFromFile(finalName, ShaderType.COMPUTE);
        AddComputeShader(finalName + shaderMain + shaderProfile, shader);
        return shader;
    }

    ShaderProgram GetVertexShader(    String name, /*ID3D11Device *pD3dDevice,*/ String shaderMain, String shaderProfile, /*CPUTVertexShaderDX11   **ppShader,*/
                                      boolean nameIsFullPathAndFilename/*=false*/) throws IOException{
        String finalName;
        if( name.charAt(0) == '$' )
        {
            finalName = name;
        } else
        {
            // Resolve name to absolute path
            /*CPUTOSServices *pServices = CPUTOSServices::GetOSServices();
            pServices->ResolveAbsolutePathAndFilename( nameIsFullPathAndFilename? name : (mShaderDirectoryName + name), &finalName);*/
            finalName = nameIsFullPathAndFilename? name : (mShaderDirectoryName + name);
        }

        // see if the shader is already in the library
        ShaderProgram pShader = FindVertexShader(finalName + shaderMain + shaderProfile, true);
        if(null!=pShader)
        {
            /**ppPixelShader = (CPUTPixelShaderDX11*) pShader;
             (*ppPixelShader)->AddRef();
             return result;*/
            return pShader;
        }
        /**ppPixelShader = CPUTPixelShaderDX11::CreatePixelShader( finalName, pD3dDevice, shaderMain, shaderProfile );
         return result;*/
        ShaderProgram shader =  GLSLProgram.createShaderProgramFromFile(finalName, ShaderType.VERTEX);
        AddVertexShader(finalName + shaderMain + shaderProfile, shader);
        return shader;
    }

    ShaderProgram GetGeometryShader(  String name, /*ID3D11Device *pD3dDevice,*/ String shaderMain, String shaderProfile, /*CPUTGeometryShaderDX11 **ppShader,*/
                                      boolean nameIsFullPathAndFilename/*=false*/)throws IOException{
        String finalName;
        if( name.charAt(0) == '$' )
        {
            finalName = name;
        } else
        {
            // Resolve name to absolute path
            /*CPUTOSServices *pServices = CPUTOSServices::GetOSServices();
            pServices->ResolveAbsolutePathAndFilename( nameIsFullPathAndFilename? name : (mShaderDirectoryName + name), &finalName);*/
            finalName = nameIsFullPathAndFilename? name : (mShaderDirectoryName + name);
        }

        // see if the shader is already in the library
        ShaderProgram pShader = FindGeometryShader(finalName + shaderMain + shaderProfile, true);
        if(null!=pShader)
        {
            /**ppPixelShader = (CPUTPixelShaderDX11*) pShader;
             (*ppPixelShader)->AddRef();
             return result;*/
            return pShader;
        }
        /**ppPixelShader = CPUTPixelShaderDX11::CreatePixelShader( finalName, pD3dDevice, shaderMain, shaderProfile );
         return result;*/
        ShaderProgram shader =  GLSLProgram.createShaderProgramFromFile(finalName, ShaderType.GEOMETRY);
        AddGeometryShader(finalName + shaderMain + shaderProfile, shader);
        return shader;
    }

    ShaderProgram GetHullShader(      String name, /*ID3D11Device *pD3dDevice,*/ String shaderMain, String shaderProfile, /*CPUTHullShaderDX11     **ppShader,*/
                                      boolean nameIsFullPathAndFilename/*=false*/) throws IOException{
        String finalName;
        if( name.charAt(0) == '$' )
        {
            finalName = name;
        } else
        {
            // Resolve name to absolute path
            /*CPUTOSServices *pServices = CPUTOSServices::GetOSServices();
            pServices->ResolveAbsolutePathAndFilename( nameIsFullPathAndFilename? name : (mShaderDirectoryName + name), &finalName);*/
            finalName = nameIsFullPathAndFilename? name : (mShaderDirectoryName + name);
        }

        // see if the shader is already in the library
        ShaderProgram pShader = FindHullShader(finalName + shaderMain + shaderProfile, true);
        if(null!=pShader)
        {
            /**ppPixelShader = (CPUTPixelShaderDX11*) pShader;
             (*ppPixelShader)->AddRef();
             return result;*/
            return pShader;
        }
        /**ppPixelShader = CPUTPixelShaderDX11::CreatePixelShader( finalName, pD3dDevice, shaderMain, shaderProfile );
         return result;*/
        ShaderProgram shader =  GLSLProgram.createShaderProgramFromFile(finalName, ShaderType.TESS_CONTROL);
        AddHullShader(finalName + shaderMain + shaderProfile, shader);
        return shader;
    }

    ShaderProgram GetDomainShader(    String name, /*ID3D11Device *pD3dDevice,*/ String shaderMain, String shaderProfile, /*CPUTDomainShaderDX11   **ppShader,*/
                                      boolean nameIsFullPathAndFilename/*=false*/) throws IOException{
        String finalName;
        if( name.charAt(0) == '$' )
        {
            finalName = name;
        } else
        {
            // Resolve name to absolute path
            /*CPUTOSServices *pServices = CPUTOSServices::GetOSServices();
            pServices->ResolveAbsolutePathAndFilename( nameIsFullPathAndFilename? name : (mShaderDirectoryName + name), &finalName);*/
            finalName = nameIsFullPathAndFilename? name : (mShaderDirectoryName + name);
        }

        // see if the shader is already in the library
        ShaderProgram pShader = FindDomainShader(finalName + shaderMain + shaderProfile, true);
        if(null!=pShader)
        {
            /**ppPixelShader = (CPUTPixelShaderDX11*) pShader;
             (*ppPixelShader)->AddRef();
             return result;*/
            return pShader;
        }
        /**ppPixelShader = CPUTPixelShaderDX11::CreatePixelShader( finalName, pD3dDevice, shaderMain, shaderProfile );
         return result;*/
        ShaderProgram shader =  GLSLProgram.createShaderProgramFromFile(finalName, ShaderType.TESS_EVAL);
        AddDomainShader(finalName + shaderMain + shaderProfile, shader);
        return shader;
    }

    // shaders - vertex, pixel
    ShaderProgram CreatePixelShaderFromMemory(     String name, /*ID3D11Device *pD3dDevice,*/ String shaderMain, String shaderProfile, /*CPUTPixelShaderDX11    **ppShader,*/ CharSequence pShaderSource ){
        ShaderProgram pShader = FindPixelShader(name + shaderMain + shaderProfile, true);
//        ASSERT( NULL == pShader, _L("Shader already exists.") );
        if(pShader != null){
            throw new IllegalArgumentException("Shader already exists.");
        }

        /**ppShader = CPUTPixelShaderDX11::CreatePixelShaderFromMemory( name, pD3dDevice, shaderMain, shaderProfile, pShaderSource);*/
        ShaderProgram ppShader = GLSLProgram.createShaderProgramFromString(pShaderSource, ShaderType.FRAGMENT);
        AddPixelShader(name + shaderMain + shaderProfile, ppShader);
        return ppShader;
    }

    ShaderProgram CreateComputeShaderFromMemory(   String name, /*ID3D11Device *pD3dDevice,*/ String shaderMain, String shaderProfile, /*CPUTComputeShaderDX11  **ppShader,*/ CharSequence pShaderSource ){
        ShaderProgram pShader = FindComputeShader(name + shaderMain + shaderProfile, true);
//        ASSERT( NULL == pShader, _L("Shader already exists.") );
        if(pShader != null){
            throw new IllegalArgumentException("Shader already exists.");
        }

        /**ppShader = CPUTPixelShaderDX11::CreatePixelShaderFromMemory( name, pD3dDevice, shaderMain, shaderProfile, pShaderSource);*/
        ShaderProgram ppShader = GLSLProgram.createShaderProgramFromString(pShaderSource, ShaderType.COMPUTE);
        AddComputeShader(name + shaderMain + shaderProfile, ppShader);
        return ppShader;
    }

    ShaderProgram CreateVertexShaderFromMemory(    String name, /*ID3D11Device *pD3dDevice,*/ String shaderMain, String shaderProfile, /*CPUTVertexShaderDX11   **ppShader,*/ CharSequence pShaderSource ){
        ShaderProgram pShader = FindVertexShader(name + shaderMain + shaderProfile, true);
//        ASSERT( NULL == pShader, _L("Shader already exists.") );
        if(pShader != null){
            throw new IllegalArgumentException("Shader already exists.");
        }

        /**ppShader = CPUTPixelShaderDX11::CreatePixelShaderFromMemory( name, pD3dDevice, shaderMain, shaderProfile, pShaderSource);*/
        ShaderProgram ppShader = GLSLProgram.createShaderProgramFromString(pShaderSource, ShaderType.VERTEX);
        AddVertexShader(name + shaderMain + shaderProfile, ppShader);
        return ppShader;
    }

    ShaderProgram CreateGeometryShaderFromMemory(  String name, /*ID3D11Device *pD3dDevice,*/ String shaderMain, String shaderProfile, /*CPUTGeometryShaderDX11 **ppShader,*/ CharSequence pShaderSource ){
        ShaderProgram pShader = FindGeometryShader(name + shaderMain + shaderProfile, true);
//        ASSERT( NULL == pShader, _L("Shader already exists.") );
        if(pShader != null){
            throw new IllegalArgumentException("Shader already exists.");
        }

        /**ppShader = CPUTPixelShaderDX11::CreatePixelShaderFromMemory( name, pD3dDevice, shaderMain, shaderProfile, pShaderSource);*/
        ShaderProgram ppShader = GLSLProgram.createShaderProgramFromString(pShaderSource, ShaderType.COMPUTE);
        AddGeometryShader(name + shaderMain + shaderProfile, ppShader);
        return ppShader;
    }
    ShaderProgram CreateHullShaderFromMemory(      String name, /*ID3D11Device *pD3dDevice,*/ String shaderMain, String shaderProfile, /*CPUTHullShaderDX11     **ppShader,*/ CharSequence pShaderSource ){
        ShaderProgram pShader = FindHullShader(name + shaderMain + shaderProfile, true);
//        ASSERT( NULL == pShader, _L("Shader already exists.") );
        if(pShader != null){
            throw new IllegalArgumentException("Shader already exists.");
        }

        /**ppShader = CPUTPixelShaderDX11::CreatePixelShaderFromMemory( name, pD3dDevice, shaderMain, shaderProfile, pShaderSource);*/
        ShaderProgram ppShader = GLSLProgram.createShaderProgramFromString(pShaderSource, ShaderType.TESS_CONTROL);
        AddHullShader(name + shaderMain + shaderProfile, ppShader);
        return ppShader;
    }
    ShaderProgram CreateDomainShaderFromMemory(    String name, /*ID3D11Device *pD3dDevice,*/ String shaderMain, String shaderProfile, /*CPUTDomainShaderDX11   **ppShader,*/ CharSequence pShaderSource ){
        ShaderProgram pShader = FindDomainShader(name + shaderMain + shaderProfile, true);
//        ASSERT( NULL == pShader, _L("Shader already exists.") );
        if(pShader != null){
            throw new IllegalArgumentException("Shader already exists.");
        }

        /**ppShader = CPUTPixelShaderDX11::CreatePixelShaderFromMemory( name, pD3dDevice, shaderMain, shaderProfile, pShaderSource);*/
        ShaderProgram ppShader = GLSLProgram.createShaderProgramFromString(pShaderSource, ShaderType.TESS_EVAL);
        AddDomainShader(name + shaderMain + shaderProfile, ppShader);
        return ppShader;
    }

    public ShaderProgram CreateShaderFromMemory(String      name,
//                                                ID3D11Device       *pD3dDevice,
                                                String      shaderMain,
                                                String      shaderProfile,
                                                CharSequence pShaderSource,
                                                ShaderType type){
        String key = name + shaderMain + shaderProfile;
        ShaderProgram ppShader = GLSLProgram.createShaderProgramFromString(pShaderSource, type);
        switch (type){
            case VERTEX:  AddVertexShader(key, ppShader); break;
            case COMPUTE: AddComputeShader(key, ppShader); break;
            case FRAGMENT:AddPixelShader(key, ppShader); break;
            case GEOMETRY:AddGeometryShader(key, ppShader); break;
            case TESS_CONTROL:AddHullShader(key, ppShader); break;
            case TESS_EVAL:AddDomainShader(key, ppShader); break;
            default:
                throw new Error("Inner Error");
        }
        return ppShader;
    }

    /*ShaderProgram CompileShaderFromFile(  String fileName,   String shaderMain, String shaderProfile*//*, ID3DBlob **ppBlob*//*);
    ShaderProgram CompileShaderFromMemory(CharSequence pShaderSource, String shaderMain, String shaderProfile*//*, ID3DBlob **ppBlob*//*);*/
}
