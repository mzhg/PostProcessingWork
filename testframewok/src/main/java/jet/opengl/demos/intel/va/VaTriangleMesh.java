package jet.opengl.demos.intel.va;

import org.lwjgl.util.vector.Vector3f;

import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.postprocessing.util.StackByte;
import jet.opengl.postprocessing.util.StackInt;

/**
 * Created by mazhen'gui on 2017/11/17.
 */

public class VaTriangleMesh extends VaRenderingModuleImpl{

    public StackByte Vertices;
    public StackInt Indices;

    protected VaTriangleMesh(){
        Vertices = new StackByte(256);
        Indices = new StackInt(32);

        assert (VaRenderingCore.IsInitialized());
    }

    public interface VertexType{
        int GetStride();
        void Store(StackByte bytes);
    }


    public void Reset( )
    {
        Vertices.clear( );
        Indices.clear( );
    }

    public int                                 AddVertex(  VertexType vert )
    {
        return /*vaTriangleMeshTools::*/AddVertex( Vertices, vert );
    }

    public void                                AddTriangle( int a, int b, int c )
    {
        /*vaTriangleMeshTools::*/AddTriangle( Indices, a, b, c );
    }

//    template< class VertexType >
    public void                         AddQuad( VertexType v0, VertexType  v1, VertexType v2, VertexType v3 )
    {
        /*vaTriangleMeshTools::*/AddQuad( Vertices, Indices, v0, v1, v2, v3 );
    }

//    template< class VertexType >
    public void                         AddTriangle( VertexType  v0,  VertexType v1,  VertexType  v2 )
    {
        /*vaTriangleMeshTools::*/AddTriangle( Vertices, Indices, v0, v1, v2 );
    }

    //template< typename VertexType >
    public VaBoundingBox                CalculateBounds( int vertexStride, int positionOffset)
    {
        return /*vaTriangleMeshTools::*/CalculateBounds( Vertices, vertexStride, positionOffset, null );
    }

    public void                                GenerateNormals( boolean counterClockwise, int vertexStride, int positionOffset, int normalOffset )
    {
        /*vaTriangleMeshTools::*/GenerateNormals( Vertices, Indices, counterClockwise,0, -1,
            vertexStride, positionOffset, normalOffset );
    }

    public void                        SetDataDirty( ){}

    private static void AddTriangle( StackInt outIndices, int a, int b, int c )
    {
        assert( ( a >= 0 ) && ( b >= 0 ) && ( c >= 0 ) );
        outIndices.push( a );
        outIndices.push( b );
        outIndices.push( c );
    }

    private static void AddTriangle( StackByte outVertices, StackInt outIndices,  VertexType v0, VertexType  v1, VertexType  v2 )
    {
        int i0 = AddVertex( outVertices, v0 );
        int i1 = AddVertex( outVertices, v1 );
        int i2 = AddVertex( outVertices, v2 );

        AddTriangle( outIndices, i0, i1, i2 );
    }

    private static int AddVertex( StackByte outVertices, VertexType  vert )
    {
        /*outVertices.push_back( vert );*/
        vert.Store(outVertices);
        return outVertices.size( )/vert.GetStride() - 1;
    }

    // This adds quad triangles in strip order ( (0, 0), (1, 0), (0, 1), (1, 1) ) - so swap the last two if doing clockwise/counterclockwise
    // (this is a bit inconsistent with AddPentagon below)
    private static void AddQuad( StackInt outIndices, int i0, int i1, int i2, int i3 )
    {
        outIndices.push( i0 );
        outIndices.push( i1 );
        outIndices.push( i2 );
        outIndices.push( i1 );
        outIndices.push( i3 );
        outIndices.push( i2 );
    }

    // This adds quad triangles in strip order ( (0, 0), (1, 0), (0, 1), (1, 1) ) - so swap the last two if doing clockwise/counterclockwise
    // (this is a bit inconsistent with AddPentagon below)
//    template< class VertexType >
    private static void AddQuad( StackByte outVertices, StackInt outIndices, VertexType v0, VertexType v1, VertexType  v2, VertexType  v3 )
    {
        int i0 = AddVertex( outVertices, v0 );
        int i1 = AddVertex( outVertices, v1 );
        int i2 = AddVertex( outVertices, v2 );
        int i3 = AddVertex( outVertices, v3 );

        AddQuad( outIndices, i0, i1, i2, i3 );
    }

