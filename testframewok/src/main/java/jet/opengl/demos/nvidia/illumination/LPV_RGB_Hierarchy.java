package jet.opengl.demos.nvidia.illumination;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector2i;

/**
 * Created by Administrator on 2017/11/13 0013.
 */

final class LPV_RGB_Hierarchy extends Hierarchy implements RTCollection_RGB{
    SimpleRT_RGB[] m_collection;
    int m_levels;

    public int Create(int levels, /*ID3D11Device* pd3dDevice,*/ int width2D, int height2D, int width3D, int height3D, int depth3D,
               int format, boolean uav, boolean doublebuffer, boolean use3DTex, boolean use2DTexArray, int numRTs /*= 1*/ )
    {
//        HRESULT hr = S_OK;
        m_collection = new SimpleRT_RGB[levels];
        for(int j=0;j<levels;j++) m_collection[j] = null;

        int i=0;
        float div=1.0f;
        for(;i<levels;i++)
        {
            m_collection[i] = new SimpleRT_RGB();

            int newWidth3D = (int)(width3D/div);
            int newHeight3D = (int)(height3D/div);
            int newDepth3D = (int)(depth3D/div);
            if(newWidth3D<4 || newHeight3D<4 || (newDepth3D<4 && use3DTex) )
                break;
            /*int cols, rows;
            ComputeRowColsForFlat3DTexture( newDepth3D, cols, rows );*/
            Vector2i gird = new Vector2i();
            Grid.ComputeRowsColsForFlat3DTexture(newDepth3D, gird);
            int newWidth2D = gird.x*newWidth3D;
            int newHeight2D = gird.y*newHeight3D;

            m_collection[i].Create( /*pd3dDevice,*/ newWidth2D, newHeight2D, newWidth3D, newHeight3D, newDepth3D, format, uav,
                doublebuffer, use3DTex, use2DTexArray, numRTs );
            div *= 2.0f;
        }
        m_levels = i;
        levels = m_levels;
        return levels;
    }

    void clearRenderTargetView(/*ID3D11DeviceContext* pd3dContext,*/ float clearColor[], boolean front, int level)
    {
        m_collection[level].clearRenderTargetView(/*pd3dContext,*/clearColor,front);
    }

    public void setLPVTransformsRotatedAndOffset(float LPVscale, ReadableVector3f LPVtranslate, Matrix4f cameraViewMatrix, ReadableVector3f viewVector)
    {
        for(int level=0; level<m_levels; level++)
            m_collection[level].setLPVTransformsRotatedAndOffset(LPVscale, LPVtranslate, cameraViewMatrix, viewVector, m_collection[level].getWidth3D(),
                    m_collection[level].getHeight3D(), m_collection[level].getDepth3D());
    }

    @Override
    public SimpleRT_RGB get(int level) {
        return m_collection[level];
    }

    //downsample level finerLevel to level finerLevel+1
    void Downsample(/*ID3D11DeviceContext* pd3dContext,*/ int finerLevel, int op)
    {
        int coarserLevel = finerLevel+1;
        if(finerLevel<0 || coarserLevel>=m_levels) return;
        if(m_collection[finerLevel].getNumRTs()==1)
        {
            super.Downsample(/*pd3dContext,*/ m_collection[finerLevel].getRed(true), m_collection[coarserLevel].getRed(true), op, 0,-1,-1,-1);
            super.Downsample(/*pd3dContext,*/ m_collection[finerLevel].getGreen(true), m_collection[coarserLevel].getGreen(true), op, 0,-1,-1,-1);
            super.Downsample(/*pd3dContext,*/ m_collection[finerLevel].getBlue(true), m_collection[coarserLevel].getBlue(true), op, 0,-1,-1,-1);
        }
        else if(m_collection[finerLevel].getNumRTs()==4)
        {
            super.Downsample(/*pd3dContext,*/ m_collection[finerLevel].getRed(true), m_collection[coarserLevel].getRed(true), op, 0, 1, 2, 3);
            super.Downsample(/*pd3dContext,*/ m_collection[finerLevel].getGreen(true), m_collection[coarserLevel].getGreen(true), op, 0, 1, 2, 3);
            super.Downsample(/*pd3dContext,*/ m_collection[finerLevel].getBlue(true), m_collection[coarserLevel].getBlue(true), op, 0, 1, 2, 3);
        }
        else
        assert(false); //this path is not implemented

    }

