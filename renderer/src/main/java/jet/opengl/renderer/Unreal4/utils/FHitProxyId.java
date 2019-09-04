package jet.opengl.renderer.Unreal4.utils;

import org.lwjgl.util.vector.ReadableVector3f;

import java.awt.Color;

import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.renderer.Unreal4.UE4Engine;

public class FHitProxyId {
    /** A uniquely identifying index for the hit proxy. */
    private int Index;

    /* Initialization constructor. */
//    private FHitProxyId(int InIndex) {Index = (InIndex);}

    /** Default constructor. */
    public FHitProxyId() {Index = (UE4Engine.INDEX_NONE);}

    /** Color conversion constructor. */
    public FHitProxyId(int color){
        Index = color;
    }

    /**
     * Maps the ID to a color which can be used to represent the ID.
     */
    public int GetColor() { return Index;}
}
