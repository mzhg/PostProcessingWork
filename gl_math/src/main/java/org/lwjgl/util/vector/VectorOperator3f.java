package org.lwjgl.util.vector;

/**
 * The interface define the operation between the two readable-vector3f.
 * @author mazhen'gui
 *
 */
public interface VectorOperator3f {

	Vector3f op(ReadableVector3f l, ReadableVector3f r, Vector3f dest);
}
