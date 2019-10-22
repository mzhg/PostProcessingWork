package com.nvidia.developer.opengl.models.sdkmesh;

public interface D3D9Enums {
    int D3DDECLTYPE_FLOAT1     = 0,  // for the type of the VertexElement9
            D3DDECLTYPE_FLOAT2     = 1,
            D3DDECLTYPE_FLOAT3     = 2,
            D3DDECLTYPE_FLOAT4     = 3,
            D3DDECLTYPE_D3DCOLOR   = 4,
            D3DDECLTYPE_UBYTE4     = 5,
            D3DDECLTYPE_SHORT2     = 6,
            D3DDECLTYPE_SHORT4     = 7,
            D3DDECLTYPE_UBYTE4N    = 8,
            D3DDECLTYPE_SHORT2N    = 9,
            D3DDECLTYPE_SHORT4N    = 10,
            D3DDECLTYPE_USHORT2N   = 11,
            D3DDECLTYPE_USHORT4N   = 12,
            D3DDECLTYPE_UDEC3      = 13,
            D3DDECLTYPE_DEC3N      = 14,
            D3DDECLTYPE_FLOAT16_2  = 15,
            D3DDECLTYPE_FLOAT16_4  = 16,
            D3DDECLTYPE_UNUSED     = 17;

    int D3DDECLUSAGE_POSITION      = 0,   // for the stream of the VertexElement9
            D3DDECLUSAGE_BLENDWEIGHT   = 1,
            D3DDECLUSAGE_BLENDINDICES  = 2,
            D3DDECLUSAGE_NORMAL        = 3,
            D3DDECLUSAGE_PSIZE         = 4,
            D3DDECLUSAGE_TEXCOORD      = 5,
            D3DDECLUSAGE_TANGENT       = 6,
            D3DDECLUSAGE_BINORMAL      = 7,
            D3DDECLUSAGE_TESSFACTOR    = 8,
            D3DDECLUSAGE_POSITIONT     = 9,
            D3DDECLUSAGE_COLOR         = 10,
            D3DDECLUSAGE_FOG           = 11,
            D3DDECLUSAGE_DEPTH         = 12,
            D3DDECLUSAGE_SAMPLE        = 13;
}
