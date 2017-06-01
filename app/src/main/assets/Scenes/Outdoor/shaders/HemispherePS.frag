#include "Terrain.frag"


#ifndef BEST_CASCADE_SEARCH
#   define BEST_CASCADE_SEARCH 1
#endif

#ifndef SMOOTH_SHADOWS
#   define SMOOTH_SHADOWS 1
#endif

void FindCascade(float3 f3PosInLightViewSpace,
                 float fCameraViewSpaceZ,
                 out float3 f3PosInCascadeProjSpace,
                 out float3 f3CascadeLightSpaceScale,
                 out float Cascade)
{
    Cascade = 0;
#if BEST_CASCADE_SEARCH
    while(Cascade < NUM_SHADOW_CASCADES)
    {
        // Find the smallest cascade which covers current point
//        SCascadeAttribs CascadeAttribs = g_LightAttribs.ShadowAttribs.Cascades[int(Cascade)];
        f3CascadeLightSpaceScale = g_f4LightSpaceScale[int(Cascade)].xyz;
        f3PosInCascadeProjSpace = f3PosInLightViewSpace * f3CascadeLightSpaceScale + g_f4LightSpaceScaledBias[int(Cascade)].xyz;
        
        // In order to perform PCF filtering without getting out of the cascade shadow map,
        // we need to be far enough from its boundaries.
        if( //Cascade == (NUM_SHADOW_CASCADES - 1) || 
            all( lessThan(abs(f3PosInCascadeProjSpace.xy), float2(1))/*- CascadeAttribs.f4LightProjSpaceFilterRadius.xy*/ ) &&
            // It is necessary to check f3PosInCascadeProjSpace.z as well since it could be behind
            // the far clipping plane of the current cascade
            // Besides, if VSM or EVSM filtering is performed, there is also z boundary
            -1.0 /*+ CascadeAttribs.f4LightProjSpaceFilterRadius.z*/ < f3PosInCascadeProjSpace.z && f3PosInCascadeProjSpace.z < 1.0  /*- CascadeAttribs.f4LightProjSpaceFilterRadius.w*/ )
            break;
        else
            Cascade++;
    }
#else
    /*[unroll]*/for(int i=0; i<(NUM_SHADOW_CASCADES+3)/4; ++i)
    {
//	    float4 v = float4(lessThan(g_fCascadeCamSpaceZEnd[i], float4(fCameraViewSpaceZ)));
		float4 v = float4(g_fCascadeCamSpaceZEnd[i] < fCameraViewSpaceZ);
	    Cascade += dot(float4(1,1,1,1), v);
    }
    if( Cascade < NUM_SHADOW_CASCADES )
    {
    //Cascade = min(Cascade, NUM_SHADOW_CASCADES - 1);
        f3CascadeLightSpaceScale = g_f4LightSpaceScale[Cascade].xyz;
        f3PosInCascadeProjSpace = f3PosInLightViewSpace * f3CascadeLightSpaceScale + g_f4LightSpaceScaledBias[Cascade].xyz;
    }
#endif
}

float2 ComputeReceiverPlaneDepthBias(float3 ShadowUVDepthDX, float3 ShadowUVDepthDY)
{    
    // Compute (dDepth/dU, dDepth/dV):
    //  
    //  | dDepth/dU |    | dX/dU    dX/dV |T  | dDepth/dX |     | dU/dX    dU/dY |-1T | dDepth/dX |
    //                 =                                     =                                      =
    //  | dDepth/dV |    | dY/dU    dY/dV |   | dDepth/dY |     | dV/dX    dV/dY |    | dDepth/dY |
    //
    //  | A B |-1   | D  -B |                      | A B |-1T   | D  -C |                                   
    //            =           / det                           =           / det                    
    //  | C D |     |-C   A |                      | C D |      |-B   A |
    //
    //  | dDepth/dU |           | dV/dY   -dV/dX |  | dDepth/dX |
    //                 = 1/det                                       
    //  | dDepth/dV |           |-dU/dY    dU/dX |  | dDepth/dY |

    float2 biasUV;
    //               dV/dY       V      dDepth/dX    D       dV/dX       V     dDepth/dY     D
    biasUV.x =   ShadowUVDepthDY.y * ShadowUVDepthDX.z - ShadowUVDepthDX.y * ShadowUVDepthDY.z;
    //               dU/dY       U      dDepth/dX    D       dU/dX       U     dDepth/dY     D
    biasUV.y = - ShadowUVDepthDY.x * ShadowUVDepthDX.z + ShadowUVDepthDX.x * ShadowUVDepthDY.z;

    float Det = (ShadowUVDepthDX.x * ShadowUVDepthDY.y) - (ShadowUVDepthDX.y * ShadowUVDepthDY.x);
	biasUV /= sign(Det) * max( abs(Det), 1e-20 );
    //biasUV = abs(Det) > 1e-7 ? biasUV / abs(Det) : 0;// sign(Det) * max( abs(Det), 1e-10 );
    return biasUV;
}

