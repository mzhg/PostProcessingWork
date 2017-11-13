package jet.opengl.demos.nvidia.illumination;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

/**
 * Created by Administrator on 2017/11/13 0013.
 */

final class LPV_Cascade extends Cascade implements RTCollection{

    SimpleRT[] m_collection;
    int m_levels;

    LPV_Cascade(float cascadeScale, Vector3f cascadeTranslate) {
        super(cascadeScale, cascadeTranslate);
    }

    @Override
    public void dispose() {

    }

    @Override
    public int Create2D(int levels, int width2D, int height2D, int width3D, int height3D, int depth3D, int format, boolean uav) {
//        HRESULT hr = S_OK;
        m_levels = levels;

        m_collection = new SimpleRT[levels];

        for(int level=0; level<m_levels; level++)
        {
            m_collection[level] = new SimpleRT();
            m_collection[level].Create2D( /*pd3dDevice,*/ width2D, height2D, width3D, height3D, depth3D, format, uav,1);
        }

        return levels;
    }

    @Override
    public int Create3D(int levels, int width, int height, int depth, int format, boolean uav) {
//        HRESULT hr = S_OK;
        m_levels = levels;

        m_collection = new SimpleRT[levels];

        for(int level=0; level<m_levels; level++)
        {
            m_collection[level] = new SimpleRT();
            m_collection[level].Create3D( /*pd3dDevice,*/ width, height, depth, format, uav, 1);
        }

        return levels;
    }

    @Override
    public int Create2DArray(int levels, int width, int height, int depth, int format, boolean uav) {
//        HRESULT hr = S_OK;
        m_levels = levels;

        m_collection = new SimpleRT[levels];

        for(int level=0; level<m_levels; level++)
        {
            m_collection[level] = new SimpleRT();
            m_collection[level].Create2DArray( /*pd3dDevice,*/ width, height, depth, format, uav,1);
        }

        return levels;
    }

    //all the LPVs in the hierarchy have the same transforms, since they are colocated
    @Override
    public void setLPVTransformsRotatedAndOffset(float LPVscale, ReadableVector3f LPVtranslate, Matrix4f cameraViewMatrix, ReadableVector3f viewVector) {
        for(int i=0; i<m_levels; i++)
            m_collection[i].setLPVTransformsRotatedAndOffset(LPVscale, LPVtranslate, cameraViewMatrix, viewVector, m_collection[i].getWidth3D(), m_collection[i].getHeight3D(),
                    m_collection[i].getDepth3D());
    }

    @Override
    public SimpleRT getRenderTarget(int level) {
        return m_collection[level];
    }

    @Override
    public int getNumLevels() {
        return m_levels;
    }
}
