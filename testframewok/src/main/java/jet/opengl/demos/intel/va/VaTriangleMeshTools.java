package jet.opengl.demos.intel.va;

import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

import java.util.Arrays;

import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.postprocessing.util.StackFloat;
import jet.opengl.postprocessing.util.StackInt;

/**
 * Created by mazhen'gui on 2018/1/2.
 */

final class VaTriangleMeshTools {

    /**
     * This adds quad triangles in strip order ( (0, 0), (1, 0), (0, 1), (1, 1) ) - so swap the last two if doing clockwise/counterclockwise
     * (this is a bit inconsistent with AddPentagon below)
     */
    static void AddQuad(StackFloat outVertices, StackInt outIndices, ReadableVector3f v0, ReadableVector3f v1, ReadableVector3f v2, ReadableVector3f v3 )
    {
        int i0 = AddVertex( outVertices, v0 );
        int i1 = AddVertex( outVertices, v1 );
        int i2 = AddVertex( outVertices, v2 );
        int i3 = AddVertex( outVertices, v3 );

        AddQuad( outIndices, i0, i1, i2, i3 );
    }

    static int AddVertex( StackFloat outVertices, ReadableVector3f vert )
    {
        outVertices.push( vert );
        return outVertices.size( )/3 - 1;
    }

    /**
     * This adds quad triangles in strip order ( (0, 0), (1, 0), (0, 1), (1, 1) ) - so swap the last two if doing clockwise/counterclockwise
     * (this is a bit inconsistent with AddPentagon below)
     * @param i0
     * @param i1
     * @param i2
     * @param i3
     */
    private static  void AddQuad( StackInt outIndices, int i0, int i1, int i2, int i3 )
    {
        outIndices.push( i0 );
        outIndices.push( i1 );
        outIndices.push( i2 );
        outIndices.push( i1 );
        outIndices.push( i3 );
        outIndices.push( i2 );
    }

    static void AddTriangle( StackInt outIndices, int a, int b, int c )
    {
        assert( ( a >= 0 ) && ( b >= 0 ) && ( c >= 0 ) );
        outIndices.push( a );
        outIndices.push( b );
        outIndices.push( c );
    }

    static void AddTriangle( StackFloat outVertices, StackInt outIndices, ReadableVector3f v0, ReadableVector3f v1, ReadableVector3f v2 )
    {
        int i0 = AddVertex( outVertices, v0 );
        int i1 = AddVertex( outVertices, v1 );
        int i2 = AddVertex( outVertices, v2 );

        AddTriangle( outIndices, i0, i1, i2 );
    }

    static void loadVertex(Vector3f out, float[] vertices, int index){
        out.x = vertices[index];
        out.y = vertices[index+1];
        out.z = vertices[index+2];
    }

    static void addVertex( float[] out,Vector3f v, int index){
        out[index]   += v.x;
        out[index+1] += v.y;
        out[index+2] += v.z;
    }

    static void setVertex( float[] out,Vector3f v, int index){
        out[index]   = v.x;
        out[index+1] = v.y;
        out[index+2] = v.z;
    }

    static void GenerateNormals( StackFloat outNormals, StackFloat vertices, StackInt indices, boolean counterClockwise){
        GenerateNormals(outNormals, vertices, indices, counterClockwise, 0, -1, true);
    }

    static void GenerateNormals( StackFloat outNormals, StackFloat vertices, StackInt indices, boolean counterClockwise, int indexFrom /*= 0*/, int indexCount /*= -1*/, boolean fixBrokenNormals /*= true*/ )
    {
        assert( outNormals.size() == vertices.size() );
        if( indexCount == -1 )
            indexCount = indices.size( );

        /*for( int i = 0; i < vertices.size( ); i++ )
            outNormals[i] = vaVector3( 0, 0, 0 );*/
        Arrays.fill(outNormals.getData(), 0, outNormals.size(), 0);
        final Vector3f a = new Vector3f();
        final Vector3f b = new Vector3f();
        final Vector3f c = new Vector3f();
        final Vector3f norm = a;   // TODO performance issue

        final float[] verticesData = vertices.getData();
        final float[] outNormalsData = outNormals.getData();

        for( int i = indexFrom; i < indexCount; i += 3 )
        {
//                const vaVector3 & a = vertices[indices[i + 0]];
//                const vaVector3 & b = vertices[indices[i + 1]];
//                const vaVector3 & c = vertices[indices[i + 2]];
            int i0 = indices.get(i+0) * 3;
            int i1 = indices.get(i+1) * 3;
            int i2 = indices.get(i+2) * 3;

            loadVertex(a, verticesData, i0);
            loadVertex(b, verticesData, i1);
            loadVertex(c, verticesData, i2);

//            vaVector3 norm;
            if( counterClockwise ) {
//                norm = vaVector3::Cross (c - a, b - a );
                Vector3f.sub(c, a, c);
                Vector3f.sub(b, a, b);
                Vector3f.cross(c,b, norm);
            }else {
//                norm = vaVector3::Cross (b - a, c - a );
                Vector3f.sub(c, a, c);
                Vector3f.sub(b, a, b);
                Vector3f.cross(b,c, norm);
            }

            float triAreaX2 = norm.length( );
            if( triAreaX2 < Numeric.EPSILON)
            {
                if( !fixBrokenNormals )
                    continue;

                if( triAreaX2 != 0.0f )
                    norm.scale(1.0f/(triAreaX2 * 10000.0f));
            }

            // don't normalize, leave it weighted by area
            /*outNormals[indices[i + 0]] += norm;
            outNormals[indices[i + 1]] += norm;
            outNormals[indices[i + 2]] += norm;*/
            addVertex(outNormalsData, norm, i0);
            addVertex(outNormalsData, norm, i1);
            addVertex(outNormalsData, norm, i2);
        }

        for( int i = 0; i < vertices.size( )/3; i++ )
        {
//            float length = outNormals[i].Length();
            final int i0 = i * 3;
            loadVertex(norm, outNormalsData, i0);

            float length = norm.length();
            if( length < Numeric.EPSILON )
                norm.set( 0.0f, 0.0f, (fixBrokenNormals)?(1.0f):(0.0f) );
            else
                norm.scale(1.0f / length);

            setVertex(outNormalsData, norm, i0);
        }
    }
}
