package jet.opengl.demos.nvidia.illumination;

import org.lwjgl.util.vector.Vector2i;
import org.lwjgl.util.vector.Vector3f;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.buffer.VertexArrayObject;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;

/**
 * Created by Administrator on 2017/11/12 0012.
 */

final class Grid implements Disposeable{

    static final int VERTICES_PER_SLICE =6,
            VERTICES_PER_LINE =2,
            LINES_PER_SLICE =4;

    // Internal State
    //===============
//    ID3D11Device*              m_pD3DDevice;
//    ID3D11DeviceContext*       m_pD3DContext;

    private VertexArrayObject m_layout;
    private BufferGL              m_renderQuadBuffer;
    private BufferGL              m_slicesBuffer;
    private BufferGL              m_boundarySlicesBuffer;
    private BufferGL              m_boundaryLinesBuffer;

    private int m_numVerticesRenderQuad;
    private int m_numVerticesSlices;
    private int m_numVerticesBoundarySlices;
    private int m_numVerticesBoundaryLines;
    private int m_cols;
    private int m_rows;

    private final int[] m_dim = new int[3];

    void Initialize( int gridWidth, int gridHeight, int gridDepth/*,void* shaderBytecode, SIZE_T shaderBytecodeLength*/ ){
        m_dim[0] = gridWidth;
        m_dim[1] = gridHeight;
        m_dim[2] = gridDepth;

        Vector2i grid = new Vector2i();
        ComputeRowsColsForFlat3DTexture(m_dim[2],/* &m_cols, &m_rows*/grid);
        m_cols = grid.x;
        m_rows = grid.y;

        CreateVertexBuffers(/*shaderBytecode, shaderBytecodeLength*/);
    }

    void DrawSlices        (  ){
        int stride = Vector3f.SIZE * 2;
        int offset = 0;
        DrawPrimitive( GLenum.GL_TRIANGLES, m_layout, m_slicesBuffer,stride, offset, 0, m_numVerticesSlices );
        //DrawPrimitive( D3D11_PRIMITIVE_TOPOLOGY_3_CONTROL_POINT_PATCHLIST, m_layout, &m_slicesBuffer,stride, offset, 0, m_numVerticesSlices );
    }

    void DrawSlice         ( int slice ){
        int stride = Vector3f.SIZE * 2;
        int offset = 0;
        DrawPrimitive( GLenum.GL_TRIANGLES, m_layout, m_slicesBuffer,
                stride, offset, VERTICES_PER_SLICE*slice, VERTICES_PER_SLICE );
    }

    void DrawSlicesToScreen(  ){
        int stride = Vector3f.SIZE * 2;
        int offset = 0;
        DrawPrimitive( GLenum.GL_TRIANGLES, m_layout, m_renderQuadBuffer,
                stride, offset, 0, m_numVerticesRenderQuad );
    }

    void DrawBoundaryQuads (  ){
        int stride = Vector3f.SIZE * 2;
        int offset = 0;
        DrawPrimitive( GLenum.GL_TRIANGLES, m_layout, m_boundarySlicesBuffer,
                stride, offset, 0, m_numVerticesBoundarySlices );
    }

    void DrawBoundaryLines (  ){
        int stride = Vector3f.SIZE * 2;
        int offset = 0;
        DrawPrimitive( GLenum.GL_LINES, m_layout, m_boundaryLinesBuffer,
                stride, offset, 0, m_numVerticesBoundaryLines  );
    }

    int  GetCols(){return m_cols;};
    int  GetRows(){return m_rows;};

    int  GetDimX() {return m_dim[0]; }
    int  GetDimY() {return m_dim[1]; }
    int  GetDimZ() {return m_dim[2]; }

    static void ComputeRowsColsForFlat3DTexture(int depth, /*int *outCols, int *outRows*/Vector2i result)
    {
        // Compute # of rows and cols for a "flat 3D-texture" configuration
        // (in this configuration all the slices in the volume are spread in a single 2D texture)
        int rows =(int)Math.floor(Math.sqrt(depth));
        int cols = rows;
        while( rows * cols < depth ) {
            cols++;
        }
        assert( rows*cols >= depth );

        result.x = cols;
        result.y = rows;
    }


