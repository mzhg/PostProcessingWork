package com.nvidia.developer.opengl.models.obj;

import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.util.List;

/** Contains the definition of a single face in a mesh */
final class MeshFace {

	// Indices into the mesh's array of vertices of those vertices the define the face
    final int[] m_verts = new int[3];
    final Vector3f m_faceNormal = new Vector3f(); // Calculated at load time
    int m_smoothingGroup;
    int m_material;    // Index into the materials array
    SubMeshObj m_pSubMesh;    // Pointer back to the mesh that contains this face
    
    public MeshFace(){
    	
    }
    
    public MeshFace(MeshFace face){
    	System.arraycopy(face.m_verts, 0, m_verts, 0, 3);
    	m_faceNormal.set(face.m_faceNormal);
    	m_smoothingGroup = face.m_smoothingGroup;
    	m_material = face.m_material;
    	m_pSubMesh = face.m_pSubMesh;
    }
    
    /**
     * Calculates the face normal using the 3 vertices that make up the face
     * @param positions The array of positions used by the containing mesh's vertices
     */
    void CalculateFaceNormal(List<Vector4f> positions){
    	List<MeshVertex> verts = m_pSubMesh.m_srcVertices;
    	Vector4f pos0 = positions.get(verts.get(m_verts[0]).m_pos);
    	Vector4f pos1 = positions.get(verts.get(m_verts[1]).m_pos);
    	Vector4f pos2 = positions.get(verts.get(m_verts[2]).m_pos);
    	
    	Vector3f.computeNormal(pos0, pos1, pos2, m_faceNormal);
    	m_faceNormal.normalise();
    }

    /**
     * Returns the weight to be used for this face when averaging normals
     * @param positions The array of positions used by the containing mesh's vertices
     * @param vertIndex Index into the face's array of positions for the vertex being used to generate the normal
     * @return A value to be used to determine the face normal's contribution when averaging normals
     */
    float GetFaceWeight(List<Vector4f> positions, int vertIndex)
    {
        return GetFaceWeight_Normalized(positions, vertIndex);
    }

    /**
     * Returns the weight to be used for this face when averaging normals, in this case a constant so that all containing faces are equal.
     * @param positions The array of positions used by the containing mesh's vertices
     * @param vertIndex Index into the face's array of positions for the vertex being used to generate the normal
     * @return A value to be used to determine the face normal's contribution when averaging normals  
     */
    float GetFaceWeight_Constant(List<Vector4f> positions, int vertIndex)
    {
        return 1.0f;
    }

    /**
     * Returns the weight to be used for this face when averaging normals, in this case a value based on the total area of the triangle.
     * @param positions The array of positions used by the containing mesh's vertices
     * @param vertIndex Index into the face's array of positions for the vertex being used to generate the normal
     * @return A value to be used to determine the face normal's contribution when averaging normals 
     */
    float GetFaceWeight_Area(List<Vector4f> positions, int vertIndex){
    	List<MeshVertex> verts = m_pSubMesh.m_srcVertices;
    	
    	Vector4f pos0 = positions.get(verts.get(m_verts[vertIndex]).m_pos);
    	Vector4f pos1 = positions.get(verts.get(m_verts[(vertIndex + 1)%3]).m_pos);
    	Vector4f pos2 = positions.get(verts.get(m_verts[(vertIndex + 2)%3]).m_pos);
    	
    	Vector3f.computeNormal(pos0, pos1, pos2, m_faceNormal);
    	return m_faceNormal.length();
    }

    /**
     * Returns the weight to be used for this face when averaging normals, in this case a value based upon the angle between the edges that share the vertex in question.
     * @param positions The array of positions used by the containing mesh's vertices
     * @param vertIndex Index into the face's array of positions for the vertex being used to generate the normal
     * @return A value to be used to determine the face normal's contribution when averaging normals
     */
    float GetFaceWeight_Normalized(List<Vector4f> positions, int vertIndex) {
    	List<MeshVertex> verts = m_pSubMesh.m_srcVertices;
    	
    	Vector4f pos0 = positions.get(verts.get(m_verts[vertIndex]).m_pos);
    	Vector4f pos1 = positions.get(verts.get(m_verts[(vertIndex + 1)%3]).m_pos);
    	Vector4f pos2 = positions.get(verts.get(m_verts[(vertIndex + 2)%3]).m_pos);
    	
    	float vert0X = pos1.x - pos0.x;
    	float vert0Y = pos1.y - pos0.y;
    	float vert0Z = pos1.z - pos0.z;
    	
    	float vert1X = pos2.x - pos0.x;
    	float vert1Y = pos2.y - pos0.y;
    	float vert1Z = pos2.z - pos0.z;
    	
    	return 1.0f - (vert0X * vert1X + vert0Y * vert1Y + vert0Z * vert1Z);
    	
    }
}
