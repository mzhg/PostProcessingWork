//----------------------------------------------------------------------------------
// File:   VarianceShadowMapping.fxh
// Author: Kevin Myers
// Email:  sdkfeedback@nvidia.com
//
// Copyright (c) 2007 NVIDIA Corporation. All rights reserved.
//
// TO  THE MAXIMUM  EXTENT PERMITTED  BY APPLICABLE  LAW, THIS SOFTWARE  IS PROVIDED
// *AS IS*  AND NVIDIA AND  ITS SUPPLIERS DISCLAIM  ALL WARRANTIES,  EITHER  EXPRESS
// OR IMPLIED, INCLUDING, BUT NOT LIMITED  TO, IMPLIED WARRANTIES OF MERCHANTABILITY
// AND FITNESS FOR A PARTICULAR PURPOSE.  IN NO EVENT SHALL  NVIDIA OR ITS SUPPLIERS
// BE  LIABLE  FOR  ANY  SPECIAL,  INCIDENTAL,  INDIRECT,  OR  CONSEQUENTIAL DAMAGES
// WHATSOEVER (INCLUDING, WITHOUT LIMITATION,  DAMAGES FOR LOSS OF BUSINESS PROFITS,
// BUSINESS INTERRUPTION, LOSS OF BUSINESS INFORMATION, OR ANY OTHER PECUNIARY LOSS)
// ARISING OUT OF THE  USE OF OR INABILITY  TO USE THIS SOFTWARE, EVEN IF NVIDIA HAS
// BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
//
//
//----------------------------------------------------------------------------------
#include "../../../shader_libs/PostProcessingHLSLCompatiable.glsl"

#if 0
shared cbuffer changing
{
    float4x4 mWorld;
    float4x4 mView;
    float4x4 mShadowView;
    float4   vDiffuseColor;
    bool bDiffuseMap;
    bool bNormalMap;
};

shared cbuffer permanent
{
    float4x4 mProj;
    float4x4 mShadowProj;
    float4 vLightPos;

    float fTextureWidth;
    float fFilterWidth = 1.0f;
    bool bShowVariance = false;
    bool     bSpecular;
    bool     bShowMD;
    bool     bShowCheb;
};


shared RasterizerState noCullRS
{
    CullMode = None;
    FillMode = Solid;
    MultisampleEnable = false;
};

shared RasterizerState backCullRS
{
    CullMode = Back;
    FillMode = Solid;
    MultisampleEnable = false;
};

shared RasterizerState frontCullRS
{
    CullMode = Front;
    FillMode = Solid;
    MultisampleEnable = false;
};

shared RasterizerState noCullMSAARS
{
    CullMode = None;
    FillMode = Solid;
    MultisampleEnable = True;
};

shared RasterizerState backCullMSAARS
{
    CullMode = Back;
    FillMode = Solid;
    MultisampleEnable = True;
};

shared DepthStencilState zWriteNoStencilDS
{
    DepthEnable = true;
    DepthFunc = Less_Equal;
    DepthWriteMask = All;
    StencilEnable = false;
};

shared BlendState RGBABS
{
    RenderTargetWriteMask[0] = 0x0f;
    BlendEnable[0] = false;
};


shared SamplerState FILT_LINEAR
{
    AddressU = Clamp;
    AddressV = Clamp;
    Filter = MIN_MAG_MIP_LINEAR;
};

shared SamplerState FILT_LINEAR_WRAP
{
    AddressU = Wrap;
    AddressV = Wrap;
    Filter = MIN_MAG_MIP_LINEAR;
};

shared SamplerState FILT_POINT_WRAP
{
    AddressU = Wrap;
    AddressV = Wrap;
    Filter = MIN_MAG_MIP_POINT;
};

shared SamplerState FILT_POINT
{
    AddressU = Clamp;
    AddressV = Clamp;
    Filter = MIN_MAG_MIP_POINT;
};

shared SamplerComparisonState  FILT_PCF
{
    AddressU = Clamp;
    AddressV = Clamp;
    Filter = COMPARISON_MIN_MAG_LINEAR_MIP_POINT;
    ComparisonFunc = Less_Equal;
};

shared Texture2D texShadowMap;
shared Texture2D texDiffuse;
shared Texture2D texNormal;

struct VSQuadOut
{
    float4 pos : SV_Position;
    float2 tex : TEXCOORD;
};


//Vertex shader that generates a full screen quad with texcoords from vertIDs
VSQuadOut VSFullscreenQuad( uint id : SV_VertexID )
{
    VSQuadOut output = (VSQuadOut)0.0f;
    output.tex = float2( (id << 1) & 2, id & 2 );
    output.pos = float4( output.tex * float2( 2.0f, -2.0f ) + float2( -1.0f, 1.0f), 0.0f, 1.0f );

    return output;
}
#endif

float BoxFilterStart( float fWidth )  //Assumes filter is odd
{
    return ( ( fWidth - 1.0f ) / 2.0f );
}