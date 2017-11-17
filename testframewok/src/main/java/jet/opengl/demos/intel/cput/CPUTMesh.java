package jet.opengl.demos.intel.cput;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.common.Disposeable;

/**
 * Created by mazhen'gui on 2017/11/13.
 */

public abstract class CPUTMesh implements Disposeable{
    static final int
            CPUT_UNKNOWN=0,

            CPUT_DOUBLE=1,
            CPUT_F32=2,

            CPUT_U64=3,
            CPUT_I64=4,

            CPUT_U32=5,
            CPUT_I32=6,

            CPUT_U16=7,
            CPUT_I16=8,

            CPUT_U8=9,
            CPUT_I8=10,

            CPUT_CHAR=11,
            CPUT_BOOL=12;
    //------------------------------------------------------------------------------
    // This map defines how the above eCPUT_VERTEX_ELEMENT_TYPE's map to internal
    // CPUT types.  Be sure to update them at the same time.
    static final int CPUT_FILE_ELEMENT_TYPE_TO_CPUT_TYPE_CONVERT[] =
            {
                    CPUT_UNKNOWN,   // 0
                    CPUT_UNKNOWN,   // 1
                    CPUT_I8,        // 2  int =  1 byte
                    CPUT_U8,        // 3 UINT, __int8 =  1 byte
                    CPUT_I16,       // 4 __int16 = 2 bytes
                    CPUT_U16,       // 5 unsigned __int16  =  2 bytes
                    CPUT_I32,       // 6 __int32 = 4 bytes
                    CPUT_U32,       // 7 unsigned __int32  =  4 bytes
                    CPUT_I64,       // 8 __int64  = 8 bytes
                    CPUT_U64,       // 9 unsigned __int64 =  8 bytes
                    CPUT_BOOL,      // 10 bool  =  1 byte - '0' = false, '1' = true, same as stl bool i/o
                    CPUT_CHAR,      // 11 signed char  = 1 byte
                    CPUT_U8,        // 12 unsigned char  = 1 byte
                    CPUT_U16,       // 13 wchar_t  = 2 bytes
                    CPUT_F32,       // 14 float  = 4 bytes
                    CPUT_DOUBLE,    // 15 double  = 8 bytes
            };

    // Corresponding sizes (in bytes) that match CPUT_DATA_FORMAT_TYPE
    static final int CPUT_DATA_FORMAT_SIZE[] =
    {
            0, //CPUT_UNKNOWN=0,

            8, //CPUT_DOUBLE,
            4, //CPUT_F32,

            8, //CPUT_U64,
            8, //CPUT_I64,

            4, //CPUT_U32,
            4, //CPUT_I32,

            2, //CPUT_U16,
            2, //CPUT_I16,

            1, //CPUT_U8,
            1, //CPUT_I8,

            1, //CPUT_CHAR
            1, //CPUT_BOOL
    };

    // Note: The indices of these strings must match the corresponding value in enum CPUT_VERTEX_ELEMENT_SEMANTIC
    static final  String CPUT_VERTEX_ELEMENT_SEMANTIC_AS_STRING[] =
    {
        "UNDEFINED",
        "UNDEFINED", // Note 1 is missing (back compatibility)
        "POSITON",
        "NORMAL",
        "TEXTURECOORD",
        "COLOR",
        "TANGENT",
        "BINORMAL"
    };

    static final int
            CPUT_VERTEX_ELEMENT_UNDEFINED    = 0,
            // Note 1 is missing (back compatibility)
            CPUT_VERTEX_ELEMENT_POSITON      = 2,
            CPUT_VERTEX_ELEMENT_NORMAL       = 3,
            CPUT_VERTEX_ELEMENT_TEXTURECOORD = 4,
            CPUT_VERTEX_ELEMENT_VERTEXCOLOR  = 5,
            CPUT_VERTEX_ELEMENT_TANGENT      = 6,
            CPUT_VERTEX_ELEMENT_BINORMAL     = 7;

    /**
     * These are hard coded, so you can add or deprecate, but not reuse them
     * If you change or add anything to this list, be sure to update the
     * CPUT_FILE_ELEMENT_TYPE_TO_CPUT_TYPE_CONVERT
     * struct below as well
     */
    static final int
    /*enum eCPUT_VERTEX_ELEMENT_TYPE : UINT
    {*/
        tMINTYPE = 1,
        tINT8=2,      // 2  int =  1 byte
        tUINT8=3,     // 3 UINT, __int8 =  1 byte
        tINT16=4,     // 4 __int16 = 2 bytes
        tUINT16=5,    // 5 unsigned __int16  =  2 bytes
        tINT32=6,     // 6 __int32 = 4 bytes
        tUINT32=7,    // 7 unsigned __int32  =  4 bytes
        tINT64=8,     // 8 __int64  = 8 bytes
        tUINT64=9,    // 9 unsigned __int64 =  8 bytes
        tBOOL=10,      // 10 bool  =  1 byte - '0' = false, '1' = true, same as stl bool i/o
        tCHAR=11,      // 11 signed char  = 1 byte
        tUCHAR=12,     // 12 unsigned char  = 1 byte
        tWCHAR=13,     // 13 wchar_t  = 2 bytes
        tFLOAT=14,     // 14 float  = 4 bytes
        tDOUBLE=15,    // 15 double  = 8 bytes

        // add new ones here
        tINVALID = 255;
//    };

    protected int mMeshTopology;
    protected int mInstanceCount = 1;

    // TODO: ? Change from virtual to #ifdef-controlled redirect to platform versions?
    // TODO: Use CPUT_MAPPED_SUBRESOURCE ??
    public abstract ByteBuffer MapVertices(CPUTRenderParameters params, int type, boolean wait/*=true*/ );
    public abstract ByteBuffer MapIndices(    CPUTRenderParameters params, int type, boolean wait/*=true*/ );
    public abstract void                     UnmapVertices( CPUTRenderParameters params );
    public abstract void                     UnmapIndices(  CPUTRenderParameters params );

    public void SetMeshTopology(int meshTopology) { mMeshTopology = meshTopology; }
    public abstract void BindVertexShaderLayout(CPUTMaterial pMaterial, CPUTMaterial pShadowCastMaterial);
    public abstract void CreateNativeResources(
            CPUTModel      pModel,
            int            meshIdx,
            int             vertexDataInfoArraySize,
            CPUTBufferInfo[] pVertexInfo,
            byte[]         pVertexData,
            CPUTBufferInfo pIndexInfo,
            int[]         pIndex
    );

    public abstract void Draw(CPUTRenderParameters renderParams, CPUTModel pModel);
    public abstract void DrawShadow(CPUTRenderParameters renderParams, CPUTModel pModel);
    public void IncrementInstanceCount() { mInstanceCount++; }
    public void DecrementInstanceCount() { mInstanceCount--; }
}
