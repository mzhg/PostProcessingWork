package org.lwjgl.util.vector;

/**
 * The interface define the operation between the vector4f and float value.
 * @author mazhen'gui
 *
 */
public interface VectorValueOperator4f {

	Vector4f op(ReadableVector4f l, float r, Vector4f dest);
}
