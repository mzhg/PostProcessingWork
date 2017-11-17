package jet.opengl.demos.intel.va;

import java.util.UUID;

/**
 * Created by mazhen'gui on 2017/11/17.
 */
public class VaUIDObject {
    protected boolean                                  m_correctlyTracked;
    protected UUID m_uid;
    protected TT_Trackee< VaUIDObject >                m_trackee;

    protected VaUIDObject(UUID  uid ){
        m_correctlyTracked = false;
        m_uid        = uid;
        m_trackee = new TT_Trackee<>(VaUIDObjectRegistrar.GetInstance().m_objects, this );
    }

    public UUID                               UIDObject_GetUID( )                { return m_uid; }
    public boolean                                         UIDObject_IsCorrectlyTracked( )    { return m_correctlyTracked; }
}
