package jet.opengl.demos.intel.va;

import java.util.List;

import jet.opengl.postprocessing.shader.Macro;

/**
 * Created by mazhen'gui on 2017/11/21.
 */

public class VaRenderMaterialCachedShadersDX11 {

    public VaDirectXVertexShader   VS_PosOnly = new VaDirectXVertexShader();
    public VaDirectXPixelShader    PS_DepthOnly = new VaDirectXPixelShader();
    public VaDirectXVertexShader   VS_Standard = new VaDirectXVertexShader();
    public VaDirectXPixelShader    PS_Forward = new VaDirectXPixelShader();
    public VaDirectXPixelShader    PS_Deferred = new VaDirectXPixelShader();

    public static class Key
    {
        public String                     WStringPart;
        public String                      AStringPart;

        // alphatest is part of the key because it determines whether PS_DepthOnly is needed at all; all other shader parameters are contained in shaderMacros
        public Key( String fileName, boolean alphaTest, String entryVS_PosOnly, String entryPS_DepthOnly, String entryVS_Standard,
                    String entryPS_Forward, String entryPS_Deferred, List<Macro> shaderMacros )
        {
            WStringPart = fileName;
            AStringPart = ((alphaTest)?("a&"):("b&")) + entryVS_PosOnly + "&" + entryPS_DepthOnly + "&" + entryVS_Standard + "&" + entryPS_Forward + "&" + entryPS_Deferred;
            for( int i = 0; shaderMacros != null && i < shaderMacros.size(); i++ ) {
                Macro macro = shaderMacros.get(i);
                AStringPart += macro.key + "&" + macro.value.toString();
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (WStringPart != null ? !WStringPart.equals(key.WStringPart) : key.WStringPart != null)
                return false;
            return AStringPart != null ? AStringPart.equals(key.AStringPart) : key.AStringPart == null;
        }

        @Override
        public int hashCode() {
            int result = WStringPart != null ? WStringPart.hashCode() : 0;
            result = 31 * result + (AStringPart != null ? AStringPart.hashCode() : 0);
            return result;
        }
    }
}
