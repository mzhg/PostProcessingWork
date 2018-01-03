package jet.opengl.demos.intel.va;

import org.lwjgl.util.vector.Vector3f;

import jet.opengl.postprocessing.util.StackFloat;
import jet.opengl.postprocessing.util.StackInt;

/**
 * Created by mazhen'gui on 2018/1/2.
 */

final class VaStandardShapes {

    private VaStandardShapes(){}

    // all of these produce shapes with center in (0, 0, 0) and each vertex magnitude of 1 (normalized), except where specified otherwise
    // front faces are counter-clockwise
    static void CreatePlane(StackFloat outVertices, StackInt outIndices, float sizeX, float sizeY ){
        final Vector3f v0 = new Vector3f( -sizeX, -sizeY, 0.0f );
        final Vector3f v1 = new Vector3f(  sizeX, -sizeY, 0.0f );
        final Vector3f v2 = new Vector3f(  sizeX,  sizeY, 0.0f );
        final Vector3f v3 = new Vector3f( -sizeX,  sizeY, 0.0f );

        VaTriangleMeshTools.AddQuad( outVertices, outIndices, v0, v3, v1, v2 );
    }

    static void CreateTetrahedron( StackFloat outVertices, StackInt outIndices, boolean shareVertices )
    {
        final float a = 1.41421f / 3.0f;
        final float b = 2.4494f / 3.0f;

        final Vector3f v0 = new Vector3f( 0.0f,   0.0f,   1.0f );
        final Vector3f v1 = new Vector3f( 2 * a,  0.0f,  -1.0f / 3.0f );
        final Vector3f v2 = new Vector3f( -a,     b,     -1.0f / 3.0f );
        final Vector3f v3 = new Vector3f( -a,    -b,     -1.0f / 3.0f );

        if( shareVertices )
        {
            final int i0 = VaTriangleMeshTools.AddVertex( outVertices, v0 );
            final int i1 = VaTriangleMeshTools.AddVertex( outVertices, v1 );
            final int i2 = VaTriangleMeshTools.AddVertex( outVertices, v2 );
            final int i3 = VaTriangleMeshTools.AddVertex( outVertices, v3 );
            VaTriangleMeshTools.AddTriangle( outIndices, i0, i1, i2 );
            VaTriangleMeshTools.AddTriangle( outIndices, i0, i2, i3 );
            VaTriangleMeshTools.AddTriangle( outIndices, i0, i3, i1 );
            VaTriangleMeshTools.AddTriangle( outIndices, i1, i3, i2 );
        }
        else
        {
            VaTriangleMeshTools.AddTriangle( outVertices, outIndices, v0, v1, v2 );
            VaTriangleMeshTools.AddTriangle( outVertices, outIndices, v0, v2, v3 );
            VaTriangleMeshTools.AddTriangle( outVertices, outIndices, v0, v3, v1 );
            VaTriangleMeshTools.AddTriangle( outVertices, outIndices, v1, v3, v2 );
        }
    }
}
