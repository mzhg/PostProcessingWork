package jet.opengl.demos.intel.cput;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;

import jet.opengl.postprocessing.common.Disposeable;

/**
 * Created by mazhen'gui on 2017/11/14.
 */

public abstract class CPUTTexture implements Disposeable{
    protected String       mName;
    protected CPUTMapType  mMappedType = CPUTMapType.CPUT_MAP_UNDEFINED;
    protected int          mNumArraySlices = 1;

    public CPUTTexture(){}

    public CPUTTexture(String name, int numArraySlices/*=1*/) /*: mMappedType(CPUT_MAP_UNDEFINED), mName(name), mNumArraySlices(numArraySlices) {}*/{
        mName = name;
        mNumArraySlices = numArraySlices;
    }

    public static CPUTTexture CreateTexture( String name, String absolutePathAndFilename, boolean loadAsSRGB ) throws IOException{
        return CPUTTextureDX11.CreateTextureDX11( name, absolutePathAndFilename, loadAsSRGB );
    }

    public static CPUTTexture CreateTextureArrayFromFilenameList( CPUTRenderParameters renderParams, String name,
                                                                  String []pAbsolutePathAndFilename, boolean loadAsSRGB )throws IOException {
        return CPUTTextureDX11.CreateTextureArrayFromFilenameList(renderParams, name, pAbsolutePathAndFilename, loadAsSRGB );
    }

    public abstract ByteBuffer MapTexture(CPUTRenderParameters params, CPUTMapType type, boolean wait/*=true*/ );
    public abstract void                      UnmapTexture( CPUTRenderParameters params ); // TODO: Store params on Map() and don't require here.
    public String GetName() { return mName; }

    public abstract void UpdateData(Buffer pData, int Format, int Type);
}