    private void CreateVertexBuffers  (/*void* shaderBytecode, SIZE_T shaderBytecodeLength*/){
       /* HRESULT hr(S_OK);

        // Create layout
        D3D11_INPUT_ELEMENT_DESC layoutDesc[] =
                {
                        { "POSITION", 0, DXGI_FORMAT_R32G32B32_FLOAT,       0, 0, D3D11_INPUT_PER_VERTEX_DATA, 0 },
                        { "TEXCOORD", 0, DXGI_FORMAT_R32G32B32_FLOAT,       0,12, D3D11_INPUT_PER_VERTEX_DATA, 0 },
                };
        UINT numElements = sizeof(layoutDesc)/sizeof(layoutDesc[0]);
        CreateLayout( layoutDesc, numElements, shaderBytecode, shaderBytecodeLength , &m_layout);*/

        int index = (0);
        VS_INPUT_GRID_STRUCT []renderQuad;
        VS_INPUT_GRID_STRUCT []slices;
        VS_INPUT_GRID_STRUCT []boundarySlices;
        VS_INPUT_GRID_STRUCT []boundaryLines;

//        try
        {
        final int VERTICES_PER_SLICE = 6,
                  VERTICES_PER_LINE =2,
                  LINES_PER_SLICE =4;

            m_numVerticesRenderQuad = VERTICES_PER_SLICE * m_dim[2];
            renderQuad = new VS_INPUT_GRID_STRUCT[ m_numVerticesRenderQuad ];

            m_numVerticesSlices = VERTICES_PER_SLICE * (m_dim[2] - 2);
            slices = new VS_INPUT_GRID_STRUCT[ m_numVerticesSlices ];

            m_numVerticesBoundarySlices = VERTICES_PER_SLICE * 2;
            boundarySlices = new VS_INPUT_GRID_STRUCT[ m_numVerticesBoundarySlices ];

            m_numVerticesBoundaryLines = VERTICES_PER_LINE * LINES_PER_SLICE * (m_dim[2]);
            boundaryLines = new VS_INPUT_GRID_STRUCT[ m_numVerticesBoundaryLines ];
        }
        /*catch(...)
        {
            hr = E_OUTOFMEMORY;
        goto cleanup;
        }*/

        assert(renderQuad!=null && m_numVerticesSlices!=0 && m_numVerticesBoundarySlices !=0&& m_numVerticesBoundaryLines!=0);

        // Vertex buffer for "m_dim[2]" quads to draw all the slices of the 3D-texture as a flat 3D-texture
        // (used to draw all the individual slices at once to the screen buffer)
        final int D3D11_BIND_VERTEX_BUFFER = GLenum.GL_ARRAY_BUFFER;
        index = 0;
        for(int z=0; z<m_dim[2]; z++)
            index = InitScreenSlice(renderQuad,z,index);
        m_renderQuadBuffer = CreateVertexBuffer(/*sizeof(VS_INPUT_GRID_STRUCT)*/Vector3f.SIZE*2*m_numVerticesRenderQuad,
                D3D11_BIND_VERTEX_BUFFER, renderQuad, m_numVerticesRenderQuad);

        // Vertex buffer for "m_dim[2]" quads to draw all the slices to a 3D texture
        // (a Geometry Shader is used to send each quad to the appropriate slice)
        index = 0;
        for( int z = 1; z < m_dim[2]-1; z++ )
            index = InitSlice( z, slices, index );
        assert(index==m_numVerticesSlices);
        m_slicesBuffer = CreateVertexBuffer(/*sizeof(VS_INPUT_GRID_STRUCT)*/Vector3f.SIZE*2*m_numVerticesSlices,
                D3D11_BIND_VERTEX_BUFFER, slices , m_numVerticesSlices);

        // Vertex buffers for boundary geometry
        //   2 boundary slices
        index = 0;
        index = InitBoundaryQuads(boundarySlices,index);
        assert(index==m_numVerticesBoundarySlices);
        m_boundarySlicesBuffer = CreateVertexBuffer(/*sizeof(VS_INPUT_GRID_STRUCT)*/Vector3f.SIZE*2*m_numVerticesBoundarySlices,
                D3D11_BIND_VERTEX_BUFFER, boundarySlices, m_numVerticesBoundarySlices);
        //   ( 4 * "m_dim[2]" ) boundary lines
        index = 0;
        index = InitBoundaryLines(boundaryLines,index);
        assert(index==m_numVerticesBoundaryLines);
        m_boundaryLinesBuffer = CreateVertexBuffer(/*sizeof(VS_INPUT_GRID_STRUCT)*/Vector3f.SIZE*2*m_numVerticesBoundaryLines,
                D3D11_BIND_VERTEX_BUFFER, boundaryLines, m_numVerticesBoundaryLines);
    }