    //upsample level coarserLevel to level coarserLevel-1
    void Upsample(/*ID3D11DeviceContext* pd3dContext,*/ int coarserLevel, int upsampleOp, int sampleType)
    {
        int finerLevel = coarserLevel-1;
        if(finerLevel<0 || coarserLevel>=m_levels) return;

        if(sampleType == SAMPLE_ACCUMULATE)
        {
            //note! for the case where we have multiple buffers we want to make one call to a specialized version of upsample, instead of iterating as below!

            //we want to upsample the coarse level and add it to the finer level. this means we have to swap buffers on the finer level (cant read and write to a float4 UAV)
            if(m_collection[finerLevel].getNumChannels() > 1)
            {
                m_collection[finerLevel].swapBuffers();
                for(int rt=0; rt<m_collection[finerLevel].getNumRTs(); rt++)
                {
                    super.Upsample(/*pd3dContext,*/ m_collection[finerLevel].getRed(true), m_collection[coarserLevel].getRed(true), upsampleOp, sampleType, m_collection[finerLevel].getRedBack().get_pSRV(rt), rt);
                    super.Upsample(/*pd3dContext,*/ m_collection[finerLevel].getGreen(true), m_collection[coarserLevel].getGreen(true), upsampleOp, sampleType, m_collection[finerLevel].getGreenBack().get_pSRV(rt), rt);
                    super.Upsample(/*pd3dContext,*/ m_collection[finerLevel].getBlue(true), m_collection[coarserLevel].getBlue(true), upsampleOp, sampleType,  m_collection[finerLevel].getBlueBack().get_pSRV(rt), rt);
                }
            }
            else
            {
                assert(m_collection[finerLevel].getNumRTs()==4); //only 4 rendertargets version is implemented
                //in this version we will be doing inplace updates
                super.UpsampleAccumulateInplace4(/*pd3dContext,*/ m_collection[finerLevel].getRed(true),0,1,2,3, m_collection[coarserLevel].getRed(true), 0,1,2,3, upsampleOp, sampleType);
                super.UpsampleAccumulateInplace4(/*pd3dContext,*/ m_collection[finerLevel].getGreen(true),0,1,2,3, m_collection[coarserLevel].getGreen(true),0,1,2,3, upsampleOp, sampleType);
                super.UpsampleAccumulateInplace4(/*pd3dContext,*/ m_collection[finerLevel].getBlue(true),0,1,2,3, m_collection[coarserLevel].getBlue(true),0,1,2,3, upsampleOp, sampleType);
            }
        }
        else
        {
            super.Upsample(/*pd3dContext,*/ m_collection[finerLevel].getRed(true), m_collection[coarserLevel].getRed(true), upsampleOp, sampleType, null, 0);
            super.Upsample(/*pd3dContext,*/ m_collection[finerLevel].getGreen(true), m_collection[coarserLevel].getGreen(true), upsampleOp, sampleType, null, 0);
            super.Upsample(/*pd3dContext,*/ m_collection[finerLevel].getBlue(true), m_collection[coarserLevel].getBlue(true), upsampleOp, sampleType, null, 0);
        }
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

    int getNumLevels() {return m_levels; };

    int getWidth3D(int level) {return  m_collection[level].getWidth3D(); }
    int getHeight3D(int level) {return  m_collection[level].getHeight3D(); }
    int getDepth3D(int level) {return  m_collection[level].getDepth3D(); }

    int getWidth2D(int level) {return  m_collection[level].getWidth2D(); }
    int getHeight2D(int level) {return  m_collection[level].getHeight2D(); }

    int getNumCols(int level) {return  m_collection[level].getNumCols(); }
    int getNumRows(int level) {return  m_collection[level].getNumRows(); }
}
