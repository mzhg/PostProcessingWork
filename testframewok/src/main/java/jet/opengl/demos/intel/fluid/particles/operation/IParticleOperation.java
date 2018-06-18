package jet.opengl.demos.intel.fluid.particles.operation;

import java.util.List;

import jet.opengl.demos.intel.fluid.particles.Particle;

/**
 * Created by Administrator on 2018/4/25 0025.
 */

public interface IParticleOperation extends Cloneable{

    /// Create another instance of this class.
    IParticleOperation create();


    /// Perform an operation on the given set of particles over a given duration.
    void operate(List<Particle> particles , float timeStep , int uFrame ) ;
}
