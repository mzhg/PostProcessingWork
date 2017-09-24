// Copyright 2012 Intel Corporation
// All Rights Reserved
//
// Permission is granted to use, copy, distribute and prepare derivative works of this
// software for any purpose and without fee, provided, that the above copyright notice
// and this statement appear in all copies. Intel makes no representations about the
// suitability of this software for any purpose. THIS SOFTWARE IS PROVIDED "AS IS."
// INTEL SPECIFICALLY DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, AND ALL LIABILITY,
// INCLUDING CONSEQUENTIAL AND OTHER INDIRECT DAMAGES, FOR THE USE OF THIS SOFTWARE,
// INCLUDING LIABILITY FOR INFRINGEMENT OF ANY PROPRIETARY RIGHTS, AND INCLUDING THE
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. Intel does not
// assume any responsibility for any errors which may appear in this software nor any
// responsibility to update it.
#include "../PostProcessingHLSLCompatiable.glsl"

//cbuffer cbCS : register( b0 )
layout(binding = 0) uniform cbCS
{
    uint2    ColorBufferTileCount;   // How many 32x32 tiles in the color buffer (rounded up)
    float2   InvColorBufferDims;     // (1/w, 1/h), where w and h are the dimensions of the color buffer

    float    PPAADEMO_gEdgeDetectionThreshold;
};