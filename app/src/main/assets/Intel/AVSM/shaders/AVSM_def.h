// Copyright 2012 Intel Corporation
// All Rights Reserved
//



#ifndef HEADER_AVSM_DEFINE
#define HEADER_AVSM_DEFINE

#include "AVSM_Node_defs.h"

// Enable bilinear filtering (disable it to enable point filtering)
#define AVSM_BILINEARF

// Defines the compositing operator according
// the selected visibily curve representation (transmittance or opacity)
// Also defines the starting value for transmittance and opacity
#define FIRST_NODE_TRANS_VALUE 1.0f

#define VOL_SHADOW_NO_SHADOW         0
#define VOL_SHADOW_AVSM              1
#define VOL_SHADOW_AVSM_GEN          2

// Use these #define to enable/disable particular sampling techniques
// In order to measure accurate performance for each algorithm all other algorithms must be disabled
// It also improves shaders compile time by a lot if you are only working on a single algorithm!
#define ENABLE_AVSM_SAMPLING

// Select ONLY ONE sampling algorithm (see aboce) and uncomment this #define if you want to enable faster filtering
//#define SAMPLING_NO_SWITCHCASE

// By enabling this macro, the shader code will record memory stats
// that can be queried by the app and toggled on/off via the UI.
// #define AVSM_ENABLE_MEMORY_STATS

#endif
