float3 GetAdvectedPosTexCoords(/*GS_OUTPUT_FLUIDSIM input*/)
{
    float3 pos = _input.cell0;

    pos -= timestep *
//        Texture_velocity.SampleLevel( samPointClamp, _input.texcoords, 0 ).xyz;
          textureLod(Texture_velocity, _input.texcoords, 0.).xyz;

    return float3(pos.x/textureWidth, pos.y/textureHeight, (pos.z+0.5)/textureDepth);
}

// texture3d.SampleLevel( samPointClamp
#define SAMPLE_NEIGHBORS( texture3d/*, input*/ ) \
    float L = textureLod(texture3d, LEFTCELL, 0.).r;     \
    float R = textureLod(texture3d, RIGHTCELL, 0.).r;    \
    float B = textureLod(texture3d, BOTTOMCELL, 0.).r;   \
    float T = textureLod(texture3d, TOPCELL, 0.).r;      \
    float D = textureLod(texture3d, DOWNCELL, 0.).r;     \
    float U = textureLod(texture3d, UPCELL, 0.).r;

float3 Gradient( Texture3D texture3d/*, VS_OUTPUT_FLUIDSIM input*/ )
{
    SAMPLE_NEIGHBORS( texture3d/*, input*/ );
    return 0.5f * float3(R - L,  T - B,  U - D);
}

float3 Gradient( Texture3D texture3d/*, GS_OUTPUT_FLUIDSIM input */)
{
    SAMPLE_NEIGHBORS( texture3d/*, input*/ );
    return 0.5f * float3(R - L,  T - B,  U - D);
}

float3 Gradient( Texture3D texture3d, /*GS_OUTPUT_FLUIDSIM input,*/ out bool isBoundary, float minSlope, out bool highEnoughSlope )
{
    SAMPLE_NEIGHBORS( texture3d/*, input*/ );

//    texture3d.SampleLevel( samPointClamp
    float LBD = textureLod(texture3d, float3 (_input.LR.x, _input.BT.x, _input.DU.x), 0).r;
    float LBC = textureLod(texture3d, float3 (_input.LR.x, _input.BT.x, _input.texcoords.z), 0).r;
    float LBU = textureLod(texture3d, float3 (_input.LR.x, _input.BT.x, _input.DU.y), 0).r;
    float LCD = textureLod(texture3d, float3 (_input.LR.x, _input.texcoords.y, _input.DU.x), 0).r;
    //float LCC = textureLod(texture3d, float3 (input.LR.x, input.texcoords.y, input.texcoords.z), 0).r;
    float LCU = textureLod(texture3d, float3 (_input.LR.x, _input.texcoords.y, _input.DU.y), 0).r;
    float LTD = textureLod(texture3d, float3 (_input.LR.x, _input.BT.y, _input.DU.x), 0).r;
    float LTC = textureLod(texture3d, float3 (_input.LR.x, _input.BT.y, _input.texcoords.z), 0).r;
    float LTU = textureLod(texture3d, float3 (_input.LR.x, _input.BT.y, _input.DU.y), 0).r;

    float CBD = textureLod(texture3d, float3 (_input.texcoords.x, _input.BT.x, _input.DU.x), 0).r;
    //float CBC = textureLod(texture3d, float3 (input.texcoords.x, input.BT.x, input.texcoords.z), 0).r;
    float CBU = textureLod(texture3d, float3 (_input.texcoords.x, _input.BT.x, _input.DU.y), 0).r;
    //float CCD = textureLod(texture3d, float3 (input.texcoords.x, input.texcoords.y, input.DU.x), 0).r;
    float CCC = textureLod(texture3d, float3 (_input.texcoords.x, _input.texcoords.y, _input.texcoords.z), 0).r;
    //float CCU = textureLod(texture3d, float3 (input.texcoords.x, input.texcoords.y, input.DU.y), 0).r;
    float CTD = textureLod(texture3d, float3 (_input.texcoords.x, _input.BT.y, _input.DU.x), 0).r;
    //float CTC = textureLod(texture3d, float3 (input.texcoords.x, input.BT.y, input.texcoords.z)), 0).r;
    float CTU = textureLod(texture3d, float3 (_input.texcoords.x, _input.BT.y, _input.DU.y), 0).r;

    float RBD = textureLod(texture3d, float3 (_input.LR.y, _input.BT.x, _input.DU.x), 0).r;
    float RBC = textureLod(texture3d, float3 (_input.LR.y, _input.BT.x, _input.texcoords.z), 0).r;
    float RBU = textureLod(texture3d, float3 (_input.LR.y, _input.BT.x, _input.DU.y), 0).r;
    float RCD = textureLod(texture3d, float3 (_input.LR.y, _input.texcoords.y, _input.DU.x), 0).r;
    //float RCC = textureLod(texture3d, float3 (input.LR.y, input.texcoords.y, input.texcoords.z), 0).r;
    float RCU = textureLod(texture3d, float3 (_input.LR.y, _input.texcoords.y, _input.DU.y), 0).r;
    float RTD = textureLod(texture3d, float3 (_input.LR.y, _input.BT.y, _input.DU.x), 0).r;
    float RTC = textureLod(texture3d, float3 (_input.LR.y, _input.BT.y, _input.texcoords.z), 0).r;
    float RTU = textureLod(texture3d, float3 (_input.LR.y, _input.BT.y, _input.DU.y), 0).r;


    // is this cell next to the LevelSet boundary
    float product = L * R * B * T * D * U;
    product *= LBD * LBC * LBU * LCD * LCU * LTD * LTC * LTU
        * CBD * CBU * CTD * CTU
        * RBD * RBC * RBU * RCD * RCU * RTD * RTC * RTU;
    isBoundary = product < 0;

    // is the slope high enough
    highEnoughSlope = (abs(R - CCC) > minSlope) || (abs(L - CCC) > minSlope) ||
        (abs(T - CCC) > minSlope) || (abs(B - CCC) > minSlope) ||
        (abs(U - CCC) > minSlope) || (abs(D - CCC) > minSlope);

    return 0.5f * float3(R - L,  T - B,  U - D);
}