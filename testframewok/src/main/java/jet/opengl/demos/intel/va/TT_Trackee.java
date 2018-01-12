package jet.opengl.demos.intel.va;

/**
 * Created by mazhen'gui on 2017/11/16.
 */

public class TT_Trackee<TTTagType> {
    TT_Tracker<TTTagType>         m_tracker;
    int                           m_index;
    TTTagType                     m_tag;

    public TT_Trackee( TT_Tracker<TTTagType> tracker, TTTagType tag ) {
        m_tracker = tracker;
        m_tag = tag;
        assert( tracker != null );

        m_tracker.m_tracker_objects.add( this );
        m_index = m_tracker.m_tracker_objects.size( ) - 1;
        assert( m_index == m_tracker.m_tracker_objects.size( ) - 1 );

        if( m_tracker.m_onAddedCallback != null )
            m_tracker.m_onAddedCallback.call(m_index);
    }

    public void release( ) {
        if( m_tracker == null )
            return;

        int index = m_index;
        assert( this == m_tracker.m_tracker_objects.get(index) );

        // not last one? move the last one to our place and update its index
        if( index < ( m_tracker.m_tracker_objects.size( ) - 1 ) )
        {
            int replacedByIndex = m_tracker.m_tracker_objects.size( ) - 1;
            if( m_tracker.m_beforeRemovedCallback != null )
                m_tracker.m_beforeRemovedCallback.call( m_index, replacedByIndex );

            /*m_tracker.m_tracker_objects[index] = m_tracker.m_tracker_objects[ replacedByIndex ];
            m_tracker.m_tracker_objects[index].m_index = index;*/

            m_tracker.m_tracker_objects.set(index, m_tracker.m_tracker_objects.get(replacedByIndex));
            m_tracker.m_tracker_objects.get(index).m_index = index;
        }
        else
        {
            if( m_tracker.m_beforeRemovedCallback != null )
                m_tracker.m_beforeRemovedCallback.call( m_index, -1 );
        }
//        m_tracker.m_tracker_objects.pop_back( );
        m_tracker.m_tracker_objects.remove(m_tracker.m_tracker_objects.size() - 1);

    }

    public TT_Tracker  GetTracker( )  { return m_tracker; };
    public TTTagType   GetTag( )      { return m_tag; }
    public int         GetIndex( )    { return m_index; }
}
