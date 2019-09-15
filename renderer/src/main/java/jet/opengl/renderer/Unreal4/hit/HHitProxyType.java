package jet.opengl.renderer.Unreal4.hit;

/**
 * Represents a hit proxy class for runtime type checks.
 */
public class HHitProxyType {
    private HHitProxyType Parent;
	private String Name;

    public HHitProxyType(HHitProxyType InParent,String InName)
    {
        Parent = (InParent);
        Name= (InName);
    }
    public HHitProxyType GetParent()  { return Parent; }
    public  String GetName(){ return Name; }
}
