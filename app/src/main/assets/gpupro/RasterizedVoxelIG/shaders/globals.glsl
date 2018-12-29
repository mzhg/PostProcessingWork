#include "../../../shader_libs/PostProcessingHLSLCompatiable.glsl"

#define CAMERA_UB_BP 0
#define LIGHT_UB_BP 1
#define CUSTOM_UB_BP 2

struct CameraCB
{
   mat4 viewMatrix;
   mat4 invTransposeViewMatrix;
   mat4 projMatrix;
   mat4 viewProjMatrix;
   float4 frustumRays[4];
   float3 position;
   float nearClipDistance;
   float farClipDistance;
   float nearFarClipDistance;
};

layout(binding = CAMERA_UB_BP) uniform CAMERA_UB  //: register(CAMERA_UB_BP)
 {
   CameraCB cameraUB;
 };

struct PointCB
{
    float3 position;
    float radius;
    float4 color;
    mat4 worldMatrix;
    float multiplier;
};

 layout(binding = LIGHT_UB_BP) uniform POINT_LIGHT_UB  //: register(LIGHT_UB_BP) \
 {
    PointCB pointUB;
 };

 struct DirectionalCB
 {
    float3 direction;
     float multiplier;
     float4 color;
     mat4 shadowViewProjMatrix;
     mat4 shadowViewProjTexMatrix;
     float invShadowMapSize;
 };

 layout(binding = LIGHT_UB_BP) uniform DIRECTIONAL_LIGHT_UB
 {
    DirectionalCB dirUB;
 };

#define COLOR_TEX_BP 0
#define NORMAL_TEX_BP 1
#define SPECULAR_TEX_BP 2
#define CUSTOM0_TEX_BP 3
#define CUSTOM1_TEX_BP 4
#define CUSTOM2_TEX_BP 5
#define CUSTOM3_TEX_BP 6
#define CUSTOM4_TEX_BP 7
#define CUSTOM5_TEX_BP 8

#define CUSTOM0_SB_BP 9
#define CUSTOM1_SB_BP 10

#define COLOR_SAM_BP 0
#define NORMAL_SAM_BP 1
#define SPECULAR_SAM_BP 2
#define CUSTOM0_SAM_BP 3
#define CUSTOM1_SAM_BP 4
#define CUSTOM2_SAM_BP 5
#define CUSTOM3_SAM_BP 6
#define CUSTOM4_SAM_BP 7
#define CUSTOM5_SAM_BP 8