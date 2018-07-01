#include "VolumeRenderer.glsl"

layout(location = 0) out float4 OutColor;

in VS_OUTPUT_GLOW_9
{
//    float4 position    : SV_Position;   // vertex position
    float2 textureM4   ;  // kernel tap texture coords
    float2 textureM3   ;  // kernel tap texture coords
    float2 textureM2   ;  // kernel tap texture coords
    float2 textureM1   ;  // kernel tap texture coords
    float2 texture0    ;  // kernel tap texture coords
    float2 textureP1   ;  // kernel tap texture coords
    float2 textureP2   ;  // kernel tap texture coords
    float2 textureP3   ;  // kernel tap texture coords
    float2 textureP4   ;  // kernel tap texture coords
}vIn;

void main()
{
    float4 col = float4(0,0,0,0);
    float4 tex;
    float threshold = 1.4;

    tex = texture(glowTex, vIn.textureM4);  //samPointClamp
    if(tex.r > threshold)
        col += tex*gaussian_3[4];
    
    tex = texture(glowTex, vIn.textureM3); 
    if(tex.r > threshold)
        col += tex*gaussian_3[3];
        
    tex = texture(glowTex, vIn.textureM2); 
    if(tex.r > threshold)
        col += tex*gaussian_3[2]; 
        
    tex = texture(glowTex, vIn.textureM1); 
    if(tex.r > threshold)
        col += tex*gaussian_3[1]; 
        
    tex = texture(glowTex, vIn.texture0); 
    if(tex.r > threshold)
        col += tex*gaussian_3[0];
        
    tex = texture(glowTex, vIn.textureP1); 
    if(tex.r > threshold)
        col += tex*gaussian_3[1]; 
      
    tex = texture(glowTex, vIn.textureP2); 
    if(tex.r > threshold)
        col += tex*gaussian_3[2]; 

    tex = texture(glowTex, vIn.textureP3); 
    if(tex.r > threshold)
        col += tex*gaussian_3[3];
       
    tex = texture(glowTex, vIn.textureP4); 
    if(tex.r > threshold)
        col += tex*gaussian_3[4];
       
    OutColor = col;
}