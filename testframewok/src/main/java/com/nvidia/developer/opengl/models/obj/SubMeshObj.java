package com.nvidia.developer.opengl.models.obj;

import java.util.ArrayList;
import java.util.HashMap;

/** Used to define a mesh that uses a single material and an array of faces */
final class SubMeshObj extends SubMesh{

	// Array of face definitions that define the submesh
    final ArrayList<MeshFace> m_rawFaces;
 // Compacted array of vertices used to define the submesh
    final ArrayList<MeshVertex> m_srcVertices;

    // Map from each vertex to its index in the flat array of vertices
    final HashMap<MeshVertex, Integer> m_vertexMap;
    
    public SubMeshObj() {
    	m_rawFaces = new ArrayList<>(128);
    	m_srcVertices = new ArrayList<>(128);
    	m_vertexMap = new HashMap<>(128);
	}
    
	@Override
	public boolean hasNormals() {
		if (m_rawFaces.isEmpty())
            return false;
		return m_srcVertices.get(0).m_normal != -1;
	}

	@Override
	public boolean hasTexCoords() {
		if (m_rawFaces.isEmpty())
            return false;
		return m_srcVertices.get(0).m_texcoord != -1;
	}

	@Override
	public boolean hasTangents() {
		if (m_rawFaces.isEmpty())
            return false;
		return m_srcVertices.get(0).m_tangent != -1;
	}

	@Override
	public boolean hasColors() {
		return false;
	}

	@Override
	public boolean hasBoneWeights() {
		return false;
	}

	/// Retrieves an index of a vertex matching the given one, adding a new one
    /// in the case of no existing match.
    /// \param[in] v Vertex to retrieve an index for
    /// \return Index into the submesh's array of vertices that contains a vertex
    ///         matching the given one, -1 if one did not exist and could not be added.
	/**
	 * Retrieves an index of a vertex matching the given one, adding a new one in the case of no existing match.
	 * @param v Vertex to retrieve an index for
	 * @return Index into the submesh's array of vertices that contains a vertex matching the given one, -1 if one did not exist and could not be added.
	 */
    int FindOrAddVertex(MeshVertex v){
    	Integer index = m_vertexMap.get(v);
    	
    	if(index == null){
    		// We need to add the vertex to our collection of vertices and
            // add it to the appropriate spot in the map
    		MeshVertex vertex = new MeshVertex(v);
            int vertexId = m_srcVertices.size();
            m_srcVertices.add(vertex);
            index = vertexId;
            
            // Add an entry to the map so that we can find this one if 
            // another matching vertex should be added
            m_vertexMap.put(vertex, vertexId);
    	}
    	
    	return index;
    }

    /**
     * Sets the normal to be used by the given vertex.  If that normal conflicts with one that is already stored in the vertex, this will 
     * split the vertex.  In this case, it will return the index of the newly created vertex to be used instead of the original index.
     * @param vertexId Index of the vertex to set the normal for
     * @param normalId Index of the normal to associate with the vertex
     * @return The index of the vertex that has been associated with the given normal.  This will be the original vertex index if
     * 	no existing normal was present.  If setting the normal caused the vertex to split, then this will be the index of
     * the new vertex that was created.
     */
    int SetNormal(int vertexId, int normalId){
    	if ((vertexId < 0) || (vertexId >= (m_srcVertices.size())))
        {
            // Invalid Vertex Id
            return -1;
        }

        MeshVertex vert = m_srcVertices.get(vertexId);
        if (normalId == vert.m_normal)
        {
            // The vertex's normal has already been set to use the given normal
            return vertexId;
        }

        if (-1 == vert.m_normal)
        {
            // Vertex's normal hasn't been set yet.  Remove the old
            // version from the map so that we can re-add it with the 
            // correct normal
            m_vertexMap.remove(vert);
            vert.m_normal = normalId;
//            m_srcVertices[vertexId] = vert;
            m_vertexMap.put(vert, vertexId);
            return vertexId;
        }

        vert = new MeshVertex(vert);
        vert.m_normal = normalId;

        // See if a vertex already exists with the new normal setting
        Integer vertIt = m_vertexMap.get(vert);

        if (vertIt != null)
        {
            // Found an existing one.
            return vertIt;
        }

        // Vertex's normal has already been set to a different normal.  We need to
        // split this vertex by creating a new vertex with the different normal
        // and add it to both the set of vertices and the map to them.
        vertexId = m_srcVertices.size();
        m_srcVertices.add(vert);
        m_vertexMap.put(vert, vertexId);
        return vertexId;
    }

    /**
     * Sets the tangent vector to be used by the given vertex.  If that tangent vector conflicts with one that is already set, this will
     * split the vertex.  In this case, it will return the index of the newly created vertex to be used instead of the original index.
     * @param vertexId Index of the vertex to set the tangent vector for
     * @param tangentId Index of the tangent vector to associate with the vertex
     * @return The index of the vertex that has been associated with the given tangent vector.  This will be the original vertex
     * 	index if no existing normal was present.  If setting the tangent vector caused the vertex to split, then this will
     * 	be the index of the new vertex that was created.
     */
    int SetTangent(int vertexId, int tangentId){
    	if ((vertexId < 0) || (vertexId >= (m_srcVertices.size())))
        {
            // Invalid Vertex Id
            return -1;
        }

        MeshVertex vert = m_srcVertices.get(vertexId);
        if (tangentId == vert.m_tangent)
        {
            // The vertex's tangent has already been set to use the given tangent
            return vertexId;
        }

        if (-1 == vert.m_tangent)
        {
            // Vertex's tangent hasn't been set yet.  Remove the old
            // version from the map so that we can re-add it with the 
            // correct tangent
            m_vertexMap.remove(vert);
            vert.m_tangent = tangentId;
//            m_srcVertices[vertexId] = vert;
            m_vertexMap.put(vert, vertexId);
            return vertexId;
        }
        vert = new MeshVertex(vert);
        vert.m_tangent = tangentId;

        // See if a vertex already exists with the new tangent setting
        Integer vertIt = m_vertexMap.get(vert);

        if (vertIt != null)
        {
            // Found an existing one.
            return vertIt;
        }

        // Vertex's tangent has already been set to a different tangent.  We need to
        // split this vertex by creating a new vertex with the different tangent
        // and add it to both the set of vertices and the map to them.
        vertexId = m_srcVertices.size();
        m_srcVertices.add(vert);
        m_vertexMap.put(vert, vertexId);
        return vertexId;
    }
}
