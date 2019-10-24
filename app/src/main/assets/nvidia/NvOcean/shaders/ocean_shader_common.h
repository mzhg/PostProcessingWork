// This code contains NVIDIA Confidential Information and is disclosed 
// under the Mutual Non-Disclosure Agreement. 
// 
// Notice 
// ALL NVIDIA DESIGN SPECIFICATIONS AND CODE ("MATERIALS") ARE PROVIDED "AS IS" NVIDIA MAKES 
// NO REPRESENTATIONS, WARRANTIES, EXPRESSED, IMPLIED, STATUTORY, OR OTHERWISE WITH RESPECT TO 
// THE MATERIALS, AND EXPRESSLY DISCLAIMS ANY IMPLIED WARRANTIES OF NONINFRINGEMENT, 
// MERCHANTABILITY, OR FITNESS FOR A PARTICULAR PURPOSE. 
// 
// NVIDIA Corporation assumes no responsibility for the consequences of use of such 
// information or for any infringement of patents or other rights of third parties that may 
// result from its use. No license is granted by implication or otherwise under any patent 
// or patent rights of NVIDIA Corporation. No third party distribution is allowed unless 
// expressly authorized by NVIDIA.  Details are subject to change without notice. 
// This code supersedes and replaces all information previously supplied. 
// NVIDIA Corporation products are not authorized for use as critical 
// components in life support devices or systems without express written approval of 
// NVIDIA Corporation. 
// 
// Copyright � 2008- 2013 NVIDIA Corporation. All rights reserved.
//
// NVIDIA Corporation and its licensors retain all intellectual property and proprietary
// rights in and to this software and related documentation and any modifications thereto.
// Any use, reproduction, disclosure or distribution of this software and related
// documentation without an express license agreement from NVIDIA Corporation is
// strictly prohibited.
//

#ifndef _OCEAN_SHADER_COMMON_H
#define _OCEAN_SHADER_COMMON_H

#define MaxNumVessels           1
#define MaxNumSpotlights        11

#define ENABLE_SHADOWS          1

#define ENABLE_GPU_SIMULATION   1
#define SPRAY_PARTICLE_SORTING  1

#define BitonicSortCSBlockSize  512

#define SPRAY_PARTICLE_COUNT    (BitonicSortCSBlockSize * 256)

#define SprayParticlesCSBlocksSize 256
#define SimulateSprayParticlesCSBlocksSize 256

//#define ENABLE_SPRAY_PARTICLES  0

const float kSpotlightShadowResolution = 2048;

#define EmitParticlesCSBlocksSize 256
#define SimulateParticlesCSBlocksSize 256
#define PSMPropagationCSBlockSize 16

#define TransposeCSBlockSize 16

#endif	// _OCEAN_SHADER_COMMON_H
