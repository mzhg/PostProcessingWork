package jet.opengl.demos.intel.va;

/**
 *
 * Created by mazhen'gui on 2017/11/18.
 */
////////////////////////////////////////////////////////////////////////////////////////////////
// Simple base class for a singleton.
//  - ensures that the class is indeed a singleton
//  - provides access to it
//  1.) inherit YourClass from vaSystemManagerSingletonBase<YourClass>
//  2.) you're responsible for creation/destruction of the object!
//
public class VaSingletonBase {
    private static Object s_instance;

    protected VaSingletonBase( )
    {
        if(s_instance != null)
            throw new Error("");
        s_instance = this;
    }

    public static<T> T       GetInstance( )      {
//        assert( s_instance != NULL ); return *s_instance;
        if(s_instance == null)
            throw new NullPointerException("Not initelized yet!");

        return (T)s_instance;
    }
}
