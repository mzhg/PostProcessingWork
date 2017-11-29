#include "../../../shader_libs/PostProcessingHLSLCompatiable.glsl"

// Copyright (c) 2011 NVIDIA Corporation. All rights reserved.
//
// TO  THE MAXIMUM  EXTENT PERMITTED  BY APPLICABLE  LAW, THIS SOFTWARE  IS PROVIDED
// *AS IS*  AND NVIDIA AND  ITS SUPPLIERS DISCLAIM  ALL WARRANTIES,  EITHER  EXPRESS
// OR IMPLIED, INCLUDING, BUT NOT LIMITED  TO, NONINFRINGEMENT,IMPLIED WARRANTIES OF
// MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.  IN NO EVENT SHALL  NVIDIA
// OR ITS SUPPLIERS BE  LIABLE  FOR  ANY  DIRECT, SPECIAL,  INCIDENTAL,  INDIRECT,  OR
// CONSEQUENTIAL DAMAGES WHATSOEVER (INCLUDING, WITHOUT LIMITATION,  DAMAGES FOR LOSS
// OF BUSINESS PROFITS, BUSINESS INTERRUPTION, LOSS OF BUSINESS INFORMATION, OR ANY
// OTHER PECUNIARY LOSS) ARISING OUT OF THE  USE OF OR INABILITY  TO USE THIS SOFTWARE,
// EVEN IF NVIDIA HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
//
// Please direct any bugs or questions to SDKFeedback@nvidia.com

//block size for the LPV propagation code
#define X_BLOCK_SIZE 8
#define Y_BLOCK_SIZE 8
#define Z_BLOCK_SIZE 4
#define TOTAL_BLOCK_SIZE 405 //(8+1)*(8+1)*(4+1)

#define SM_SIZE 1024 //the resolution of the shadow map for the scene

#define RSM_RES 512 //the resolution of the RSMs (this has to be a power of two, see note below)
#define RSM_RES_M_1 RSM_RES-1 //one RSM_RES-1 (to be used for doing modulus with RSM_RES - note that any code using this assumes RSM_RES is a power of two)
#define RSM_RES_SQR RSM_RES*RSM_RES
#define RSM_RES_SQR_M_1 RSM_RES_SQR-1

#define g_LPVWIDTH 32 //resolution of the 3D LPV
#define g_LPVHEIGHT 32 //resolution of the 3D LPV
#define g_LPVDEPTH 32 //resolution of the 3D LPV

#define USE_MULTIPLE_BOUNCES

#define HIERARCHICAL_INIT_LEVEL 0 //if we are using the hiearchy model we always initialize the top level, and downsample down to the other levels

//#define USE_SINGLE_CHANNELS

#define MAX_P_SAMPLES 32