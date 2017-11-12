package jet.opengl.demos.nvidia.illumination;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;

/**
 * Created by Administrator on 2017/11/12 0012.
 */
final class SimpleRT_RGB extends BasicLPVTransforms implements Disposeable{
    private final SimpleRT[] Red = new SimpleRT[2];
    private final SimpleRT[] Green = new SimpleRT[2];
    private final SimpleRT[] Blue = new SimpleRT[2];
    private int m_currentBuffer;
    private boolean m_doubleBuffer;
    BufferGL m_pRenderToLPV_VB;

    private SimpleRT CreateRT(/*ID3D11Device* pd3dDevice,*/ int width2D, int height2D, int width3D, int height3D, int depth3D,
                  int format, boolean uav, boolean use3DTex, boolean use2DTexArray, int numRTs /*= 1*/) {
        SimpleRT rt = new SimpleRT();
        if(use3DTex)
             rt.Create3D( /*pd3dDevice,*/ width3D, height3D, depth3D, format, uav, numRTs);
        else if(use2DTexArray)
             rt.Create2DArray( /*pd3dDevice,*/ width3D, height3D, depth3D, format, uav, numRTs);
        else
             rt.Create2D( /*pd3dDevice,*/ width2D, height2D, width3D, height3D, depth3D, format, uav, numRTs);

        return rt;
    }

    private int getBackbufferIndex()
    {
        if(m_doubleBuffer) return 1-m_currentBuffer;
        else return m_currentBuffer;
    }

    SimpleRT_RGB() {
        Red[0] = Red[1] = null;
        Green[0] = Green[1] = null;
        Blue[0] = Blue[1] = null;
        m_currentBuffer = 0;
        m_doubleBuffer = false;
        m_pRenderToLPV_VB = null;
    };
    void Create( /*ID3D11Device* pd3dDevice,*/ int width2D, int height2D, int width3D, int height3D, int depth3D, int format,
                 boolean uav , boolean doublebuffer, boolean use3DTex, boolean use2DTexArray, int numRTs /*= 1*/)
    {
//        HRESULT hr = S_OK;

        Red[0] = new SimpleRT();
        Green[0] = new SimpleRT();
        Blue[0] = new SimpleRT();

        Red[0] = CreateRT(/*pd3dDevice,*/ width2D, height2D, width3D, height3D, depth3D, format, uav, use3DTex, use2DTexArray, numRTs);
        Green[0] = CreateRT(/*pd3dDevice,*/ width2D, height2D, width3D, height3D, depth3D, format, uav, use3DTex, use2DTexArray, numRTs);
        Blue[0] = CreateRT(/*pd3dDevice,*/ width2D, height2D, width3D, height3D, depth3D, format, uav, use3DTex, use2DTexArray, numRTs);

        m_doubleBuffer = doublebuffer;
        if(m_doubleBuffer)
        {
            Red[1] = new SimpleRT();
            Green[1] = new SimpleRT();
            Blue[1] = new SimpleRT();
            Red[1] = CreateRT(/*, pd3dDevice,*/ width2D, height2D, width3D, height3D, depth3D, format, uav, use3DTex, use2DTexArray, numRTs);
            Green[1] = CreateRT(/*, pd3dDevice,*/width2D, height2D, width3D, height3D, depth3D, format, uav, use3DTex, use2DTexArray, numRTs);
            Blue[1] = CreateRT(/*, pd3dDevice,*/ width2D, height2D, width3D, height3D, depth3D, format, uav, use3DTex, use2DTexArray, numRTs);
        }

        //create the Vertex Buffer to use for rendering to this grid
        int numVertices = 6 * depth3D;
        VS_INPUT_GRID_STRUCT[] verticesLPV = new VS_INPUT_GRID_STRUCT[numVertices];

        for(int d=0;d<depth3D;d++)
        {
            verticesLPV[d*6] =    Tex3PosVertex( -1.0f, -1.0f, 0.0f, 0.0f, 1.0f, (float)d);
            verticesLPV[d*6+1] =  Tex3PosVertex( -1.0f, 1.0f,  0.0f, 0.0f, 0.0f, (float)d);
            verticesLPV[d*6+2] =  Tex3PosVertex( 1.0f, -1.0f,  0.0f, 1.0f, 1.0f, (float)d);

            verticesLPV[d*6+3] =  Tex3PosVertex( 1.0f, -1.0f,  0.0f, 1.0f, 1.0f, (float)d);
            verticesLPV[d*6+4] =  Tex3PosVertex( -1.0f, 1.0f,  0.0f, 0.0f, 0.0f, (float)d);
            verticesLPV[d*6+5] =  Tex3PosVertex( 1.0f, 1.0f,   0.0f, 1.0f, 0.0f, (float)d);
        }

        /*D3D11_BUFFER_DESC bd;
        D3D11_SUBRESOURCE_DATA InitData;
        bd.Usage = D3D11_USAGE_DEFAULT;
        bd.ByteWidth = sizeof( Tex3PosVertex ) * numVertices;
        bd.BindFlags = D3D11_BIND_VERTEX_BUFFER;
        bd.CPUAccessFlags = 0;
        bd.MiscFlags = 0;
        InitData.pSysMem = verticesLPV;
        V_RETURN(  pd3dDevice->CreateBuffer( &bd, &InitData, &m_pRenderToLPV_VB ) );
        delete verticesLPV;*/
        ByteBuffer bytes = CacheBuffer.wrap(VS_INPUT_GRID_STRUCT.SIZE, verticesLPV);
        m_pRenderToLPV_VB = new BufferGL();
        m_pRenderToLPV_VB.initlize(GLenum.GL_ARRAY_BUFFER, VS_INPUT_GRID_STRUCT.SIZE * numVertices, bytes, GLenum.GL_STATIC_DRAW);
        m_pRenderToLPV_VB.unbind();
    }

