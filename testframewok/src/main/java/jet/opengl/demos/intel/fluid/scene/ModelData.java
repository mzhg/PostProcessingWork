package jet.opengl.demos.intel.fluid.scene;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import jet.opengl.demos.intel.fluid.render.ApiBase;
import jet.opengl.demos.intel.fluid.render.MeshBase;
import jet.opengl.demos.intel.fluid.render.Pass;
import jet.opengl.demos.intel.fluid.render.Technique;

/**
 * Sharable data for a model.<p></p>

 The point of having this be separate from ModelNode is that a "model"
 (a collection of Meshes with its associated materials) can occur in
 a scene multiple times, but the system should share (not duplicate)
 those resources.  In principle, the Mesh and Material objects could
 be shared (that is, sharing could occur at a lower level than ModelData)
 but sharing at the ModelData level provides additional convenience.<p></p>
 *
 * Created by Administrator on 2018/3/13 0013.
 */

public class ModelData {
    private final ArrayList<MeshBase> mMeshes = new ArrayList<>();


    public MeshBase  newMesh( ApiBase renderApi ) {
        MeshBase mesh = renderApi.newMesh( this ) ;
        addMesh( mesh ); ;
        return mesh ;
    }

    public int getNumMeshes(){
        return mMeshes.size();
    }

    public MeshBase getMesh(int index ) {
        assert ( index < mMeshes.size() ) ;
        return mMeshes.get(index);
    }

    public void render( ApiBase renderApi ){
        for( MeshBase mesh : mMeshes ){
            // Set up render state for each pass.
            // TODO: FIXME: Improve support for multiple passes.
            //              Maybe use render queues: tuples of <pass,node> . Populate and manage queues.
            //              Iterate through the passes.
            //              Each pass could be associated with a particular target (or vice-versa).
            final Technique technique = mesh.getTechnique() ;
//            ASSERT( technique ) ; //
            if( technique != null)
            {   // Mesh has a render technique.
                List<Pass> passes      = technique.getPasses() ;
                assert ( ! passes.isEmpty() ) ;
                for( Pass pass : passes )
                {   // For each pass in this technique...
                    pass.apply( renderApi ) ;
                    mesh.render() ;

                    //renderApi->RenderSimpleText( "ModelData" , Vec3( 0.0f , 0.0f, 0.0f ) , /* use screen space */ false ) ;

                }
            }
        }
    }

    private void addMesh( MeshBase mesh ){
        mMeshes.add( mesh ) ;
    }
}
