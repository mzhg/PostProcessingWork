package jet.opengl.demos.intel.va;

import java.util.ArrayList;

/**
 * Created by mazhen'gui on 2017/11/16.
 */

public class TT_Tracker<TTTagType> {
    ArrayList< TT_Trackee<TTTagType>>            m_tracker_objects = new ArrayList<>();

    public TrackeeAddedCallbackType                            m_onAddedCallback;
    public TrackeeBeforeRemovedCallbackType                    m_beforeRemovedCallback;

    public TT_Tracker( )                                     { }
//    virtual ~vaTT_Tracker( );
    public ArrayList< TT_Trackee<TTTagType>> GetTrackedObjects( )          { return m_tracker_objects; };

    public TTTagType                                           get( int idx)        { return m_tracker_objects.get(idx).m_tag; }
//        const TTTagType                                     operator[]( std::size_t idx) const  { return m_tracker_objects[idx]->m_tag; }
    public void set(int idx, TTTagType value) {m_tracker_objects.get(idx).m_tag = value;}
    public  int                                              size( )                        { return m_tracker_objects.size();  }

    public void                                                SetAddedCallback(  TrackeeAddedCallbackType  callback )                   { m_onAddedCallback   = callback; }
    public void                                                SetBeforeRemovedCallback(  TrackeeBeforeRemovedCallbackType  callback )   { m_beforeRemovedCallback = callback; }

    public interface TrackeeAddedCallbackType{
        void call(int newTrackeeIndex);
    }

    public interface TrackeeBeforeRemovedCallbackType{
        void call(int toBeRemovedTrackeeIndex, int toBeReplacedByTrackeeIndex);
    }
}