float ComputeShadowAmount(in float3 f3PosInLightViewSpace, in float fCameraSpaceZ, out float Cascade)
{
    float3 f3PosInCascadeProjSpace = float3(0), f3CascadeLightSpaceScale = float3(0);
    FindCascade( f3PosInLightViewSpace.xyz, fCameraSpaceZ, f3PosInCascadeProjSpace, f3CascadeLightSpaceScale, Cascade);
    if( Cascade == NUM_SHADOW_CASCADES )
        return 1.0;

    float3 f3ShadowMapUVDepth;
    f3ShadowMapUVDepth.xy = float2(-0.5, -0.5) + float2(0.5, 0.5) * f3PosInCascadeProjSpace.xy;
    f3ShadowMapUVDepth.z = f3PosInCascadeProjSpace.z;
    
//    float3 f3PosInLightViewSpace_dFdx = float3(dFdx(f3PosInLightViewSpace.x), dFdx(f3PosInLightViewSpace.y), dFdx(f3PosInLightViewSpace.z));
//    float3 f3PosInLightViewSpace_dFdy = float3(dFdy(f3PosInLightViewSpace.x), dFdy(f3PosInLightViewSpace.y), dFdy(f3PosInLightViewSpace.z));
    float3 f3ddXShadowMapUVDepth = dFdx(f3PosInLightViewSpace) * f3CascadeLightSpaceScale * float3(0.5,-0.5,1);
    float3 f3ddYShadowMapUVDepth = dFdy(f3PosInLightViewSpace) * f3CascadeLightSpaceScale * float3(0.5,-0.5,1);

    float2 f2DepthSlopeScaledBias = ComputeReceiverPlaneDepthBias(f3ddXShadowMapUVDepth, f3ddYShadowMapUVDepth);
    float2 ShadowMapDim; float Elems;
//    g_tex2DShadowMap.GetDimensions(ShadowMapDim.x, ShadowMapDim.y, Elems);
	int3 TexelSize = textureSize(g_tex2DShadowMap, 0);
	ShadowMapDim = float2(TexelSize.xy); Elems = float(TexelSize.z);
    f2DepthSlopeScaledBias /= ShadowMapDim.xy;

    float fractionalSamplingError = dot( float2(1.f, 1.f), abs(f2DepthSlopeScaledBias.xy) );
    f3ShadowMapUVDepth.z += fractionalSamplingError;
    
    float fLightAmount = // g_tex2DShadowMap.SampleCmp(samComparison, float3(f3ShadowMapUVDepth.xy, Cascade), float(f3ShadowMapUVDepth.z)).x;
    					 texture(g_tex2DShadowMap, float4(f3ShadowMapUVDepth.xy, Cascade, f3ShadowMapUVDepth.z));

#if SMOOTH_SHADOWS
int2 Offsets[4] = int2[4]
		(
			int2(-1,-1),
			int2(+1,-1),
			int2(-1,+1),
			int2(+1,+1)
		);
//        [unroll]
        for(int i=0; i<4; ++i)
        {
            float fDepthBias = dot(Offsets[i].xy, f2DepthSlopeScaledBias.xy);
            fLightAmount += 
         			//g_tex2DShadowMap.SampleCmp(samComparison, float3(f3ShadowMapUVDepth.xy, Cascade), f3ShadowMapUVDepth.z + fDepthBias, Offsets[i]).x;
         			  textureOffset(g_tex2DShadowMap, float4(f3ShadowMapUVDepth.xy,Cascade,f3ShadowMapUVDepth.z + fDepthBias), Offsets[i]);
        }
        fLightAmount /= 5;
#endif
    return fLightAmount;
}

