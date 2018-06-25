package nv.samples.smoke;

import org.lwjgl.util.vector.Vector3f;

import java.nio.ByteBuffer;

import jet.opengl.demos.intel.cput.D3D11_INPUT_ELEMENT_DESC;
import jet.opengl.demos.intel.cput.ID3D11InputLayout;
import jet.opengl.demos.intel.va.VaDirectXTools;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.Numeric;

class Grid {
    private static final int VERTICES_PER_SLICE = 6;
    private static final int VERTICES_PER_LINE = 2;
    private static final int LINES_PER_SLICE = 4;

    // Internal State
    //===============
//    ID3D10Device*              m_pD3DDevice;

//    ID3D10InputLayout*         m_layout;
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
    private ID3D11InputLayout m_layout;

    private final int[] m_dim = new int[3];

    void Initialize( int gridWidth, int gridHeight, int gridDepth,GLSLProgram technique ){
        m_dim[0] = gridWidth;
        m_dim[1] = gridHeight;
        m_dim[2] = gridDepth;

        long value = SmokeDemo.computeRowColsForFlat3DTexture(m_dim[2]/*, &m_cols, &m_rows*/);
        m_cols = Numeric.decodeFirst(value);
        m_rows = Numeric.decodeSecond(value);

        CreateVertexBuffers(technique);
    }

    void DrawSlices        (  ){
        int stride = Vector3f.SIZE * 2;
        int offset = 0;
        DrawPrimitive( GLenum.GL_TRIANGLES, m_layout, m_slicesBuffer,
                stride, offset, 0, m_numVerticesSlices );
    }

    void DrawSlice         ( int slice ){
        int stride = Vector3f.SIZE * 2;
        int offset =  0 ;
        DrawPrimitive( GLenum.GL_TRIANGLES, m_layout, m_slicesBuffer,
                stride, offset, VERTICES_PER_SLICE*slice, VERTICES_PER_SLICE );
    }

    void DrawSlicesToScreen(  ){
        int stride = Vector3f.SIZE * 2;
        int offset =  0 ;
        DrawPrimitive( GLenum.GL_TRIANGLES, m_layout, m_renderQuadBuffer,
                stride, offset, 0, m_numVerticesRenderQuad );
    }
    void DrawBoundaryQuads (  ){
        int stride = Vector3f.SIZE * 2;
        int offset =  0 ;
        DrawPrimitive( GLenum.GL_TRIANGLES, m_layout, m_boundarySlicesBuffer,
                stride, offset, 0, m_numVerticesBoundarySlices );
    }
    void DrawBoundaryLines (  ){
        int stride = Vector3f.SIZE * 2;
        int offset =  0 ;
        DrawPrimitive( GLenum.GL_LINES, m_layout, m_boundaryLinesBuffer,
                stride, offset, 0, m_numVerticesBoundaryLines  );
    }

    int  GetCols(){return m_cols;};
    int  GetRows(){return m_rows;};

    int  GetDimX() {return m_dim[0]; }
    int  GetDimY() {return m_dim[1]; }
    int  GetDimZ() {return m_dim[2]; }

    private static final class VS_INPUT_FLUIDSIM_STRUCT{
        final Vector3f Pos = new Vector3f();
        final Vector3f Tex = new Vector3f();  // Cell coordinates in 0-"texture dimension" range

        void set(VS_INPUT_FLUIDSIM_STRUCT ohs){
            Pos.set(ohs.Pos);
            Tex.set(ohs.Tex);
        }
    }

