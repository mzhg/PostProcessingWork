package jet.opengl.demos.intel.va;

/**
 * Created by mazhen'gui on 2017/11/16.
 */

public class VaImguiHierarchyObject {
    private static int                                              s_lastID;
    private String                                                  m_persistentObjectID;

    protected VaImguiHierarchyObject( ) {
        // create unique string id for each new object
        m_persistentObjectID = "IHO" + s_lastID;
        s_lastID++;
    }

    /**
     * this is just so that the title of the collapsing header can be the same for multiple different objects, as ImGui tracks them by string id which defaults to the title
     */
    private String                                          IHO_GetPersistentObjectID( )   { return m_persistentObjectID; }

    /** this string can change at runtime! */
    protected String                                          IHO_GetInstanceInfo( )        { return "Unnamed"; }

    /** draw your own IMGUI stuff here, and call IHO_DrawIfOpen on sub-elements */
    protected void                                            IHO_Draw( )                         { }

    public
    static void  DrawCollapsable(VaImguiHierarchyObject obj, boolean display_frame /*= true*/, boolean default_open /*= false*/,
                                 boolean indent /*= true*/ ){

    }

    public
    static void DrawCollapsable(VaImguiHierarchyObject obj, boolean display_frame /*= true*/, boolean default_open /*= false*/ ){
        DrawCollapsable(obj, display_frame, default_open, true);
    }

    public static void DrawCollapsable(VaImguiHierarchyObject obj, boolean display_frame /*= true*/){
        DrawCollapsable(obj, display_frame, false, true);
    }

    public static void DrawCollapsable( VaImguiHierarchyObject obj){
        DrawCollapsable(obj, true, false, true);
    }
}
