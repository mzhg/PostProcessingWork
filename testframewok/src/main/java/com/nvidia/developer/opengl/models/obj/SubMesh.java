package com.nvidia.developer.opengl.models.obj;

import org.lwjgl.util.vector.Matrix4f;

import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.util.StackInt;

/**
 * Used to define a mesh that uses a single material and an array of faces
 */
public abstract class SubMesh {

	// Material Id used by the sub mesh
	protected int m_materialId;

    // Mapping of bones used by the mesh to the full set of bones contained in the model
    // Local indices are contiguous and the array value at that index contains the index
    // of the full set that contains the actual bone node
    protected final StackInt m_boneMap = new StackInt();

    // Index of the node that is the parent of this sub-mesh
    protected int m_parentBone = -1;

    // Matching array of transforms that map from mesh space to bone space
    protected final List<Matrix4f> m_meshToBoneTransforms = new ArrayList<>();

    protected float[] m_vertices;
    protected int[] m_indices;

	protected int m_vertexCount;
	protected int m_indexCount;

	protected int m_normalOffset = -1; // in floats (zero == no component)
	protected int m_texCoordOffset = -1; // in floats (zero == no component)
	protected int m_texCoordCount = 0;
	protected int m_tangentOffset = -1; // in floats (zero == no component)
	protected int m_colorCount = 0;
	protected int m_colorOffset = -1; // in floats (zero == no component)
	protected int m_bonesPerVertex = 0;
	protected int m_boneIndexOffset = -1;   // in floats (zero == no component)
	protected int m_boneWeightOffset = -1; //  in floats (zero == no component)
	protected int m_vertSize = 0; // in floats
	
	/**
	 * Checks to see if the submesh's vertices contain normals
	 * @return True if the submesh's vertices contain normals, false if no normals exist.
	 */
	public abstract boolean hasNormals();

	/**
	 * Checks to see if the submesh's vertices contain texture coordinates
	 * @return True if the submesh's vertices contain texture coordinates, false if no texture coordinates exist.
	 */
	public abstract boolean hasTexCoords();

	/**
	 * Checks to see if the submesh's vertices contain tangents
	 * @return True if the submesh's vertices contain tangents, false if no tangents exist.
	 */
	public abstract boolean hasTangents();

	/**
	 * Checks to see if the submesh's vertices contain colors
	 * @return True if the submesh's vertices contain colors, false if no colors exist.
	 */
    public abstract boolean hasColors();
    
    /**
     * Checks to see if the submesh's vertices contain bone weights
     * @return True if the submesh's vertices contain bone weights, false if no bone weights exist.
     */
    public abstract boolean hasBoneWeights();

    /// Get the array of compiled vertices.
	/// The array of the optimized, compiled vertices for rendering
	/// \return the pointer to the start of the first vertex
	public float[] getVertices() { return m_vertices; }

	/// Get the array of compiled indices
	/// \return pointer to the array of indices
	public int[] getIndices() { return m_indices; }

	///@{
	/// Get the offset within the vertex of each attrib.
	/// \return the offset (in number of floats) of each attrib from the base of the vertex
	public int getPositionOffset() { return 0; }
	public int getNormalOffset() { return m_normalOffset; }
	public int getTexCoordOffset() { return m_texCoordOffset; }
	public int getTangentOffset() { return m_tangentOffset; }
	public int getColorOffset() { return m_colorOffset; }
	public int getBoneIndexOffset() { return m_boneIndexOffset; }
	public int getBoneWeightOffset() { return m_boneWeightOffset; }
	///@}
	public int getTexCoordCount() { return m_texCoordCount; }
	public int getColorCount() { return m_colorCount; }
	public int getBonesPerVertex() { return m_bonesPerVertex; }
	public int getNumBonesInMesh() { return m_boneMap.size(); }

	/**
	 * Get the size of a compiled vertex.
	 * @return the size of the merged vertex (in number of floats)
	 */
	public int getVertexSize() { return m_vertSize; }

	/**
	 * Get the count of vertices in the compiled array.
	 * @return the vertex count in the compiled (renderable) array
	 */
	public int getVertexCount() { return m_vertexCount; }

	/**
	 * The rendering index count.
	 * @return the number of indices in the given array
	 */
	public int getIndexCount() { return m_indexCount; }
}
