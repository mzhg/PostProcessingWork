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

    float4 GV[8];
    float4 BC[8];
    GV[0] = GV[1] = GV[2] = GV[3] = GV[4] = GV[5] = GV[6] = GV[7] = float4(0,0,0,0);
    BC[0] = BC[1] = BC[2] = BC[3] = BC[4] = BC[5] = BC[6] = BC[7] = float4(0,0,0,0);


    if(g_useGVOcclusion || g_useMultipleBounces)
    {
        GV[0] = loadOffsetTexValue(g_txGV, globalThreadId); //GV_z_z_z
        GV[1] = loadOffsetTexValue(g_txGV, globalThreadId + float3(0,0,-1) ); //GV_z_z_m1
        GV[2] = loadOffsetTexValue(g_txGV, globalThreadId + float3(0,-1,0) ); //GV_z_m1_z
        GV[3] = loadOffsetTexValue(g_txGV, globalThreadId + float3(-1,0,0) ); //GV_m1_z_z
        GV[4] = loadOffsetTexValue(g_txGV, globalThreadId + float3(-1,-1,0) ); //GV_m1_m1_z
        GV[5] = loadOffsetTexValue(g_txGV, globalThreadId + float3(-1,0,-1) ); //GV_m1_z_m1
        GV[6] = loadOffsetTexValue(g_txGV, globalThreadId + float3(0,-1,-1) ); //GV_z_m1_m1
        GV[7] = loadOffsetTexValue(g_txGV, globalThreadId + float3(-1,-1,-1) ); //GV_m1_m1_m1

       #ifdef USE_MULTIPLE_BOUNCES
        BC[0] = loadOffsetTexValue(g_txGVColor, globalThreadId); //BC_z_z_z
        BC[1] = loadOffsetTexValue(g_txGVColor, globalThreadId + float3(0,0,-1) ); //BC_z_z_m1
        BC[2] = loadOffsetTexValue(g_txGVColor, globalThreadId + float3(0,-1,0) ); //BC_z_m1_z
        BC[3] = loadOffsetTexValue(g_txGVColor, globalThreadId + float3(-1,0,0) ); //BC_m1_z_z
        BC[4] = loadOffsetTexValue(g_txGVColor, globalThreadId + float3(-1,-1,0) ); //BC_m1_m1_z
        BC[5] = loadOffsetTexValue(g_txGVColor, globalThreadId + float3(-1,0,-1) ); //BC_m1_z_m1
        BC[6] = loadOffsetTexValue(g_txGVColor, globalThreadId + float3(0,-1,-1) ); //BC_z_m1_m1
        BC[7] = loadOffsetTexValue(g_txGVColor, globalThreadId + float3(-1,-1,-1) ); //BC_m1_m1_m1
       #endif
    }


    int index = 0;
    for(int neighbor=0;neighbor<6;neighbor++)
    {
        float4 inSHCoefficientsRed = float4(0,0,0,0);
        float4 inSHCoefficientsGreen = float4(0,0,0,0);
        float4 inSHCoefficientsBlue = float4(0,0,0,0);

        float4 neighborOffset = g_propagateValues[neighbor*6].neighborOffsets;

        //load the light value in the neighbor cell
        loadOffsetTexValues(g_txLPVRed, g_txLPVGreen, g_txLPVBlue, globalThreadId + neighborOffset.xyz, inSHCoefficientsRed, inSHCoefficientsGreen, inSHCoefficientsBlue );

        #ifdef USE_MULTIPLE_BOUNCES
        float4 GVSHReflectCoefficients = float4(0,0,0,0);
        float4 GVReflectanceColor = float4(0,0,0,0);
        float4 wSH;
        if(g_useMultipleBounces)
        {
            SH( float3(neighborOffset.x,neighborOffset.y,neighborOffset.z),wSH );

            if(neighbor==1)//if(neighborOffset.x>0)
            {
                 GVSHReflectCoefficients = (GV[7] + GV[5] + GV[4] + GV[3])*0.25;
                 GVReflectanceColor =      (BC[7] + BC[5] + BC[4] + BC[3])*0.25;
            }
            else if(neighbor==3)//else if(neighborOffset.x<0)
            {
                GVSHReflectCoefficients = (GV[6] + GV[1] + GV[2] + GV[0])*0.25;
                GVReflectanceColor =      (BC[6] + BC[1] + BC[2] + BC[0])*0.25;
            }
            else if(neighbor==4)//else if(neighborOffset.y>0)
            {
                GVSHReflectCoefficients = (GV[2] + GV[4] + GV[6] + GV[7])*0.25;
                GVReflectanceColor =      (BC[2] + BC[4] + BC[6] + BC[7])*0.25;
            }
            else if(neighbor==5)//else if(neighborOffset.y<0)
            {
                GVSHReflectCoefficients = (GV[0] + GV[3] + GV[1] + GV[5])*0.25;
                GVReflectanceColor =      (BC[0] + BC[3] + BC[1] + BC[5])*0.25;
            }
            else if(neighbor==0)//else if(neighborOffset.z>0)
            {
                GVSHReflectCoefficients = (GV[1] + GV[5] + GV[6] + GV[7])*0.25;
                GVReflectanceColor =      (BC[1] + BC[5] + BC[6] + BC[7])*0.25;
            }
            else if(neighbor==2)//else if(neighborOffset.z<0)
            {
                GVSHReflectCoefficients = (GV[0] + GV[3] + GV[2] + GV[4])*0.25;
                GVReflectanceColor =      (BC[0] + BC[3] + BC[2] + BC[4])*0.25;
            }
        }
        #endif

        //find what the occlusion probability is in this given direction
        float4 GVSHCoefficients = float4(0,0,0,0);
        if(g_useGVOcclusion)
        {
            if(neighbor==1)//if(neighborOffset.x>0)
            {
                 GVSHCoefficients = (GV[6] + GV[1] + GV[2] + GV[0])*0.25;
            }
            else if(neighbor==3)//else if(neighborOffset.x<0)
            {
                GVSHCoefficients = (GV[7] + GV[5] + GV[4] + GV[3])*0.25;
            }
            else if(neighbor==4)//else if(neighborOffset.y>0)
            {
                GVSHCoefficients = (GV[0] + GV[3] + GV[1] + GV[5])*0.25;
            }
            else if(neighbor==5)//else if(neighborOffset.y<0)
            {
                GVSHCoefficients = (GV[2] + GV[4] + GV[6] + GV[7])*0.25;
            }
            else if(neighbor==0)//else if(neighborOffset.z>0)
            {
                GVSHCoefficients = (GV[0] + GV[3] + GV[2] + GV[4])*0.25;
            }
            else if(neighbor==2)//else if(neighborOffset.z<0)
            {
                GVSHCoefficients = (GV[1] + GV[5] + GV[6] + GV[7])*0.25;
            }
        }


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

            float occlusion = 1.0f;
            //find the probability of occluding light
            if(g_useGVOcclusion)
                occlusion = 1.0 - max(0,min(1.0f, occlusionAmplifier*innerProductSH(GVSHCoefficients , dirSH )));

            float inRedFlux = 0;
            float inGreenFlux = 0;
            float inBlueFlux = 0;

            {
                //approximate our SH coefficients in the direction dir.
                //to do this we sum the product of the stored SH coefficients with the SH basis function in the direction dir
                float redFlux = occlusion * solidAngle *    max(0,(inSHCoefficientsRed.x  *dirSH.x + inSHCoefficientsRed.y  *dirSH.y + inSHCoefficientsRed.z  *dirSH.z + inSHCoefficientsRed.w  *dirSH.w));
                float greenFlux = occlusion * solidAngle * max(0,(inSHCoefficientsGreen.x*dirSH.x + inSHCoefficientsGreen.y*dirSH.y + inSHCoefficientsGreen.z*dirSH.z + inSHCoefficientsGreen.w*dirSH.w));
                float blueFlux = occlusion * solidAngle *    max(0,(inSHCoefficientsBlue.x *dirSH.x + inSHCoefficientsBlue.y *dirSH.y + inSHCoefficientsBlue.z *dirSH.z + inSHCoefficientsBlue.w *dirSH.w));

                inRedFlux += redFlux;
                inGreenFlux += greenFlux;
                inBlueFlux += blueFlux;


                #ifdef USE_MULTIPLE_BOUNCES
                float blockerDiffuseIllumination = innerProductSH(GVSHReflectCoefficients , wSH );
                if(blockerDiffuseIllumination>0.0f && g_useMultipleBounces)
                {
                    float weightedRedFlux =   blockerDiffuseIllumination * redFlux * reflectedLightAmplifier * GVReflectanceColor.r;
                    float weightedGreenFlux = blockerDiffuseIllumination * greenFlux * reflectedLightAmplifier * GVReflectanceColor.g;
                    float weightedBlueFlux =  blockerDiffuseIllumination * blueFlux * reflectedLightAmplifier * GVReflectanceColor.b;

                    SHCoefficientsRed   += float4(weightedRedFlux*GVSHReflectCoefficients.x,weightedRedFlux*GVSHReflectCoefficients.y,weightedRedFlux*GVSHReflectCoefficients.z,weightedRedFlux*GVSHReflectCoefficients.w);
                    SHCoefficientsGreen   += float4(weightedGreenFlux*GVSHReflectCoefficients.x,weightedGreenFlux*GVSHReflectCoefficients.y,weightedGreenFlux*GVSHReflectCoefficients.z,weightedGreenFlux*GVSHReflectCoefficients.w);
                    SHCoefficientsBlue   += float4(weightedBlueFlux*GVSHReflectCoefficients.x,weightedBlueFlux*GVSHReflectCoefficients.y,weightedBlueFlux*GVSHReflectCoefficients.z,weightedBlueFlux*GVSHReflectCoefficients.w);
                }

                #endif
            }

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