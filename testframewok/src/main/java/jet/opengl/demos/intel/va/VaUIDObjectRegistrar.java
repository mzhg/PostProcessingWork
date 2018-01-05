package jet.opengl.demos.intel.va;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.util.LogUtil;

/**
 * Created by mazhen'gui on 2017/11/17.
 */

public final class VaUIDObjectRegistrar implements Disposeable{
    private static  VaUIDObjectRegistrar g_Instance;

    TT_Tracker< VaUIDObject > m_objects;

    private Map<UUID, VaUIDObject/*, vaGUIDComparer*/ > m_objectsMap = new HashMap<>();

    public static VaUIDObjectRegistrar GetInstance() {
        CreateInstanceIfNot();
        return g_Instance;
    }

    private static void CreateInstanceIfNot() {
        if(g_Instance == null){
            g_Instance = new VaUIDObjectRegistrar();
        }
    }

    private VaUIDObjectRegistrar(){
        m_objects = new TT_Tracker<>();
        m_objects.SetAddedCallback( /*std::bind( &vaUIDObjectRegistrar::UIDObjectTrackeeAddedCallback, this, std::placeholders::_1 )*/
            this::UIDObjectTrackeeAddedCallback);
        m_objects.SetBeforeRemovedCallback( /*std::bind( &vaUIDObjectRegistrar::UIDObjectTrackeeBeforeRemovedCallback, this, std::placeholders::_1, std::placeholders::_2 )*/
            this::UIDObjectTrackeeBeforeRemovedCallback);
    }

    @Override
    public void dispose() {
        assert( m_objects.size() == 0 );
        assert( m_objectsMap.size() == 0 );

        g_Instance = null;
    }

    /*   ~vaUIDObjectRegistrar( )
    {
        // not 0? memory leak or not all objects deleted before the registrar was deleted (bug)
        assert( m_objects.size() == 0 );
        assert( m_objectsMap.size() == 0 );
    }*/

    protected void UIDObjectTrackeeAddedCallback( int newTrackeeIndex ){
//        auto it = m_objectsMap.find( m_objects[newTrackeeIndex]->m_uid );
        final VaUIDObject object = m_objects.get(newTrackeeIndex);
        VaUIDObject it = m_objectsMap.get(object.m_uid);
        if( it != /*m_objectsMap.end( )*/ null )
        {
            //VA_ASSERT_ALWAYS( "New vaUIDObject created but the ID already exists: this is a bug, the new object will not be tracked and will not be searchable by vaUIDObjectRegistrar::Find" );
            LogUtil.e(LogUtil.LogType.DEFAULT, "New vaUIDObject created but the ID already exists: this is a bug, the new object will not be tracked and will not be searchable by vaUIDObjectRegistrar::Find");
            object.m_correctlyTracked = false;
        }
        else
        {
//            m_objectsMap.insert( std::make_pair( m_objects[newTrackeeIndex]->m_uid, m_objects[newTrackeeIndex] ) );
            m_objectsMap.put(object.m_uid, object);
            object.m_correctlyTracked = true;
        }
    }

    protected void UIDObjectTrackeeBeforeRemovedCallback( int toBeRemovedTrackeeIndex, int toBeReplacedByTrackeeIndex ){
        // if we're not correctly tracked, there's no point removing us from the map (in fact, we will likely remove another instance)
        final VaUIDObject object = m_objects.get(toBeRemovedTrackeeIndex);
        if( !object.m_correctlyTracked )
        {
            LogUtil.w(LogUtil.LogType.DEFAULT, "Deleting an untracked vaUIDObject; There were errors on creation, check the log." );
            return;
        }

        VaUIDObject it = m_objectsMap.remove( object.m_uid );
        if( it == /*m_objectsMap.end( )*/ null )
        {
            LogUtil.e(LogUtil.LogType.DEFAULT, "Deleting a tracked vaUIDObject that couldn't be found: this is an indicator of a more serious error such as an algorithm bug or a memory overwrite. Don't ignore it.");
            assert( false );
        }
        else
        {
            // if this happens, we're removing wrong object - this is a serious error, don't ignore it!
            assert( object == it/*->second */);

            object.m_correctlyTracked = false;
//            m_objectsMap.erase( it );
        }
    }

    public
    static<T> T                                   Find( UUID uid ) {
        if (uid == /*VaCore.GUIDNull ()*/ null )
            return null;

        /*auto it = vaUIDObjectRegistrar::GetInstance ().m_objectsMap.find(uid);
        if (it == vaUIDObjectRegistrar::GetInstance ().m_objectsMap.end() )
        {
            return nullptr;
        }
       else
        {
#ifdef _DEBUG
            T * ret = dynamic_cast < T * > (it -> second);
            assert (ret != NULL);
            return ret;
#else
            return static_cast < T * > (it -> second);
#endif
        }*/

        return (T) GetInstance().m_objectsMap.get(uid);
    }

//    template< class T >
    public static<T> T                                  ReconnectDependency( /*std::shared_ptr<T> & outSharedPtr,*/ UUID uid ){
        /*T * obj;
        obj = Find<T>( uid );
        if( obj != nullptr )
            outSharedPtr = std::static_pointer_cast<T>( obj->shared_from_this( ) );
        else
            outSharedPtr = nullptr;*/
        return Find(uid);
    }
}
