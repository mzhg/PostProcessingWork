// Copyright 2012 Intel Corporation
// All Rights Reserved

#ifndef HEADER_AVSM_GEN_DEFINE
#define HEADER_AVSM_GEN_DEFINE

//////////////////////////////////////////////
// Defines
//////////////////////////////////////////////

#include "AVSM_Node_defs.h"

#define AVSM_GEN_SOFT

// FLT_MAX
#define AVSM_GEN_EMPTY_NODE_DEPTH   (3.40282E38)

// enable bilinear filtering
#define AVSM_GEN_BILINEARF

#define AVSM_GEN_TRANS_BIT_COUNT    (12)
#define AVSM_GEN_MAX_UNNORM_TRANS   ((1 << AVSM_GEN_TRANS_BIT_COUNT) - 1)
#define AVSM_GEN_TRANS_MASK         (0xFFFFFFFF - AVSM_GEN_MAX_UNNORM_TRANS)
#define AVSM_GEN_TRANS_MASK2        (0x7FFFFFFF - AVSM_GEN_MAX_UNNORM_TRANS)

#endif   // HEADER_AVSM_GEN_DEFINE
