package jet.opengl.demos.intel.fluid.particles;

import java.util.ArrayList;

/**
 * Container for particle system.<p></p>

 A ParticleSystem contains multiple ParticleGroups where each group
 contains multiple ParticleOperations and a Particle array.<p></p>
 */

public class ParticleSystem {
    private final ArrayList<ParticleGroup>  mParticleGroups = new ArrayList<>();

    public ParticleSystem() {}
//    public ParticleSystem( const ParticleSystem & that ) ;
//    ParticleSystem & operator=( const ParticleSystem & that ) ;

    /// Add given ParticleGroup to the end of the current list of them.
    public void PushBack( ParticleGroup  pclGrp )
    {
        mParticleGroups.add( pclGrp ) ;
    }

    /*Iterator      Begin()       { return mParticleGroups.Begin() ; }
    ConstIterator Begin() const { return mParticleGroups.Begin() ; }

    Iterator      End()       { return mParticleGroups.End()   ; }
    ConstIterator End() const { return mParticleGroups.End()   ; }*/

    public void clear() {
        for(ParticleGroup group : mParticleGroups){
            group.clear();
        }
        mParticleGroups.clear();
    }

    public void update( float timeStep , int uFrame ){
        final int numParticleGroups = mParticleGroups.size() ;
        for( int iPclGrp = 0 ; iPclGrp < numParticleGroups ; ++ iPclGrp )
        {   // Run through groups in order.
            ParticleGroup particleGroup = mParticleGroups.get(iPclGrp) ;
            particleGroup.update( timeStep , uFrame ) ;
        }
    }

   /* void AddIndirectPodAssignment( const IndirectAddress & dst , const IndirectAddress & src , size_t sizeInBytes ) ;
    void AddIndirectAddressAssignment( const IndirectAddress & dst , const IndirectAddress & src ) ;

    private boolean    FindReferent( ReferenceIndex & refIdx , void * referentAddress ) const ;
    private void *  Dereference( const ReferenceIndex & refIdx ) const ;

    void    CopyAssignments( const ParticleSystem & that ) ;*/
}
