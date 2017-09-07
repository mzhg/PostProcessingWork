package com.nvidia.developer.opengl.models.obj;

//Defines a vertex within a mesh via indices into
// the large, shared arrays of individual elements
final class MeshVertex {

	int m_pos = -1;      // Index into the shared positions array
    int m_normal = -1;   // Index into the shared normals array
    int m_texcoord = -1; // Index into the shared texture coordinates array
    int m_tangent = -1;  // Index into the shared tangent array 
    
    public MeshVertex(){}
    
    public MeshVertex(MeshVertex v){
    	m_pos = v.m_pos;
    	m_normal = v.m_normal;
    	m_texcoord = v.m_texcoord;
    	m_tangent = v.m_tangent;
    }
	
    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + m_normal;
		result = prime * result + m_pos;
		result = prime * result + m_tangent;
		result = prime * result + m_texcoord;
		return result;
	}
	
    @Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MeshVertex other = (MeshVertex) obj;
		if (m_normal != other.m_normal)
			return false;
		if (m_pos != other.m_pos)
			return false;
		if (m_tangent != other.m_tangent)
			return false;
		if (m_texcoord != other.m_texcoord)
			return false;
		return true;
	}
}
