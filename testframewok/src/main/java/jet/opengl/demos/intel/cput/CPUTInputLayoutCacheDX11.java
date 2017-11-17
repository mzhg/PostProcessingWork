package jet.opengl.demos.intel.cput;

import java.util.HashMap;
import java.util.Map;

import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.StringUtils;

/**
 * Created by mazhen'gui on 2017/11/15.
 */
final class CPUTInputLayoutCacheDX11 {
    static CPUTInputLayoutCacheDX11 GetInputLayoutCache(){
        if(null == mpInputLayoutCache)
        {
            mpInputLayoutCache = new CPUTInputLayoutCacheDX11();
        }
        return mpInputLayoutCache;
    }
    static void DeleteInputLayoutCache(){
        /*if(mpInputLayoutCache)
        {
            delete mpInputLayoutCache;
            mpInputLayoutCache = NULL;
        }*/
        mpInputLayoutCache = null;
    }

    ID3D11InputLayout GetLayout(/*ID3D11Device *pDevice,*/ D3D11_INPUT_ELEMENT_DESC[] pDXLayout, /*CPUTVertexShaderDX11*/int program){
        // Generate the vertex layout pattern portion of the key
        String layoutKey = GenerateLayoutKey(pDXLayout);

        // Append the vertex shader pointer to the key for layout<->vertex shader relationship
        String address = /*ptoc(pVertexShader)*/ String.valueOf(program);
        layoutKey += address;

        // Do we already have one like this?
        ID3D11InputLayout layout = mLayoutList.get(layoutKey);
        if( /*mLayoutList[layoutKey]*/ layout != null)
        {
            /**ppInputLayout = mLayoutList[layoutKey];
            (*ppInputLayout)->AddRef();
            return CPUT_SUCCESS;*/
            return layout;
        }
        // Not found, create a new ID3D11InputLayout object

        // How many elements are in the input layout?
        int numInputLayoutElements=0;
        while(null != pDXLayout[numInputLayoutElements].SemanticName)
        {
            numInputLayoutElements++;
        }
        // Create the input layout
        /*HRESULT hr;  TODO create layout
        ID3DBlob *pBlob = pVertexShader->GetBlob();
        hr = pDevice->CreateInputLayout( pDXLayout, numInputLayoutElements, pBlob->GetBufferPointer(), pBlob->GetBufferSize(), ppInputLayout );
        ASSERT( SUCCEEDED(hr), _L("Error creating input layout.") );
        CPUTSetDebugName( *ppInputLayout, _L("CPUTInputLayoutCacheDX11::GetLayout()") );*/

        // Store this layout object in our map
        /*mLayoutList[layoutKey] = *ppInputLayout;*/
        mLayoutList.put(layoutKey, layout);

        // Addref for storing it in our map as well as returning it (count should be = 2 at this point)
        /*(*ppInputLayout)->AddRef();
        return CPUT_SUCCESS;*/
        return layout;
    }

    void ClearLayoutCache(){
        // iterate over the entire map - and release each layout object
        /*std::map<cString, ID3D11InputLayout*>::iterator mapIterator;

        for(mapIterator = mLayoutList.begin(); mapIterator != mLayoutList.end(); mapIterator++)
        {
            mapIterator->second->Release();  // release the ID3D11InputLayout*
        }*/
        mLayoutList.clear();
    }

    private CPUTInputLayoutCacheDX11(){}

    // convert the D3D11_INPUT_ELEMENT_DESC to string key
    private String GenerateLayoutKey(D3D11_INPUT_ELEMENT_DESC[] pDXLayout){
        // TODO:  Duh!  We can simply memcmp the DX layouts == use the layout input description directly as the key.
        //        We just need to know how many elements, or NULL terminate it.
        //        Uses less memory, faster, etc...
        //        Duh!

        if(StringUtils.isEmpty(pDXLayout[0].SemanticName))
        {
            return ("");
        }
        // TODO: Use shorter names, etc...
        /*ASSERT( (pDXLayout[0].Format>=0) && (pDXLayout[0].Format<=DXGI_FORMAT_BC7_UNORM_SRGB), _L("Invalid DXGI Format.") );*/
        // Start first layout entry and no comma.
        String layoutKey = pDXLayout[0].SemanticName + ":" + /*gpDXGIFormatNames[pDXLayout[0].Format]*/ TextureUtils.getFormatName(pDXLayout[0].Format);
        for( int index=1; null != pDXLayout[index].SemanticName; index++ )
        {
            /*ASSERT( (pDXLayout[index].Format>=0) && (pDXLayout[index].Format<=DXGI_FORMAT_BC7_UNORM_SRGB), _L("Invalid DXGI Format.") );*/
            // Add a comma and the next layout entry
            layoutKey = layoutKey +"," + pDXLayout[index].SemanticName + ":" + /*gpDXGIFormatNames[pDXLayout[index].Format]*/
                    TextureUtils.getFormatName(pDXLayout[index].Format);
        }
        return layoutKey;
    }

//    private void VerifyLayoutCompatibility(D3D11_INPUT_ELEMENT_DESC pDXLayout/*, ID3DBlob *pVertexShaderBlob*/);

    private static CPUTInputLayoutCacheDX11 mpInputLayoutCache;
    private final Map<String, ID3D11InputLayout> mLayoutList = new HashMap<>();
}

