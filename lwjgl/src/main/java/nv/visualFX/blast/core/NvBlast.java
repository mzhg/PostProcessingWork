// This code contains NVIDIA Confidential Information and is disclosed to you
// under a form of NVIDIA software license agreement provided separately to you.
//
// Notice
// NVIDIA Corporation and its licensors retain all intellectual property and
// proprietary rights in and to this software and related documentation and
// any modifications thereto. Any use, reproduction, disclosure, or
// distribution of this software and related documentation without an express
// license agreement from NVIDIA Corporation is strictly prohibited.
//
// ALL NVIDIA DESIGN SPECIFICATIONS, CODE ARE PROVIDED "AS IS.". NVIDIA MAKES
// NO WARRANTIES, EXPRESSED, IMPLIED, STATUTORY, OR OTHERWISE WITH RESPECT TO
// THE MATERIALS, AND EXPRESSLY DISCLAIMS ALL IMPLIED WARRANTIES OF NONINFRINGEMENT,
// MERCHANTABILITY, AND FITNESS FOR A PARTICULAR PURPOSE.
//
// Information and code furnished is believed to be accurate and reliable.
// However, NVIDIA Corporation assumes no responsibility for the consequences of use of such
// information or for any infringement of patents or other rights of third parties that may
// result from its use. No license is granted by implication or otherwise under any patent
// or patent rights of NVIDIA Corporation. Details are subject to change without notice.
// This code supersedes and replaces all information previously supplied.
// NVIDIA Corporation products are not authorized for use as critical
// components in life support devices or systems without express written approval of
// NVIDIA Corporation.
//
// Copyright (c) 2016-2017 NVIDIA Corporation. All rights reserved.
package nv.visualFX.blast.core;

import nv.visualFX.blast.lowlevel.Asset;

/**
 * Created by mazhen'gui on 2017/9/9.
 */

public final class NvBlast {
    ///////////////////////////////////////////////////////////////////////////////
//	NvBlastAsset functions
///////////////////////////////////////////////////////////////////////////////
///@{
    /**
     * Calculates the memory requirements for an asset based upon its descriptor.  Use this function
     * when building an asset with NvBlastCreateAsset.
     * @param desc Asset descriptor (see NvBlastAssetDesc).
     * @return the memory size (in bytes) required for the asset, or zero if desc is invalid.
     */
    public static int NvBlastGetAssetMemorySize(NvBlastAssetDesc desc/*, NvBlastLog logFn*/){
        return Asset.getMemorySize(desc);
    }


    /**
     Returns the number of bytes of scratch memory that the user must supply to NvBlastCreateAsset,
     based upon the descriptor that will be passed into that function.

     @param[in] desc		The asset descriptor that will be passed into NvBlastCreateAsset.
     @param[in] logFn	User-supplied message function (see NvBlastLog definition).  May be NULL.

     @return	the number of bytes of scratch memory required for a call to NvBlastCreateAsset with that descriptor.
     */
    public static int NvBlastGetRequiredScratchForCreateAsset(NvBlastAssetDesc desc/*, NvBlastLog logFn*/){
        return Asset.createRequiredScratch(desc);
    }


    /**
     Asset-building function.

     Constructs an NvBlastAsset in-place at the address given by the user.  The address must point to a block
     of memory of at least the size given by NvBlastGetAssetMemorySize(desc, logFn), and must be 16-byte aligned.

     Support chunks (marked in the NvBlastChunkDesc struct) must provide full coverage over the asset.
     This means that from any leaf chunk to the root node, exactly one chunk must be support.  If this condition
     is not met the function fails to create an asset.

     Any bonds described by NvBlastBondDesc descriptors that reference non-support chunks will be removed.
     Duplicate bonds will be removed as well (bonds that are between the same chunk pairs).

     Chunks in the asset should be arranged such that sibling chunks (chunks with the same parent) are contiguous.
     Chunks are also should be arranged such that leaf chunks (chunks with no children) are at the end of the chunk list.
     If chunks aren't arranged properly the function fails to create an asset.

     @param desc		Asset descriptor (see NvBlastAssetDesc).

     @return pointer to new NvBlastAsset (will be the same address as mem), or NULL if unsuccessful.
     */
    public static NvBlastAsset NvBlastCreateAsset(NvBlastAssetDesc desc/*, NvBlastLog logFn*/){
        return Asset.create(/*mem, */desc/*, scratch, logFn*/);
    }
}
