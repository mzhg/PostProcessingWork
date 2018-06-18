package jet.opengl.demos.intel.fluid.vortonfluid;

import org.lwjgl.util.vector.Readable;

import java.util.ArrayList;
import java.util.List;

import jet.opengl.demos.intel.fluid.impulsion.PhysicalObject;
import jet.opengl.demos.intel.fluid.particles.Particle;
import jet.opengl.demos.intel.fluid.utils.UniformGridFloat;
import jet.opengl.postprocessing.util.StackFloat;

/** Simulation with mutually interacting fluid and rigid bodies.

 \note   This class lost all of its members so at this point it's just a namespace.
    Convert this class to a namespace.<p></p>
 Created by Administrator on 2018/6/2 0002.
 */
public final class FluidBodySim {

    public static void solveBoundaryConditions(ArrayList<Particle> particles , float ambientFluidDensity , float fluidSpecificHeatCapacity ,
                                               ArrayList<PhysicalObject> physicalObjects , boolean bRespectAngVel ){

    }

    public static void RemoveEmbeddedParticles( ArrayList<Particle> particles , ArrayList<PhysicalObject> physicalObjects ){

    }
    public static void BuoyBodies(UniformGridFloat densityGrid , float ambientFluidDensity , Readable gravityAcceleration ,
                                  ArrayList<PhysicalObject> physicalObjects ){

    }

    public static void computeParticleProximityToWalls(StackFloat proximities , ArrayList<Particle> particles , ArrayList<PhysicalObject> physicalObjects , float maxProximity ){

    }
    private FluidBodySim(){}
}
