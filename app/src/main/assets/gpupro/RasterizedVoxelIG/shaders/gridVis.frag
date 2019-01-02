#include "globals.glsl"
#include "globalIllum.glsl"

/*Texture2D normalMap: register(NORMAL_TEX_BP); // normalDepth
SamplerState normalMapSampler: register(NORMAL_SAM_BP);*/
layout(binding = NORMAL_TEX_BP) uniform sampler2D normalMap;

/*StructuredBuffer<VOXEL> fineGridBuffer: register(CUSTOM0_SB_BP);
StructuredBuffer<VOXEL> coarseGridBuffer: register(CUSTOM1_SB_BP);*/

layout(binding = CUSTOM0_SB_BP) buffer StructuredBuffer0
{
   VOXEL fineGridBuffer[];
};

layout(binding = CUSTOM1_SB_BP) buffer StructuredBuffer1
{
   VOXEL coarseGridBuffer[];
};

//GLOBAL_CAMERA_UB(cameraUB);

/*cbuffer CUSTOM_UB: register(CUSTOM_UB_BP)
{
	struct
	{
    matrix gridViewProjMatrices[6];
		float4 gridCellSizes;
	  float4 gridPositions[2];
		float4 snappedGridPositions[2];
	}customUB;
};*/
struct CustomCB
{
    mat4 gridViewProjMatrices[6];
    float4 gridCellSizes;
	float4 gridPositions[2];
    float4 snappedGridPositions[2];
};

layout(binding = CUSTOM_UB_BP) uniform CUSTOM_UB
{
    CustomCB customUB;
};

in GS_OUTPUT
{
  float4 position/*: SV_POSITION*/;
  float2 texCoords/*: TEXCOORD*/;
	float3 frustumRay/*: FRUSTUM_RAY*/;
}_input;

/*struct FS_OUTPUT
{
  float4 fragColor: SV_TARGET;
};*/

out float4 fragColor;

// reconstruct world-space position from depth
float4 DecodePosition(in float depth,in float3 frustumRay)
{
  float4 position;
	float3 frustumRayN = normalize(frustumRay);
	position.xyz = cameraUB.position+(frustumRayN*depth);
	position.w = 1.0f;
	return position;
}

//FS_OUTPUT main(GS_OUTPUT input)
void main()
{
//  FS_OUTPUT output;
	float4 bumpDepth = texture(normalMap,_input.texCoords);  // normalMapSampler
	float4 position = DecodePosition(bumpDepth.w,_input.frustumRay);

	// find for the current pixel best voxel representation
	int gridRes = 0;
	float3 offset = (position.xyz-customUB.snappedGridPositions[0].xyz)*customUB.gridCellSizes.y;
	float dist = length(offset);
	if(dist>15.0f)
	{
    offset = (position.xyz-customUB.snappedGridPositions[1].xyz)*customUB.gridCellSizes.w;
    dist = length(offset);
		gridRes = (dist < 15.0f) ? 1 : 2;
	}

	float3 color = float3(0.5f,0.5f,0.5f);

	// if voxel could be retrieved, get color
	if(gridRes < 2)
	{
		// get index of current voxel
		offset = round(offset);
		int3 voxelPos = int3(16,16,16)+int3(offset.x,offset.y,offset.z);
		if((voxelPos.x<0)||(voxelPos.x>31)||(voxelPos.y<0)||(voxelPos.y>31)||(voxelPos.z<0)||(voxelPos.z>31))
			discard;
		int gridIndex = GetGridIndex(voxelPos);

		// get voxel
		VOXEL voxel;
		if(gridRes==0)
			voxel = fineGridBuffer[gridIndex];
		else
			voxel = coarseGridBuffer[gridIndex];

		// decode color
	  color = DecodeColor(voxel.colorMask);
	}

	fragColor = float4(color,1.0f);
}