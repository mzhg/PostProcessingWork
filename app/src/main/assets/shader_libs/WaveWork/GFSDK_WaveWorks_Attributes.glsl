#include "GFSDK_WaveWorks_Quadtree.glsl"

layout(binding=1) uniform attr_vs_buffer
{
    float3 g_WorldEye;
    float  g_UseTextureArrays;
    float4 g_UVScaleCascade0123;
};

layout(binding=0) uniform sampler2D g_samplerDisplacementMap0;
layout(binding=1) uniform sampler2D g_samplerDisplacementMap1;
layout(binding=2) uniform sampler2D g_samplerDisplacementMap2;
layout(binding=3) uniform sampler2D g_samplerDisplacementMap3;
layout(binding=0) uniform sampler2DArray g_samplerDisplacementMapTextureArray;

layout(binding=4) uniform attr_ps_buffer
{
    float g_TexelLength_x2_PS;
    float g_Cascade1Scale_PS;
    float g_Cascade1TexelScale_PS;
    float g_Cascade1UVOffset_PS;
    float g_Cascade2Scale_PS;
    float g_Cascade2TexelScale_PS;
    float g_Cascade2UVOffset_PS;
    float g_Cascade3Scale_PS;
    float g_Cascade3TexelScale_PS;
    float g_Cascade3UVOffset_PS;
};

layout(binding=4) uniform sampler2D g_samplerGradientMap0;
layout(binding=5) uniform sampler2D g_samplerGradientMap1;
layout(binding=6) uniform sampler2D g_samplerGradientMap2;
layout(binding=7) uniform sampler2D g_samplerGradientMap3;
layout(binding=1) uniform sampler2DArray g_samplerGradientMapTextureArray;

struct GFSDK_WAVEWORKS_INTERPOLATED_VERTEX_OUTPUT
{
    float4 tex_coord_cascade01			/*SEMANTIC(TEXCOORD0)*/;
    float4 tex_coord_cascade23			/*SEMANTIC(TEXCOORD1)*/;
    float4 blend_factor_cascade0123	/*SEMANTIC(TEXCOORD2)*/;
    float3 eye_vec						/*SEMANTIC(TEXCOORD3)*/;
};

struct GFSDK_WAVEWORKS_VERTEX_OUTPUT
{
    /*centroid*/ GFSDK_WAVEWORKS_INTERPOLATED_VERTEX_OUTPUT interp;
    float3 pos_world;
    float3 pos_world_undisplaced;
    float3 world_displacement;
};

