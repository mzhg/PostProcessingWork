// Copyright 2012 Intel Corporation
// All Rights Reserved

#ifndef H_AVSM_RESOLVE
#define H_AVSM_RESOLVE

#include "AVSM.glsl"

#define MAX_NODES 300

void InitAVSMData(inout AVSMData data)
{
    data.depth[0] = mEmptyNode.xxxx;
    data.trans[0] = FIRST_NODE_TRANS_VALUE.xxxx;
#if AVSM_RT_COUNT > 1
    data.depth[1] = mEmptyNode.xxxx;
    data.trans[1] = FIRST_NODE_TRANS_VALUE.xxxx;
#endif
#if AVSM_RT_COUNT > 2
    data.depth[2] = mEmptyNode.xxxx;
    data.trans[2] = FIRST_NODE_TRANS_VALUE.xxxx;
#endif
#if AVSM_RT_COUNT > 3
    data.depth[3] = mEmptyNode.xxxx;
    data.trans[3] = FIRST_NODE_TRANS_VALUE.xxxx;
#endif
}

// These resolve functions read the previously captured (in a per pixel linked list) light blockers,
// sort them and insert them in our AVSM
// Note that AVSMs can be created submitting blockers in any order, in this particular case we might want to sort the blockers
// only the reduce temporal artifacts introduced by the non deterministic fragments shading order and AVSM lossy compression algorithm

//AVSMData_PSOut AVSMInsertionSortResolvePS(FullScreenTriangleVSOut Input)

layout(location = 0) out vec4 OutColors[AVSM_RT_COUNT];

void main()
{
    AVSMData data;

    // Initialize AVSM data
    InitAVSMData(data);

    // Get fragment viewport coordinates
    int2 screenAddress = int2(gl_FragCoord.xy);

    // Get offset to the first node
    uint nodeOffset = LT_GetFirstSegmentNodeOffset(screenAddress);

    // Fetch nodes
    uint nodeCount = 0;
    ListTexSegmentNode nodes[MAX_NODES];
    /*[loop]*/while ((nodeOffset != NODE_LIST_NULL) && (nodeCount < MAX_NODES)) {
        // Get node..
        ListTexSegmentNode node = LT_GetSegmentNode(nodeOffset);

        // Insertion Sort
        int i = int(nodeCount);
        while (i > 0) {
            if (nodes[i-1].sortKey < node.sortKey) {
                nodes[i] = nodes[i-1];
                i--;
            } else break;

        }
        nodes[i] = node;

        // Increase node count and move to next node
        nodeOffset = node.next;
        nodeCount++;
    }

    // Insert nodes into our AVSM
    /*[loop]*/for (uint i = 0; i < nodeCount; ++i) {
        InsertSegmentAVSM(nodes[i].depth, nodes[i].trans, data);
    }

    for(int i = 0; i < AVSM_RT_COUNT; i++)
    {
        OutColors[i] = data.trans[i];
    }

}

//////////////////////////////////////////////
// Other algorithms
//////////////////////////////////////////////

#endif // H_AVSM_RESOLVE