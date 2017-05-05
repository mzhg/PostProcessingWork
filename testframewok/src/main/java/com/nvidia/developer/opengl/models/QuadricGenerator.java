package com.nvidia.developer.opengl.models;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

public interface QuadricGenerator {

	void genVertex(float x, float y, Vector3f position, Vector3f normal, Vector2f texCoord, Vector4f color);
}