GFSDK_WAVEWORKS_VERTEX_OUTPUT GFSDK_WaveWorks_GetDisplacedVertex(float4 In_vPos)
{
	// Get starting position and distance to camera
	float3 pos_world_undisplaced = GFSDK_WaveWorks_GetUndisplacedVertexWorldPosition(In_vPos);
	float  distance = length(g_WorldEye - pos_world_undisplaced);

	// UVs
	float2 uv_world_cascade0 = pos_world_undisplaced.xy * g_UVScaleCascade0123.x;
	float2 uv_world_cascade1 = pos_world_undisplaced.xy * g_UVScaleCascade0123.y;
	float2 uv_world_cascade2 = pos_world_undisplaced.xy * g_UVScaleCascade0123.z;
	float2 uv_world_cascade3 = pos_world_undisplaced.xy * g_UVScaleCascade0123.w;

	// cascade blend factors
	float4 blendfactors;
	float4 cascade_spatial_size = 1.0/g_UVScaleCascade0123.xyzw;
	blendfactors.x = 1.0;
	blendfactors.yzw = saturate(0.25*(cascade_spatial_size.yzw*24.0-distance)/cascade_spatial_size.yzw);
	blendfactors.yzw *= blendfactors.yzw;


	// Displacement map
	#if defined(GFSDK_WAVEWORKS_GL)
		float3 displacement;
		if(g_UseTextureArrays > 0)
		{
			displacement =  blendfactors.x * SampleTex2Dlod(g_textureArrayDisplacementMap, g_samplerDisplacementMapTextureArray, vec3(uv_world_cascade0, 0.0), 0).xyz;
			displacement += blendfactors.y==0? float3(0,0,0) : blendfactors.y * SampleTex2Dlod(g_textureArrayDisplacementMap, g_samplerDisplacementMapTextureArray, vec3(uv_world_cascade1, 1.0), 0).xyz;
			displacement += blendfactors.z==0? float3(0,0,0) : blendfactors.z * SampleTex2Dlod(g_textureArrayDisplacementMap, g_samplerDisplacementMapTextureArray, vec3(uv_world_cascade2, 2.0), 0).xyz;
			displacement += blendfactors.w==0? float3(0,0,0) : blendfactors.w * SampleTex2Dlod(g_textureArrayDisplacementMap, g_samplerDisplacementMapTextureArray, vec3(uv_world_cascade3, 3.0), 0).xyz;
		}
		else
		{
			displacement =  blendfactors.x * SampleTex2Dlod(g_textureDisplacementMap0, g_samplerDisplacementMap0, uv_world_cascade0, 0).xyz;
			displacement += blendfactors.y==0? float3(0,0,0) : blendfactors.y * SampleTex2Dlod(g_textureDisplacementMap1, g_samplerDisplacementMap1, uv_world_cascade1, 0).xyz;
			displacement += blendfactors.z==0? float3(0,0,0) : blendfactors.z * SampleTex2Dlod(g_textureDisplacementMap2, g_samplerDisplacementMap2, uv_world_cascade2, 0).xyz;
			displacement += blendfactors.w==0? float3(0,0,0) : blendfactors.w * SampleTex2Dlod(g_textureDisplacementMap3, g_samplerDisplacementMap3, uv_world_cascade3, 0).xyz;
		}
	#else
		float3 displacement =  blendfactors.x * textureLod(/*g_textureDisplacementMap0,*/ g_samplerDisplacementMap0, uv_world_cascade0, 0).xyz;
			   displacement += blendfactors.y==0? float3(0,0,0) : blendfactors.y * textureLod(/*g_textureDisplacementMap1,*/ g_samplerDisplacementMap1, uv_world_cascade1, 0).xyz;
			   displacement += blendfactors.z==0? float3(0,0,0) : blendfactors.z * textureLod(/*g_textureDisplacementMap2,*/ g_samplerDisplacementMap2, uv_world_cascade2, 0).xyz;
			   displacement += blendfactors.w==0? float3(0,0,0) : blendfactors.w * textureLod(/*g_textureDisplacementMap3,*/ g_samplerDisplacementMap3, uv_world_cascade3, 0).xyz;
	#endif

	float3 pos_world = pos_world_undisplaced + displacement;

	// Output
	GFSDK_WAVEWORKS_VERTEX_OUTPUT Output;
	Output.interp.eye_vec = g_WorldEye - pos_world;
	Output.interp.tex_coord_cascade01.xy = uv_world_cascade0;
	Output.interp.tex_coord_cascade01.zw = uv_world_cascade1;
	Output.interp.tex_coord_cascade23.xy = uv_world_cascade2;
	Output.interp.tex_coord_cascade23.zw = uv_world_cascade3;
	Output.interp.blend_factor_cascade0123 = blendfactors;
	Output.pos_world = pos_world;
	Output.pos_world_undisplaced = pos_world_undisplaced;
	Output.world_displacement = displacement;
	return Output;
}