void CombineMaterials(in float4 MtrlWeights,
                      in float2 f2TileUV,
                      out float3 SurfaceColor,
                      out float3 SurfaceNormalTS)
{
    SurfaceNormalTS = float3(0);
    // Normalize weights and compute base material weight
    MtrlWeights /= max( dot(MtrlWeights, float4(1,1,1,1)) , 1 );
    float BaseMaterialWeight = saturate(1.0 - dot(MtrlWeights, float4(1,1,1,1)));
    
    // The mask is already sharp

    ////Sharpen the mask
    //float2 TmpMin2 = min(MtrlWeights.rg, MtrlWeights.ba);
    //float Min = min(TmpMin2.r, TmpMin2.g);
    //Min = min(Min, BaseMaterialWeight);
    //float p = 4;
    //BaseMaterialWeight = pow(BaseMaterialWeight-Min, p);
    //MtrlWeights = pow(MtrlWeights-Min, p);
    //float NormalizationFactor = dot(MtrlWeights, float4(1,1,1,1)) + BaseMaterialWeight;
    //MtrlWeights /= NormalizationFactor;
    //BaseMaterialWeight /= NormalizationFactor;

	// Get diffuse color of the base material
    float4 BaseMaterialDiffuse = // g_tex2DTileTextures[0].Sample(samLinearWrap, f2TileUV.xy / g_TerrainAttribs.m_fBaseMtrlTilingScale);
    								texture(g_tex2DTileTextures[0], f2TileUV.xy / g_fBaseMtrlTilingScale);
    float4x4 MaterialColors = float4x4(0);

    // Get tangent space normal of the base material
#if TEXTURING_MODE == TM_MATERIAL_MASK_NM
    float3 BaseMaterialNormal = //g_tex2DTileNormalMaps[0].Sample(samLinearWrap, f2TileUV.xy / g_TerrainAttribs.m_fBaseMtrlTilingScale);
    							  texture(g_tex2DTileNormalMaps[0], f2TileUV.xy / g_fBaseMtrlTilingScale).rgb;
    float4x3 MaterialNormals = float4x3(0);
#endif

    float4 f4TilingScale = g_f4TilingScale;
    float fTilingScale[5] = float[5](0, f4TilingScale.x, f4TilingScale.y, f4TilingScale.z, f4TilingScale.w);
    // Load material colors and normals
    for(int iTileTex = 1; iTileTex < NUM_TILE_TEXTURES; iTileTex++)
	{
        const float fThresholdWeight = 3.f/256.f;
        MaterialColors[iTileTex-1] = 
			MtrlWeights[iTileTex-1] > fThresholdWeight ? 
//				g_tex2DTileTextures[iTileTex].Sample(samLinearWrap, f2TileUV.xy  / fTilingScale[iTileTex]) : 0.f;
				texture(g_tex2DTileTextures[iTileTex], f2TileUV.xy  / fTilingScale[iTileTex]) : float4(0.0);
#if TEXTURING_MODE == TM_MATERIAL_MASK_NM
        MaterialNormals[iTileTex-1] = 
			MtrlWeights[iTileTex-1] > fThresholdWeight ? 
//				g_tex2DTileNormalMaps[iTileTex].Sample(samLinearWrap, f2TileUV.xy / fTilingScale[iTileTex]) : 0.f;
				texture(g_tex2DTileNormalMaps[iTileTex], f2TileUV.xy / fTilingScale[iTileTex]).rgb : float3(0.0);
#endif
	}
    // Blend materials and normals using the weights
    SurfaceColor = BaseMaterialDiffuse.rgb * BaseMaterialWeight + mul(MtrlWeights, MaterialColors).rgb;

#if TEXTURING_MODE == TM_MATERIAL_MASK_NM
    SurfaceNormalTS = BaseMaterialNormal * BaseMaterialWeight +MaterialNormals * MtrlWeights;
    SurfaceNormalTS = normalize(SurfaceNormalTS*2.0-1.0);
#endif
}

layout(location = 0) out float4 OutColor;
layout(location = 1) out float4 OutColor1;

in SHemisphereVSOutput
{
//    float4 f4PosPS ;// SV_Position;
    float2 TileTexUV  ;// TileTextureUV;
    float3 f3Normal ;// Normal;
    float3 f3PosInLightViewSpace ;// POS_IN_LIGHT_VIEW_SPACE;
    float3 f3PosInWorldSapce;
    float fCameraSpaceZ ;// CAMERA_SPACE_Z;
    float2 f2MaskUV0 ;// MASK_UV0;
    float3 f3Tangent ;// TANGENT;
    float3 f3Bitangent ;// BITANGENT;
    float3 f3SunLightExtinction ;// EXTINCTION;
    float3 f3AmbientSkyLight ;// AMBIENT_SKY_LIGHT;
}In;

