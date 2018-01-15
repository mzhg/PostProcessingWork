package jet.opengl.demos.intel.cput;

import java.util.Arrays;

import jet.opengl.postprocessing.common.BlendState;
import jet.opengl.postprocessing.common.DepthStencilState;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.common.RasterizerState;
import jet.opengl.postprocessing.util.LogUtil;
import jet.opengl.postprocessing.util.Numeric;

/**
 * Created by mazhen'gui on 2018/1/15.
 */

public class CPUTRenderStateBlockDX11 extends CPUTRenderStateBlock {
    public static final int
            ePARAM_TYPE_TYPELESS = 0,
            ePARAM_TYPE_INT = 1,
            ePARAM_TYPE_UINT = 2,
            ePARAM_TYPE_FLOAT = 3,
            ePARAM_TYPE_BOOL = 4,
            ePARAM_TYPE_SHORT = 5,
            ePARAM_TYPE_CHAR = 6,
            ePARAM_TYPE_UCHAR = 7,
            ePARAM_TYPE_STRING = 8, // Does string make sense?  Could copy it.
            ePARAM_TYPE_D3D11_BLEND =9,
            ePARAM_TYPE_D3D11_BLEND_OP=10,
            ePARAM_TYPE_DEPTH_WRITE_MASK=11,
            ePARAM_TYPE_D3D11_COMPARISON_FUNC=12,
            ePARAM_TYPE_D3D11_STENCIL_OP=13,
            ePARAM_TYPE_D3D11_FILL_MODE=14,
            ePARAM_TYPE_D3D11_CULL_MODE=15,
            ePARAM_TYPE_D3D11_FILTER=16,
            ePARAM_TYPE_D3D11_TEXTURE_ADDRESS_MODE=17;

    // The state descriptor describes all of the states.
    // We read it in when creating assets.  We keep it around in case we need to adjust and recreate.
    final CPUTRenderStateDX11        mStateDesc = new CPUTRenderStateDX11();

    // Each of the native state objects.
    /*ID3D11BlendState           *mpBlendState;
    ID3D11DepthStencilState    *mpDepthStencilState;
    ID3D11RasterizerState      *mpRasterizerState;
    ID3D11SamplerState         *mpSamplerState[D3D11_COMMONSHADER_SAMPLER_SLOT_COUNT];*/
    int                        mNumSamplers;

    @Override
    public void LoadRenderStateBlock(String fileName) {

    }

    @Override
    public void SetRenderStates(CPUTRenderParameters renderParams) {

    }

    @Override
    public void CreateNativeResources() {

    }

