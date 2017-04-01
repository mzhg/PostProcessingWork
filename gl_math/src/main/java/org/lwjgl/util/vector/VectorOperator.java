package org.lwjgl.util.vector;

/**
 * The interface define the operation between the two readable-vector2f.
 * @author mazhen'gui
 *
 */
public interface VectorOperator<T> {

	T op(T l, T r, T dest);
}
