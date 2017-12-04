package jet.opengl.demos.nvidia.illumination;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

/**
 * Created by Administrator on 2017/11/13 0013.
 */

final class LPV_RGB_Cascade extends Cascade implements RTCollection_RGB{
    SimpleRT_RGB[] m_collection;
    int m_levels;


    LPV_RGB_Cascade(float cascadeScale, Vector3f cascadeTranslate) {
        super(cascadeScale, cascadeTranslate);
    }

    public int Create(int levels, /*ID3D11Device* pd3dDevice,*/ int width2D, int height2D, int width3D, int height3D, int depth3D,
               int format, boolean uav, boolean doublebuffer, boolean use3DTex, boolean use2DTexArray, int numRTs /*= 1*/ )
    {
//        HRESULT hr = S_OK;

        m_collection = new SimpleRT_RGB[levels];
        for(int j=0;j<levels;j++) m_collection[j] = null;

        m_levels = levels;
        for(int level=0; level<m_levels; level++)
        {
            m_collection[level] = new SimpleRT_RGB();
            m_collection[level].Create( /*pd3dDevice,*/ width2D, height2D, width3D, height3D, depth3D, format, uav, doublebuffer, use3DTex, use2DTexArray, numRTs );
        }

        return levels;
    }

    public void clearRenderTargetView(/*ID3D11DeviceContext* pd3dContext,*/ float clearColor[], boolean front, int level)
    {
        m_collection[level].clearRenderTargetView(/*pd3dContext,*/clearColor,front);
    }

    public void setLPVTransformsRotatedAndOffset(float LPVscale, ReadableVector3f LPVtranslate, Matrix4f cameraViewMatrix, ReadableVector3f viewVector)
    {
        Vector3f tmp0 = new Vector3f();
        for(int level=0; level<m_levels; level++) {
            m_collection[level].setLPVTransformsRotatedAndOffset(LPVscale * getCascadeScale(level), Vector3f.add(LPVtranslate ,getCascadeTranslate(level), tmp0),
                    cameraViewMatrix, viewVector, m_collection[level].
            getWidth3D(), m_collection[level].getHeight3D(), m_collection[level].getDepth3D());
        }
    }

    @Override
    public SimpleRT_RGB get(int level) {
        return m_collection[level];
    }

    public SimpleRT getRed(int level, boolean front/*=true*/) { return m_collection[level].getRed(front); }
    public SimpleRT getBlue(int level, boolean front/*=true*/) { return m_collection[level].getBlue(front); }
    public SimpleRT getGreen(int level, boolean front/*=true*/) { return m_collection[level].getGreen(front); }

    public SimpleRT getRedFront(int level) { return m_collection[level].getRedFront(); }
    public SimpleRT getBlueFront(int level) { return m_collection[level].getBlueFront(); }
    public SimpleRT getGreenFront(int level) { return m_collection[level].getGreenFront(); }

    public SimpleRT getRedBack(int level) { return m_collection[level].getRedBack(); }
    public SimpleRT getBlueBack(int level) { return m_collection[level].getBlueBack();  }
    public SimpleRT getGreenBack(int level) { return m_collection[level].getGreenBack(); }

    void swapBuffers(int level) { m_collection[level].swapBuffers();  }

    int getCurrentBuffer(int level) { return m_collection[level].getCurrentBuffer(); }

    public int getNumLevels() {return m_levels; };

    public int getWidth3D(int level) {return  m_collection[level].getWidth3D(); }
    public int getHeight3D(int level) {return  m_collection[level].getHeight3D(); }
    public int getDepth3D(int level) {return  m_collection[level].getDepth3D(); }

    public int getWidth2D(int level) {return  m_collection[level].getWidth2D(); }
    public int getHeight2D(int level) {return  m_collection[level].getHeight2D(); }

    public int getNumCols(int level) {return  m_collection[level].getNumCols(); }
    public int getNumRows(int level) {return  m_collection[level].getNumRows(); }

    @Override
    public void dispose() {

    }
}
