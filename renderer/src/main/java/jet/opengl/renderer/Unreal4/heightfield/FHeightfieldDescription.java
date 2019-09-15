package jet.opengl.renderer.Unreal4.heightfield;

import java.util.ArrayList;
import java.util.HashMap;

import jet.opengl.postprocessing.util.Recti;

public class FHeightfieldDescription {
    public final Recti Rect = new Recti();
    public int DownsampleFactor;
    public final Recti DownsampledRect = new Recti();

    public final HashMap<FHeightfieldComponentTextures, ArrayList<FHeightfieldComponentDescription>> ComponentDescriptions = new HashMap<>();
}
