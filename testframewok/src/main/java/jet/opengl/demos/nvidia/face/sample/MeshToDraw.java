package jet.opengl.demos.nvidia.face.sample;

/**
 * Created by mazhen'gui on 2017/9/7.
 */

final class MeshToDraw {
    Material 	m_pMtl;
    IRenderable 		m_pMesh;

    // Parameters for SSS
    int			m_normalMapSize;			// Pixel size of normal map
    float		m_averageUVScale;			// Average UV scale of the mesh

    MeshToDraw(Material mtl, IRenderable mesh, int noramlMapSize, float averageUVScale){
        m_pMtl = mtl;
        m_pMesh = mesh;
        m_normalMapSize = noramlMapSize;
        m_averageUVScale = averageUVScale;
    }

    MeshToDraw(Material mtl, IRenderable mesh){
        this(mtl, mesh, 0,0);
    }

}
