package jet.opengl.demos.intel.va;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by mazhen'gui on 2017/11/18.
 */

final class VaAssetRenderMaterial extends VaAsset {
    public VaRenderMaterial Resource;
    protected VaAssetRenderMaterial(VaAssetPack pack, VaRenderMaterial material, String name) {
        super(pack, VaAssetType.RenderMaterial, name, material);

        Resource = material;
    }

    public VaRenderMaterial    GetMaterial( )                       { return Resource; }

    static VaAssetRenderMaterial CreateAndLoad( VaAssetPack pack, String name, VaStream inStream ) throws IOException{
        UUID uid;
//        VERIFY_TRUE_RETURN_ON_FALSE( inStream.ReadValue<vaGUID>( uid ) );
        uid = inStream.ReadUUID();
        VaRenderMaterial newResource = VaRenderMaterialManager.GetInstance( ).CreateRenderMaterial( uid );

        if( newResource == null )
            return null;

        if( newResource.Load( inStream ) )
        {
            return new VaAssetRenderMaterial( pack, newResource, name );
        }
        else
        {
            return null;
        }
    }

//    static shared_ptr<vaAssetRenderMaterial>        SafeCast( const shared_ptr<vaAsset> & asset ) { assert( asset->Type == vaAssetType::RenderMaterial ); return std::dynamic_pointer_cast<vaAssetRenderMaterial, vaAsset>( asset ); }

    protected String                                  IHO_GetInstanceInfo( )        { return //vaStringTools::Format( "material: %s ", Name().c_str() );
                                                                                        "material: " + Name();}
    protected void                                    IHO_Draw( ){
        /*vaAsset::IHO_Draw();

        ImGui::Text("Textures:");
        ImGui::Indent();
        IHO_DrawTextureInfo( "Albedo:    %s", Resource->GetTextureAlbedo() );
        IHO_DrawTextureInfo( "Normalmap: %s", Resource->GetTextureNormalmap() );
        IHO_DrawTextureInfo( "Specular:  %s", Resource->GetTextureSpecular() );
        IHO_DrawTextureInfo( "Emissive:  %s", Resource->GetTextureEmissive() );
        ImGui::Unindent();

        ImGui::Text("Settings");
        ImGui::Indent();
        vaRenderMaterial::MaterialSettings settings = Resource->GetSettings();
        ImGui::ColorEdit4( "Color mult albedo", &settings.ColorMultAlbedo.x );
        ImGui::ColorEdit3( "Color mult specular", &settings.ColorMultSpecular.x );
        ImGui::ColorEdit3( "Color mult emissive", &settings.ColorMultEmissive.x );
        ImGui::Combo( "Culling mode", (int*)&settings.FaceCull, "None\0Front\0Back" );
        ImGui::Checkbox( "Transparent", &settings.Transparent );
        ImGui::Checkbox( "AlphaTest", &settings.AlphaTest );
        ImGui::Checkbox( "ReceiveShadows", &settings.ReceiveShadows );
        ImGui::Checkbox( "Wireframe", &settings.Wireframe );
        ImGui::InputFloat( "Specular Pow", &settings.SpecPow, 0.1f );
        ImGui::InputFloat( "Specular Mul", &settings.SpecMul, 0.1f );

        if( Resource->GetSettings() != settings )
            Resource->SetSettings( settings );

        ImGui::Unindent();*/
    }

    @Override
    public boolean Save(VaStream outStream) {
        throw new UnsupportedOperationException();
    }
}
