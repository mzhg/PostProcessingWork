package com.nvidia.developer.opengl.models.obj;

import org.lwjgl.util.vector.Matrix4f;

import jet.opengl.postprocessing.util.StackInt;

/** NvSkeletonNode holds the definition of a single node in an NvSkeleton.*/
public class NvSkeletonNode {

	String m_name;
    int m_parentNode;
    final Matrix4f m_parentRelTransform = new Matrix4f();
    final StackInt m_childNodes = new StackInt();
    final StackInt m_meshes = new StackInt();
}
