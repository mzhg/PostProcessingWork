package jet.opengl.demos.intel.fluid.particles;

import java.util.ArrayList;
import java.util.Iterator;

import jet.opengl.demos.intel.fluid.particles.operation.IParticleOperation;

/**
 * Group of particles and operations to perform on them.<p></p>
 * Created by Administrator on 2018/6/2 0002.
 */

public class ParticleGroup implements Iterable<IParticleOperation>{
    /** Dynamic array of particles which this group owns and on which all ParticleOperations in this group act. */
    private final ArrayList<Particle> mParticles = new ArrayList<>();

    /** Dynamic array of particle operations which operate on the particles that this group owns. */
    private final ArrayList<IParticleOperation> mParticleOps = new ArrayList<>();

    public ParticleGroup() {}
    public ParticleGroup( ParticleGroup that )  { set(that);}
    public ParticleGroup set( ParticleGroup  that ) {
        if( this != that )
        {   // Not self-copy.
            clear() ;   // Delete all previous items in this object.
            for(Particle p : that.mParticles){
                mParticles.add(p.clone());
            }
            for( IParticleOperation  pclOpOrig : that.mParticleOps )
            {   // For each particle operation in the original group...
//                IParticleOperation * pclOpOrig = * pclOpIter ;
                // Duplicate the original.
                IParticleOperation  pclOpDupe = pclOpOrig/*.clone()*/ ;
                // Remember the duplicate.
                mParticleOps.add( pclOpDupe ) ;
            }
        }
        return this ;
    }

    /// Add given ParticleOperation to the end of the current list of them.
    public void pushBack( IParticleOperation  pclOp )
    {
        mParticleOps.add( pclOp ) ;
    }

    public int getNumParticles() { return mParticles.size() ; }

    public boolean hasParticles() { return ! mParticles.isEmpty() ; }

    public boolean hasOperations() { return mParticleOps.isEmpty() ; }


    public void clear(){
        mParticles.clear() ;
        mParticleOps.clear();
    }

    public void update( float timeStep , int uFrame ) {
        final int numOps = mParticleOps.size() ;

        for( int iOp = 0 ; iOp < numOps ; ++ iOp )
        {   // Run operations in order.
            IParticleOperation  pOp = mParticleOps.get(iOp) ;
            pOp.operate( mParticles , timeStep , uFrame ) ;
        }
    }

    public ArrayList<Particle> getParticles() { return mParticles ; }

    public IParticleOperation getOperation( int pclOpIdx ) { return mParticleOps.get(pclOpIdx) ; }

    public int  indexOfOperation( IParticleOperation pclOpAddress ) { return mParticleOps.indexOf(pclOpAddress);}

    @Override
    public Iterator<IParticleOperation> iterator() {
        return mParticleOps.iterator();
    }
}