    private int InitScreenSlice        (VS_INPUT_GRID_STRUCT[] vertices, int z, int index){
        VS_INPUT_GRID_STRUCT tempVertex1 = new VS_INPUT_GRID_STRUCT();
        VS_INPUT_GRID_STRUCT tempVertex2 = new VS_INPUT_GRID_STRUCT();
        VS_INPUT_GRID_STRUCT tempVertex3 = new VS_INPUT_GRID_STRUCT();
        VS_INPUT_GRID_STRUCT tempVertex4 = new VS_INPUT_GRID_STRUCT();

        // compute the offset (px, py) in the "flat 3D-texture" space for the slice with given 'z' coordinate
        int column      = z % m_cols;
        int row         = (int)Math.floor((float)(z/m_cols));
        int px = column * m_dim[0];
        int py = row    * m_dim[1];

        float w = (m_dim[0]);
        float h = (m_dim[1]);

        float Width  = (m_cols * m_dim[0])*2.0f;
        float Height = (m_rows * m_dim[1])*2.0f;

        float left   = -1.0f + (px*2.0f/Width);
        float right  = -1.0f + ((px+w)*2.0f/Width);
        float top    =  1.0f - py*2.0f/Height;
        float bottom =  1.0f - ((py+h)*2.0f/Height);

        tempVertex1.pos   .set( left   , top    , 0.0f      );
        tempVertex1.Tex   .set( 0      , 0      , (z)  );

        tempVertex2.pos   .set( right  , top    , 0.0f      );
        tempVertex2.Tex   .set( w      , 0      , (z)  );

        tempVertex3.pos   .set( right  , bottom , 0.0f      );
        tempVertex3.Tex   .set( w      , h      , (z)  );

        tempVertex4.pos   .set( left   , bottom , 0.0f      );
        tempVertex4.Tex   .set( 0      , h      , (z)  );

        vertices[index++] = tempVertex1;
        vertices[index++] = tempVertex2;
        vertices[index++] = tempVertex3;
        vertices[index++] = tempVertex1;
        vertices[index++] = tempVertex3;
        vertices[index++] = tempVertex4;
        return index;
    }

    private int InitSlice             (int z, VS_INPUT_GRID_STRUCT[] vertices, int index){
        VS_INPUT_GRID_STRUCT tempVertex1 = new VS_INPUT_GRID_STRUCT();
        VS_INPUT_GRID_STRUCT tempVertex2 = new VS_INPUT_GRID_STRUCT();
        VS_INPUT_GRID_STRUCT tempVertex3 = new VS_INPUT_GRID_STRUCT();
        VS_INPUT_GRID_STRUCT tempVertex4 = new VS_INPUT_GRID_STRUCT();

        int w = m_dim[0];
        int h = m_dim[1];

        float left   = -1.0f + 2.0f/w;
        float right  =  1.0f - 2.0f/w;
        float top    =  1.0f - 2.0f/h;
        float bottom = -1.0f + 2.0f/h;

        tempVertex1.pos .set( left     , top       , 0.0f      );
        tempVertex1.Tex .set( 1.0f     , 1.0f      , (z)  );

        tempVertex2.pos .set( right    , top       , 0.0f      );
        tempVertex2.Tex .set( (w-1.0f) , 1.0f      , (z)  );

        tempVertex3.pos .set( right    , bottom    , 0.0f      );
        tempVertex3.Tex .set( (w-1.0f) , (h-1.0f)  , (z)  );

        tempVertex4.pos .set( left     , bottom    , 0.0f      );
        tempVertex4.Tex .set( 1.0f     , (h-1.0f)  , (z)  );


        (vertices)[index++] = tempVertex1;
        (vertices)[index++] = tempVertex2;
        (vertices)[index++] = tempVertex3;
        (vertices)[index++] = tempVertex1;
        (vertices)[index++] = tempVertex3;
        (vertices)[index++] = tempVertex4;
        return index;
    }
    private int InitLine              (float x1, float y1, float x2, float y2, int z,
                                VS_INPUT_GRID_STRUCT[] vertices, int index){
        VS_INPUT_GRID_STRUCT tempVertex = new VS_INPUT_GRID_STRUCT();
        int w = m_dim[0];
        int h = m_dim[1];

        tempVertex.pos  .set( x1*2.0f/w - 1.0f , -y1*2.0f/h + 1.0f , 0.5f      );
        tempVertex.Tex  .set( 0.0f             , 0.0f              , (z)  );
        (vertices)[index++] = tempVertex;

        tempVertex = new VS_INPUT_GRID_STRUCT();
        tempVertex.pos  .set( x2*2.0f/w - 1.0f , -y2*2.0f/h + 1.0f , 0.5f      );
        tempVertex.Tex  .set( 0.0f             , 0.0f              , (z)  );
        (vertices)[index++] = tempVertex;

        return index;
    }