GFSDK_WAVEWORKS_VERTEX_OUTPUT GFSDK_WaveWorks_GetDisplacedVertexAfterTessellation(float4 In0, float4 In1, float4 In2, float3 BarycentricCoords)
{
	// Get starting position
	float3 tessellated_ws_position =	In0.xyz * BarycentricCoords.x +
											In1.xyz * BarycentricCoords.y +
											In2.xyz * BarycentricCoords.z;
	float3 pos_world_undisplaced = tessellated_ws_position;


	// blend factors for cascades
	float4 blendfactors;
	float distance = length(g_WorldEye - pos_world_undisplaced);
	float4 cascade_spatial_size = 1.0/g_UVScaleCascade0123.xyzw;
	blendfactors.x = 1.0;
	blendfactors.yzw = saturate(0.25*(cascade_spatial_size.yzw*24.0-distance)/cascade_spatial_size.yzw);
	blendfactors.yzw *= blendfactors.yzw;

	// UVs
	float2 uv_world_cascade0 = pos_world_undisplaced.xy * g_UVScaleCascade0123.x;
	float2 uv_world_cascade1 = pos_world_undisplaced.xy * g_UVScaleCascade0123.y;
	float2 uv_world_cascade2 = pos_world_undisplaced.xy * g_UVScaleCascade0123.z;
	float2 uv_world_cascade3 = pos_world_undisplaced.xy * g_UVScaleCascade0123.w;

	// Displacement map
	#if defined(GFSDK_WAVEWORKS_GL)
		float3 displacement;
		if(g_UseTextureArrays > 0)
		{
			displacement =  blendfactors.x * SampleTex2Dlod(g_textureArrayDisplacementMap, g_samplerDisplacementMapTextureArray, vec3(uv_world_cascade0, 0.0), 0).xyz;
			displacement += blendfactors.y==0? float3(0,0,0) : blendfactors.y * SampleTex2Dlod(g_textureArrayDisplacementMap, g_samplerDisplacementMapTextureArray, vec3(uv_world_cascade1, 1.0), 0).xyz;
			displacement += blendfactors.z==0? float3(0,0,0) : blendfactors.z * SampleTex2Dlod(g_textureArrayDisplacementMap, g_samplerDisplacementMapTextureArray, vec3(uv_world_cascade2, 2.0), 0).xyz;
			displacement += blendfactors.w==0? float3(0,0,0) : blendfactors.w * SampleTex2Dlod(g_textureArrayDisplacementMap, g_samplerDisplacementMapTextureArray, vec3(uv_world_cascade3, 3.0), 0).xyz;
		}
		else
		{
			displacement =  blendfactors.x * SampleTex2Dlod(g_textureDisplacementMap0, g_samplerDisplacementMap0, uv_world_cascade0, 0).xyz;
			displacement += blendfactors.y==0? float3(0,0,0) : blendfactors.y * SampleTex2Dlod(g_textureDisplacementMap1, g_samplerDisplacementMap1, uv_world_cascade1, 0).xyz;
			displacement += blendfactors.z==0? float3(0,0,0) : blendfactors.z * SampleTex2Dlod(g_textureDisplacementMap2, g_samplerDisplacementMap2, uv_world_cascade2, 0).xyz;
			displacement += blendfactors.w==0? float3(0,0,0) : blendfactors.w * SampleTex2Dlod(g_textureDisplacementMap3, g_samplerDisplacementMap3, uv_world_cascade3, 0).xyz;
		}
	#else
		float3 displacement =  blendfactors.x * textureLod(/*g_textureDisplacementMap0,*/ g_samplerDisplacementMap0, uv_world_cascade0, 0).xyz;
			   displacement += blendfactors.y==0? float3(0,0,0) : blendfactors.y * textureLod(/*g_textureDisplacementMap1,*/ g_samplerDisplacementMap1, uv_world_cascade1, 0).xyz;
			   displacement += blendfactors.z==0? float3(0,0,0) : blendfactors.z * textureLod(/*g_textureDisplacementMap2,*/ g_samplerDisplacementMap2, uv_world_cascade2, 0).xyz;
			   displacement += blendfactors.w==0? float3(0,0,0) : blendfactors.w * textureLod(/*g_textureDisplacementMap3,*/ g_samplerDisplacementMap3, uv_world_cascade3, 0).xyz;
	#endif

	float3 pos_world = pos_world_undisplaced + displacement;

	// Output
	GFSDK_WAVEWORKS_VERTEX_OUTPUT Output;
	Output.interp.eye_vec = g_WorldEye - pos_world;
	Output.interp.tex_coord_cascade01.xy = uv_world_cascade0;
	Output.interp.tex_coord_cascade01.zw = uv_world_cascade1;
	Output.interp.tex_coord_cascade23.xy = uv_world_cascade2;
	Output.interp.tex_coord_cascade23.zw = uv_world_cascade3;
	Output.interp.blend_factor_cascade0123 = blendfactors;
	Output.pos_world = pos_world;
	Output.pos_world_undisplaced = pos_world_undisplaced;
	Output.world_displacement = displacement;
	return Output;
}

