package jet.opengl.demos.gpupro.rvi;

final class RENDER_TARGET_CONFIG {
    private RT_CONFIG_DESC desc;

    boolean Create(RT_CONFIG_DESC desc)
    {
        this.desc = desc;  // todo
        return true;
    }

    RT_CONFIG_DESC GetDesc()
    {
        return desc;
    }
}
