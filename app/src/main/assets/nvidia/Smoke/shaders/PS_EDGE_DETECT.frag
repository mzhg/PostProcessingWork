#include "VolumeRenderer.glsl"

layout(location = 0) out float4 OutColor;

in VS_OUTPUT_EDGE
{
    // There's no textureUV11 because its weight is zero.
//    float4 position      : SV_Position;   // vertex position
    float2 textureUV00   ;  // kernel tap texture coords
    float2 textureUV01   ;  // kernel tap texture coords
    float2 textureUV02   ;  // kernel tap texture coords
    float2 textureUV10   ;  // kernel tap texture coords
    float2 textureUV12   ;  // kernel tap texture coords
    float2 textureUV20   ;  // kernel tap texture coords
    float2 textureUV21   ;  // kernel tap texture coords
    float2 textureUV22   ;  // kernel tap texture coords
}vIn;

//
// A full-screen edge detection pass to locate artifacts
//  these artifacts are located on a downsized version of the rayDataTexture
// We use a smaller texture both to accurately find all the depth artifacts
//  when raycasting to this smaller size and to save on the cost of this pass
// Use col.a to find depth edges of objects occluding the smoke
// Use col.g to find the edges where the camera near plane cuts the smoke volume
//
void main()
{
    // We need eight samples (the centre has zero weight in both kernels).
    float4 col;
    col = texture(rayDataTexSmall, vIn.textureUV00);   //samPointClamp
    float g00 = col.a;
    if(col.g < 0)
        g00 *= -1;
    col = texture(rayDataTexSmall, vIn.textureUV01); 
    float g01 = col.a;
    if(col.g < 0)
        g01 *= -1;
    col = texture(rayDataTexSmall, vIn.textureUV02); 
    float g02 = col.a;
    if(col.g < 0)
        g02 *= -1;
    col = texture(rayDataTexSmall, vIn.textureUV10); 
    float g10 = col.a;
    if(col.g < 0)
        g10 *= -1;
    col = texture(rayDataTexSmall, vIn.textureUV12); 
    float g12 = col.a;
    if(col.g < 0)
        g12 *= -1;
    col = texture(rayDataTexSmall, vIn.textureUV20); 
    float g20 = col.a;
    if(col.g < 0)
        g20 *= -1;
    col = texture(rayDataTexSmall, vIn.textureUV21); 
    float g21 = col.a;
    if(col.g < 0)
        g21 *= -1;
    col = texture(rayDataTexSmall, vIn.textureUV22); 
    float g22 = col.a;
    if(col.g < 0)
        g22 *= -1;
        
    // Sobel in horizontal dir.
    float sx = 0;
    sx -= g00;
    sx -= g01 * 2;
    sx -= g02;
    sx += g20;
    sx += g21 * 2;
    sx += g22;
    // Sobel in vertical dir - weights are just rotated 90 degrees.
    float sy = 0;
    sy -= g00;
    sy += g02;
    sy -= g10 * 2;
    sy += g12 * 2;
    sy -= g20;
    sy += g22;

    float e = EdgeDetectScalar(sx, sy, edgeThreshold);
    OutColor = float4(e,e,e,1);
}