GFSDK_WAVEWORKS_VERTEX_OUTPUT GFSDK_WaveWorks_GetDisplacedVertexAfterTessellationQuad(float4 In0, float4 In1, float4 In2, float4 In3, float2 UV)
{
	// Get starting position
	float3 tessellated_ws_position =	In2.xyz*UV.x*UV.y +
											In0.xyz*(1.0-UV.x)*UV.y +
											In1.xyz*(1.0-UV.x)*(1.0-UV.y) +
											In3.xyz*UV.x*(1.0-UV.y);
	float3 pos_world_undisplaced = tessellated_ws_position;

	// blend factors for cascades
	float4 blendfactors;
	float distance = length(g_WorldEye - pos_world_undisplaced);
	float4 cascade_spatial_size = 1.0/g_UVScaleCascade0123.xyzw;
	blendfactors.x = 1.0;
	blendfactors.yzw = saturate(0.25*(cascade_spatial_size.yzw*24.0-distance)/cascade_spatial_size.yzw);
	blendfactors.yzw *= blendfactors.yzw;

	// UVs
	float2 uv_world_cascade0 = pos_world_undisplaced.xy * g_UVScaleCascade0123.x;
	float2 uv_world_cascade1 = pos_world_undisplaced.xy * g_UVScaleCascade0123.y;
	float2 uv_world_cascade2 = pos_world_undisplaced.xy * g_UVScaleCascade0123.z;
	float2 uv_world_cascade3 = pos_world_undisplaced.xy * g_UVScaleCascade0123.w;

	// Displacement map
	#if defined(GFSDK_WAVEWORKS_GL)
		float3 displacement;
		if(g_UseTextureArrays > 0)
		{
			displacement =  blendfactors.x * SampleTex2Dlod(g_textureArrayDisplacementMap, g_samplerDisplacementMapTextureArray, vec3(uv_world_cascade0, 0.0), 0).xyz;
			displacement += blendfactors.y==0? float3(0,0,0) : blendfactors.y * SampleTex2Dlod(g_textureArrayDisplacementMap, g_samplerDisplacementMapTextureArray, vec3(uv_world_cascade1, 1.0), 0).xyz;
			displacement += blendfactors.z==0? float3(0,0,0) : blendfactors.z * SampleTex2Dlod(g_textureArrayDisplacementMap, g_samplerDisplacementMapTextureArray, vec3(uv_world_cascade2, 2.0), 0).xyz;
			displacement += blendfactors.w==0? float3(0,0,0) : blendfactors.w * SampleTex2Dlod(g_textureArrayDisplacementMap, g_samplerDisplacementMapTextureArray, vec3(uv_world_cascade3, 3.0), 0).xyz;
		}
		else
		{
			displacement =  blendfactors.x * SampleTex2Dlod(g_textureDisplacementMap0, g_samplerDisplacementMap0, uv_world_cascade0, 0).xyz;
			displacement += blendfactors.y==0? float3(0,0,0) : blendfactors.y * SampleTex2Dlod(g_textureDisplacementMap1, g_samplerDisplacementMap1, uv_world_cascade1, 0).xyz;
			displacement += blendfactors.z==0? float3(0,0,0) : blendfactors.z * SampleTex2Dlod(g_textureDisplacementMap2, g_samplerDisplacementMap2, uv_world_cascade2, 0).xyz;
			displacement += blendfactors.w==0? float3(0,0,0) : blendfactors.w * SampleTex2Dlod(g_textureDisplacementMap3, g_samplerDisplacementMap3, uv_world_cascade3, 0).xyz;
		}
	#else
		float3 displacement =  blendfactors.x * textureLod(/*g_textureDisplacementMap0,*/ g_samplerDisplacementMap0, uv_world_cascade0, 0).xyz;
			   displacement += blendfactors.y==0? float3(0,0,0) : blendfactors.y * textureLod(/*g_textureDisplacementMap1,*/ g_samplerDisplacementMap1, uv_world_cascade1, 0).xyz;
			   displacement += blendfactors.z==0? float3(0,0,0) : blendfactors.z * textureLod(/*g_textureDisplacementMap2,*/ g_samplerDisplacementMap2, uv_world_cascade2, 0).xyz;
			   displacement += blendfactors.w==0? float3(0,0,0) : blendfactors.w * textureLod(/*g_textureDisplacementMap3,*/ g_samplerDisplacementMap3, uv_world_cascade3, 0).xyz;
	#endif

	float3 pos_world = pos_world_undisplaced + displacement;

	// Output
	GFSDK_WAVEWORKS_VERTEX_OUTPUT Output;
	Output.interp.eye_vec = g_WorldEye - pos_world;
	Output.interp.tex_coord_cascade01.xy = uv_world_cascade0;
	Output.interp.tex_coord_cascade01.zw = uv_world_cascade1;
	Output.interp.tex_coord_cascade23.xy = uv_world_cascade2;
	Output.interp.tex_coord_cascade23.zw = uv_world_cascade3;
	Output.interp.blend_factor_cascade0123 = blendfactors;
	Output.pos_world = pos_world;
	Output.pos_world_undisplaced = pos_world_undisplaced;
	Output.world_displacement = displacement;
	return Output;
}

struct GFSDK_WAVEWORKS_SURFACE_ATTRIBUTES
{
	float3 normal;
	float3 eye_dir;
	float foam_surface_folding;
	float foam_turbulent_energy;
	float foam_wave_hats;
};

