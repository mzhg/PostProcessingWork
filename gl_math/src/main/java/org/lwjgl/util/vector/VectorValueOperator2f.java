package org.lwjgl.util.vector;

/**
 * The interface define the operation between the vector2f and float value.
 * @author mazhen'gui
 *
 */
public interface VectorValueOperator2f {

	Vector2f op(ReadableVector2f l, float r, Vector2f dest);
}
