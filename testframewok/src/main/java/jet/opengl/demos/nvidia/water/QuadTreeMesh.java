package jet.opengl.demos.nvidia.water;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;

import jet.opengl.demos.nvidia.waves.GFSDK_WaveWorks_Quadtree;
import jet.opengl.demos.nvidia.waves.GFSDK_WaveWorks_Quadtree_Params;

/**
 * Created by mazhen'gui on 2017/8/23.
 */

public class QuadTreeMesh implements WaterMesh {
    private GFSDK_WaveWorks_Quadtree m_Quadtree;

    public QuadTreeMesh(GFSDK_WaveWorks_Quadtree_Params params){
        m_Quadtree = new GFSDK_WaveWorks_Quadtree();
        m_Quadtree.init(params);
    }

    public void updateParams(GFSDK_WaveWorks_Quadtree_Params params){
        m_Quadtree.updateParams(params);
    }

    public void createPatch(int x, int y, int lod, boolean enabled){
        m_Quadtree.createPatch(x, y, lod, enabled);
    }

    public void deletePatch(int x, int y, int lod){
        m_Quadtree.deletePatch(x, y, lod);
    }

    public void drawMesh(Matrix4f matView, Matrix4f matProj, Vector2f pViewportDims){
        m_Quadtree.drawMesh(matView, matProj, pViewportDims);
    }

    public void setFrustumCullMargin (float margin){
        m_Quadtree.setFrustumCullMargin(margin);
    }
}
