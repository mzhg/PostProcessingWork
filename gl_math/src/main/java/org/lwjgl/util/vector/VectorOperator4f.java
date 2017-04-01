package org.lwjgl.util.vector;

/**
 * The interface define the operation between the two readable-vector4f.
 * @author mazhen'gui
 *
 */
public interface VectorOperator4f {

	Vector4f op(ReadableVector4f l, ReadableVector4f r, Vector4f dest);
}
