package jet.opengl.demos.nvidia.shadows;

import com.nvidia.developer.opengl.utils.BoundingBox;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

final class MeshInstance {

	private RigidMesh m_mesh;
    private String m_name;
    private Matrix4f m_worldTransform;
    private final BoundingBox m_bounds = new BoundingBox();
    
    public MeshInstance(RigidMesh mesh, String name, Matrix4f worldTransform) {
    	m_mesh = mesh;
    	m_name = name;
    	
    	if(worldTransform != null)
    		m_worldTransform = worldTransform;
    	else
    		m_worldTransform = new Matrix4f();

        m_bounds.setFromExtent(m_mesh.getCenter(), m_mesh.getExtents());

        if(worldTransform != null)
            BoundingBox.transform(m_worldTransform, m_bounds,m_bounds);
	}
    
    String getName() { return m_name; }
    RigidMesh getMesh() { return m_mesh;}

    void setWorldTransform(Matrix4f worldTransform) {
        m_worldTransform.load(worldTransform);
        m_bounds.setFromExtent(m_mesh.getCenter(), m_mesh.getExtents());
        BoundingBox.transform(worldTransform, m_bounds,m_bounds);  // update the world bounding box.
    }

    BoundingBox getBounds() { return m_bounds;}

    Matrix4f getWorldTransform() { return m_worldTransform; }

    Vector3f getCenter() { return m_mesh.getCenter(); }
    Vector3f getExtents() { return m_mesh.getExtents(); }

    Vector3f getWorldCenter() { 
//	        	return nv.vec3f(m_worldTransform * nv.vec4f(getCenter(), 1.0f)); 
//    	return VectorUtil.transformVector3(getCenter(), m_worldTransform, null);
//    	return Matrix4f.transformVector(m_worldTransform, getCenter(), null);
        return m_bounds.center(null);
    }
    
    /*void draw(SoftShadowSceneRenderProgram shader){
    	shader.setWorldMatrix(m_worldTransform);
        m_mesh.render(shader);
    }*/
    
    /*void draw(IShader shader){
    	m_mesh.render(shader);
    }*/
    
    int accumStatsIndex(int numIndices){
    	return numIndices + m_mesh.getIndexCount();
    }
    
    int accumStatsVertex(int numVertices){
    	return numVertices + m_mesh.getVertexCount();
    }
}
