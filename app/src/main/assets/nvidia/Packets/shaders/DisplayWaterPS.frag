
#include "WavePackets.glsl"

out vec4 oColor;

in vec2 Tex;
in vec3 Pos; // world space position

void main()
{
    // the derivative of the water displacement texture gives us the water surface normal
    float3 pos = textureLod(g_waterPosTex, Tex.xy, 0).xyz + textureLod(g_waterHeightTex, Tex.xy, 0).xyz;  // LinearSampler
    float3 nVec = cross(ddx(pos), -ddy(pos));
    if (dot(nVec, nVec) <= 0)
        nVec = float3(0, -1, 0);
    else
        nVec = normalize(nVec);
    float3 vDir = normalize(In.Pos - g_mWorld[3].xyz);	// view vector
    float3 rDir = vDir - (2.0*dot(vDir, nVec))*nVec;	// reflection vector
    // diffuse/reflective lighting
    float3 color = float3(0.5, 0.6, 0.8);
    float fac = 1.0 - (1.0 - abs(nVec.y) + abs(rDir.y))*(1.0 - abs(nVec.y) + abs(rDir.y));
    Out.oColor.xyz = fac*fac*float3(0.5, 0.6, 0.8);
    // add few specular glares
    const float3 glareDir1 = normalize(float3(-1, -0.75, 1));
    const float3 glareDir2 = normalize(float3(1, -0.75, -1));
    const float3 glareDir3 = normalize(float3(1, -0.75, 1));
    const float3 glareDir4 = normalize(float3(-1, -0.75, -1));
    const float3 glareDir5 = normalize(float3(0, -1, 0));
    oColor.xyz += 100.0*pow(max(dot(-rDir, glareDir5), max(dot(-rDir, glareDir4), max(dot(-rDir, glareDir3), max(dot(-rDir, glareDir2), max(0.0, dot(-rDir, glareDir1)))))), 5000);
    // grid overlay
    float floorFactor = 1.0;
    float sth = 0.06;
    float posfac = 1.2*80.0 / SCENE_EXTENT;
    if (frac(posfac*pos.x) < sth)
        floorFactor = 0.5 - 0.5*cos(-PI + 2.0*PI*frac(posfac*pos.x)/ sth);
    if (frac(posfac*pos.z) < sth)
        floorFactor = min(floorFactor, 0.5 - 0.5*cos(-PI + 2.0*PI*frac(posfac*pos.z) / sth));
    oColor.xyz *= (0.75+0.25*floorFactor);
    float waterDepth = 1.0 + 0.9*pow(textureLod(g_waterTerrainTex, Pos.xz / SCENE_EXTENT + float2(0.5, 0.5), 0).z, 4);
    oColor.xyz = waterDepth*Out.oColor.xyz;
    oColor.w = 1.0;
}