    private void CreateVertexBuffers  (GLSLProgram technique){
// Create layout
        D3D11_INPUT_ELEMENT_DESC layoutDesc[] =
                {
                        VaDirectXTools.CD3D11_INPUT_ELEMENT_DESC( "POSITION", 0, GLenum.GL_RGB32F,       0, 0, 0, 0 ),
                        VaDirectXTools.CD3D11_INPUT_ELEMENT_DESC( "TEXCOORD", 0, GLenum.GL_RGB32F,       0,12, 0, 0 ),
                };
        /*UINT numElements = sizeof(layoutDesc)/sizeof(layoutDesc[0]);
        CreateLayout( layoutDesc, numElements, technique, &m_layout);*/
        m_layout = ID3D11InputLayout.createInputLayoutFrom(layoutDesc);

        int index = 0;
        VS_INPUT_FLUIDSIM_STRUCT[] renderQuad;
        VS_INPUT_FLUIDSIM_STRUCT[] slices;
        VS_INPUT_FLUIDSIM_STRUCT[] boundarySlices;
        VS_INPUT_FLUIDSIM_STRUCT[] boundaryLines;

        m_numVerticesRenderQuad = VERTICES_PER_SLICE * m_dim[2];
        renderQuad = new VS_INPUT_FLUIDSIM_STRUCT[ m_numVerticesRenderQuad ];

        m_numVerticesSlices = VERTICES_PER_SLICE * (m_dim[2] - 2);
        slices = new VS_INPUT_FLUIDSIM_STRUCT[ m_numVerticesSlices ];

        m_numVerticesBoundarySlices = VERTICES_PER_SLICE * 2;
        boundarySlices = new VS_INPUT_FLUIDSIM_STRUCT[ m_numVerticesBoundarySlices ];

        m_numVerticesBoundaryLines = VERTICES_PER_LINE * LINES_PER_SLICE * (m_dim[2]);
        boundaryLines = new VS_INPUT_FLUIDSIM_STRUCT[ m_numVerticesBoundaryLines ];

//        assert(renderQuad && m_numVerticesSlices && m_numVerticesBoundarySlices && m_numVerticesBoundaryLines);

        // Vertex buffer for "m_dim[2]" quads to draw all the slices of the 3D-texture as a flat 3D-texture
        // (used to draw all the individual slices at once to the screen buffer)
        index = 0;
        for(int z=0; z<m_dim[2]; z++)
            index = InitScreenSlice(renderQuad,z, index);
        m_renderQuadBuffer = CreateVertexBuffer(/*sizeof(VS_INPUT_FLUIDSIM_STRUCT)*/Vector3f.SIZE * 2 *m_numVerticesRenderQuad,
                GLenum.GL_ARRAY_BUFFER, renderQuad, m_numVerticesRenderQuad);

        // Vertex buffer for "m_dim[2]" quads to draw all the slices to a 3D texture
        // (a Geometry Shader is used to send each quad to the appropriate slice)
        index = 0;
        for( int z = 1; z < m_dim[2]-1; z++ )
            index = InitSlice( z, slices, index );
        assert(index==m_numVerticesSlices);
        m_slicesBuffer = CreateVertexBuffer(/*sizeof(VS_INPUT_FLUIDSIM_STRUCT)*/Vector3f.SIZE * 2*m_numVerticesSlices,
                GLenum.GL_ARRAY_BUFFER, slices , m_numVerticesSlices);

        // Vertex buffers for boundary geometry
        //   2 boundary slices
        index = 0;
        index = InitBoundaryQuads(boundarySlices,index);
        assert(index==m_numVerticesBoundarySlices);
        m_boundarySlicesBuffer = CreateVertexBuffer(Vector3f.SIZE * 2*m_numVerticesBoundarySlices,
                GLenum.GL_ARRAY_BUFFER, boundarySlices, m_numVerticesBoundarySlices);
        //   ( 4 * "m_dim[2]" ) boundary lines
        index = 0;
        index = InitBoundaryLines(boundaryLines,index);
        assert(index==m_numVerticesBoundaryLines);
        m_boundaryLinesBuffer = CreateVertexBuffer(Vector3f.SIZE * 2*m_numVerticesBoundaryLines,
                GLenum.GL_ARRAY_BUFFER, boundaryLines, m_numVerticesBoundaryLines);
    }

    int InitScreenSlice        (VS_INPUT_FLUIDSIM_STRUCT[] vertices, int z, int index){
        VS_INPUT_FLUIDSIM_STRUCT tempVertex1 = new VS_INPUT_FLUIDSIM_STRUCT();
        VS_INPUT_FLUIDSIM_STRUCT tempVertex2 = new VS_INPUT_FLUIDSIM_STRUCT();
        VS_INPUT_FLUIDSIM_STRUCT tempVertex3 = new VS_INPUT_FLUIDSIM_STRUCT();
        VS_INPUT_FLUIDSIM_STRUCT tempVertex4 = new VS_INPUT_FLUIDSIM_STRUCT();

        // compute the offset (px, py) in the "flat 3D-texture" space for the slice with given 'z' coordinate
        int column      = z % m_cols;
        int row         = (int)Math.floor((float)(z/m_cols));
        int px = column * m_dim[0];
        int py = row    * m_dim[1];

        float w = (m_dim[0]);
        float h = (m_dim[1]);

        float Width  = (m_cols * m_dim[0]);
        float Height = (m_rows * m_dim[1]);

        float left   = -1.0f + (px*2.0f/Width);
        float right  = -1.0f + ((px+w)*2.0f/Width);
        float top    =  1.0f - py*2.0f/Height;
        float bottom =  1.0f - ((py+h)*2.0f/Height);

        tempVertex1.Pos   .set( left   , top    , 0.0f      );
        tempVertex1.Tex   .set( 0      , 0      ,(z)  );

        tempVertex2.Pos   .set( right  , top    , 0.0f      );
        tempVertex2.Tex   .set( w      , 0      , (z)  );

        tempVertex3.Pos   .set( right  , bottom , 0.0f      );
        tempVertex3.Tex   .set( w      , h      ,(z)  );

        tempVertex4.Pos   .set( left   , bottom , 0.0f      );
        tempVertex4.Tex   .set( 0      , h      ,(z)  );

        vertices[index++] = (tempVertex1);
        vertices[index++] = (tempVertex2);
        vertices[index++] = (tempVertex3);
        vertices[index++] = (tempVertex1);
        vertices[index++] = (tempVertex3);
        vertices[index++] = (tempVertex4);

        return index;
    }