    public Object ReadValue( CPUTConfigEntry pValue, CPUTRenderStateMapEntry[] pRenderStateList/*, void *pDest*/ ){
        String lowerCaseName = pValue.NameAsString();

        if(pRenderStateList == null){
            return null;
        }

        boolean found = false;
        // Find it in the map.  TODO: could use a real map.  Maybe with a binary search, lexical storage, etc.
//        for( CPUTRenderStateMapEntry pCur = pRenderStateList; pCur.name != null ; pCur++ )
        for(int i = 0; i < pRenderStateList.length; i++)
        {
            CPUTRenderStateMapEntry pCur = pRenderStateList[i];
            if(pCur.name == null)
                return null;

//            found = 0 == _wcsicmp( lowerCaseName.data(), pCur->name.data() );
            found = lowerCaseName.equalsIgnoreCase(pCur.name);
            if( found )
            {
                // We found it.  Now convert it from the text file's string to its internal representation

                // There must be a more-generic way to do the following.  write( void*, void*, type ).
                // Use function pointer array to ValueAsInt() and similar, so we can call them without the switch?
                // Might require they all have same signature ==> use void pointers, and cast internally?
                switch( pCur.type )
                {
                    case ePARAM_TYPE_TYPELESS: throw new Error("Inner error!"); // Should not get here.
                    case ePARAM_TYPE_INT:      return pValue.ValueAsInt();
                    case ePARAM_TYPE_UINT:     return pValue.ValueAsInt();
                    case ePARAM_TYPE_BOOL:     return pValue.ValueAsBool();
                    case ePARAM_TYPE_FLOAT:    return pValue.ValueAsFloat();
                    case ePARAM_TYPE_SHORT:    return (short)pValue.ValueAsInt();
                    case ePARAM_TYPE_CHAR:     return (char)pValue.ValueAsInt();  // TODO char is not same sa java.
                    case ePARAM_TYPE_UCHAR:    return (char)pValue.ValueAsInt();
                    case ePARAM_TYPE_STRING: break; // Not sure how to deal with this yet.  Strings can have different sizes
                    // The following types must be converted from string to enum.  They achieve this with a translation map
                    case ePARAM_TYPE_D3D11_BLEND:      return FindMapEntryByName(pValue.ValueAsString());// found = pBlendMap->FindMapEntryByName(          (int*)&((char*)pDest)[pCur->offset], pValue->ValueAsString() ); break;
                    case ePARAM_TYPE_D3D11_BLEND_OP:   return FindMapEntryByName(pValue.ValueAsString());//found = pBlendOpMap->FindMapEntryByName(        (int*)&((char*)pDest)[pCur->offset], pValue->ValueAsString() ); break;
                    case ePARAM_TYPE_DEPTH_WRITE_MASK: return FindMapEntryByName(pValue.ValueAsString());//found = pDepthWriteMaskMap->FindMapEntryByName( (int*)&((char*)pDest)[pCur->offset], pValue->ValueAsString() ); break;
                    case ePARAM_TYPE_D3D11_STENCIL_OP: return FindMapEntryByName(pValue.ValueAsString());//found = pStencilOpMap->FindMapEntryByName(      (int*)&((char*)pDest)[pCur->offset], pValue->ValueAsString() ); break;
                    case ePARAM_TYPE_D3D11_FILL_MODE:  return FindMapEntryByName(pValue.ValueAsString());//found = pFillModeMap->FindMapEntryByName(       (int*)&((char*)pDest)[pCur->offset], pValue->ValueAsString() ); break;
                    case ePARAM_TYPE_D3D11_CULL_MODE:  return FindMapEntryByName(pValue.ValueAsString());//found = pCullModeMap->FindMapEntryByName(       (int*)&((char*)pDest)[pCur->offset], pValue->ValueAsString() ); break;
                    case ePARAM_TYPE_D3D11_FILTER:     return FindMapEntryByName(pValue.ValueAsString());//found = pFilterMap->FindMapEntryByName(         (int*)&((char*)pDest)[pCur->offset], pValue->ValueAsString() ); break;
                    case ePARAM_TYPE_D3D11_COMPARISON_FUNC:      return FindMapEntryByName(pValue.ValueAsString());//found = pComparisonMap->FindMapEntryByName(     (int*)&((char*)pDest)[pCur->offset], pValue->ValueAsString() ); break;
                    case ePARAM_TYPE_D3D11_TEXTURE_ADDRESS_MODE: return FindMapEntryByName(pValue.ValueAsString());//found = pTextureAddressMap->FindMapEntryByName( (int*)&((char*)pDest)[pCur->offset], pValue->ValueAsString() ); break;
                }
                break; // From for.  We found it, so we're done.
            }
        }
//        ASSERT( found, _L( "Unkown render state: '") + pValue->NameAsString() + _L("'.") );

        return null;
    }

    public Object ReadProperties(
            CPUTConfigFile                file,
            String                 blockName,
            CPUTRenderStateMapEntry[] pMap//,
//            void                          *pDest
    ){
        CPUTConfigBlock pProperties = file.GetBlockByName(blockName);
        if( pProperties == null )
        {
            // Note: We choose not to assert here.  The nature of the parameter block is that
            // only the values that deviate from default need to be present.  It is very
            // common that blocks will be missing
            LogUtil.e(LogUtil.LogType.DEFAULT, "Couldn't the find the specical block by the name: " + blockName);
            return /*CPUT_ERROR_PARAMETER_BLOCK_NOT_FOUND*/ null;
        }

        int count = pProperties.ValueCount();
        for( int ii=0; ii<count; ii++ )
        {
            // Get the next property
            CPUTConfigEntry pValue = pProperties.GetValue(ii);
//            ASSERT( pValue->IsValid(), _L("Invalid Value: '")+pValue->NameAsString()+_L("'.") );
            if(!pValue.IsValid()){
                LogUtil.e(LogUtil.LogType.DEFAULT, "Invalid Value: '" + pValue.NameAsString() + "'");
                continue;
            }

            return ReadValue( pValue, pMap/*, pDest*/ );
        }

        return null;
    }