float3x3 TBN(float3 T, float3 B, float3 N)
{
	return transpose(mat3(T, B, N));
}

void main()
{
	const float g_fEarthReflectance = 0.4;
	float3 EarthNormal = normalize(In.f3Normal);
    float3 EarthTangent = normalize(In.f3Tangent);
    float3 EarthBitangent = normalize(In.f3Bitangent);
    float3 f3TerrainNormal;
    f3TerrainNormal.xz = 
    				//g_tex2DNormalMap.Sample(samLinearMirror, In.f2MaskUV0.xy).xy;
    				texture(g_tex2DNormalMap, In.f2MaskUV0.xy).xy;
    // Since UVs are mirrored, we have to adjust normal coords accordingly:
    float2 f2XZSign = sign( 0.5 - frac(In.f2MaskUV0.xy/2.0) );
    f3TerrainNormal.xz *= f2XZSign;

    f3TerrainNormal.y = sqrt( saturate(1.0 - dot(f3TerrainNormal.xz,f3TerrainNormal.xz)) );
    //float3 Tangent   = normalize(float3(1,0,In.HeightMapGradients.x));
    //float3 Bitangent = normalize(float3(0,1,In.HeightMapGradients.y));
    f3TerrainNormal = normalize( mul(f3TerrainNormal, TBN(EarthTangent, EarthNormal, EarthBitangent)) );
    
    float4 MtrlWeights = 
    					//g_tex2DMtrlMap.Sample(samLinearMirror, In.f2MaskUV0.xy);
    					texture(g_tex2DMtrlMap, In.f2MaskUV0.xy);
    float3 SurfaceColor, SurfaceNormalTS;
    CombineMaterials(MtrlWeights, In.TileTexUV, SurfaceColor.xyz, SurfaceNormalTS);
    
    float3 f3TerrainTangent = normalize( cross(f3TerrainNormal, float3(0,0,1)) );
    float3 f3TerrainBitangent = normalize( cross(f3TerrainTangent, f3TerrainNormal) );
    float3 f3Normal = normalize( mul(SurfaceNormalTS.xyz, TBN(f3TerrainTangent, f3TerrainNormal, f3TerrainBitangent)) );

    // Attenuate extraterrestrial sun color with the extinction factor
    float3 f3SunLight = g_f4ExtraterrestrialSunColor.rgb * In.f3SunLightExtinction;
    // Ambient sky light is not pre-multiplied with the sun intensity
    float3 f3AmbientSkyLight = g_f4ExtraterrestrialSunColor.rgb * In.f3AmbientSkyLight;
    // Account for occlusion by the ground plane
    f3AmbientSkyLight *= saturate((1 + dot(EarthNormal, f3Normal))/2.f);   //TODO Here have problems

    // We need to divide diffuse color by PI to get the reflectance value
    float3 SurfaceReflectance = SurfaceColor * g_fEarthReflectance / PI;

    float Cascade;
    float fLightAmount = ComputeShadowAmount(In.f3PosInLightViewSpace.xyz, In.fCameraSpaceZ, Cascade);
    fLightAmount = max(1.0, fLightAmount);
    float DiffuseIllumination = max(0.0, dot(f3Normal, g_f4DirOnLight.xyz));  //TODO Here have problems
//    DiffuseIllumination = clamp(DiffuseIllumination, 0.1, 0.2);
    
    float3 f3CascadeColor = float3(0);
    if( g_bVisualizeCascades )
    {
        f3CascadeColor = (Cascade < NUM_SHADOW_CASCADES ? g_f4CascadeColors[int(Cascade)].rgb : float3(1,1,1)) / 8.0 ;
    }
    
    float3 f3FinalColor = f3CascadeColor +  SurfaceReflectance * (fLightAmount*DiffuseIllumination*f3SunLight + f3AmbientSkyLight + 0.4);

    OutColor = float4( f3FinalColor, 1);
    
    float mZFar =g_fFarPlaneZ;
	float mZNear = g_fNearPlaneZ;
	float fCamSpaceZ = mZFar*mZNear/(mZFar-gl_FragCoord.z*(mZFar-mZNear));
	OutColor1 = float4(In.f3PosInWorldSapce, fCamSpaceZ);
}