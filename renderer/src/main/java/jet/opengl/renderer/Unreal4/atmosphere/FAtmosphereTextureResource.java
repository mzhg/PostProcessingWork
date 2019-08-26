package jet.opengl.renderer.Unreal4.atmosphere;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.Texture3DDesc;
import jet.opengl.postprocessing.texture.TextureDataDesc;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.texture.TextureUtils;

final class FAtmosphereTextureResource {

    public static final int
            E_Transmittance = 0,
            E_Irradiance = 1,
            E_Inscatter = 2;

    /** Texture type */
    public int TexType;
    public TextureGL TextureRHI;

    /** Size of the vector field (X). */
    public int SizeX;
    /** Size of the vector field (Y). */
    public int SizeY;
    /** Size of the vector field (Z). */
    public int SizeZ;

    /** Static Texture Data.  */
    private ByteBuffer TextureData;

    public FAtmosphereTextureResource(FAtmospherePrecomputeParameters PrecomputeParams, ByteBuffer InTextureData, int Type){
        TexType = Type;
        int DataSize = 4; //sizeof(FColor);
        switch(TexType)
        {
            default:
            case E_Transmittance:
            {
                SizeX = PrecomputeParams.TransmittanceTexWidth;
                SizeY = PrecomputeParams.TransmittanceTexHeight;
                SizeZ = 1;
            }
            break;
            case E_Irradiance:
            {
                SizeX = PrecomputeParams.IrradianceTexWidth;
                SizeY = PrecomputeParams.IrradianceTexHeight;
                SizeZ = 1;
            }
            break;
            case E_Inscatter:
            {
                SizeX = PrecomputeParams.InscatterMuSNum * PrecomputeParams.InscatterNuNum;
                SizeY = PrecomputeParams.InscatterMuNum;
                SizeZ = PrecomputeParams.InscatterAltitudeSampleNum;
                DataSize = 8; //sizeof(FFloat16Color);
            }
            break;
        }

        int TotalSize = SizeX * SizeY * SizeZ * DataSize;
        if (InTextureData != null && InTextureData.remaining() == TotalSize)
        {
            // Grab a copy of the static volume data.
//            InTextureData.GetCopy(&TextureData, false);
            TextureData = InTextureData;
        }
        else
        {
            // Memzero...
            /*InTextureData.Lock(LOCK_READ_WRITE);
            void* TempVolume = InTextureData.Realloc(TotalSize);
            FMemory::Memzero(TempVolume, TotalSize);
            InTextureData.Unlock();*/
        }
    }

    /**
     * Initialize RHI resources.
     */
    public void InitRHI()
    {
        if (TextureData != null /*&& GetFeatureLevel() >= ERHIFeatureLevel::SM4*/)
        {
            switch(TexType)
            {
                default:
                case E_Transmittance:
                {
					final int DataSize = SizeX * SizeY * 4/*sizeof(FColor)*/;
                    /*FAtmosphereResourceBulkDataInterface BulkDataInterface(TextureData, DataSize);
                    FRHIResourceCreateInfo CreateInfo(&BulkDataInterface);
                    TextureRHI = RHICreateTexture2D(
                            SizeX, SizeY, PF_B8G8R8A8,
                            *//*NumMips=*//* 1,
                            *//*NumSamples=*//* 1,
                            *//*Flags=*//* TexCreate_ShaderResource,
                            *//*BulkData=*//* CreateInfo );
                    RHIBindDebugLabelName(TextureRHI, TEXT("E_Transmittance"));*/

                    TextureDataDesc data = new TextureDataDesc(GLenum.GL_RGBA, GLenum.GL_UNSIGNED_BYTE, TextureData);
                    TextureRHI = TextureUtils.createTexture2D(new Texture2DDesc(SizeX, SizeY,GLenum.GL_RGBA8), data);
                    TextureRHI.setName("E_Transmittance");
                }
                break;
                case E_Irradiance:
                {
					/*const uint32 DataSize = SizeX * SizeY * sizeof(FColor);
                    FAtmosphereResourceBulkDataInterface BulkDataInterface(TextureData, DataSize);
                    FRHIResourceCreateInfo CreateInfo(&BulkDataInterface);
                    TextureRHI = RHICreateTexture2D(
                            SizeX, SizeY, PF_B8G8R8A8,
                            *//*NumMips=*//* 1,
                            *//*NumSamples=*//* 1,
                            *//*Flags=*//* TexCreate_ShaderResource,
                            *//*BulkData=*//* CreateInfo );
                    RHIBindDebugLabelName(TextureRHI, TEXT("E_Irradiance"));*/

                    TextureDataDesc data = new TextureDataDesc(GLenum.GL_RGBA, GLenum.GL_UNSIGNED_BYTE, TextureData);
                    TextureRHI = TextureUtils.createTexture2D(new Texture2DDesc(SizeX, SizeY,GLenum.GL_RGBA8), data);
                    TextureRHI.setName("E_Irradiance");
                }
                break;
                case E_Inscatter:
                {
					/*const uint32 DataSize = SizeX * SizeY * SizeZ * sizeof(FFloat16Color);
                    FAtmosphereResourceBulkDataInterface BulkDataInterface(TextureData, DataSize);
                    FRHIResourceCreateInfo CreateInfo(&BulkDataInterface);
                    TextureRHI = RHICreateTexture3D(
                            SizeX, SizeY, SizeZ, PF_FloatRGBA,
                            *//*NumMips=*//* 1,
                            *//*Flags=*//* TexCreate_ShaderResource,
                            *//*BulkData=*//* CreateInfo );
                    RHIBindDebugLabelName(TextureRHI, TEXT("E_Inscatter"));*/
                    TextureDataDesc data = new TextureDataDesc(GLenum.GL_RGBA, GLenum.GL_HALF_FLOAT, TextureData);
                    Texture3DDesc desc = new Texture3DDesc(SizeX, SizeY, SizeZ, 1, GLenum.GL_RGBA16F);
                    TextureRHI = TextureUtils.createTexture3D(desc, data);
                    TextureRHI.setName("E_Inscatter");
                }
                break;
            }

//            FMemory::Free(TextureData);
            TextureData = null;
        }
    }

    /**
     * Release RHI resources.
     */
    public void ReleaseRHI()
    {
        TextureRHI.dispose();
    }
}
