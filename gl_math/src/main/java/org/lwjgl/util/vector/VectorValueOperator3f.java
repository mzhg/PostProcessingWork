package org.lwjgl.util.vector;

/**
 * The interface define the operation between the vector3f and float value.
 * @author mazhen'gui
 *
 */
public interface VectorValueOperator3f {

	Vector3f op(ReadableVector3f l, float r, Vector3f dest);
}