GFSDK_WAVEWORKS_SURFACE_ATTRIBUTES GFSDK_WaveWorks_GetSurfaceAttributes(GFSDK_WAVEWORKS_INTERPOLATED_VERTEX_OUTPUT In)
{
	// Calculate eye vector.
	// Beware: 'eye_vec' is a large number, 32bit floating point required.
	float3 eye_dir = normalize(In.eye_vec);

	// --------------- Water body color

	float4 grad_fold0;
	float4 grad_fold1;
	float4 grad_fold2;
	float4 grad_fold3;

	#if defined(GFSDK_WAVEWORKS_GL)
		float3 displacement;
		if(g_UseTextureArrays > 0)
		{
			grad_fold0 = SampleTex2D(g_textureArrayGradientMap, g_samplerGradientMapTextureArray, vec3(In.tex_coord_cascade01.xy, 0.0));
			grad_fold1 = SampleTex2D(g_textureArrayGradientMap, g_samplerGradientMapTextureArray, vec3(In.tex_coord_cascade01.zw, 1.0));
			grad_fold2 = SampleTex2D(g_textureArrayGradientMap, g_samplerGradientMapTextureArray, vec3(In.tex_coord_cascade23.xy, 2.0));
			grad_fold3 = SampleTex2D(g_textureArrayGradientMap, g_samplerGradientMapTextureArray, vec3(In.tex_coord_cascade23.zw, 3.0));
		}
		else
		{
			grad_fold0 = SampleTex2D(g_textureGradientMap0, g_samplerGradientMap0, In.tex_coord_cascade01.xy);
			grad_fold1 = SampleTex2D(g_textureGradientMap1, g_samplerGradientMap1, In.tex_coord_cascade01.zw);
			grad_fold2 = SampleTex2D(g_textureGradientMap2, g_samplerGradientMap2, In.tex_coord_cascade23.xy);
			grad_fold3 = SampleTex2D(g_textureGradientMap3, g_samplerGradientMap3, In.tex_coord_cascade23.zw);
		}
	#else

		grad_fold0 = texture(/*g_textureGradientMap0,*/ g_samplerGradientMap0, In.tex_coord_cascade01.xy);
		grad_fold1 = texture(/*g_textureGradientMap1,*/ g_samplerGradientMap1, In.tex_coord_cascade01.zw);
		grad_fold2 = texture(/*g_textureGradientMap2,*/ g_samplerGradientMap2, In.tex_coord_cascade23.xy);
		grad_fold3 = texture(/*g_textureGradientMap3,*/ g_samplerGradientMap3, In.tex_coord_cascade23.zw);
	#endif

	float2 grad;
	grad.xy = grad_fold0.xy*In.blend_factor_cascade0123.x +
				   grad_fold1.xy*In.blend_factor_cascade0123.y*g_Cascade1TexelScale_PS +
				   grad_fold2.xy*In.blend_factor_cascade0123.z*g_Cascade2TexelScale_PS +
				   grad_fold3.xy*In.blend_factor_cascade0123.w*g_Cascade3TexelScale_PS;

	float c2c_scale = 0.25; // larger cascaded cover larger areas, so foamed texels cover larger area, thus, foam intensity on these needs to be scaled down for uniform foam look

	float foam_turbulent_energy =
					  // accumulated foam energy with blendfactors
					  100.0*grad_fold0.w *
					  lerp(c2c_scale, grad_fold1.w, In.blend_factor_cascade0123.y)*
					  lerp(c2c_scale, grad_fold2.w, In.blend_factor_cascade0123.z)*
					  lerp(c2c_scale, grad_fold3.w, In.blend_factor_cascade0123.w);


	float foam_surface_folding =
						// folding for foam "clumping" on folded areas
    				   max(-100,
					  (1.0-grad_fold0.z) +
					  (1.0-grad_fold1.z) +
					  (1.0-grad_fold2.z) +
					  (1.0-grad_fold3.z));

	// Calculate normal here.
	float3 normal = normalize(float3(grad.xy, g_TexelLength_x2_PS));

	float hats_c2c_scale = 0.5;		// the larger is the wave, the higher is the chance to start breaking at high folding, so folding for smaller cascade s is decreased
	float foam_wave_hats =
      				   10.0*(-0.55 + // this allows hats to appear on breaking places only. Can be tweaked to represent Beaufort scale better
					  (1.0-grad_fold0.z) +
					  hats_c2c_scale*(1.0-grad_fold1.z) +
					  hats_c2c_scale*hats_c2c_scale*(1.0-grad_fold2.z) +
					  hats_c2c_scale*hats_c2c_scale*hats_c2c_scale*(1.0-grad_fold3.z));


	// Output
	GFSDK_WAVEWORKS_SURFACE_ATTRIBUTES Output;
	Output.normal = normal;
	Output.eye_dir = eye_dir;
	Output.foam_surface_folding = foam_surface_folding;
	Output.foam_turbulent_energy = log(1.0 + foam_turbulent_energy);
	Output.foam_wave_hats = foam_wave_hats;
	return Output;
}
