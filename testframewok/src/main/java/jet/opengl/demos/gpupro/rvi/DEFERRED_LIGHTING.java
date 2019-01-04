package jet.opengl.demos.gpupro.rvi;

final class DEFERRED_LIGHTING extends IPOST_PROCESSOR{

    DEFERRED_LIGHTING(){
        name = "DeferredLighting";
    }
    @Override
    boolean Create() {
        return true;
    }

    @Override
    DX11_RENDER_TARGET GetOutputRT() {
        return null;
    }

    @Override
    void AddSurfaces() {
        for(int i=0;i<DX11_RENDERER.getInstance().GetNumLights();i++)
        {
            ILIGHT light = DX11_RENDERER.getInstance().GetLight(i);
            if(light.IsActive())
                light.AddLitSurface();
        }
    }
}