    private static final class CPUTRenderStateDX11{
        final BlendState BlendDesc = new BlendState();
        final DepthStencilState DepthStencilDesc = new DepthStencilState();
        final RasterizerState RasterizerDesc = new RasterizerState();
//        D3D11_SAMPLER_DESC       SamplerDesc[D3D11_COMMONSHADER_SAMPLER_SLOT_COUNT];
        final float[]                    BlendFactor = new float[4];
        int                     SampleMask;

        CPUTRenderStateDX11() { SetDefaults(); }
        void SetDefaults(){
            BlendDesc.set(BlendState.g_DefaultBlendState);
            DepthStencilDesc.set(DepthStencilState.g_DefaultDSState);
            RasterizerDesc.set(RasterizerState.g_DefaultRSState);

            BlendFactor[0] = BlendFactor[1] = BlendFactor[2] = BlendFactor[3] = 1.f;
            SampleMask = 0xFFFFFFFF;
        }
    }

    public static final class CPUTRenderStateMapEntry{
        public String         name;
        public int            type;
        public int            offset;
    }

    private static final class StringToIntMapEntry implements Comparable<StringToIntMapEntry>{
        String name;
        int value;

        StringToIntMapEntry(String name, int value){
            this.name = name;
            this.value = value;
        }

        @Override
        public int compareTo(StringToIntMapEntry o) {
            return name.compareTo(o.name);
        }
    }

    private static final String _L(String s) { return s;}

