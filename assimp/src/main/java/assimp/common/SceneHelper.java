package assimp.common;

import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;

public class SceneHelper {

	// scene we're working on
	Scene scene;

	// prefix to be added to all identifiers in the scene ...
	String id;

	// hash table to quickly check whether a name is contained in the scene
	final IntSet hashes = new IntArraySet();
	
	public SceneHelper() {
	}

	public SceneHelper(Scene scene) {
		this.scene = scene;
	}
	
}