    private int InitBoundaryQuads     (VS_INPUT_GRID_STRUCT[] vertices, int index){
        index = InitSlice( 0, vertices, index );
        index = InitSlice( m_dim[2]-1, vertices, index );
        return index;
    }

    private int InitBoundaryLines     (VS_INPUT_GRID_STRUCT[] vertices, int index){
        int w = m_dim[0];
        int h = m_dim[1];

        for( int z = 0; z < m_dim[2]; z++ )
        {
            // bottom
            index = InitLine( 0.0f, 1.0f, (w), 1.0f, z, vertices, index );
            // top
            index = InitLine( 0.0f, (h), (w), (h), z, vertices, index );
            // left
            index = InitLine( 1.0f, 0.0f, 1.0f, (h), z, vertices, index );
            //right
            index = InitLine( (w), 0.0f, (w), (h), z, vertices, index );
        }

        return index;
    }

//    private void CreateLayout( D3D11_INPUT_ELEMENT_DESC* layoutDesc, UINT numElements, void* shaderBytecode, SIZE_T shaderBytecodeLength, ID3D11InputLayout** layout);
    private BufferGL CreateVertexBuffer( int ByteWidth, int bindFlags, /*ID3D11Buffer** vertexBuffer,*/VS_INPUT_GRID_STRUCT[] vertices,int numVertices){
        FloatBuffer data = CacheBuffer.getCachedFloatBuffer(vertices.length * Vector3f.SIZE * 2);
        for(VS_INPUT_GRID_STRUCT vs : vertices){
            vs.pos.store(data);
            vs.Tex.store(data);
        }

        data.flip();

        BufferGL buffer = new BufferGL();
        buffer.initlize(bindFlags, ByteWidth, data, GLenum.GL_STATIC_DRAW);
        buffer.unbind();
        return buffer;
    }

    // D3D11 helper functions
    private void DrawPrimitive( int PrimitiveType, VertexArrayObject layout, BufferGL vertexBuffer,int stride, int offset, int StartVertex, int VertexCount ){
        /*m_pD3DContext->IASetPrimitiveTopology( PrimitiveType );
        m_pD3DContext->IASetInputLayout( layout );
        m_pD3DContext->IASetVertexBuffers( 0, 1, vertexBuffer, stride, offset );
        m_pD3DContext->Draw( VertexCount, StartVertex );*/
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, vertexBuffer.getBuffer());
        gl.glEnableVertexAttribArray(0);
        gl.glVertexAttribPointer(0, 3, GLenum.GL_FLOAT, false, stride, 0);
        gl.glEnableVertexAttribArray(1);
        gl.glVertexAttribPointer(1, 3, GLenum.GL_FLOAT, false, stride, stride/2);

        gl.glDrawArrays(PrimitiveType, StartVertex, VertexCount);

        gl.glDisableVertexAttribArray(0);
        gl.glDisableVertexAttribArray(1);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
    }

    @Override
    public void dispose() {
        CommonUtil.safeRelease(m_layout);
        CommonUtil.safeRelease(m_renderQuadBuffer);
        CommonUtil.safeRelease(m_slicesBuffer);
        CommonUtil.safeRelease(m_boundarySlicesBuffer);
        CommonUtil.safeRelease(m_boundaryLinesBuffer);
    }
}