    private static VS_INPUT_GRID_STRUCT Tex3PosVertex(float x, float y, float z, float s, float t, float r){
        return new VS_INPUT_GRID_STRUCT(x,y,z,s,t,r);
    }

    void clearRenderTargetView(/*ID3D11DeviceContext* pd3dContext,*/ final float clearColor[], boolean front)
    {
        int Buffer = m_currentBuffer;
        if(!front)
        {
            if(!m_doubleBuffer) return;
            Buffer = getBackbufferIndex();
        }
        final GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();

        for(int i=0;i<Red[Buffer].getNumRTs();i++)
//        pd3dContext->ClearRenderTargetView( Red[Buffer]->get_pRTV(i), clearColor );
            gl.glClearTexImage(Red[Buffer].get_pRTV(i).getTexture(), 0, TextureUtils.measureFormat(Red[Buffer].get_pRTV(i).getFormat()),
                    TextureUtils.measureDataType(Red[Buffer].get_pRTV(i).getFormat()),
                    clearColor != null ? CacheBuffer.wrap(clearColor):null);
        for(int i=0;i<Green[Buffer].getNumRTs();i++)
//        pd3dContext->ClearRenderTargetView( Green[Buffer]->get_pRTV(i), clearColor );
        gl.glClearTexImage(Green[Buffer].get_pRTV(i).getTexture(), 0, TextureUtils.measureFormat(Green[Buffer].get_pRTV(i).getFormat()),
                TextureUtils.measureDataType(Green[Buffer].get_pRTV(i).getFormat()),
                clearColor != null ? CacheBuffer.wrap(clearColor):null);
        for(int i=0;i<Blue[Buffer].getNumRTs();i++)
//        pd3dContext->ClearRenderTargetView( Blue[Buffer]->get_pRTV(i), clearColor );
        gl.glClearTexImage(Blue[Buffer].get_pRTV(i).getTexture(), 0, TextureUtils.measureFormat(Blue[Buffer].get_pRTV(i).getFormat()),
                TextureUtils.measureDataType(Blue[Buffer].get_pRTV(i).getFormat()),
                clearColor != null ? CacheBuffer.wrap(clearColor):null);
    }

    SimpleRT getRed(boolean front/*=true*/) { int Buffer = front? m_currentBuffer : getBackbufferIndex(); return Red[Buffer]; }
    SimpleRT getBlue(boolean front/*=true*/) { int Buffer = front? m_currentBuffer : getBackbufferIndex();  return Blue[Buffer]; }
    SimpleRT getGreen(boolean front/*=true*/) { int Buffer = front? m_currentBuffer : getBackbufferIndex();  return Green[Buffer]; }

    SimpleRT getRedFront() { return Red[m_currentBuffer]; }
    SimpleRT getBlueFront() { return Blue[m_currentBuffer]; }
    SimpleRT getGreenFront() { return Green[m_currentBuffer]; }

    SimpleRT getRedBack() { return Red[getBackbufferIndex()]; }
    SimpleRT getBlueBack() { return Blue[getBackbufferIndex()]; }
    SimpleRT getGreenBack() { return Green[getBackbufferIndex()]; }

    int getNumChannels() { return Red[m_currentBuffer].getNumChannels(); }

    int getNumRTs() {return Red[m_currentBuffer].getNumRTs(); }

    void swapBuffers() {if(m_doubleBuffer) m_currentBuffer = 1 - m_currentBuffer; }

    int getCurrentBuffer() {return m_currentBuffer; }

    int getWidth3D() {return  Red[0].getWidth3D(); }
    int getHeight3D() {return Red[0].getHeight3D(); }
    int getDepth3D() {return  Red[0].getDepth3D(); }

    int getWidth2D() {return  Red[0].getWidth2D(); }
    int getHeight2D() {return  Red[0].getHeight2D(); }

    int getNumCols() {return  Red[0].getNumCols(); }
    int getNumRows() {return  Red[0].getNumRows(); }


    public void dispose()
    {
        CommonUtil.safeRelease(Red[0]);
        CommonUtil.safeRelease(Green[0]);
        CommonUtil.safeRelease(Blue[0]);
        CommonUtil.safeRelease(Red[1]);
        CommonUtil.safeRelease(Green[1]);
        CommonUtil.safeRelease(Blue[1]);
        CommonUtil.safeRelease( m_pRenderToLPV_VB );
    }
}
