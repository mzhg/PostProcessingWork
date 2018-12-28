package jet.opengl.demos.gpupro.rvi;

abstract class IPOST_PROCESSOR {
    String name;
    boolean active = true;

    abstract boolean Create();

    abstract DX11_RENDER_TARGET GetOutputRT();

    abstract void AddSurfaces();

	String GetName()
    {
        return name;
    }

    void SetActive(boolean active)
    {
        this.active = active;
    }

    boolean IsActive()
    {
        return active;
    }
}
