#include "LPV_Propagate.glsl"
in vec3 tex;

layout(location = 0) out float4 RedOut    /*: SV_Target0*/;
layout(location = 1) out float4 GreenOut /*: SV_Target1*/;
layout(location = 2) out float4 BlueOut   /* : SV_Target2*/;

layout(location = 3) out float4 RedAccumOut    /*: SV_Target3*/;
layout(location = 4) out float4 GreenAccumOut/*: SV_Target4*/;
layout(location = 5) out float4 BlueAccumOut   /* : SV_Target5*/;

void main()
{
    uint3 globalThreadId = uint3(gl_FragCoord.x, gl_FragCoord.y, tex.z);
    int4 readIndex = int4(globalThreadId.x,globalThreadId.y,globalThreadId.z,0);

    float3 offsets[6];
    offsets[0] = float3(0,0,1);
    offsets[1] = float3(1,0,0);
    offsets[2] = float3(0,0,-1);
    offsets[3] = float3(-1,0,0);
    offsets[4] = float3(0,1,0);
    offsets[5] = float3(0,-1,0);


    float4 faceCoeffs[6];
    faceCoeffs[0] = clampledCosineCoeff(offsets[0]);
    faceCoeffs[1] = clampledCosineCoeff(offsets[1]);
    faceCoeffs[2] = clampledCosineCoeff(offsets[2]);
    faceCoeffs[3] = clampledCosineCoeff(offsets[3]);
    faceCoeffs[4] = clampledCosineCoeff(offsets[4]);
    faceCoeffs[5] = clampledCosineCoeff(offsets[5]);



    float4 SHCoefficientsRed = float4(0,0,0,0);
    float4 SHCoefficientsGreen = float4(0,0,0,0);
    float4 SHCoefficientsBlue = float4(0,0,0,0);

    int index = 0;
    for(int neighbor=0;neighbor<6;neighbor++)
    {
        float4 inSHCoefficientsRed = float4(0,0,0,0);
        float4 inSHCoefficientsGreen = float4(0,0,0,0);
        float4 inSHCoefficientsBlue = float4(0,0,0,0);

        float4 neighborOffset = g_propagateValues[neighbor*6].neighborOffsets;

        //load the light value in the neighbor cell
        loadOffsetTexValues(g_txLPVRed, g_txLPVGreen, g_txLPVBlue, globalThreadId + neighborOffset.xyz, inSHCoefficientsRed, inSHCoefficientsGreen, inSHCoefficientsBlue );

        for(int face=0;face<6;face++)
        {
            //evaluate the SH approximation of the intensity coming from the neighboring cell to this face
            float3 dir;
            dir.x = g_propagateValues[index].x;
            dir.y = g_propagateValues[index].y;
            dir.z = g_propagateValues[index].z;
            dir = normalize(dir);
            float solidAngle = g_propagateValues[index].solidAngle;

            float4 dirSH;
            SH(dir,dirSH);

            float inRedFlux = 0;
            float inGreenFlux = 0;
            float inBlueFlux = 0;

            //approximate our SH coefficients in the direction dir.
            //to do this we sum the product of the stored SH coefficients with the SH basis function in the direction dir
            float redFlux =   solidAngle * max(0,(inSHCoefficientsRed.x  *dirSH.x + inSHCoefficientsRed.y  *dirSH.y + inSHCoefficientsRed.z  *dirSH.z + inSHCoefficientsRed.w  *dirSH.w));
            float greenFlux = solidAngle * max(0,(inSHCoefficientsGreen.x*dirSH.x + inSHCoefficientsGreen.y*dirSH.y + inSHCoefficientsGreen.z*dirSH.z + inSHCoefficientsGreen.w*dirSH.w));
            float blueFlux =  solidAngle * max(0,(inSHCoefficientsBlue.x *dirSH.x + inSHCoefficientsBlue.y *dirSH.y + inSHCoefficientsBlue.z *dirSH.z + inSHCoefficientsBlue.w *dirSH.w));

            inRedFlux += redFlux;
            inGreenFlux += greenFlux;
            inBlueFlux += blueFlux;

            float4 coeffs = faceCoeffs[face];

            inRedFlux *= fluxAmplifier;
            inGreenFlux *= fluxAmplifier;
            inBlueFlux *= fluxAmplifier;

            SHCoefficientsRed   +=  float4(inRedFlux   * coeffs.x, inRedFlux   * coeffs.y, inRedFlux   * coeffs.z, inRedFlux   * coeffs.w);
            SHCoefficientsGreen +=  float4(inGreenFlux * coeffs.x, inGreenFlux * coeffs.y, inGreenFlux * coeffs.z, inGreenFlux * coeffs.w);
            SHCoefficientsBlue  +=  float4(inBlueFlux  * coeffs.x, inBlueFlux  * coeffs.y, inBlueFlux  * coeffs.z, inBlueFlux  * coeffs.w);

            index++;
        }
    }

    //write back the updated flux
    if(g_copyPropToAccum)
    {
        SHCoefficientsRed   +=  texelFetch(g_txLPVRed, readIndex.xyz, readIndex.w);
        SHCoefficientsGreen +=  texelFetch(g_txLPVGreen, readIndex.xyz, readIndex.w);
        SHCoefficientsBlue  +=  texelFetch(g_txLPVBlue, readIndex.xyz, readIndex.w);
    }

//    PS_PROP_OUTPUT output;

    /*output.*/RedOut = SHCoefficientsRed;
    /*output.*/GreenOut = SHCoefficientsGreen;
    /*output.*/BlueOut = SHCoefficientsBlue;

    float4 AccumSHCoefficientsRed, AccumSHCoefficientsGreen, AccumSHCoefficientsBlue;
    /*output.*/RedAccumOut = SHCoefficientsRed;
    /*output.*/GreenAccumOut = SHCoefficientsGreen;
    /*output.*/BlueAccumOut = SHCoefficientsBlue;
}