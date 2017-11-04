package jet.opengl.demos.nvidia.shadows;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

final class MeshInstance {

	RigidMesh m_mesh;
    String m_name;
    Matrix4f m_worldTransform;
    
    public MeshInstance(RigidMesh mesh, String name, Matrix4f worldTransform) {
    	m_mesh = mesh;
    	m_name = name;
    	
    	if(worldTransform != null)
    		m_worldTransform = worldTransform;
    	else
    		m_worldTransform = new Matrix4f();
	}
    
    String getName() { return m_name; }

    void setWorldTransform(Matrix4f worldTransform) { m_worldTransform.load(worldTransform); }
    Matrix4f getWorldTransform() { return m_worldTransform; }

    Vector3f getCenter() { return m_mesh.getCenter(); }
    Vector3f getExtents() { return m_mesh.getExtents(); }

    Vector3f getWorldCenter() { 
//	        	return nv.vec3f(m_worldTransform * nv.vec4f(getCenter(), 1.0f)); 
//    	return VectorUtil.transformVector3(getCenter(), m_worldTransform, null);
    	return Matrix4f.transformVector(m_worldTransform, getCenter(), null);
    }
    
    /*void draw(SSSceneShader shader){
    	shader.setWorldMatrix(m_worldTransform);
        m_mesh.render(shader);
    }
    
    void draw(IShader shader){
    	m_mesh.render(shader);
    }*/
    
    int accumStatsIndex(int numIndices){
    	return numIndices + m_mesh.getIndexCount();
    }
    
    int accumStatsVertex(int numVertices){
    	return numVertices + m_mesh.getVertexCount();
    }
}
