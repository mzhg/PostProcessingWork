package jet.opengl.demos.intel.fluid.render;

import java.util.ArrayList;

/**
 * Created by Administrator on 2018/3/13 0013.
 */

public class Effect {
    private final ArrayList<Technique>  mTechniques = new ArrayList<>();

    public Effect(){
        addNewTechnique();
    }

    public Technique addNewTechnique(){
        Technique  newTechnique = new Technique( this ) ;
        mTechniques.add( newTechnique ) ;
        return newTechnique ;
    }

    public void clearTechniques(){
        mTechniques.clear();
    }

    public ArrayList<Technique> getTechniques() { return mTechniques ; }
}
