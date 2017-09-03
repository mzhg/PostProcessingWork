package assimp.importer.ms3d;

import java.util.ArrayList;

import org.lwjgl.util.vector.Vector3f;

final class TempJoint extends TempComment{

	final byte[] name = new byte[32];
	final byte[] parentName = new byte[32];
	final Vector3f rotation = new Vector3f();
	final Vector3f position = new Vector3f();

	final ArrayList<TempKeyFrame> rotFrames = new ArrayList<TempKeyFrame>();
	final ArrayList<TempKeyFrame> posFrames = new ArrayList<TempKeyFrame>();
}
