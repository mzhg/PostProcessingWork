package jet.opengl.demos.intel.fluid.impulsion;

import java.util.ArrayList;

/**
 * Created by Administrator on 2018/4/25 0025.
 */

public class DynaWorld {
    private final ArrayList<RigidBody> mBodies = new ArrayList<>();


    public void addBody( RigidBody  body )
    {
        mBodies.add( body ) ;
    }

    public void updateBodies( float timeStep ){
        for( RigidBody body : mBodies)
        {
            body.update( timeStep ) ;
        }
    }
}
