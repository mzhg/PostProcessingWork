package org.lwjgl.util.vector;

/**
 * The interface define the operation between the two readable-vector2f.
 * @author mazhen'gui
 *
 */
public interface VectorOperator2f {

	Vector2f op(ReadableVector2f l, ReadableVector2f r, Vector2f dest);
}
