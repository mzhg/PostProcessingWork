package jet.opengl.demos.nvidia.illumination;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector2i;

import jet.opengl.postprocessing.util.CommonUtil;

/**
 * Created by Administrator on 2017/11/12 0012.
 */

final class LPV_Hierarchy extends Hierarchy implements RTCollection{
    private SimpleRT[] m_collection;
    private int m_levels;

    int getWidth2D(int level) {return m_collection[level].getWidth2D();}
    int getHeight2D(int level) {return m_collection[level].getHeight2D();}

    @Override
    public int Create2D(int levels, int width2D, int height2D, int width3D, int height3D, int depth3D, int format, boolean uav) {
//        HRESULT hr = S_OK;
        m_collection = new SimpleRT[levels];
        for(int j=0;j<levels;j++) m_collection[j] = null;
        int i=0;
        float div=1.0f;
        for(;i<levels;i++)
        {
            //note: this is assuming that we are only using this hierarchy to for 2D textures encoding 3D textures. should have a bools saying if that is the case or not. for the moment this is true

            m_collection[i] = new SimpleRT();
            int newWidth3D = (int)(width3D/div);
            int newHeight3D = (int)(height3D/div);
            int newDepth3D = (int)(depth3D/div);
            if(newWidth3D<4 || newHeight3D<4 || newDepth3D<4 )
                break;
            int cols, rows;
            Vector2i grid = new Vector2i();
            Grid.ComputeRowsColsForFlat3DTexture( newDepth3D, /*cols, rows*/grid );
            int newWidth2D = grid.x*newWidth3D;
            int newHeight2D = grid.y*newHeight3D;

            m_collection[i].Create2D( /*pd3dDevice,*/ newWidth2D, newHeight2D, newWidth3D, newHeight3D, newDepth3D, format, uav,1 );
            div *= 2.0f;
        }
        m_levels = i;
        levels = m_levels;
        return levels;
    }

    @Override
    public int Create3D(int levels, int width, int height, int depth, int format, boolean uav) {
        m_collection = new SimpleRT[levels];
        int i=0;
        float div=1.0f;
        for(;i<levels;i++)
        {
            m_collection[i] = new SimpleRT();
            int newWidth = (int)(width/div);
            int newHeight = (int)(height/div);
            int newDepth = (int)(depth/div);
            if(newWidth<4 || newHeight<4 || newDepth<4 )
                break;
            m_collection[i].Create3D(/* pd3dDevice,*/ newWidth, newHeight, newDepth, format, uav,1 );
            div *= 2.0f;
        }
        m_levels = i;
        levels = m_levels;
        return levels;
    }

    @Override
    public int Create2DArray(int levels, int width, int height, int depth, int format, boolean uav) {
        m_collection = new SimpleRT[levels];
        int i=0;
        float div=1.0f;
        for(;i<levels;i++)
        {
            m_collection[i] = new SimpleRT();
            int newWidth = (int)(width/div);
            int newHeight = (int)(height/div);
            int newDepth = (int)(depth/div);
            if(newWidth<4 || newHeight<4 || newDepth<4 )
                break;
            m_collection[i].Create2DArray(/* pd3dDevice,*/ newWidth, newHeight, newDepth, format, uav,1 );
            div *= 2.0f;
        }
        m_levels = i;
        levels = m_levels;
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

    //downsample level finerLevel to level finerLevel+1
    void Downsample(/*ID3D11DeviceContext* pd3dContext,*/ int finerLevel,int op)
    {
        int coarserLevel = finerLevel+1;
        if(finerLevel<0 || coarserLevel>=m_levels) return;
        if(m_collection[finerLevel].getNumRTs()==1)
            super.Downsample(/*pd3dContext,*/m_collection[finerLevel], m_collection[coarserLevel], op, 0,-1,-1,-1);
        else if(m_collection[finerLevel].getNumRTs()==4)
            super.Downsample(/*pd3dContext,*/m_collection[finerLevel], m_collection[coarserLevel], op, 0, 1, 2, 3);
        else
        assert(false); // this path is not implemented yet!
    }

    //upsample level coarserLevel to level coarserLevel-1
    void Upsample(/*ID3D11DeviceContext* pd3dContext,*/ int coarserLevel, int upsampleOp, int sampleType)
    {
        int finerLevel = coarserLevel-1;
        if(finerLevel<0 || coarserLevel>=m_levels) return;
        if(sampleType == SAMPLE_ACCUMULATE)
        {
            //this option is not implemented since we dont have double buffering in SimpleRT!
            assert(false);

        }
        else
            super.Upsample(/*pd3dContext,*/ m_collection[finerLevel], m_collection[coarserLevel], upsampleOp, sampleType, null,-1);
    }

    @Override
    public void dispose() {
        for(int i=0;i<m_levels;i++)
            CommonUtil.safeRelease(m_collection[i]);
        m_collection = null;
    }
}