    int InitSlice             (int z, VS_INPUT_FLUIDSIM_STRUCT[] vertices, int index){
        VS_INPUT_FLUIDSIM_STRUCT tempVertex1 = new VS_INPUT_FLUIDSIM_STRUCT();
        VS_INPUT_FLUIDSIM_STRUCT tempVertex2 = new VS_INPUT_FLUIDSIM_STRUCT();
        VS_INPUT_FLUIDSIM_STRUCT tempVertex3 = new VS_INPUT_FLUIDSIM_STRUCT();
        VS_INPUT_FLUIDSIM_STRUCT tempVertex4 = new VS_INPUT_FLUIDSIM_STRUCT();

        int w = m_dim[0];
        int h = m_dim[1];

        float left   = -1.0f + 2.0f/w;
        float right  =  1.0f - 2.0f/w;
        float top    =  1.0f - 2.0f/h;
        float bottom = -1.0f + 2.0f/h;

        tempVertex1.Pos.set( left     , top       , 0.0f      );
        tempVertex1.Tex.set( 1.0f     , 1.0f      , (z)  );

        tempVertex2.Pos.set( right    , top       , 0.0f      );
        tempVertex2.Tex.set( (w-1.0f) , 1.0f      , (z)  );

        tempVertex3.Pos.set( right    , bottom    , 0.0f      );
        tempVertex3.Tex.set( (w-1.0f) , (h-1.0f)  , (z)  );

        tempVertex4.Pos.set( left     , bottom    , 0.0f      );
        tempVertex4.Tex.set( 1.0f     , (h-1.0f)  , (z)  );

        vertices[index++] = (tempVertex1);
        vertices[index++] = (tempVertex2);
        vertices[index++] = (tempVertex3);
        vertices[index++] = (tempVertex1);
        vertices[index++] = (tempVertex3);
        vertices[index++] = (tempVertex4);

        return index;
    }

    int InitLine              (float x1, float y1, float x2, float y2, int z,
                                VS_INPUT_FLUIDSIM_STRUCT[] vertices, int index){
        VS_INPUT_FLUIDSIM_STRUCT tempVertex = new VS_INPUT_FLUIDSIM_STRUCT();
        int w = m_dim[0];
        int h = m_dim[1];

        tempVertex.Pos.set( x1*2.0f/w - 1.0f , -y1*2.0f/h + 1.0f , 0.5f      );
        tempVertex.Tex.set( 0.0f             , 0.0f              , (z)  );
        vertices[index++] = (tempVertex);

        tempVertex = new VS_INPUT_FLUIDSIM_STRUCT();
        tempVertex.Pos.set( x2*2.0f/w - 1.0f , -y2*2.0f/h + 1.0f , 0.5f      );
        tempVertex.Tex.set( 0.0f             , 0.0f              , (z)  );
        vertices[index++] = (tempVertex);

        return index;
    }

    int InitBoundaryQuads     (VS_INPUT_FLUIDSIM_STRUCT[] vertices, int index){
        index = InitSlice( 0, vertices, index );
        index = InitSlice( m_dim[2]-1, vertices, index );
        return index;
    }

    int InitBoundaryLines     (VS_INPUT_FLUIDSIM_STRUCT[] vertices, int index){
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

//    HRESULT CreateLayout( D3D10_INPUT_ELEMENT_DESC* layoutDesc, UINT numElements,ID3D10EffectTechnique* technique, ID3D10InputLayout** layout);
    BufferGL CreateVertexBuffer( int ByteWidth, int target, VS_INPUT_FLUIDSIM_STRUCT[] vertices,int numVertices){
        BufferGL buffer = new BufferGL();

        ByteBuffer bytes = CacheBuffer.getCachedByteBuffer(vertices.length * Vector3f.SIZE * 2);
        for(VS_INPUT_FLUIDSIM_STRUCT vertice : vertices){
            vertice.Pos.store(bytes);
            vertice.Tex.store(bytes);
        }
        bytes.flip();
        buffer.initlize(target, bytes.remaining(), bytes, GLenum.GL_STATIC_DRAW);
        return buffer;
    }

    // D3D10 helper functions
    void DrawPrimitive(int PrimitiveType, ID3D11InputLayout layout, BufferGL vertexBuffer,int stride, int offset, int StartVertex, int VertexCount ){
        /*m_pD3DDevice->IASetPrimitiveTopology( PrimitiveType );
        m_pD3DDevice->IASetInputLayout( layout );
        m_pD3DDevice->IASetVertexBuffers( 0, 1, vertexBuffer, stride, offset );
        m_pD3DDevice->Draw( VertexCount, StartVertex );*/

        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        gl.glBindBuffer(vertexBuffer.getTarget(), vertexBuffer.getBuffer());

        layout.bind();
        gl.glDrawArrays(PrimitiveType, StartVertex, VertexCount);
        layout.unbind();

        gl.glBindBuffer(vertexBuffer.getTarget(), 0);
    }

}
