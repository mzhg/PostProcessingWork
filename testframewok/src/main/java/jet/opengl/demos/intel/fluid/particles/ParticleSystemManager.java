package jet.opengl.demos.intel.fluid.particles;

import java.util.ArrayList;

/**
 * Created by Administrator on 2018/6/2 0002.
 */

public class ParticleSystemManager {
    private final ArrayList<ParticleSystem> mParticleSystems = new ArrayList<>();

    public ParticleSystemManager() {}

    /// Add given ParticleSystem to the end of the current list of them.
    public void pushBack( ParticleSystem  pclSys )
    {
        mParticleSystems.add( pclSys ) ;
    }

    public void clear() {
        for( ParticleSystem pclSys : mParticleSystems )
        {
            pclSys.clear();
        }

        mParticleSystems.clear();
    }
    public void update( float timeStep , int uFrame ){
        for( ParticleSystem pclSys : mParticleSystems )
        {
            pclSys.update( timeStep , uFrame ) ;
        }
    }
}
