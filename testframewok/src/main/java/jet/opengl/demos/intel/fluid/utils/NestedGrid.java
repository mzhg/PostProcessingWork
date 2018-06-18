package jet.opengl.demos.intel.fluid.utils;

/**
 * Created by Administrator on 2018/6/2 0002.
 */

public class NestedGrid {
    /** Get indices of minimal cell in child layer of cluster represented by specified cell in parent layer.

     Each cell in a parent layer represents a grid cluster of typically
     2*2*2=8 cells in the child layer.  This routine calculates the index of
     the "minimal" cell in the child layer grid cluster, i.e. the cell in the
     child layer which corresponds to minimum corner cell of the grid cluster
     represented by the cell in the parent layer with the specified index.

     The cells in the child layer that belong to the same grid cluster would
     be visited by this code:

     \verbatim

     int i[3] ; // i is the increment past the minimum corner cell in the grid cluster.
     int j[3] ; // j indexes into the child layer.
     for( i[2] = 0 ; i[2] <= decimations[2] ; ++ i[2] )
     {
     j[2] = i[2] + clusterMinIndices[2] ;
     for( i[1] = 0 ; i[1] <= decimations[1] ; ++ i[1] )
     {
     j[1] = i[1] + clusterMinIndices[1] ;
     for( i[0] = 0 ; i[0] <= decimations[0] ; ++ i[0] )
     {
     j[0] = i[0] + clusterMinIndices[0] ;
     // Use j to index into child layer.
     }
     }
     }

     \endverbatim

     \param clusterMinIndices - (out) index of minimal cell in child layer grid cluster represented by given parent cell

     \param decimations - (in) ratios of dimensions of child layer to its parent, for each axis.
     This must be the same as the result of calling GetDecimations for the intended parent layer.

     \param indicesOfParentCell - (in) index of cell in parent layer.

     \see GetDecimations

     */
    public static void getChildClusterMinCornerIndex( int clusterMinIndices[] , int decimations[] , int indicesOfParentCell[] )
    {
        clusterMinIndices[ 0 ] = indicesOfParentCell[ 0 ] * decimations[ 0 ] ;
        clusterMinIndices[ 1 ] = indicesOfParentCell[ 1 ] * decimations[ 1 ] ;
        clusterMinIndices[ 2 ] = indicesOfParentCell[ 2 ] * decimations[ 2 ] ;
    }
}
