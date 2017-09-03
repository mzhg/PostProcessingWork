package assimp.common;

/** A single face in a mesh, referring to multiple vertices. <p>
*
* If mNumIndices is 3, we call the face 'triangle', for mNumIndices > 3 
* it's called 'polygon' (hey, that's just a definition!).
* <br>
* aiMesh::mPrimitiveTypes can be queried to quickly examine which types of
* primitive are actually present in a mesh. The #aiProcess_SortByPType flag 
* executes a special post-processing algorithm which splits meshes with
* *different* primitive types mixed up (e.g. lines and triangles) in several
* 'clean' submeshes. Furthermore there is a configuration option (
* #AI_CONFIG_PP_SBP_REMOVE) to force #aiProcess_SortByPType to remove 
* specific kinds of primitives from the imported scene, completely and forever.
* In many cases you'll probably want to set this setting to 
* @code 
* aiPrimitiveType_LINE|aiPrimitiveType_POINT
* @endcode
* Together with the #aiProcess_Triangulate flag you can then be sure that
* #aiFace::mNumIndices is always 3. 
* @note Take a look at the @link data Data Structures page @endlink for
* more information on the layout and winding order of a face.
*/
public abstract class Face implements Copyable<Face>{
	
	public static Face createInstance(int numIndices){
		switch (numIndices) {
		case 3: return new TriangleFace();
		default: return new HeapMemFace(new int[numIndices]);
		}
	}

	public abstract int getNumIndices();
	
	public abstract void set(int index, int i);
	
	public abstract int get(int index);
	
	public Face set(Face o) {
		if(o == this)
			return this;
		
		int count = Math.min(getNumIndices(), o.getNumIndices());
		for(int i = 0; i < count; i++){
			set(i, o.get(i));
		}
		return this;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if(!(obj instanceof Face))
			return false;
		
		Face other = (Face) obj;
		if(other.getNumIndices() != 3)
			return false;
		
		for(int i = 0; i < 3; i++){
			if(get(i) != other.get(i))
				return false;
		}
		return true;
	}
}
