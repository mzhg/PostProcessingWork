//----------------------------------------------------------------------------------
// File:        NvModel/NvSkeleton.h
// SDK Version: v3.00 
// Email:       gameworks@nvidia.com
// Site:        http://developer.nvidia.com/
//
// Copyright (c) 2014-2015, NVIDIA CORPORATION. All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
//  * Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
//  * Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
//  * Neither the name of NVIDIA CORPORATION nor the names of its
//    contributors may be used to endorse or promote products derived
//    from this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS ``AS IS'' AND ANY
// EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
// PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
// CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
// EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
// PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
// PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
// OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//
//----------------------------------------------------------------------------------
package com.nvidia.developer.opengl.models.obj;

import org.lwjgl.util.vector.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * NvSkeleton holds an array of nodes that comprise the hierarchy of 
 * transformations for the model.  These nodes form the basis for 
 * applying hierarchical transformations to meshes within a model as
 * well as skinning those meshes.  An NvSkeleton is a tree where each
 * node has at most one parent, and the index of a parent node MUST
 * be less than that of all of its children.  This allows all transforms
 * to be propagated through the tree with a single sequential traversal
 * of the array and any operations performed on nodes in such a traversal
 * to be performed on any child's parent before the child itself.  Note
 * that a skeleton may contain more than one "tree" (i.e. multiple nodes
 * with no parent node), but by definition, node 0 is the root of a tree. 
 */
public class NvSkeleton {

	/** All nodes in the skeleton */
	protected final List<NvSkeletonNode> m_nodes = new ArrayList<>();
	
	/** Matrices containing the current, model-space 
     transforms for each corresponding node */
	protected final List<Matrix4f> m_nodeTransforms = new ArrayList<>();
	
	/** Default constructor.  Creates skeleton with 0 bones.*/
	public NvSkeleton() {}

	/**
	 * Constructor initializes skeleton with the given array of nodes
	 * @param pNodes
	 */
	public NvSkeleton(NvSkeletonNode ...pNodes){
		if(pNodes == null || pNodes.length == 0)
			return;
		
		int numNodes = pNodes.length;
		for(int nodeIndex = 0; nodeIndex < numNodes; nodeIndex ++){
			NvSkeletonNode srcNode = pNodes[nodeIndex];
			m_nodes.add(srcNode);
			
			if(srcNode.m_parentNode >= nodeIndex)
				throw new IllegalArgumentException();
			
			if (-1 == srcNode.m_parentNode)
            {
//                m_nodeTransforms[nodeIndex] = pSrcNode->m_parentRelTransform;
				m_nodeTransforms.add(new Matrix4f(srcNode.m_parentRelTransform));
            }
            else
            {
//                m_nodeTransforms[nodeIndex] = m_nodeTransforms[pSrcNode->m_parentNode] * pSrcNode->m_parentRelTransform;
            	Matrix4f transform = Matrix4f.mul(m_nodeTransforms.get(srcNode.m_parentNode), srcNode.m_parentRelTransform, null);
            	m_nodeTransforms.add(transform);
            }
		}
	}

	/**
	 * Retrieves the number of nodes contained in the skeleton
	 * @return Number of nodes contained in the skeleton
	 */
	public int GetNumNodes() { return m_nodes.size(); }

	/**
	 * Retrieves the index of the first node in the skeleton with the given name
	 * @param name Name of the node to find
	 * @return The index of the first node with a name matching that provided, but -1 if no node contained a matching name
	 * @note There may be more than one node in the skeleton with the given name.  The matching node with the lowest index will be returned.
	 */
	public int GetNodeIndexByName(String name) {
		int numNodes = m_nodes.size();
        for (int nodeIndex = 0; nodeIndex < numNodes; ++nodeIndex)
        {
            if (m_nodes.get(nodeIndex).m_name.equals(name))
            {
                return nodeIndex;
            }
        }
        return -1;
	}

	/**
	 * Retrieves the first node in the skeleton with the given name
	 * @param name Name of the node to find
	 * @return A pointer to the node whose name matches that provided. NULL if no node contained a matching name
	 * @note There may be more than one node in the skeleton with the given name.  The matching node with the lowest index will be returned.
	 */
	public NvSkeletonNode GetNodeByName(String name){
		for(NvSkeletonNode node : m_nodes){
			if(node.m_name.equals(name)){
				return node;
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves the node in the node array at the given index
	 * @param index Index of the node to retrieve
	 * @return A pointer to the node at the index provided. NULL if the index was not valid
	 */
	public NvSkeletonNode GetNodeByIndex(int index){
		try {
			return m_nodes.get(index);
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	/**
	 * Retrieves a pointer to the array of matrices representing the current model-space transforms for the nodes of the skeleton
	 * @return A pointer to the array of current transform matrices, null if there are no nodes, thus no current transforms.
	 */
	public List<Matrix4f> GetTransforms(){
		return m_nodeTransforms.isEmpty() ? null : m_nodeTransforms;
	}

	/**
	 * Retrieves a pointer to the matrix representing the current model-space transform of the node at the given index.
	 * @param index Index of the node in the skeleton for which to retrieve a current transform
	 * @return A pointer to the matrix representing the current transform of the node at the given index, but NULL if the index was invalid.
	 */
	public Matrix4f GetTransform(int index){
		try {
			return m_nodeTransforms.get(index);
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}
}