    static VaBoundingBox CalculateBounds( /*const std::vector<VertexType> &*/StackByte vertices, int vertexStride, int positionOffset, VaBoundingBox result )
    {
//        vaVector3 bmin( FLT_MAX, FLT_MAX, FLT_MAX ), bmax( FLT_MIN, FLT_MIN, FLT_MIN );
        Vector3f bmin = new Vector3f();
        Vector3f bmax = new Vector3f();
        Vector3f.initAsMinMax(bmax, bmin);

        Vector3f position = new Vector3f();
        int count = vertices.size()/vertexStride;
        byte[] orginData = vertices.getData();
        for( int i = 0; i < count; i++ )
        {
            /*bmin = vaVector3::ComponentMin( bmin, vertices[i].Position );
            bmax = vaVector3::ComponentMax( bmax, vertices[i].Position );*/
            position.x = Numeric.getFloat(orginData, positionOffset + 0);
            position.y = Numeric.getFloat(orginData, positionOffset + 4);
            position.z = Numeric.getFloat(orginData, positionOffset + 8);
            Vector3f.min(bmin, position, bmin);
            Vector3f.max(bmax, position, bmax);

            positionOffset += vertexStride;
        }

        if(result == null)
            result = new VaBoundingBox();

        result.Min.set(bmin);
        Vector3f.sub(bmax , bmin, result.Size);

        return /*new  VaBoundingBox( bmin, Vector3f.sub(bmax , bmin, position) )*/ result;
    }

    static void GenerateNormals( /*std::vector<VertexType> &*/ StackByte vertices, /*const std::vector<uint32> &*/StackInt indices,
                                 boolean counterClockwise, int indexFrom /*= 0*/, int indexCount /*= -1*/,
                                 int vertexStride, int positionOffset, int normalOffset)
    {
        if( indexCount == -1 )
            indexCount = indices.size();
        /*for( int i = 0; i < vertices.size( ); i++ )
            vertices[i].Normal = vaVector4( 0, 0, 0, 0 );*/

        Vector3f a = new Vector3f();
        Vector3f b = new Vector3f();
        Vector3f c = new Vector3f();
        Vector3f norm = c;

        byte[] verticesData = vertices.getData();
        int[] indicesData = indices.getData();

        final int verticeCount = vertices.size()/vertexStride;
        float[] normals = new float[verticeCount * 3];

        for( int i = indexFrom; i < indexCount; i += 3 )
        {
            /*const vaVector3 & a = vertices[indices[i + 0]].Position;
            const vaVector3 & b = vertices[indices[i + 1]].Position;
            const vaVector3 & c = vertices[indices[i + 2]].Position;*/

            a.x = Numeric.getFloat(verticesData, indicesData[i+0]*vertexStride + positionOffset);
            a.y = Numeric.getFloat(verticesData, indicesData[i+0]*vertexStride + positionOffset + 4);
            a.z = Numeric.getFloat(verticesData, indicesData[i+0]*vertexStride + positionOffset + 8);
            b.x = Numeric.getFloat(verticesData, indicesData[i+1]*vertexStride + positionOffset);
            b.y = Numeric.getFloat(verticesData, indicesData[i+1]*vertexStride + positionOffset + 4);
            b.z = Numeric.getFloat(verticesData, indicesData[i+1]*vertexStride + positionOffset + 8);
            c.x = Numeric.getFloat(verticesData, indicesData[i+2]*vertexStride + positionOffset);
            c.y = Numeric.getFloat(verticesData, indicesData[i+2]*vertexStride + positionOffset + 4);
            c.z = Numeric.getFloat(verticesData, indicesData[i+2]*vertexStride + positionOffset + 8);


            if( counterClockwise ) {
//                norm = vaVector3::Cross( c - a, b - a );
                Vector3f.sub(b, a, b);
                Vector3f.sub(c, a, a);
                Vector3f.cross(a, b, norm);
            }else {
//                norm = vaVector3::Cross (b - a, c - a );
                Vector3f.sub(b, a, b);  // b-a
                Vector3f.sub(c, a, a);  // c-a
                Vector3f.cross(b, a, norm);
            }

            float triAreaX2 = norm.length( );
            if( triAreaX2 < Numeric.EPSILON ) continue;

            // don't normalize, leave it weighted by area
            /*vertices[indices[i + 0]].Normal.AsVec3( ) += norm;
            vertices[indices[i + 1]].Normal.AsVec3( ) += norm;
            vertices[indices[i + 2]].Normal.AsVec3( ) += norm;*/

            normals[indicesData[i+0] * 3 + 0] += norm.x;
            normals[indicesData[i+0] * 3 + 1] += norm.y;
            normals[indicesData[i+0] * 3 + 2] += norm.z;

            normals[indicesData[i+1] * 3 + 0] += norm.x;
            normals[indicesData[i+1] * 3 + 1] += norm.y;
            normals[indicesData[i+1] * 3 + 2] += norm.z;

            normals[indicesData[i+2] * 3 + 0] += norm.x;
            normals[indicesData[i+2] * 3 + 1] += norm.y;
            normals[indicesData[i+2] * 3 + 2] += norm.z;
        }

        for( int i = 0; i < /*(int)vertices.size( )*/verticeCount; i++ ) {
//            vertices[i].Normal = vertices[i].Normal.Normalize( );
            norm.load(normals, i * 3).normalise();

            Numeric.getBytes(norm.x, verticesData, i * vertexStride + normalOffset);
            Numeric.getBytes(norm.y, verticesData, i * vertexStride + normalOffset + 4);
            Numeric.getBytes(norm.z, verticesData, i * vertexStride + normalOffset + 8);
        }
    }

}