    private static final StringToIntMapEntry[] g_StateEntries = {
            new StringToIntMapEntry( _L("d3d11_blend_zero"),             GLenum.GL_ZERO) ,
            new StringToIntMapEntry( _L("d3d11_blend_one"),              GLenum.GL_ONE ),
            new StringToIntMapEntry( _L("d3d11_blend_src_color"),        GLenum.GL_SRC_COLOR ),
            new StringToIntMapEntry( _L("d3d11_blend_inv_src_color"),    GLenum.GL_ONE_MINUS_SRC_COLOR ),
            new StringToIntMapEntry( _L("d3d11_blend_src_alpha"),        GLenum.GL_SRC_ALPHA ),
            new StringToIntMapEntry( _L("d3d11_blend_inv_src_alpha"),    GLenum.GL_ONE_MINUS_SRC_ALPHA ),
            new StringToIntMapEntry( _L("d3d11_blend_dest_alpha"),       GLenum.GL_DST_ALPHA ),
            new StringToIntMapEntry( _L("d3d11_blend_inv_dest_alpha"),   GLenum.GL_ONE_MINUS_DST_ALPHA ),
            new StringToIntMapEntry( _L("d3d11_blend_dest_color"),       GLenum.GL_DST_COLOR ),
            new StringToIntMapEntry( _L("d3d11_blend_inv_dest_color"),   GLenum.GL_ONE_MINUS_DST_COLOR ),
            new StringToIntMapEntry( _L("d3d11_blend_src_alpha_sat"),    GLenum.GL_SRC_ALPHA_SATURATE ),
            new StringToIntMapEntry( _L("d3d11_blend_blend_factor"),     GLenum.GL_CONSTANT_COLOR),
            new StringToIntMapEntry( _L("d3d11_blend_inv_blend_factor"), GLenum.GL_ONE_MINUS_CONSTANT_COLOR ),
            new StringToIntMapEntry( _L("d3d11_blend_src1_color"),       GLenum.GL_SRC1_COLOR ),
            new StringToIntMapEntry( _L("d3d11_blend_inv_src1_color"),   GLenum.GL_ONE_MINUS_SRC1_COLOR ),
            new StringToIntMapEntry( _L("d3d11_blend_src1_alpha"),       GLenum.GL_SRC1_ALPHA ),
            new StringToIntMapEntry( _L("d3d11_blend_inv_src1_alpha"),   GLenum.GL_ONE_MINUS_SRC1_ALPHA ),

            new StringToIntMapEntry( _L("d3d11_blend_op_add"),          GLenum.GL_FUNC_ADD ),
            new StringToIntMapEntry( _L("d3d11_blend_op_subtract"),     GLenum.GL_FUNC_SUBTRACT ),
            new StringToIntMapEntry( _L("d3d11_blend_op_rev_subtract"), GLenum.GL_FUNC_REVERSE_SUBTRACT ),
            new StringToIntMapEntry( _L("d3d11_blend_op_min"),          GLenum.GL_MIN ),
            new StringToIntMapEntry( _L("d3d11_blend_op_max"),          GLenum.GL_MAX ),

            new StringToIntMapEntry( _L("D3D11_DEPTH_WRITE_MASK_ZERO"), 0 ),
            new StringToIntMapEntry( _L("D3D11_DEPTH_WRITE_MASK_ALL"),  0xFFFFFFFF ),

            new StringToIntMapEntry( _L("D3D11_COMPARISON_NEVER"),         GLenum.GL_NEVER),
            new StringToIntMapEntry( _L("D3D11_COMPARISON_LESS"),          GLenum.GL_LESS ),
            new StringToIntMapEntry( _L("D3D11_COMPARISON_EQUAL"),         GLenum.GL_EQUAL ),
            new StringToIntMapEntry( _L("D3D11_COMPARISON_LESS_EQUAL"),    GLenum.GL_LEQUAL ),
            new StringToIntMapEntry( _L("D3D11_COMPARISON_GREATER"),       GLenum.GL_GREATER ),
            new StringToIntMapEntry( _L("D3D11_COMPARISON_NOT_EQUAL"),     GLenum.GL_NOTEQUAL),
            new StringToIntMapEntry( _L("D3D11_COMPARISON_GREATER_EQUAL"), GLenum.GL_GEQUAL),
            new StringToIntMapEntry( _L("D3D11_COMPARISON_ALWAYS"),        GLenum.GL_ALWAYS),

            new StringToIntMapEntry( _L("D3D11_STENCIL_OP_KEEP"),     GLenum.GL_KEEP ),
            new StringToIntMapEntry( _L("D3D11_STENCIL_OP_ZERO"),     GLenum.GL_ZERO ),
            new StringToIntMapEntry( _L("D3D11_STENCIL_OP_REPLACE"),  GLenum.GL_REPLACE ),
            new StringToIntMapEntry( _L("D3D11_STENCIL_OP_INCR_SAT"), GLenum.GL_INCR ),
            new StringToIntMapEntry( _L("D3D11_STENCIL_OP_DECR_SAT"), GLenum.GL_DECR ),
            new StringToIntMapEntry( _L("D3D11_STENCIL_OP_INVERT"),   GLenum.GL_INVERT ),
            new StringToIntMapEntry( _L("D3D11_STENCIL_OP_INCR"),     GLenum.GL_INCR_WRAP ),
            new StringToIntMapEntry( _L("D3D11_STENCIL_OP_DECR"),     GLenum.GL_DECR_WRAP ),

            new StringToIntMapEntry( _L("D3D11_FILL_WIREFRAME"),      GLenum.GL_LINE ),
            new StringToIntMapEntry( _L("D3D11_FILL_SOLID"),          GLenum.GL_FILL ),

            new StringToIntMapEntry( _L("D3D11_CULL_NONE"),           GLenum.GL_NONE ),
            new StringToIntMapEntry( _L("D3D11_CULL_FRONT"),          GLenum.GL_FRONT ),
            new StringToIntMapEntry( _L("D3D11_CULL_BACK"),           GLenum.GL_BACK ),

            new StringToIntMapEntry( _L("D3D11_FILTER_MIN_MAG_MIP_POINT"),                          Numeric.encode((short)GLenum.GL_NEAREST, (short)GLenum.GL_NEAREST_MIPMAP_NEAREST)),  // first for mag, second for min
            new StringToIntMapEntry( _L("D3D11_FILTER_MIN_MAG_POINT_MIP_LINEAR"),                   Numeric.encode((short)GLenum.GL_NEAREST, (short)GLenum.GL_NEAREST_MIPMAP_LINEAR) ),
            new StringToIntMapEntry( _L("D3D11_FILTER_MIN_POINT_MAG_LINEAR_MIP_POINT"),             Numeric.encode((short)GLenum.GL_LINEAR, (short)GLenum.GL_NEAREST_MIPMAP_NEAREST) ),
            new StringToIntMapEntry( _L("D3D11_FILTER_MIN_POINT_MAG_MIP_LINEAR"),                   Numeric.encode((short)GLenum.GL_LINEAR, (short)GLenum.GL_NEAREST_MIPMAP_LINEAR) ),
            new StringToIntMapEntry( _L("D3D11_FILTER_MIN_LINEAR_MAG_MIP_POINT"),                   Numeric.encode((short)GLenum.GL_NEAREST, (short)GLenum.GL_LINEAR_MIPMAP_NEAREST) ),
            new StringToIntMapEntry( _L("D3D11_FILTER_MIN_LINEAR_MAG_POINT_MIP_LINEAR"),            Numeric.encode((short)GLenum.GL_NEAREST, (short)GLenum.GL_LINEAR_MIPMAP_LINEAR) ),
            new StringToIntMapEntry( _L("D3D11_FILTER_MIN_MAG_LINEAR_MIP_POINT"),                   Numeric.encode((short)GLenum.GL_LINEAR, (short)GLenum.GL_LINEAR_MIPMAP_NEAREST) ),
            new StringToIntMapEntry( _L("D3D11_FILTER_MIN_MAG_MIP_LINEAR"),                         Numeric.encode((short)GLenum.GL_LINEAR, (short)GLenum.GL_LINEAR_MIPMAP_LINEAR) ),
            /*new StringToIntMapEntry( _L("D3D11_FILTER_ANISOTROPIC"),                                D3D11_FILTER_ANISOTROPIC ),
            new StringToIntMapEntry( _L("D3D11_FILTER_COMPARISON_MIN_MAG_MIP_POINT"),               D3D11_FILTER_COMPARISON_MIN_MAG_MIP_POINT ),
            new StringToIntMapEntry( _L("D3D11_FILTER_COMPARISON_MIN_MAG_POINT_MIP_LINEAR"),        D3D11_FILTER_COMPARISON_MIN_MAG_POINT_MIP_LINEAR ),
            new StringToIntMapEntry( _L("D3D11_FILTER_COMPARISON_MIN_POINT_MAG_LINEAR_MIP_POINT"),  D3D11_FILTER_COMPARISON_MIN_POINT_MAG_LINEAR_MIP_POINT),
            new StringToIntMapEntry( _L("D3D11_FILTER_COMPARISON_MIN_POINT_MAG_MIP_LINEAR"),        D3D11_FILTER_COMPARISON_MIN_POINT_MAG_MIP_LINEAR ),
            new StringToIntMapEntry( _L("D3D11_FILTER_COMPARISON_MIN_LINEAR_MAG_MIP_POINT"),        D3D11_FILTER_COMPARISON_MIN_LINEAR_MAG_MIP_POINT ),
            new StringToIntMapEntry( _L("D3D11_FILTER_COMPARISON_MIN_LINEAR_MAG_POINT_MIP_LINEAR"), D3D11_FILTER_COMPARISON_MIN_LINEAR_MAG_POINT_MIP_LINEAR ),
            new StringToIntMapEntry( _L("D3D11_FILTER_COMPARISON_MIN_MAG_LINEAR_MIP_POINT"),        D3D11_FILTER_COMPARISON_MIN_MAG_LINEAR_MIP_POINT ),
            new StringToIntMapEntry( _L("D3D11_FILTER_COMPARISON_MIN_MAG_MIP_LINEAR"),              D3D11_FILTER_COMPARISON_MIN_MAG_MIP_LINEAR ),
            new StringToIntMapEntry( _L("D3D11_FILTER_COMPARISON_ANISOTROPIC"),                     D3D11_FILTER_COMPARISON_ANISOTROPIC ),*/

            new StringToIntMapEntry( _L("D3D11_TEXTURE_ADDRESS_WRAP"),        GLenum.GL_REPEAT ),
            new StringToIntMapEntry( _L("D3D11_TEXTURE_ADDRESS_MIRROR"),      GLenum.GL_MIRRORED_REPEAT ),
            new StringToIntMapEntry( _L("D3D11_TEXTURE_ADDRESS_CLAMP"),       GLenum.GL_CLAMP_TO_EDGE ),
            new StringToIntMapEntry( _L("D3D11_TEXTURE_ADDRESS_BORDER"),      GLenum.GL_CLAMP_TO_BORDER ),
            new StringToIntMapEntry( _L("D3D11_TEXTURE_ADDRESS_MIRROR_ONCE"), GLenum.GL_MIRROR_CLAMP_TO_EDGE ),
    };

    static {
        Arrays.sort(g_StateEntries);
    }

    private static int FindMapEntryByName(String name){
        int low = 0;
        int high = g_StateEntries.length - 1;

        while (low <= high) {
            final int mid = (low + high) >>> 1;
            final StringToIntMapEntry midVal = g_StateEntries[mid];
            final int compareResult = midVal.name.compareTo(name);

            if (/*midVal < key*/ compareResult < 0)
                low = mid + 1;
            else if (/*midVal > key*/ compareResult > 0)
                high = mid - 1;
            else
                return midVal.value;
        }

        throw new IllegalArgumentException("Unkown name: " + name);
    }
}
