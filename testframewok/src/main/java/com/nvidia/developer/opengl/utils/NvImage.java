package com.nvidia.developer.opengl.utils;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CachaRes;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.FileUtils;
import jet.opengl.postprocessing.util.LogUtil;
import jet.opengl.postprocessing.util.Numeric;

/**
 * GL-based image loading, representation and handling
 * Support loading of images from DDS files and data, including
 * cube maps, arrays mipmap levels, formats, etc.
 * The class does NOT encapsulate a GL texture object, only the
 * client side pixel data that could be used to create such a texture
 *
 * @author Nvidia 2014-9-4:19:50
 */
public class NvImage {
    protected int _width;
    protected int _height;
    protected int _depth;
    protected int _levelCount;
    protected int _layers;
    protected int _format = GL_RGBA;
    protected int _internalFormat = GL_RGBA8;
    protected int _type = GL_UNSIGNED_BYTE;
    protected int _elementSize;
    protected boolean _cubeMap;

    protected static boolean upperLeftOrigin = true;
    protected static boolean m_expandDXT = false;
    protected static NvGfxAPIVersion m_gfxAPIVersion = NvGfxAPIVersion.GLES2;
    protected static FormatInfo[] formatTable = new FormatInfo[]{new FormatInfo("dds", new ReadDDS(), null)};

    protected List<byte[]> _data = new ArrayList<byte[]>();

    /** Static elements used to dispatch to proper sub-readers */
    private static final class FormatInfo{
        String extension;
        ImageReader reader;
        ImageWriter writer;

        private FormatInfo(String extension, ImageReader reader,
                           ImageWriter writer) {
            this.extension = extension;
            this.reader = reader;
            this.writer = writer;
        }
    }

    /**
     * Sets the image origin to top or bottom.
     * Sets the origin to be assumed when loading image data from file or data block
     * By default, the image library places the origin of images at the
     * lower-left corner, to make it map exactly to OpenGL screen coords.
     * This flips the image, and it might make it incompatible with
     * the texture coordinate conventions of an imported model.
     * @param ul true if the origin is in the upper left (D3D/DDS) or bottom-left (GL)
     */
    public static void upperLeftOrigin(boolean ul){
        upperLeftOrigin = ul;
    }

    /**
     * Create a new GL texture and upload the given image to it.<p>
     * @return the GL texture ID on success, 0 on failure
     */
    @CachaRes
    public int updaloadTexture(){
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        int texID = gl.glGenTexture();

        NvGfxAPIVersion api = getAPIVersion();
        int internalFormat = api.isGLES ? getFormat() : getInternalFormat();

        if (isCubeMap()) {
            gl.glBindTexture(GLenum.GL_TEXTURE_CUBE_MAP, texID);
            for (int f = GLenum.GL_TEXTURE_CUBE_MAP_POSITIVE_X; f <= GLenum.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z; f++) {
                int w = getWidth();
                int h = getHeight();
                for (int l = 0; l < getMipLevels(); l++) {
                    if (isCompressed()) {
                        gl.glCompressedTexImage2D( f, l, internalFormat, w, h, 0, CacheBuffer.wrap(getLevel(l, f)));
                    } else {
                        gl.glTexImage2D( f, l, internalFormat, w, h, 0, getFormat(), getType(), CacheBuffer.wrap(getLevel(l, f)));
                        GLCheck.checkError();
                    }

                    w >>= 1;
                    h >>= 1;
                    w = (w != 0) ? w : 1;
                    h = (h != 0) ? h : 1;
                }
            }

            gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);
            gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_MIN_FILTER, getMipLevels() == 1? GLenum.GL_LINEAR : GLenum.GL_LINEAR_MIPMAP_LINEAR);
            gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_WRAP_R, GLenum.GL_CLAMP_TO_EDGE);
            gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_CLAMP_TO_EDGE);
            gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_CLAMP_TO_EDGE);
        } else {
            gl.glBindTexture(GLenum.GL_TEXTURE_2D, texID);

            int w = getWidth();
            int h = getHeight();
            for (int l = 0; l < getMipLevels(); l++) {
                if (isCompressed()) {
                    gl.glCompressedTexImage2D(GLenum.GL_TEXTURE_2D, l, internalFormat, w, h, 0, CacheBuffer.wrap(getLevel(l)));
                } else {
                    gl.glTexImage2D(GLenum.GL_TEXTURE_2D, l, internalFormat, w, h, 0, getFormat(), getType(), CacheBuffer.wrap(getLevel(l)));
                }

                w >>= 1;
                h >>= 1;
                w = (w != 0) ? w : 1;
                h = (h != 0) ? h : 1;
            }

            gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);
            gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MIN_FILTER, getMipLevels() == 1? GLenum.GL_LINEAR : GLenum.GL_LINEAR_MIPMAP_LINEAR);
            gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_WRAP_R, GLenum.GL_CLAMP_TO_EDGE);
            gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_CLAMP_TO_EDGE);
        }

        GLCheck.checkError();
        return texID;
    }

    /**
     * Create a new GL texture directly from DDS file.
     * @param filename the image filename (and path) to load
     * @return the GL texture ID on success, 0 on failure
     */
    public static int uploadTextureFromDDSFile(String filename)throws IOException{
        int texID = 0;
        NvImage image = new NvImage();
        if(image.loadImageFromFile(filename)){
            texID = image.updaloadTexture();
        }

        return texID;
    }

    /**
     * Create a new GL texture directly from DDS file inputStram.
     * @param in the image data where to read.
     * @return the GL texture ID on success, 0 on failure
     */
    public static int uploadTextureFromDDSFile(InputStream in)throws IOException{
        int texID = 0;
        NvImage image = new NvImage();
        if(image.loadImageFromStream(in, "dds")){
            texID = image.updaloadTexture();
        }

        return texID;
    }

    /**
     * Create a new GL texture directly from DDS file-formatted data
     * @param ddsData the pointer to the DDS file data
     * @param offset the start position of the ddsData to read.
     * @param length the number bytes to read.
     * @return the GL texture ID on success, 0 on failure
     */
    public static int uploadTextureFromDDSData(byte[] ddsData, int offset, int length){
        int texID = 0;
        NvImage image = new NvImage();
        if(image.loadImageFromFileData(ddsData, offset, length, "dds")){
            texID = image.updaloadTexture();
        }

        return texID;
    }

    /**
     * Create a new NvImage (no texture) directly from DDS file
     * @param filename the image filename (and path) to load
     * @return a pointer to the NvImage representing the file or null on failure
     */
    public static NvImage createFromDDSFile(String filename)throws IOException{
        NvImage image = new NvImage();
        image.loadImageFromFile(filename);
        return image;
    }

    /**
     * Convert a flat "cross" image to  a cubemap<p>
     * Convert a suitable image from a cubemap cross to a cubemap
     * @return true on success or false for unsuitable source images
     */
    public boolean convertCrossToCubemap(){
        //can't already be a cubemap
        if (isCubeMap())
            return false;

        //mipmaps are not supported
        if (_levelCount != 1)
            return false;

        //compressed textures are not supported
        if (isCompressed())
            return false;

        //this function only supports vertical cross format for now (3 wide by 4 high)
        if (  (_width / 3 != _height / 4) || (_width % 3 != 0) || (_height % 4 != 0) || (_depth != 0))
            return false;

        //get the source data
        byte[] data = _data.get(0);

        int fWidth = _width / 3;
        int fHeight = _height / 4;

        //remove the old pointer from the vector
//	    _data.pop_back();
        _data.remove(_data.size() - 1);

        byte[] face = new byte[ fWidth * fHeight * _elementSize];
        int ptr;

        //extract the faces

        // positive X
        ptr = 0; //face;
        for (int j=0; j<fHeight; j++) {
//	        memcpy( ptr, &data[((_height - (fHeight + j + 1))*_width + 2 * fWidth) * _elementSize], fWidth*_elementSize);
            System.arraycopy(data, ((_height - (fHeight + j + 1))*_width + 2 * fWidth) * _elementSize, face, ptr, fWidth*_elementSize);
            ptr += fWidth*_elementSize;
        }
        _data.add(face);

        // negative X
        face = new byte[ fWidth * fHeight * _elementSize];
        ptr = 0; // face;
        for (int j=0; j<fHeight; j++) {
//	        memcpy( ptr, &data[(_height - (fHeight + j + 1))*_width*_elementSize], fWidth*_elementSize);
            System.arraycopy(data, (_height - (fHeight + j + 1))*_width*_elementSize, face, ptr, fWidth*_elementSize);
            ptr += fWidth*_elementSize;
        }
        _data.add(face);

        // positive Y
        face = new byte[ fWidth * fHeight * _elementSize];
        ptr = 0; //face;
        for (int j=0; j<fHeight; j++) {
//	        memcpy( ptr, &data[((4 * fHeight - j - 1)*_width + fWidth)*_elementSize], fWidth*_elementSize);
            System.arraycopy(data, ((4 * fHeight - j - 1)*_width + fWidth)*_elementSize, face, ptr, fWidth*_elementSize);
            ptr += fWidth*_elementSize;
        }
        _data.add(face);

        // negative Y
        face = new byte[ fWidth * fHeight * _elementSize];
        ptr = 0; //face;
        for (int j=0; j<fHeight; j++) {
//	        memcpy( ptr, &data[((2*fHeight - j - 1)*_width + fWidth)*_elementSize], fWidth*_elementSize);
            System.arraycopy(data, ((2*fHeight - j - 1)*_width + fWidth)*_elementSize, face, ptr, fWidth*_elementSize);
            ptr += fWidth*_elementSize;
        }
        _data.add(face);

        // positive Z
        face = new byte[ fWidth * fHeight * _elementSize];
        ptr = 0;
        for (int j=0; j<fHeight; j++) {
//	        memcpy( ptr, &data[((_height - (fHeight + j + 1))*_width + fWidth) * _elementSize], fWidth*_elementSize);
            System.arraycopy(data, ((_height - (fHeight + j + 1))*_width + fWidth) * _elementSize, face, ptr, fWidth*_elementSize);
            ptr += fWidth*_elementSize;
        }
        _data.add(face);

        // negative Z
        face = new byte[ fWidth * fHeight * _elementSize];
        ptr = 0;
        for (int j=0; j<fHeight; j++) {
            for (int i=0; i<fWidth; i++) {
//	            memcpy( ptr, &data[(j*_width + 2 * fWidth - (i + 1))*_elementSize], _elementSize);
                System.arraycopy(data, (j*_width + 2 * fWidth - (i + 1))*_elementSize, face, ptr, _elementSize);
                ptr += _elementSize;
            }
        }
        _data.add(face);

        //set the new # of faces, width and height
        _layers = 6;
        _width = fWidth;
        _height = fHeight;
        _cubeMap = true;

        return true;
    }

    public boolean setImage(int width, int height, int format, int type, byte[] data){
        //check parameters before destroying the old image
        int elementSize;
        int internalFormat;

        switch (format) {
            case GL_ALPHA:
                switch (type) {
                    case GL_UNSIGNED_BYTE:
                        internalFormat = GL_ALPHA8;
                        elementSize = 1;
                        break;
                    case GL_UNSIGNED_SHORT:
                        internalFormat = GL_ALPHA16;
                        elementSize = 2;
                        break;
                    case GL_FLOAT:
                        internalFormat = GL_ALPHA32F;
                        elementSize = 4;
                        break;
                    case GL_HALF_FLOAT:
                        internalFormat = GL_ALPHA16F;
                        elementSize = 2;
                        break;
                    default:
                        return false; //format/type combo not supported
                }
                break;
            case GL_LUMINANCE:
                switch (type) {
                    case GL_UNSIGNED_BYTE:
                        internalFormat = GL_LUMINANCE8;
                        elementSize = 1;
                        break;
                    case GL_UNSIGNED_SHORT:
                        internalFormat = GL_LUMINANCE16;
                        elementSize = 2;
                        break;
                    case GL_FLOAT:
                        internalFormat = GL_LUMINANCE32F_ARB;
                        elementSize = 4;
                        break;
                    case GL_HALF_FLOAT:
                        internalFormat = GL_LUMINANCE16F_ARB;
                        elementSize = 2;
                        break;
                    default:
                        return false; //format/type combo not supported
                }
                break;
            case GL_LUMINANCE_ALPHA:
                switch (type) {
                    case GL_UNSIGNED_BYTE:
                        internalFormat = GL_LUMINANCE8_ALPHA8;
                        elementSize = 2;
                        break;
                    case GL_UNSIGNED_SHORT:
                        internalFormat = GL_LUMINANCE16_ALPHA16;
                        elementSize = 4;
                        break;
                    case GL_FLOAT:
                        internalFormat = GL_LUMINANCE_ALPHA32F_ARB;
                        elementSize = 8;
                        break;
                    case GL_HALF_FLOAT:
                        internalFormat = GL_LUMINANCE_ALPHA16F_ARB;
                        elementSize = 4;
                        break;
                    default:
                        return false; //format/type combo not supported
                }
                break;
            case GL_RGB:
                switch (type) {
                    case GL_UNSIGNED_BYTE:
                        internalFormat = GL_RGB8;
                        elementSize = 3;
                        break;
                    case GL_UNSIGNED_SHORT:
                        internalFormat = GL_RGB16;
                        elementSize = 6;
                        break;
                    case GL_FLOAT:
                        internalFormat = GL_RGB32F;
                        elementSize = 12;
                        break;
                    case GL_HALF_FLOAT_ARB:
                        internalFormat = GL_RGB16F;
                        elementSize = 6;
                        break;
                    default:
                        return false; //format/type combo not supported
                }
                break;
            case GL_RGBA:
                switch (type) {
                    case GL_UNSIGNED_BYTE:
                        internalFormat = GL_RGBA8;
                        elementSize = 4;
                        break;
                    case GL_UNSIGNED_SHORT:
                        internalFormat = GL_RGBA16;
                        elementSize = 8;
                        break;
                    case GL_FLOAT:
                        internalFormat = GL_RGBA32F;
                        elementSize = 16;
                        break;
                    case GL_HALF_FLOAT_ARB:
                        internalFormat = GL_RGBA16F;
                        elementSize = 8;
                        break;
                    default:
                        return false; //format/type combo not supported
                }
                break;
            default:
                //bad format
                return false;
        }


        //clear old data
        freeData();

        byte[] newImage = new byte[width*height*elementSize];
//	    memcpy( newImage, data, width*height*elementSize);
        System.arraycopy(data, 0, newImage, 0, width*height*elementSize);

        _data.add(newImage);

        _width = width;
        _height = height;
        _elementSize = elementSize;
        _internalFormat = internalFormat;
        _levelCount = 1;
        _layers = 1;
        _depth = 0;
        _format = format;
        _type = type;
        _cubeMap = false;

        return true;
    }

    /** Clear all the image data. */
    public void freeData(){
        _data.clear();
    }

    /**
     * The size (in bytes) of a selected mipmap level of the image
     * @param level the Size in bytes of a level of the image
     * @return the mipmap level whose size if to be returned
     */
    public int getImageSize(int level){
        boolean compressed = isCompressed();
        int w = _width >> level;
        int h = _height >> level;
        int d = _depth >> level;
        w = (w != 0) ? w : 1;
        h = (h != 0) ? h : 1;
        d = (d != 0) ? d : 1;
        int bw = (compressed) ? ( w + 3 ) / 4 : w;
        int bh = (compressed) ? ( h + 3 ) / 4 : h;
        int elementSize = _elementSize;

        return bw*bh*d*elementSize;
    }

    /**
     * Get a pointer to the pixel data for a given mipmap level.
     * @param level the mipmap level [0, getMipLevels)
     * @return a pointer to the data
     */
    public byte[] getLevel(int level){
        return getLevel(level, GLenum.GL_TEXTURE_CUBE_MAP_POSITIVE_X);
    }

    /**
     * Get a pointer to the pixel data for a given mipmap level and cubemap face.
     * @param level the mipmap level [0, getMipLevels)
     * @param face the cubemap face (GL_TEXTURE_CUBE_MAP_*_*)
     * @return a pointer to the data
     */
    public byte[] getLevel(int level, int face){
        assert( level < _levelCount);
        assert( face >= GLenum.GL_TEXTURE_CUBE_MAP_POSITIVE_X && face <= GLenum.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z);
        assert( face == GLenum.GL_TEXTURE_CUBE_MAP_POSITIVE_X || _cubeMap);

        face = face - GLenum.GL_TEXTURE_CUBE_MAP_POSITIVE_X;

        assert( (face*_levelCount + level) < _data.size());

        // make sure we don't hand back a garbage pointer
        if (level >=_levelCount || face >= _layers)
            return null;

        return _data.get(face*_levelCount + level);
    }

    /**
     * Whether or not the image is compressed
     * @return boolean whether the data is a crompressed format
     */
    public boolean isCompressed(){
        switch(_format) {
            case GL_COMPRESSED_RGB_S3TC_DXT1_EXT:
            case GL_COMPRESSED_RGBA_S3TC_DXT1_EXT:
            case GL_COMPRESSED_RGBA_S3TC_DXT3_EXT:
            case GL_COMPRESSED_RGBA_S3TC_DXT5_EXT:
            case GL_COMPRESSED_LUMINANCE_LATC1_EXT:
            case GL_COMPRESSED_SIGNED_LUMINANCE_LATC1_EXT:
            case GL_COMPRESSED_LUMINANCE_ALPHA_LATC2_EXT:
            case GL_COMPRESSED_SIGNED_LUMINANCE_ALPHA_LATC2_EXT:
            case GL_COMPRESSED_RG_RGTC2:
            case GL_COMPRESSED_RED_RGTC1:
            case GL_COMPRESSED_SIGNED_RG_RGTC2:
            case GL_COMPRESSED_SIGNED_RED_RGTC1:
                return true;
        }
        return false;
    }

    /**
     * Get a pointer to the pixel data for a given mipmap level and array slice.
     * @param level the mipmap level [0, {@link #getMipLevels})
     * @param layer the layer index [0, {@link #getLayers})
     * @return a pointer to the data
     */
    public byte[] getLayerLevel(int level, int layer){
        assert( level < _levelCount);
        assert( layer < _layers);

        assert( (layer*_levelCount + level) < _data.size());

        // make sure we don't hand back a garbage pointer
        if (level >=_levelCount || layer >= _layers)
            return null;

        return _data.get( layer*_levelCount + level);
    }

    /**
     * Loads an image from file-formatted data.<p>
     * Initialize an image from file-formatted memory; only DDS files are supported
     * @param fileData the block of memory representing the entire image file
     * @param fileExt the file extension string; must be "dds"
     * @return true on success, false on failure
     */
    public boolean loadImageFromFileData(byte[] fileData, int offset, int length, String fileExt){
        int formatCount = formatTable.length;

        //try to match by format first
        for ( int ii = 0; ii < formatCount; ii++) {
//	        if ( ! strcasecmp( formatTable[ii].extension, fileExt)) {
            if(formatTable[ii].extension.equalsIgnoreCase(fileExt)) {
                //extension matches, load it
                return formatTable[ii].reader.invoke(fileData,offset, length, this);
            }
        }

        return false;
    }

    /**
     * Loads an image from inputStream.<p>
     * Initialize an image from inputStream; only DDS files are supported
     * @param in the inputStram where the image data come from.
     * @param fileExt the file extension string; must be "dds"
     * @return true on success, false on failure
     */
    public boolean loadImageFromStream(InputStream in, String fileExt) throws IOException{
        int formatCount = formatTable.length;

        //try to match by format first
        for ( int ii = 0; ii < formatCount; ii++) {
//	        if ( ! strcasecmp( formatTable[ii].extension, fileExt)) {
            if(formatTable[ii].extension.equalsIgnoreCase(fileExt)) {
                //extension matches, load it
                return formatTable[ii].reader.invoke(in, this);
            }
        }

        return false;
    }

    /**
     * Loads an image from filename.<p>
     * Initialize an image from image file; only DDS files are supported
     * @param filename the image file name.
     * @return true on success, false on failure
     */
    public boolean loadImageFromFile(String filename) throws IOException{
        int formatCount = formatTable.length;

        int index = filename.lastIndexOf('.');
        String fileExt = filename.substring(index + 1);
        //try to match by format first
        for ( int ii = 0; ii < formatCount; ii++) {
//	        if ( ! strcasecmp( formatTable[ii].extension, fileExt)) {
            if(formatTable[ii].extension.equalsIgnoreCase(fileExt)) {
                //extension matches, load it
                return formatTable[ii].reader.invoke(filename, this);
            }
        }

        return false;
    }

    /**
     * Whether or not the image's pixel format has an explicit alpha channel
     * @return boolean whether the image has explicit alpha channel
     */
    public boolean hasAlpha(){
        switch(_format) {
            case GL_COMPRESSED_RGB_S3TC_DXT1_EXT:
            case GL_COMPRESSED_RGBA_S3TC_DXT1_EXT:
            case GL_COMPRESSED_RGBA_S3TC_DXT3_EXT:
            case GL_COMPRESSED_RGBA_S3TC_DXT5_EXT:
            case GL_COMPRESSED_LUMINANCE_LATC1_EXT:
            case GL_COMPRESSED_SIGNED_LUMINANCE_LATC1_EXT:
            case GL_COMPRESSED_LUMINANCE_ALPHA_LATC2_EXT:
            case GL_COMPRESSED_SIGNED_LUMINANCE_ALPHA_LATC2_EXT:
            case GL_ALPHA:
            case GL_LUMINANCE_ALPHA:
            case GL_RGBA:
            case GL_RGBA_INTEGER:
            case GL_BGRA:
                return true;
        }
        return false;
    }

    protected void flipSurface(byte[] surf, int width, int height, int depth){
        int lineSize;

        depth = (depth > 0) ? depth : 1;

        if(!isCompressed()){
            lineSize = _elementSize * width;
            int sliceSize = lineSize * height;

            byte[] tempBuf = new byte[lineSize];

            for(int ii = 0; ii < depth; ii++){
                int top = ii * sliceSize;
                int bottom = top + (sliceSize - lineSize);

                for ( int jj = 0; jj < (height >> 1); jj++) {
                    System.arraycopy(surf, top, tempBuf, 0, lineSize);
                    System.arraycopy(surf, bottom, surf, top, lineSize);
                    System.arraycopy(tempBuf, 0, surf, bottom, lineSize);

                    top += lineSize;
                    bottom -= lineSize;
                }
            }
        }else{
            FlipBlocks flipblocks;
            width = (width + 3) / 4;
            height = (height + 3) / 4;
            int blockSize = 0;

            switch(_format){
                case GL_COMPRESSED_RGBA_S3TC_DXT1_EXT:
                    blockSize = 8;
                    flipblocks = new FlipBlocksDxtc1();
                    break;
                case GL_COMPRESSED_RGBA_S3TC_DXT3_EXT:
                    blockSize = 16;
                    flipblocks = new FlipBlocksDxtc3();
                    break;
                case GL_COMPRESSED_RGBA_S3TC_DXT5_EXT:
                    blockSize = 16;
                    flipblocks = new FlipBlocksDxtc5();
                    break;
                case GL_COMPRESSED_LUMINANCE_LATC1_EXT:
                case GL_COMPRESSED_SIGNED_LUMINANCE_LATC1_EXT:
                case GL_COMPRESSED_RED_RGTC1:
                case GL_COMPRESSED_SIGNED_RED_RGTC1:
                    blockSize = 8;
                    flipblocks = new FlipBlocksBc4();
                    break;
                case GL_COMPRESSED_LUMINANCE_ALPHA_LATC2_EXT:
                case GL_COMPRESSED_SIGNED_LUMINANCE_ALPHA_LATC2_EXT:
                case GL_COMPRESSED_RG_RGTC2:
                case GL_COMPRESSED_SIGNED_RG_RGTC2:
                    blockSize = 16;
                    flipblocks = new FlipBlocksBc5();
                    break;
                default:
                    return;
            }

            lineSize = width * blockSize;
            byte[] tempBuf = new byte[lineSize];

            int topIndex = 0;
            int bottomIndex = (height-1) * lineSize;

            DXTColBlock[] top = new DXTColBlock[lineSize / 8];
            DXTColBlock[] bottom = new DXTColBlock[lineSize / 8];
            for (int i = 0; i < top.length; i++) {
                top[i] = new DXTColBlock();
                bottom[i] = new DXTColBlock();
            }

            for (int j = 0; j < Math.max(height >> 1, 1); j++){
                fillDxtColBlocks(top, surf, topIndex);
                fillDxtColBlocks(bottom, surf, bottomIndex);

                if(topIndex == bottomIndex){
                    flipblocks.invoke(top, width);
                    writeDxtColBlocks(top, surf, topIndex);
                    break;
                }

                flipblocks.invoke(top, width);
                flipblocks.invoke(bottom, width);

                writeDxtColBlocks(top, surf, topIndex);
                writeDxtColBlocks(bottom, surf, bottomIndex);

                // swap(bottom, top, linesize);
                // bottom -. tmp
                System.arraycopy(surf, bottomIndex, tempBuf, 0, lineSize);
                // top -. bottom
                System.arraycopy(surf, topIndex, surf, bottomIndex, lineSize);
                // tmp -. top
                System.arraycopy(tempBuf, 0, surf, topIndex, lineSize);

                topIndex += lineSize;
                bottomIndex -= lineSize;
            }
        }

    }

    private static void fillDxtColBlocks(DXTColBlock[] blocks, byte[] data, int offset) {
        for (int i = 0; i < blocks.length; i++) {
            offset = blocks[i].read(data, offset);
        }
    }

    private static final void writeDxtColBlocks(DXTColBlock[] blocks, byte[] data,
                                                int offset) {
        for (int i = 0; i < blocks.length; i++) {
            offset = blocks[i].write(data, offset);
        }
    }

    /**
     * The image width in pixels
     * @return the width of the image in pixels
     */
    public int getWidth() { return _width;}

    /**
     * The image height in pixels
     * @return the height of the image in pixels
     */
    public int getHeight() { return _height;}

    /**
     * The image depth in pixels.<p>
     * This is the third dimension of a 3D/volume image, NOT the color-depth
     * @return the depth of the image (0 for images with no depth)
     */
    public int getDepth() { return _depth;}

    /**
     * The number of miplevels.
     * @return the number of mipmap levels available for the image
     */
    public int getMipLevels() {return _levelCount;}

    /**
     * The number of cubemap faces.
     * @return the number of cubemap faces available for the image (0 for non-cubemap images)
     */
    public int getFaces() {return _cubeMap ? _layers : 0;}

    /**
     * The number of layers in a texture array
     * @return the number of layers for use in texture arrays
     */
    public int getLayers() {return _layers;}

    /**
     * The GL format of the image
     * @return the format of the image data (GL_RGB, GL_BGR, etc)
     */
    public int getFormat() {return _format;}

    /**
     * The GL internal format of the image
     * @return the suggested internal format for the data
     */
    public int getInternalFormat() {return _internalFormat;}

    /**
     * The GL type of the pixel data
     * @return the type of the image data
     */
    public int getType() {return _type;};

    /**
     * Whether or not the image is a cubemap
     * @return boolean whether the image represents a cubemap
     */
    public boolean isCubeMap(){return _cubeMap;};

    /**
     * Whether or not the image is an array texture
     * @return boolean whether the image represents a texture array
     */
    public boolean isArray() { return _layers > 1; }

    /**
     * Whether or not the image is a volume (3D) image
     * @return boolean whether the image represents a volume
     */
    public boolean isVolume() { return _depth > 0; }

    /**
     * Set the API version to be targetted for image loading.
     * Images may be loaded differently for OpenGL ES and OpenGL.  This function
     * sends a hint to the loader which allows it to target the desired API level.
     * @param api the desired target API. Default is GL4 (highest-end features)
     */
    public static void setAPIVersion(NvGfxAPIVersion api){
        m_gfxAPIVersion = api;
    }

    /**
     * Gets the current API-level for targetting the loading of images
     * @return the current API level
     */
    public static NvGfxAPIVersion getAPIVersion(){
        return m_gfxAPIVersion;
    }

    /**
     * Enables or disables automatic expansion of DXT images to RGBA
     * @param expand expand true enables DXT-to-RGBA expansion.  False passes
     * DXT images through as-is
     */
    public static void setDXTExpansion(boolean expand) { m_expandDXT = expand; }

    /**
     * Gets the status of automatic DXT expansion
     * @return true if DXT images will be expanded, false if they will be passed through
     */
    public static boolean getDXTExpansion() { return m_expandDXT; }

    private interface ImageReader{
        boolean invoke(byte[] fileData, int offset, int length, NvImage i);
        boolean invoke(InputStream in, NvImage i) throws IOException;
        boolean invoke(String filename, NvImage i)throws IOException;
    }

    private interface ImageWriter{
        boolean invoke(OutputStream out, int size, NvImage i);
    }

    private interface FlipBlocks{
        void invoke(DXTColBlock[] blocks, int numBlocks);
    }

    private static final class FlipBlocksDxtc1 implements FlipBlocks{

        @Override
        public void invoke(DXTColBlock[] blocks, int numBlocks) {
            for(int i = 0; i < numBlocks;i++){
                Numeric.swap(blocks[i].row, 0, 3);
                Numeric.swap(blocks[i].row, 1, 2);
            }
        }
    }

    private static final class FlipBlocksDxtc3 implements FlipBlocks{

        @Override
        public void invoke(DXTColBlock[] blocks, int numBlocks) {
            DXT3AlphaBlock alphaBlock = new DXT3AlphaBlock();

            int index = 0;
            for (int i = 0; i < numBlocks; i++) {
                blocks[index].toAlphaBlock(alphaBlock);

                Numeric.swap(alphaBlock.row, 0, 3);
                Numeric.swap(alphaBlock.row, 1, 2);

                blocks[index].fromAlphaBlock(alphaBlock);

                index++;
                DXTColBlock curblock = blocks[index];

                Numeric.swap(curblock.row, 0, 3);
                Numeric.swap(curblock.row, 1, 2);

                index++;
            }
        }

    }

    private static final class FlipBlocksDxtc5 implements FlipBlocks{

        @Override
        public void invoke(DXTColBlock[] blocks, int numBlocks) {
            DXT5AlphaBlock alphaBlock = new DXT5AlphaBlock();

            int index = 0;
            for (int i = 0; i < numBlocks; i++) {
                blocks[index].toAlpha5Block(alphaBlock);

                flipDxt5Alpha(alphaBlock);

                blocks[index].fromAlpha5Block(alphaBlock);

                index++;
                DXTColBlock curblock = blocks[index];

                Numeric.swap(curblock.row, 0, 3);
                Numeric.swap(curblock.row, 1, 2);
                index++;
            }
        }

    }

    private static final class FlipBlocksBc4 implements FlipBlocks{

        @Override
        public void invoke(DXTColBlock[] blocks, int numBlocks) {
            DXT5AlphaBlock alphaBlock = new DXT5AlphaBlock();

            for(int i = 0; i < numBlocks; i++){
                blocks[i].toAlpha5Block(alphaBlock);
                flipDxt5Alpha(alphaBlock);
                blocks[i].fromAlpha5Block(alphaBlock);
            }
        }
    }

    private static final class FlipBlocksBc5 implements FlipBlocks{

        @Override
        public void invoke(DXTColBlock[] blocks, int numBlocks) {
            DXT5AlphaBlock alphaBlock = new DXT5AlphaBlock();

            int index = 0;
            for(int i = 0; i < numBlocks; i++){
                blocks[index].toAlpha5Block(alphaBlock);
                flipDxt5Alpha(alphaBlock);
                blocks[index].fromAlpha5Block(alphaBlock);

                index ++;

                blocks[index].toAlpha5Block(alphaBlock);
                flipDxt5Alpha(alphaBlock);
                blocks[index].fromAlpha5Block(alphaBlock);

                index ++;
            }
        }
    }

    // TODO 该方法需要严格测试
    private static void flipDxt5Alpha(DXT5AlphaBlock block) {
        byte[][] gBits = new byte[4][4];

        final int mask = 0x00000007; // bits = 00 00 01 11
        int bits = 0;
        int a = block.row[0] & 255;
        int b = block.row[1] & 255;
        int c = block.row[2] & 255;
        // TODO need valid
        bits = a | (b << 8) | (c << 16);

        gBits[0][0] = (byte)(bits & mask);
        bits >>= 3;
        gBits[0][1] = (byte)(bits & mask);
        bits >>= 3;
        gBits[0][2] = (byte)(bits & mask);
        bits >>= 3;
        gBits[0][3] = (byte)(bits & mask);
        bits >>= 3;
        gBits[1][0] = (byte)(bits & mask);
        bits >>= 3;
        gBits[1][1] = (byte)(bits & mask);
        bits >>= 3;
        gBits[1][2] = (byte)(bits & mask);
        bits >>= 3;
        gBits[1][3] = (byte)(bits & mask);

        bits = 0;
        a = block.row[3] & 255;
        b = block.row[4] & 255;
        c = block.row[5] & 255;
        bits = a | (b << 8) | (c << 16);

        gBits[2][0] = (byte)(bits & mask);
        bits >>= 3;
        gBits[2][1] = (byte)(bits & mask);
        bits >>= 3;
        gBits[2][2] = (byte)(bits & mask);
        bits >>= 3;
        gBits[2][3] = (byte)(bits & mask);
        bits >>= 3;
        gBits[3][0] = (byte)(bits & mask);
        bits >>= 3;
        gBits[3][1] = (byte)(bits & mask);
        bits >>= 3;
        gBits[3][2] = (byte)(bits & mask);
        bits >>= 3;
        gBits[3][3] = (byte)(bits & mask);

        // clear existing alpha bits
        Arrays.fill(block.row, (byte)0);

        int pBits = 0;
        pBits = pBits | (gBits[3][0] << 0);
        pBits = pBits | (gBits[3][1] << 3);
        pBits = pBits | (gBits[3][2] << 6);
        pBits = pBits | (gBits[3][3] << 9);

        pBits = pBits | (gBits[2][0] << 12);
        pBits = pBits | (gBits[2][1] << 15);
        pBits = pBits | (gBits[2][2] << 18);
        pBits = pBits | (gBits[2][3] << 21);

        block.row[0] = (byte) (pBits);
        block.row[1] = (byte) (pBits >> 8);
        block.row[2] = (byte) (pBits >> 16);
        block.row[3] = (byte) (pBits >> 24);

        pBits = block.row[3] & 255;  // TODO need valid

        pBits = pBits | (gBits[1][0] << 0);
        pBits = pBits | (gBits[1][1] << 3);
        pBits = pBits | (gBits[1][2] << 6);
        pBits = pBits | (gBits[1][3] << 9);

        pBits = pBits | (gBits[0][0] << 12);
        pBits = pBits | (gBits[0][1] << 15);
        pBits = pBits | (gBits[0][2] << 18);
        pBits = pBits | (gBits[0][3] << 21);

        block.row[3] = (byte) (pBits);
        block.row[4] = (byte) (pBits >> 8);
        block.row[5] = (byte) (pBits >> 16);
    }

    private static final class ReadDDS implements ImageReader{

        @Override
        public boolean invoke(byte[] fileData, int offset, int length, NvImage i) {
            try {
                return invoke(new ByteArrayInputStream(fileData), i);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return false;
        }

        @Override
        public boolean invoke(String filename, NvImage i) throws IOException{
            return invoke(new BufferedInputStream(FileUtils.open(filename)), i);
        }

        @Override
        public boolean invoke(InputStream in, NvImage i) throws IOException{
            DataInputStream fp = new DataInputStream(in);

            try {
                // read in file marker, make sure its a DDS file
                byte[] filecode = new byte[4];
                fp.read(filecode);
                if (!new String(filecode).equals("DDS ")) {
                    fp.close();
                    System.err.println("This is not a DDS file!");
                    return false;
                }

                // read in DDS header
                DDS_HEADER ddsh = new DDS_HEADER();
                DDS_HEADER_10 ddsh10 = new DDS_HEADER_10();

                ddsh.read(fp);

                // check if image is a volume texture
                if(((ddsh.dwCaps2 & DDSF_VOLUME)!=0) && (ddsh.dwDepth > 0))
                    i._depth = ddsh.dwDepth;
                else
                    i._depth = 0;

                if (((ddsh.ddspf.dwFlags & DDSF_FOURCC)!=0) && (ddsh.ddspf.dwFourCC == FOURCC_DX10)) {
                    //This DDS file uses the DX10 header extension
                    //fread(&ddsh10, sizeof(DDS_HEADER_10), 1, fp);
//			        fp.Read( sizeof(DDS_HEADER_10), &ddsh10);
                    ddsh10.read(fp);
                }

                // There are flags that are supposed to mark these fields as valid, but some dds files don't set them properly
                i._width = ddsh.dwWidth;
                i._height = ddsh.dwHeight;

                if ((ddsh.dwFlags & DDSF_MIPMAPCOUNT)!=0) {
                    i._levelCount = ddsh.dwMipMapCount;
                }
                else
                    i._levelCount = 1;

                //check cube-map faces, the DX10 parser will override this
                if ( (ddsh.dwCaps2 & DDSF_CUBEMAP)!=0 && !((ddsh.ddspf.dwFlags & DDSF_FOURCC) != 0&& ddsh.ddspf.dwFourCC == FOURCC_DX10)) {
                    //this is a cubemap, count the faces
                    i._layers = 0;
                    i._layers += (ddsh.dwCaps2 & DDSF_CUBEMAP_POSITIVEX) != 0 ? 1 : 0;
                    i._layers += (ddsh.dwCaps2 & DDSF_CUBEMAP_NEGATIVEX) != 0 ? 1 : 0;
                    i._layers += (ddsh.dwCaps2 & DDSF_CUBEMAP_POSITIVEY) != 0 ? 1 : 0;
                    i._layers += (ddsh.dwCaps2 & DDSF_CUBEMAP_NEGATIVEY) != 0 ? 1 : 0;
                    i._layers += (ddsh.dwCaps2 & DDSF_CUBEMAP_POSITIVEZ) != 0 ? 1 : 0;
                    i._layers += (ddsh.dwCaps2 & DDSF_CUBEMAP_NEGATIVEZ) != 0 ? 1 : 0;

                    //check for a complete cubemap
                    if ( (i._layers != 6) || (i._width != i._height) ) {
                        //fclose(fp);
                        fp.close();
                        return false;
                    }

                    i._cubeMap = true;
                }
                else {
                    //not a cubemap
                    i._layers = 1;
                    i._cubeMap = false;
                }

                boolean btcCompressed = false;
                int bytesPerElement = 0;

                // figure out what the image format is
                if ((ddsh.ddspf.dwFlags & DDSF_FOURCC) !=0 )
                {
                    switch(ddsh.ddspf.dwFourCC)
                    {
                        case FOURCC_DXT1:
                            i._format = GL_COMPRESSED_RGBA_S3TC_DXT1_EXT;
                            i._internalFormat = GL_COMPRESSED_RGBA_S3TC_DXT1_EXT;
                            i._type = GL_COMPRESSED_RGBA_S3TC_DXT1_EXT;
                            bytesPerElement = 8;
                            btcCompressed = true;
                            break;

                        case FOURCC_DXT2:
                        case FOURCC_DXT3:
                            i._format = GL_COMPRESSED_RGBA_S3TC_DXT3_EXT;
                            i._internalFormat = GL_COMPRESSED_RGBA_S3TC_DXT3_EXT;
                            i._type = GL_COMPRESSED_RGBA_S3TC_DXT3_EXT;
                            bytesPerElement = 16;
                            btcCompressed = true;
                            break;

                        case FOURCC_DXT4:
                        case FOURCC_DXT5:
                            i._format = GL_COMPRESSED_RGBA_S3TC_DXT5_EXT;
                            i._internalFormat = GL_COMPRESSED_RGBA_S3TC_DXT5_EXT;
                            i._type = GL_COMPRESSED_RGBA_S3TC_DXT5_EXT;
                            bytesPerElement = 16;
                            btcCompressed = true;
                            break;

                        case FOURCC_ATI1:
                            i._format = GL_COMPRESSED_RED_RGTC1;
                            i._internalFormat = GL_COMPRESSED_RED_RGTC1;
                            i._type = GL_COMPRESSED_RED_RGTC1;
                            bytesPerElement = 8;
                            btcCompressed = true;
                            break;

                        case FOURCC_BC4U:
                            i._format = GL_COMPRESSED_RED_RGTC1;
                            i._internalFormat = GL_COMPRESSED_RED_RGTC1;
                            i._type = GL_COMPRESSED_RED_RGTC1;
                            bytesPerElement = 8;
                            btcCompressed = true;
                            break;

                        case FOURCC_BC4S:
                            i._format = GL_COMPRESSED_SIGNED_RED_RGTC1;
                            i._internalFormat = GL_COMPRESSED_SIGNED_RED_RGTC1;
                            i._type = GL_COMPRESSED_SIGNED_RED_RGTC1;
                            bytesPerElement = 8;
                            btcCompressed = true;
                            break;

                        case FOURCC_ATI2:
                            i._format = GL_COMPRESSED_RG_RGTC2; //GL_COMPRESSED_LUMINANCE_ALPHA_LATC2_EXT;
                            i._internalFormat = GL_COMPRESSED_RG_RGTC2; //GL_COMPRESSED_LUMINANCE_ALPHA_LATC2_EXT;
                            i._type = GL_COMPRESSED_RG_RGTC2; //GL_COMPRESSED_LUMINANCE_ALPHA_LATC2_EXT;
                            bytesPerElement = 16;
                            btcCompressed = true;
                            break;

                        case FOURCC_BC5S:
                            i._format = GL_COMPRESSED_SIGNED_RG_RGTC2; //GL_COMPRESSED_LUMINANCE_ALPHA_LATC2_EXT;
                            i._internalFormat = GL_COMPRESSED_SIGNED_RG_RGTC2; //GL_COMPRESSED_LUMINANCE_ALPHA_LATC2_EXT;
                            i._type = GL_COMPRESSED_SIGNED_RG_RGTC2; //GL_COMPRESSED_LUMINANCE_ALPHA_LATC2_EXT;
                            bytesPerElement = 16;
                            btcCompressed = true;
                            break;

                        case FOURCC_R8G8B8:
                            i._format = GL_BGR;
                            i._internalFormat = GL_RGB8;
                            i._type = GL_UNSIGNED_BYTE;
                            bytesPerElement = 3;
                            break;

                        case FOURCC_A8R8G8B8:
                            i._format = GL_BGRA;
                            i._internalFormat = GL_RGBA8;
                            i._type = GL_UNSIGNED_BYTE;
                            bytesPerElement = 4;
                            break;

                        case FOURCC_X8R8G8B8:
                            i._format = GL_BGRA;
                            i._internalFormat = GL_RGB8;
                            i._type = GL_UNSIGNED_INT_8_8_8_8;
                            bytesPerElement = 4;
                            break;

                        case FOURCC_R5G6B5:
                            i._format = GL_BGR;
                            i._internalFormat = GL_RGB5;
                            i._type = GL_UNSIGNED_SHORT_5_6_5;
                            bytesPerElement = 2;
                            break;
                        case FOURCC_A8:
                            i._format = GL_ALPHA;
                            i._internalFormat = GL_ALPHA8;
                            i._type = GL_UNSIGNED_BYTE;
                            bytesPerElement = 1;
                            break;

                        case FOURCC_A2B10G10R10:
                            i._format = GL_RGBA;
                            i._internalFormat = GL_RGB10_A2;
                            i._type = GL_UNSIGNED_INT_10_10_10_2;
                            bytesPerElement = 4;
                            break;

                        case FOURCC_A8B8G8R8:
                            i._format = GL_RGBA;
                            i._internalFormat = GL_RGBA8;
                            i._type = GL_UNSIGNED_BYTE;
                            bytesPerElement = 4;
                            break;

                        case FOURCC_X8B8G8R8:
                            i._format = GL_RGBA;
                            i._internalFormat = GL_RGB8;
                            i._type = GL_UNSIGNED_INT_8_8_8_8;
                            bytesPerElement = 4;
                            break;

                        case FOURCC_A2R10G10B10:
                            i._format = GL_BGRA;
                            i._internalFormat = GL_RGB10_A2;
                            i._type = GL_UNSIGNED_INT_10_10_10_2;
                            bytesPerElement = 4;
                            break;

                        case FOURCC_G16R16:
                            i._format = GL_RG;
                            i._internalFormat = GL_RG16;
                            i._type = GL_UNSIGNED_SHORT;
                            bytesPerElement = 4;
                            break;

                        case FOURCC_A16B16G16R16:
                            i._format = GL_RGBA;
                            i._internalFormat = GL_RGBA16;
                            i._type = GL_UNSIGNED_SHORT;
                            bytesPerElement = 8;
                            break;

                        case FOURCC_L8:
                            i._format = GL_LUMINANCE;
                            i._internalFormat = GL_LUMINANCE8;
                            i._type = GL_UNSIGNED_BYTE;
                            bytesPerElement = 1;
                            break;

                        case FOURCC_A8L8:
                            i._format = GL_LUMINANCE_ALPHA;
                            i._internalFormat = GL_LUMINANCE8_ALPHA8;
                            i._type = GL_UNSIGNED_BYTE;
                            bytesPerElement = 2;
                            break;

                        case FOURCC_L16:
                            i._format = GL_LUMINANCE;
                            i._internalFormat = GL_LUMINANCE16;
                            i._type = GL_UNSIGNED_SHORT;
                            bytesPerElement = 2;
                            break;

                        case FOURCC_Q16W16V16U16:
                            i._format = GL_RGBA;
                            i._internalFormat = GL_RGBA16_SNORM;
                            i._type = GL_SHORT;
                            bytesPerElement = 8;
                            break;

                        case FOURCC_R16F:
                            i._format = GL_RED;
                            i._internalFormat = GL_R16F;
                            i._type = GL_HALF_FLOAT_ARB;
                            bytesPerElement = 2;
                            break;

                        case FOURCC_G16R16F:
                            i._format = GL_RG;
                            i._internalFormat = GL_RG16F;
                            i._type = GL_HALF_FLOAT_ARB;
                            bytesPerElement = 4;
                            break;

                        case FOURCC_A16B16G16R16F:
                            i._format = GL_RGBA;
                            i._internalFormat = GL_RGBA16F;
                            i._type = GL_HALF_FLOAT_ARB;
                            bytesPerElement = 8;
                            break;

                        case FOURCC_R32F:
                            i._format = GL_RED;
                            i._internalFormat = GL_R32F;
                            i._type = GL_FLOAT;
                            bytesPerElement = 4;
                            break;

                        case FOURCC_G32R32F:
                            i._format = GL_RG;
                            i._internalFormat = GL_RG32F;
                            i._type = GL_FLOAT;
                            bytesPerElement = 8;
                            break;

                        case FOURCC_A32B32G32R32F:
                            i._format = GL_RGBA;
                            i._internalFormat = GL_RGBA32F;
                            i._type = GL_FLOAT;
                            bytesPerElement = 16;
                            break;

                        case FOURCC_DX10:
                            TempData p = new TempData();
                            if (!translateDX10Format(ddsh10, i, p)) {
                                //fclose(fp);
                                fp.close();
                                return false; //translation from DX10 failed
                            }

                            bytesPerElement = p.bytesPerElement;
                            btcCompressed = p.btcCompressed;
                            break;

                        case FOURCC_UNKNOWN:
                        case FOURCC_X1R5G5B5:
                        case FOURCC_A1R5G5B5:
                        case FOURCC_A4R4G4B4:
                        case FOURCC_R3G3B2:
                        case FOURCC_A8R3G3B2:
                        case FOURCC_X4R4G4B4:
                        case FOURCC_A4L4:
                        case FOURCC_D16_LOCKABLE:
                        case FOURCC_D32:
                        case FOURCC_D24X8:
                        case FOURCC_D16:
                        case FOURCC_D32F_LOCKABLE:
                            //these are unsupported for now
                        default:
                            //fclose(fp);
                            fp.close();
                            return false;
                    }
                }
                else if (ddsh.ddspf.dwFlags == DDSF_RGBA && ddsh.ddspf.dwRGBBitCount == 32)
                {
                    if ( ddsh.ddspf.dwRBitMask == 0xff && ddsh.ddspf.dwGBitMask == 0xff00 && ddsh.ddspf.dwBBitMask == 0xff0000 && ddsh.ddspf.dwABitMask == 0xff000000 ) {
                        //RGBA8 order
                        i._format = GL_RGBA;
                        i._internalFormat = GL_RGBA8;
                        i._type = GL_UNSIGNED_BYTE;
                    }
                    else if ( ddsh.ddspf.dwRBitMask == 0xff0000 && ddsh.ddspf.dwGBitMask == 0xff00 && ddsh.ddspf.dwBBitMask == 0xff && ddsh.ddspf.dwABitMask == 0xff000000 ) {
                        //BGRA8 order
                        i._format = GL_BGRA;
                        i._internalFormat = GL_RGBA8;
                        i._type = GL_UNSIGNED_BYTE;
                    }
                    else if ( ddsh.ddspf.dwRBitMask == 0x3ff00000 && ddsh.ddspf.dwGBitMask == 0xffc00 && ddsh.ddspf.dwBBitMask == 0x3ff && ddsh.ddspf.dwABitMask == 0xc0000000 ) {
                        //BGR10_A2 order
                        i._format = GL_RGBA;
                        i._internalFormat = GL_RGB10_A2;
                        i._type = GL_UNSIGNED_INT_2_10_10_10_REV; //GL_UNSIGNED_INT_10_10_10_2;
                    }
                    else if ( ddsh.ddspf.dwRBitMask == 0x3ff && ddsh.ddspf.dwGBitMask == 0xffc00 && ddsh.ddspf.dwBBitMask == 0x3ff00000 && ddsh.ddspf.dwABitMask == 0xc0000000 ) {
                        //RGB10_A2 order
                        i._format = GL_RGBA;
                        i._internalFormat = GL_RGB10_A2;
                        i._type = GL_UNSIGNED_INT_10_10_10_2;
                    }
                    else {
                        //we'll just guess BGRA8, because that is the common legacy format for improperly labeled files
                        i._format = GL_BGRA;
                        i._internalFormat = GL_RGBA8;
                        i._type = GL_UNSIGNED_BYTE;
                    }
                    bytesPerElement = 4;
                }
                else if (ddsh.ddspf.dwFlags == DDSF_RGB  && ddsh.ddspf.dwRGBBitCount == 32)
                {
                    if ( ddsh.ddspf.dwRBitMask == 0xffff && ddsh.ddspf.dwGBitMask == 0xffff0000 && ddsh.ddspf.dwBBitMask == 0x00 && ddsh.ddspf.dwABitMask == 0x00 ) {
                        i._format = GL_RG;
                        i._internalFormat = GL_RG16;
                        i._type = GL_UNSIGNED_SHORT;
                    }
                    else if ( ddsh.ddspf.dwRBitMask == 0xff && ddsh.ddspf.dwGBitMask == 0xff00 && ddsh.ddspf.dwBBitMask == 0xff0000 && ddsh.ddspf.dwABitMask == 0x00 ) {
                        i._format = GL_RGBA;  // TODO GL_RGB ??
                        i._internalFormat = GL_RGBA8;
                        i._type = GL_UNSIGNED_INT_8_8_8_8;
                    }
                    else if ( ddsh.ddspf.dwRBitMask == 0xff0000 && ddsh.ddspf.dwGBitMask == 0xff00 && ddsh.ddspf.dwBBitMask == 0xff && ddsh.ddspf.dwABitMask == 0x00 ) {
                        i._format = GL_BGRA;  // TODO GL_GBR ??
                        i._internalFormat = GL_RGBA8;
                        i._type = GL_UNSIGNED_INT_8_8_8_8;
                    }
                    else {
                        // probably a poorly labeled file with BGRX semantics
                        i._format = GL_BGRA;   // TODO GL_BGR
                        i._internalFormat = GL_RGBA8;
                        i._type = GL_UNSIGNED_INT_8_8_8_8;
                    }
                    bytesPerElement = 4;
                }
                else if (ddsh.ddspf.dwFlags == DDSF_RGB  && ddsh.ddspf.dwRGBBitCount == 24)
                {
                    i._format = GL_BGR;
                    i._internalFormat = GL_RGB8;
                    i._type = GL_UNSIGNED_BYTE;
                    bytesPerElement = 3;
                }
                // these cases revived from NVHHDDS...
                else if ((ddsh.ddspf.dwRGBBitCount == 16) &&
                        (ddsh.ddspf.dwRBitMask == 0x0000F800) &&
                        (ddsh.ddspf.dwGBitMask == 0x000007E0) &&
                        (ddsh.ddspf.dwBBitMask == 0x0000001F) &&
                        (ddsh.ddspf.dwABitMask == 0x00000000))
                {
                    // We support D3D's R5G6B5, which is actually RGB in linear
                    // memory.  It is equivalent to GL's GL_UNSIGNED_SHORT_5_6_5
                    i._format = GL_BGR;
                    i._internalFormat = GL_RGB5;
                    i._type = GL_UNSIGNED_SHORT_5_6_5;
                    bytesPerElement = 2;
                }
                else if ((ddsh.ddspf.dwRGBBitCount == 8) &&
                        (ddsh.ddspf.dwRBitMask == 0x00000000) &&
                        (ddsh.ddspf.dwGBitMask == 0x00000000) &&
                        (ddsh.ddspf.dwBBitMask == 0x00000000) &&
                        (ddsh.ddspf.dwABitMask == 0x000000FF))
                {
                    // We support D3D's A8
                    i._format = GL_ALPHA;
                    i._internalFormat = GL_ALPHA8;
                    i._type = GL_UNSIGNED_BYTE;
                    bytesPerElement = 1;
                }
                else if ((ddsh.ddspf.dwRGBBitCount == 8) &&
                        (ddsh.ddspf.dwRBitMask == 0x000000FF) &&
                        (ddsh.ddspf.dwGBitMask == 0x00000000) &&
                        (ddsh.ddspf.dwBBitMask == 0x00000000) &&
                        (ddsh.ddspf.dwABitMask == 0x00000000))
                {
                    // We support D3D's L8 (flagged as 8 bits of red only)
                    i._format = GL_LUMINANCE;
                    i._internalFormat = GL_LUMINANCE8;
                    i._type = GL_UNSIGNED_BYTE;
                    bytesPerElement = 1;
                }
                else if ((ddsh.ddspf.dwRGBBitCount == 16) &&
                        (((ddsh.ddspf.dwRBitMask == 0x000000FF) &&
                                (ddsh.ddspf.dwGBitMask == 0x00000000) &&
                                (ddsh.ddspf.dwBBitMask == 0x00000000) &&
                                (ddsh.ddspf.dwABitMask == 0x0000FF00)) ||
                                ((ddsh.ddspf.dwRBitMask == 0x000000FF) && // GIMP header for L8A8
                                        (ddsh.ddspf.dwGBitMask == 0x000000FF) &&  // Ugh
                                        (ddsh.ddspf.dwBBitMask == 0x000000FF) &&
                                        (ddsh.ddspf.dwABitMask == 0x0000FF00)))
                        )
                {
                    // We support D3D's A8L8 (flagged as 8 bits of red and 8 bits of alpha)
                    i._format = GL_LUMINANCE_ALPHA;
                    i._internalFormat = GL_LUMINANCE8_ALPHA8;
                    i._type = GL_UNSIGNED_BYTE;
                    bytesPerElement = 2;
                }
                // else fall back to L8 generic handling if capable.
                else if (ddsh.ddspf.dwRGBBitCount == 8)
                {
                    i._format = GL_LUMINANCE;
                    i._internalFormat = GL_LUMINANCE8;
                    i._type = GL_UNSIGNED_BYTE;
                    bytesPerElement = 1;
                }
                // else, we can't decode this file... :-(
                else
                {
                    LogUtil.i(LogUtil.LogType.NV_FRAMEWROK, "! Error decoding DDS file.");
                    //fclose(fp);
                    fp.close();
                    return false;
                }

                i._elementSize = bytesPerElement;

                i._data.clear();

                final NvGfxAPIVersion api = getAPIVersion();

                boolean isES = (api == NvGfxAPIVersion.GLES2 || api ==NvGfxAPIVersion.GLES3_0 || api == NvGfxAPIVersion.GLES3_1);
                boolean mustExpandDXT = m_expandDXT &&
                        ((i._format == GL_COMPRESSED_RGBA_S3TC_DXT1_EXT) ||
                                (i._format == GL_COMPRESSED_RGBA_S3TC_DXT3_EXT) ||
                                (i._format == GL_COMPRESSED_RGBA_S3TC_DXT5_EXT));

                for (int face = 0; face < i._layers; face++) {
                    int w = i._width, h = i._height, d = (i._depth > 0) ? i._depth : 1;
                    for (int level = 0; level < i._levelCount; level++) {
                        int bw = (btcCompressed) ? (w+3)/4 : w;
                        int bh = (btcCompressed) ? (h+3)/4 : h;
                        int size = bw*bh*d*bytesPerElement;

                        byte[] pixels = new byte[size];

                        //fread( data, size, 1, fp);
                        fp.read(pixels);

                        if (upperLeftOrigin && !i._cubeMap)
                            i.flipSurface( pixels, w, h, d);

                        if (isES)
                            i.componentSwapSurface(pixels, w, h, d);

                        // do we need to expand DXT?
                        if (mustExpandDXT) {
                            byte[] expandedPixels = i.expandDXT(pixels, w, h, d);
                            pixels = expandedPixels;
                        }

                        i._data.add(pixels);

                        //reduce mip sizes
                        w = ( w > 1) ? w >> 1 : 1;
                        h = ( h > 1) ? h >> 1 : 1;
                        d = ( d > 1) ? d >> 1 : 1;
                    }
                }

                if (mustExpandDXT) {
                    i._format = GL_RGBA;
                    i._type = GL_UNSIGNED_BYTE;
                }
                fp.close();
                return true;
            } catch (IOException e) {
                throw e;
            }
        }

    }

    // 该方法由问题
    protected byte[] expandDXT(byte[] surf, int width, int height, int depth){
        if(depth == 0) depth = 1;

        int index = 0;
        int[] dest = new int[width * height * depth];
        int plane = 0;

        int bh = (height + 3) / 4;
        int bw = (width + 3) / 4;

        ColorBlock color = new ColorBlock();

        if (_format == GL_COMPRESSED_RGBA_S3TC_DXT1_EXT) {
            BlockDXT1 block = new BlockDXT1();
            for (int k = 0; k < depth; k++) {

                for (int j = 0; j < bh; j++) {
                    int yBlockSize = Math.min(4, height - 4 * j);

                    for (int i = 0; i < bw; i++) {
                        int xBlockSize = Math.min(4, width - 4 * i);
//	                    nv::BlockDXT1* block = (nv::BlockDXT1*)surf;
//	                    nv::ColorBlock color;

                        index = block.load(surf, index);
                        block.decodeBlock(color);

                        // Write color block.
                        for (int y = 0; y < yBlockSize; y++) {
                            for (int x = 0; x < xBlockSize; x++) {
//	                            plane[4*i+x + (4*j+y)*width] = (uint32_t)color.color(x, y);
                                dest[plane + 4*i+x + (4*j+y)*width] = color.get(x, y);
                            }
                        }

//	                    surf += sizeof(nv::BlockDXT1); // 64bits
                    }
                }

                plane += width * height;
            }
        } else if (_format == GL_COMPRESSED_RGBA_S3TC_DXT3_EXT) {
            BlockDXT3 block = new BlockDXT3();
            for (int k = 0; k < depth; k++) {

                for (int j = 0; j < bh; j++) {
                    int yBlockSize = Math.min(4, height - 4 * j);

                    for (int i = 0; i < bw; i++) {
                        int xBlockSize = Math.min(4, width - 4 * i);
//	                    nv::BlockDXT3* block = (nv::BlockDXT3*)surf;
//	                    nv::ColorBlock color;

                        index = block.load(surf, index);
                        block.decodeBlock(color);

                        // Write color block.
                        for (int y = 0; y < yBlockSize; y++) {
                            for (int x = 0; x < xBlockSize; x++) {
//	                            plane[4*i+x + (4*j+y)*width] = (uint32_t)color.color(x, y);
                                dest[plane + 4*i+x + (4*j+y)*width] = color.get(x, y);
                            }
                        }

//	                    surf += sizeof(nv::BlockDXT3); // 64bits
                    }
                }

                plane += width * height;
            }
        } else {
            BlockDXT5 block = new BlockDXT5();
            for (int k = 0; k < depth; k++) {

                for (int j = 0; j < bh; j++) {
                    int yBlockSize = Math.min(4, height - 4 * j);

                    for (int i = 0; i < bw; i++) {
                        int xBlockSize = Math.min(4, width - 4 * i);
//	                    nv::BlockDXT5* block = (nv::BlockDXT5*)surf;
//	                    nv::ColorBlock color;
                        index = block.load(surf, index);
                        block.decodeBlock(color);

                        // Write color block.
                        for (int y = 0; y < yBlockSize; y++) {
                            for (int x = 0; x < xBlockSize; x++) {
//	                            plane[4*i+x + (4*j+y)*width] = (uint32_t)color.color(x, y);
                                dest[plane + 4*i+x + (4*j+y)*width] = color.get(x, y);
                            }
                        }

//	                    surf += sizeof(nv::BlockDXT5); // 64bits
                    }
                }

                plane += width * height;
            }
        }

        byte[] dst = new byte[dest.length * 4];
        Numeric.toBytes(dest, 0, dst, 0, dest.length);
        return dst;
    }

    protected void componentSwapSurface(byte[] surf, int width, int height, int depth){
        if(depth == 0) depth = 1;

        if (_type != GL_UNSIGNED_BYTE)
            return;
        if (isCompressed())
            return;

        int i = 0;

        if (_format == GL_BGR) {
            for ( int ii = 0; ii < depth; ii++) {
                for ( int jj = 0; jj < height; jj++) {
                    for ( int kk = 0; kk < width; kk++) {
                        byte tmp = surf[0 + i];
                        surf[0 + i] = surf[2 + i];
                        surf[2 + i] = tmp;
                        i += 3;
                    }
                }
            }
            _format = GL_RGB;
        } else if (_format == GL_BGRA) {
            for ( int ii = 0; ii < depth; ii++) {
                for ( int jj = 0; jj < height; jj++) {
                    for ( int kk = 0; kk < width; kk++) {
                        byte tmp = surf[0 + i];
                        surf[0 + i] = surf[2 + i];
                        surf[2 + i] = tmp;
                        i += 4;
                    }
                }
            }
            _format = GL_RGBA;
        }
    }

    private static boolean translateDX10Format(DDS_HEADER_10 header, NvImage i, TempData p){
        Logger logger = LogUtil.getNVFrameworkLogger();
        if(logger.isLoggable(Level.INFO)){
            logger.info( "translating DX10 Format\n");
            logger.info( String.format("  header.dxgiFormat = %x\n", header.dxgiFormat));
            logger.info( String.format("  header.resourceDimension = %x\n", header.resourceDimension));
            logger.info( String.format("  header.arraySize = %x\n", header.arraySize));
            logger.info( String.format("  header.miscFlag = %x\n", header.miscFlag));
        }

        switch (header.resourceDimension) {
            case DDS10_RESOURCE_DIMENSION_TEXTURE1D:
            case DDS10_RESOURCE_DIMENSION_TEXTURE2D:
            case DDS10_RESOURCE_DIMENSION_TEXTURE3D:
                //do I really need to do anything here ?
                break;
            case DDS10_RESOURCE_DIMENSION_UNKNOWN:
            case DDS10_RESOURCE_DIMENSION_BUFFER:
            default:
                // these are presently unsupported formats
                logger.severe( "Bad resource dimension\n");
                return false;
        }

        switch (header.dxgiFormat) {
            case DDS10_FORMAT_R32G32B32A32_FLOAT:
                set_type_info( GL_RGBA32F, GL_RGBA, GL_FLOAT, 16, i, p);
                break;

            case DDS10_FORMAT_R32G32B32A32_UINT:
                set_type_info( GL_RGBA32UI, GL_RGBA_INTEGER, GL_UNSIGNED_INT, 16, i, p);
                break;

            case DDS10_FORMAT_R32G32B32A32_SINT:
                set_type_info( GL_RGBA32I, GL_RGBA_INTEGER, GL_INT, 16, i, p);
                break;

            case DDS10_FORMAT_R32G32B32_FLOAT:
                set_type_info( GL_RGBA32F, GL_RGB, GL_FLOAT, 12, i, p);
                break;

            case DDS10_FORMAT_R32G32B32_UINT:
                set_type_info( GL_RGB32UI, GL_RGB_INTEGER, GL_UNSIGNED_INT, 12, i, p);
                break;

            case DDS10_FORMAT_R32G32B32_SINT:
                set_type_info( GL_RGB32I, GL_RGB_INTEGER, GL_INT, 12, i, p);
                break;

            case DDS10_FORMAT_R16G16B16A16_FLOAT:
                set_type_info( GL_RGBA16F, GL_RGBA, GL_HALF_FLOAT, 8, i, p);
                break;

            case DDS10_FORMAT_R16G16B16A16_UNORM:
                set_type_info( GL_RGBA16, GL_RGBA, GL_UNSIGNED_SHORT, 8, i, p);
                break;

            case DDS10_FORMAT_R16G16B16A16_UINT:
                set_type_info( GL_RGBA16UI, GL_RGBA_INTEGER, GL_UNSIGNED_SHORT, 8, i, p);
                break;

            case DDS10_FORMAT_R16G16B16A16_SNORM:
                set_type_info( GL_RGBA16_SNORM, GL_RGBA, GL_SHORT, 8, i, p);
                break;

            case DDS10_FORMAT_R16G16B16A16_SINT:
                set_type_info( GL_RGBA16I, GL_RGBA_INTEGER, GL_SHORT, 8, i, p);
                break;

            case DDS10_FORMAT_R32G32_FLOAT:
                set_type_info( GL_RG32F, GL_RG, GL_FLOAT, 8, i, p);
                break;

            case DDS10_FORMAT_R32G32_UINT:
                set_type_info( GL_RG32UI, GL_RG_INTEGER, GL_UNSIGNED_INT, 8, i, p);
                break;

            case DDS10_FORMAT_R32G32_SINT:
                set_type_info( GL_RG32I, GL_RG_INTEGER, GL_INT, 8, i, p);
                break;

            case DDS10_FORMAT_R32G8X24_TYPELESS:
            case DDS10_FORMAT_D32_FLOAT_S8X24_UINT:
            case DDS10_FORMAT_R32_FLOAT_X8X24_TYPELESS:
            case DDS10_FORMAT_X32_TYPELESS_G8X24_UINT:
                //these formats have no real direct mapping to OpenGL
                // fail creation
                return false;

            case DDS10_FORMAT_R10G10B10A2_UNORM:
                set_type_info( GL_RGB10_A2, GL_RGBA, GL_UNSIGNED_INT_2_10_10_10_REV, 4, i, p); // is the rev version needed?
                break;

            case DDS10_FORMAT_R10G10B10A2_UINT:
                //doesn't exist in OpenGL
                return false;

            case DDS10_FORMAT_R11G11B10_FLOAT:
                set_type_info( GL_R11F_G11F_B10F_EXT, GL_RGB, GL_UNSIGNED_INT_10F_11F_11F_REV_EXT, 4, i, p);
                break;

            case DDS10_FORMAT_R8G8B8A8_UNORM:
                set_type_info( GL_RGBA8, GL_RGBA, GL_UNSIGNED_BYTE, 4, i, p);
                break;

            case DDS10_FORMAT_R8G8B8A8_UNORM_SRGB:
                set_type_info( GL_SRGB8_ALPHA8, GL_RGBA, GL_UNSIGNED_BYTE, 4, i, p);
                break;

            case DDS10_FORMAT_R8G8B8A8_UINT:
                set_type_info( GL_RGBA8UI, GL_RGBA_INTEGER, GL_UNSIGNED_BYTE, 4, i, p);
                break;

            case DDS10_FORMAT_R8G8B8A8_SNORM:
                set_type_info( GL_RGBA8_SNORM, GL_RGBA, GL_BYTE, 4, i, p);
                break;

            case DDS10_FORMAT_R8G8B8A8_SINT:
                set_type_info( GL_RGBA8UI, GL_RGBA_INTEGER, GL_BYTE, 4, i, p);
                break;

            case DDS10_FORMAT_R16G16_FLOAT:
                set_type_info( GL_RG16F, GL_RG, GL_HALF_FLOAT, 4, i, p);
                break;

            case DDS10_FORMAT_R16G16_UNORM:
                set_type_info( GL_RG16, GL_RG, GL_UNSIGNED_SHORT, 4, i, p);
                break;

            case DDS10_FORMAT_R16G16_UINT:
                set_type_info( GL_RG16UI, GL_RG_INTEGER, GL_UNSIGNED_SHORT, 4, i, p);
                break;

            case DDS10_FORMAT_R16G16_SNORM:
                set_type_info( GL_RG16_SNORM, GL_RG, GL_SHORT, 4, i, p);
                break;

            case DDS10_FORMAT_R16G16_SINT:
                set_type_info( GL_RG16I, GL_RG_INTEGER, GL_SHORT, 4, i, p);
                break;

            case DDS10_FORMAT_D32_FLOAT:
                set_type_info( GL_DEPTH_COMPONENT32F, GL_DEPTH, GL_FLOAT, 4, i, p);
                break;

            case DDS10_FORMAT_R32_FLOAT:
                set_type_info( GL_R32F, GL_RED, GL_FLOAT, 4, i, p);
                break;

            case DDS10_FORMAT_R32_UINT:
                set_type_info( GL_R32UI, GL_RED_INTEGER, GL_UNSIGNED_INT, 4, i, p);
                break;

            case DDS10_FORMAT_R32_SINT:
                set_type_info( GL_R32I, GL_RED_INTEGER, GL_INT, 4, i, p);
                break;

            //these seem a little problematic to deal with
            case DDS10_FORMAT_R24G8_TYPELESS:
            case DDS10_FORMAT_D24_UNORM_S8_UINT:
            case DDS10_FORMAT_R24_UNORM_X8_TYPELESS:
            case DDS10_FORMAT_X24_TYPELESS_G8_UINT:
                //OpenGL doesn't really offer a packed depth stencil textures
                return false;

            case DDS10_FORMAT_R8G8_UNORM:
                set_type_info( GL_RG8, GL_RG, GL_UNSIGNED_BYTE, 2, i, p);
                break;

            case DDS10_FORMAT_R8G8_UINT:
                set_type_info( GL_RG8UI, GL_RG_INTEGER, GL_UNSIGNED_BYTE, 2, i, p);
                break;

            case DDS10_FORMAT_R8G8_SNORM:
                set_type_info( GL_RG8_SNORM, GL_RG, GL_BYTE, 2, i, p);
                break;

            case DDS10_FORMAT_R8G8_SINT:
                set_type_info( GL_RG8I, GL_RG_INTEGER, GL_BYTE, 2, i, p);
                break;

            case DDS10_FORMAT_R16_FLOAT:
                set_type_info( GL_R16F, GL_RED, GL_HALF_FLOAT, 2, i, p);
                break;

            case DDS10_FORMAT_D16_UNORM:
                set_type_info( GL_DEPTH_COMPONENT16, GL_DEPTH, GL_UNSIGNED_SHORT, 2, i, p);
                break;

            case DDS10_FORMAT_R16_UNORM:
                set_type_info( GL_R16, GL_RED, GL_UNSIGNED_SHORT, 2, i, p);
                break;

            case DDS10_FORMAT_R16_UINT:
                set_type_info( GL_R16UI, GL_RED_INTEGER, GL_UNSIGNED_SHORT, 2, i, p);
                break;

            case DDS10_FORMAT_R16_SNORM:
                set_type_info( GL_R16_SNORM, GL_RED, GL_SHORT, 2, i, p);
                return false;

            case DDS10_FORMAT_R16_SINT:
                set_type_info( GL_R16I, GL_RED_INTEGER, GL_SHORT, 2, i, p);
                break;

            case DDS10_FORMAT_R8_UNORM:
                set_type_info( GL_R8, GL_RED, GL_UNSIGNED_BYTE, 1, i, p);
                break;

            case DDS10_FORMAT_R8_UINT:
                set_type_info( GL_R8UI, GL_RED_INTEGER, GL_UNSIGNED_BYTE, 1, i, p);
                break;

            case DDS10_FORMAT_R8_SNORM:
                set_type_info( GL_R8_SNORM, GL_RED, GL_BYTE, 1, i, p);
                break;

            case DDS10_FORMAT_R8_SINT:
                set_type_info( GL_R8I, GL_RED_INTEGER, GL_BYTE, 1, i, p);
                break;

            case DDS10_FORMAT_A8_UNORM:
                set_type_info( GL_ALPHA8, GL_ALPHA, GL_UNSIGNED_BYTE, 1, i, p);
                break;

            case DDS10_FORMAT_R9G9B9E5_SHAREDEXP:
                set_type_info( GL_RGB9_E5, GL_RGB, GL_UNSIGNED_INT_5_9_9_9_REV, 4, i, p);
                break;

            case DDS10_FORMAT_R8G8_B8G8_UNORM:
            case DDS10_FORMAT_G8R8_G8B8_UNORM:
                // unsure how to interpret these formats
                return false;

            case DDS10_FORMAT_BC1_UNORM:
                set_compressed_type_info( GL_COMPRESSED_RGBA_S3TC_DXT1_EXT, GL_COMPRESSED_RGBA_S3TC_DXT1_EXT, GL_UNSIGNED_BYTE, 8, i, p);
                break;

            case DDS10_FORMAT_BC1_UNORM_SRGB:
                set_compressed_type_info( GL_COMPRESSED_RGBA_S3TC_DXT1_EXT, GL_COMPRESSED_RGBA_S3TC_DXT1_EXT, GL_UNSIGNED_BYTE, 8, i, p);
                break;

            case DDS10_FORMAT_BC2_UNORM:
                set_compressed_type_info( GL_COMPRESSED_RGBA_S3TC_DXT3_EXT, GL_COMPRESSED_RGBA_S3TC_DXT3_EXT, GL_UNSIGNED_BYTE, 16, i, p);
                break;

            case DDS10_FORMAT_BC2_UNORM_SRGB:
                set_compressed_type_info( GL_COMPRESSED_RGBA_S3TC_DXT1_EXT, GL_COMPRESSED_RGBA_S3TC_DXT1_EXT, GL_UNSIGNED_BYTE, 16, i, p);
                break;

            case DDS10_FORMAT_BC3_UNORM:
                set_compressed_type_info( GL_COMPRESSED_RGBA_S3TC_DXT1_EXT, GL_COMPRESSED_RGBA_S3TC_DXT1_EXT, GL_UNSIGNED_BYTE, 16, i, p);
                break;

            case DDS10_FORMAT_BC3_UNORM_SRGB:
                set_compressed_type_info( GL_COMPRESSED_RGBA_S3TC_DXT1_EXT, GL_COMPRESSED_RGBA_S3TC_DXT1_EXT, GL_UNSIGNED_BYTE, 16, i, p);
                break;

            case DDS10_FORMAT_BC4_UNORM:
                set_compressed_type_info( GL_COMPRESSED_RED_RGTC1, GL_COMPRESSED_RED_RGTC1, GL_UNSIGNED_BYTE, 8, i, p);
                break;

            case DDS10_FORMAT_BC4_SNORM:
                set_compressed_type_info( GL_COMPRESSED_SIGNED_RED_RGTC1, GL_COMPRESSED_SIGNED_RED_RGTC1, GL_UNSIGNED_BYTE, 8, i, p);
                break;

            case DDS10_FORMAT_BC5_UNORM:
                set_compressed_type_info( GL_COMPRESSED_RG_RGTC2, GL_COMPRESSED_RG_RGTC2, GL_UNSIGNED_BYTE, 16, i, p);
                break;

            case DDS10_FORMAT_BC5_SNORM:
                set_compressed_type_info( GL_COMPRESSED_SIGNED_RG_RGTC2, GL_COMPRESSED_SIGNED_RG_RGTC2, GL_UNSIGNED_BYTE, 16, i, p);
                break;

            case DDS10_FORMAT_B5G6R5_UNORM:
                set_type_info( GL_RGB5, GL_BGR, GL_UNSIGNED_SHORT_5_6_5, 2, i, p);
                break;

            case DDS10_FORMAT_B5G5R5A1_UNORM:
                set_type_info( GL_RGB5_A1, GL_BGRA, GL_UNSIGNED_SHORT_5_5_5_1, 2, i, p);
                break;

            case DDS10_FORMAT_B8G8R8A8_UNORM:
                set_type_info( GL_RGBA8, GL_BGRA, GL_UNSIGNED_BYTE, 2, i, p);
                break;

            case DDS10_FORMAT_B8G8R8X8_UNORM:
                set_type_info( GL_RGB8, GL_BGRA, GL_UNSIGNED_BYTE, 4, i, p);
                break;

            case DDS10_FORMAT_B8G8R8A8_UNORM_SRGB:
                set_type_info( GL_SRGB8_ALPHA8, GL_BGRA, GL_UNSIGNED_BYTE, 4, i, p);
                break;

            case DDS10_FORMAT_B8G8R8X8_UNORM_SRGB:
                set_type_info( GL_SRGB8, GL_BGRA, GL_UNSIGNED_BYTE, 4, i, p);
                break;

            case DDS10_FORMAT_R32G32B32A32_TYPELESS:
            case DDS10_FORMAT_R32G32B32_TYPELESS:
            case DDS10_FORMAT_R16G16B16A16_TYPELESS:
            case DDS10_FORMAT_R32G32_TYPELESS:
            case DDS10_FORMAT_R10G10B10A2_TYPELESS:
            case DDS10_FORMAT_R8G8B8A8_TYPELESS:
            case DDS10_FORMAT_R16G16_TYPELESS:
            case DDS10_FORMAT_R32_TYPELESS:
            case DDS10_FORMAT_R8G8_TYPELESS:
            case DDS10_FORMAT_R16_TYPELESS:
            case DDS10_FORMAT_R8_TYPELESS:
            case DDS10_FORMAT_BC1_TYPELESS:
            case DDS10_FORMAT_BC3_TYPELESS:
            case DDS10_FORMAT_BC4_TYPELESS:
            case DDS10_FORMAT_BC2_TYPELESS:
            case DDS10_FORMAT_BC5_TYPELESS:
            case DDS10_FORMAT_B8G8R8A8_TYPELESS:
            case DDS10_FORMAT_B8G8R8X8_TYPELESS:
                //unclear what to do with typeless formats, leave them as unsupported for now
                // in the future it might make sense to use a default type, if these are common
                return false;

            case DDS10_FORMAT_R10G10B10_XR_BIAS_A2_UNORM:
            case DDS10_FORMAT_R1_UNORM:
            case DDS10_FORMAT_BC6H_TYPELESS:
            case DDS10_FORMAT_BC6H_UF16:
            case DDS10_FORMAT_BC6H_SF16:
            case DDS10_FORMAT_BC7_TYPELESS:
            case DDS10_FORMAT_BC7_UNORM:
            case DDS10_FORMAT_BC7_UNORM_SRGB:
                //these formats are unsupported presently
                return false;

            case (int) DDS10_FORMAT_FORCE_UINT:
            case DDS10_FORMAT_UNKNOWN:
            default:
                //these are errors
                return false;
        };

        i._layers = header.arraySize;
        i._cubeMap = (header.miscFlag & 0x4) != 0;
        return true;
    }

    // surface description flags
    private static final int DDSF_CAPS           = 0x00000001;
    private static final int DDSF_HEIGHT         = 0x00000002;
    private static final int DDSF_WIDTH          = 0x00000004;
    private static final int DDSF_PITCH          = 0x00000008;
    private static final int DDSF_PIXELFORMAT    = 0x00001000;
    private static final int DDSF_MIPMAPCOUNT    = 0x00020000;
    private static final int DDSF_LINEARSIZE     = 0x00080000;
    private static final int DDSF_DEPTH          = 0x00800000;

    // pixel format flags
    private static final int DDSF_ALPHAPIXELS    = 0x00000001;
    private static final int DDSF_FOURCC         = 0x00000004;
    private static final int DDSF_RGB            = 0x00000040;
    private static final int DDSF_RGBA           = 0x00000041;

    // dwCaps1 flags
    private static final int DDSF_COMPLEX         = 0x00000008;
    private static final int DDSF_TEXTURE         = 0x00001000;
    private static final int DDSF_MIPMAP          = 0x00400000;

    // dwCaps2 flags
    private static final int DDSF_CUBEMAP         = 0x00000200;
    private static final int DDSF_CUBEMAP_POSITIVEX  = 0x00000400;
    private static final int DDSF_CUBEMAP_NEGATIVEX  = 0x00000800;
    private static final int DDSF_CUBEMAP_POSITIVEY  = 0x00001000;
    private static final int DDSF_CUBEMAP_NEGATIVEY  = 0x00002000;
    private static final int DDSF_CUBEMAP_POSITIVEZ  = 0x00004000;
    private static final int DDSF_CUBEMAP_NEGATIVEZ  = 0x00008000;
    private static final int DDSF_CUBEMAP_ALL_FACES  = 0x0000FC00;
    private static final int DDSF_VOLUME          = 0x00200000;

    // compressed texture types
    private static final int FOURCC_UNKNOWN       = 0;

    private static final int FOURCC_R8G8B8        = 20;
    private static final int FOURCC_A8R8G8B8      = 21;
    private static final int FOURCC_X8R8G8B8      = 22;
    private static final int FOURCC_R5G6B5        = 23;
    private static final int FOURCC_X1R5G5B5      = 24;
    private static final int FOURCC_A1R5G5B5      = 25;
    private static final int FOURCC_A4R4G4B4      = 26;
    private static final int FOURCC_R3G3B2        = 27;
    private static final int FOURCC_A8            = 28;
    private static final int FOURCC_A8R3G3B2      = 29;
    private static final int FOURCC_X4R4G4B4      = 30;
    private static final int FOURCC_A2B10G10R10   = 31;
    private static final int FOURCC_A8B8G8R8      = 32;
    private static final int FOURCC_X8B8G8R8      = 33;
    private static final int FOURCC_G16R16        = 34;
    private static final int FOURCC_A2R10G10B10   = 35;
    private static final int FOURCC_A16B16G16R16  = 36;

    private static final int FOURCC_L8            = 50;
    private static final int FOURCC_A8L8          = 51;
    private static final int FOURCC_A4L4          = 52;
    private static final int FOURCC_DXT1          = 0x31545844; //(MAKEFOURCC('D','X','T','1'))
    private static final int FOURCC_DXT2          = 0x32545844; //(MAKEFOURCC('D','X','T','1'))
    private static final int FOURCC_DXT3          = 0x33545844; //(MAKEFOURCC('D','X','T','3'))
    private static final int FOURCC_DXT4          = 0x34545844; //(MAKEFOURCC('D','X','T','3'))
    private static final int FOURCC_DXT5          = 0x35545844; //(MAKEFOURCC('D','X','T','5'))
    private static final int FOURCC_ATI1          = 0x31495441; //NvUtils.makefourcc('A','T','I','1');
    private static final int FOURCC_ATI2          = 0x32495441; //NvUtils.makefourcc('A','T','I','2');
    private static final int FOURCC_BC4U          = 0x55344342; //NvUtils.makefourcc('B','C','4','U');
    private static final int FOURCC_BC4S          = 0x53344342; //NvUtils.makefourcc('B','C','4','S');
    private static final int FOURCC_BC5S          = 0x53354342; //NvUtils.makefourcc('B','C','5','S');

    private static final int FOURCC_D16_LOCKABLE  = 70;
    private static final int FOURCC_D32           = 71;
    private static final int FOURCC_D24X8         = 77;
    private static final int FOURCC_D16           = 80;

    private static final int FOURCC_D32F_LOCKABLE = 82;

    private static final int FOURCC_L16           = 81;

    private static final int FOURCC_DX10          = 0x30315844; //NvUtils.makefourcc('D','X','1','0');

    // signed normalized formats
    private static final int FOURCC_Q16W16V16U16  = 110;

    // Floating point surface formats

    // s10e5 formats (16-bits per channel)
    private static final int FOURCC_R16F          = 111;
    private static final int FOURCC_G16R16F       = 112;
    private static final int FOURCC_A16B16G16R16F = 113;

    // IEEE s23e8 formats (32-bits per channel)
    private static final int FOURCC_R32F          = 114;
    private static final int FOURCC_G32R32F       = 115;
    private static final int FOURCC_A32B32G32R32F = 116;

    //DXGI enums
    private static final int DDS10_FORMAT_UNKNOWN = 0;
    private static final int DDS10_FORMAT_R32G32B32A32_TYPELESS = 1;
    private static final int DDS10_FORMAT_R32G32B32A32_FLOAT = 2;
    private static final int DDS10_FORMAT_R32G32B32A32_UINT = 3;
    private static final int DDS10_FORMAT_R32G32B32A32_SINT = 4;
    private static final int DDS10_FORMAT_R32G32B32_TYPELESS = 5;
    private static final int DDS10_FORMAT_R32G32B32_FLOAT = 6;
    private static final int DDS10_FORMAT_R32G32B32_UINT = 7;
    private static final int DDS10_FORMAT_R32G32B32_SINT = 8;
    private static final int DDS10_FORMAT_R16G16B16A16_TYPELESS = 9;
    private static final int DDS10_FORMAT_R16G16B16A16_FLOAT = 10;
    private static final int DDS10_FORMAT_R16G16B16A16_UNORM = 11;
    private static final int DDS10_FORMAT_R16G16B16A16_UINT = 12;
    private static final int DDS10_FORMAT_R16G16B16A16_SNORM = 13;
    private static final int DDS10_FORMAT_R16G16B16A16_SINT = 14;
    private static final int DDS10_FORMAT_R32G32_TYPELESS = 15;
    private static final int DDS10_FORMAT_R32G32_FLOAT = 16;
    private static final int DDS10_FORMAT_R32G32_UINT = 17;
    private static final int DDS10_FORMAT_R32G32_SINT = 18;
    private static final int DDS10_FORMAT_R32G8X24_TYPELESS = 19;
    private static final int DDS10_FORMAT_D32_FLOAT_S8X24_UINT = 20;
    private static final int DDS10_FORMAT_R32_FLOAT_X8X24_TYPELESS = 21;
    private static final int DDS10_FORMAT_X32_TYPELESS_G8X24_UINT = 22;
    private static final int DDS10_FORMAT_R10G10B10A2_TYPELESS = 23;
    private static final int DDS10_FORMAT_R10G10B10A2_UNORM = 24;
    private static final int DDS10_FORMAT_R10G10B10A2_UINT = 25;
    private static final int DDS10_FORMAT_R11G11B10_FLOAT = 26;
    private static final int DDS10_FORMAT_R8G8B8A8_TYPELESS = 27;
    private static final int DDS10_FORMAT_R8G8B8A8_UNORM = 28;
    private static final int DDS10_FORMAT_R8G8B8A8_UNORM_SRGB = 29;
    private static final int DDS10_FORMAT_R8G8B8A8_UINT = 30;
    private static final int DDS10_FORMAT_R8G8B8A8_SNORM = 31;
    private static final int DDS10_FORMAT_R8G8B8A8_SINT = 32;
    private static final int DDS10_FORMAT_R16G16_TYPELESS = 33;
    private static final int DDS10_FORMAT_R16G16_FLOAT = 34;
    private static final int DDS10_FORMAT_R16G16_UNORM = 35;
    private static final int DDS10_FORMAT_R16G16_UINT = 36;
    private static final int DDS10_FORMAT_R16G16_SNORM = 37;
    private static final int DDS10_FORMAT_R16G16_SINT = 38;
    private static final int DDS10_FORMAT_R32_TYPELESS = 39;
    private static final int DDS10_FORMAT_D32_FLOAT = 40;
    private static final int DDS10_FORMAT_R32_FLOAT = 41;
    private static final int DDS10_FORMAT_R32_UINT = 42;
    private static final int DDS10_FORMAT_R32_SINT = 43;
    private static final int DDS10_FORMAT_R24G8_TYPELESS = 44;
    private static final int DDS10_FORMAT_D24_UNORM_S8_UINT = 45;
    private static final int DDS10_FORMAT_R24_UNORM_X8_TYPELESS = 46;
    private static final int DDS10_FORMAT_X24_TYPELESS_G8_UINT = 47;
    private static final int DDS10_FORMAT_R8G8_TYPELESS = 48;
    private static final int DDS10_FORMAT_R8G8_UNORM = 49;
    private static final int DDS10_FORMAT_R8G8_UINT = 50;
    private static final int DDS10_FORMAT_R8G8_SNORM = 51;
    private static final int DDS10_FORMAT_R8G8_SINT = 52;
    private static final int DDS10_FORMAT_R16_TYPELESS = 53;
    private static final int DDS10_FORMAT_R16_FLOAT = 54;
    private static final int DDS10_FORMAT_D16_UNORM = 55;
    private static final int DDS10_FORMAT_R16_UNORM = 56;
    private static final int DDS10_FORMAT_R16_UINT = 57;
    private static final int DDS10_FORMAT_R16_SNORM = 58;
    private static final int DDS10_FORMAT_R16_SINT = 59;
    private static final int DDS10_FORMAT_R8_TYPELESS = 60;
    private static final int DDS10_FORMAT_R8_UNORM = 61;
    private static final int DDS10_FORMAT_R8_UINT = 62;
    private static final int DDS10_FORMAT_R8_SNORM = 63;
    private static final int DDS10_FORMAT_R8_SINT = 64;
    private static final int DDS10_FORMAT_A8_UNORM = 65;
    private static final int DDS10_FORMAT_R1_UNORM = 66;
    private static final int DDS10_FORMAT_R9G9B9E5_SHAREDEXP = 67;
    private static final int DDS10_FORMAT_R8G8_B8G8_UNORM = 68;
    private static final int DDS10_FORMAT_G8R8_G8B8_UNORM = 69;
    private static final int DDS10_FORMAT_BC1_TYPELESS = 70;
    private static final int DDS10_FORMAT_BC1_UNORM = 71;
    private static final int DDS10_FORMAT_BC1_UNORM_SRGB = 72;
    private static final int DDS10_FORMAT_BC2_TYPELESS = 73;
    private static final int DDS10_FORMAT_BC2_UNORM = 74;
    private static final int DDS10_FORMAT_BC2_UNORM_SRGB = 75;
    private static final int DDS10_FORMAT_BC3_TYPELESS = 76;
    private static final int DDS10_FORMAT_BC3_UNORM = 77;
    private static final int DDS10_FORMAT_BC3_UNORM_SRGB = 78;
    private static final int DDS10_FORMAT_BC4_TYPELESS = 79;
    private static final int DDS10_FORMAT_BC4_UNORM = 80;
    private static final int DDS10_FORMAT_BC4_SNORM = 81;
    private static final int DDS10_FORMAT_BC5_TYPELESS = 82;
    private static final int DDS10_FORMAT_BC5_UNORM = 83;
    private static final int DDS10_FORMAT_BC5_SNORM = 84;
    private static final int DDS10_FORMAT_B5G6R5_UNORM = 85;
    private static final int DDS10_FORMAT_B5G5R5A1_UNORM = 86;
    private static final int DDS10_FORMAT_B8G8R8A8_UNORM = 87;
    private static final int DDS10_FORMAT_B8G8R8X8_UNORM = 88;
    private static final int DDS10_FORMAT_R10G10B10_XR_BIAS_A2_UNORM = 89;
    private static final int DDS10_FORMAT_B8G8R8A8_TYPELESS = 90;
    private static final int DDS10_FORMAT_B8G8R8A8_UNORM_SRGB = 91;
    private static final int DDS10_FORMAT_B8G8R8X8_TYPELESS = 92;
    private static final int DDS10_FORMAT_B8G8R8X8_UNORM_SRGB = 93;
    private static final int DDS10_FORMAT_BC6H_TYPELESS = 94;
    private static final int DDS10_FORMAT_BC6H_UF16 = 95;
    private static final int DDS10_FORMAT_BC6H_SF16 = 96;
    private static final int DDS10_FORMAT_BC7_TYPELESS = 97;
    private static final int DDS10_FORMAT_BC7_UNORM = 98;
    private static final int DDS10_FORMAT_BC7_UNORM_SRGB = 99;
    private static final long DDS10_FORMAT_FORCE_UINT = 0xffffffffl;

    //DDS 10 resource dimension enums
    private static final int DDS10_RESOURCE_DIMENSION_UNKNOWN = 0;
    private static final int DDS10_RESOURCE_DIMENSION_BUFFER = 1;
    private static final int DDS10_RESOURCE_DIMENSION_TEXTURE1D = 2;
    private static final int DDS10_RESOURCE_DIMENSION_TEXTURE2D = 3;
    private static final int DDS10_RESOURCE_DIMENSION_TEXTURE3D = 4;

    private static final int GL_COMPRESSED_RGB_S3TC_DXT1_EXT  = 33776;
    private static final int GL_COMPRESSED_RGBA_S3TC_DXT1_EXT  = 33777;
    private static final int GL_COMPRESSED_RGBA_S3TC_DXT3_EXT  = 33778;
    private static final int GL_COMPRESSED_RGBA_S3TC_DXT5_EXT  = 33779;

    private static final int GL_COMPRESSED_RED_RGTC1  = 36283;
    private static final int GL_COMPRESSED_SIGNED_RED_RGTC1  = 36284;
    private static final int GL_COMPRESSED_RG_RGTC2  = 36285;
    private static final int GL_COMPRESSED_SIGNED_RG_RGTC2  = 36286;

    private static final int GL_COMPRESSED_LUMINANCE_LATC1_EXT  = 35952;
    private static final int GL_COMPRESSED_SIGNED_LUMINANCE_LATC1_EXT  = 35953;
    private static final int GL_COMPRESSED_LUMINANCE_ALPHA_LATC2_EXT  = 35954;
    private static final int GL_COMPRESSED_SIGNED_LUMINANCE_ALPHA_LATC2_EXT  = 35955;

    private static final int GL_ALPHA32F  = 34838;
    private static final int GL_ALPHA16F  = 34844;
    private static final int GL_RGBA32F  = 34836;
    private static final int GL_RGBA16F  = 34842;
    private static final int GL_RGB32UI  = 36209;
    private static final int GL_RGB32I  = 36227;
    private static final int GL_RGB32F  = 34837;
    private static final int GL_RGB16F  = 34843;
    private static final int GL_RED_INTEGER  = 36244;
    private static final int GL_RG32F  = 33328;
    private static final int GL_RG  = 33319;
    private static final int GL_RG8  = 33323;
    private static final int GL_RG8I  = 33335;
    private static final int GL_RG8UI  = 33336;
    private static final int GL_RG_INTEGER  = 33320;
    private static final int GL_RG16  = 33324;
    private static final int GL_RG16I  = 33337;
    private static final int GL_RG16F  = 33327;
    private static final int GL_RG16UI  = 33338;
    private static final int GL_RG32UI  = 33340;
    private static final int GL_RG32I  = 33339;
    private static final int GL_R32F  = 33326;
    private static final int GL_R32UI  = 33334;
    private static final int GL_R32I  = 33333;
    private static final int GL_R16F  = 33325;
    private static final int GL_R16UI  = 33332;
    private static final int GL_R16I  = 33331;
    private static final int GL_R16  = 33322;
    private static final int GL_R8  = 33321;
    private static final int GL_R8UI  = 33330;
    private static final int GL_R8I  = 33329;
    private static final int GL_HALF_FLOAT  = 5131;
    private static final int GL_HALF_FLOAT_ARB  = 5131;
    private static final int GL_RGBA32UI  = 36208;
    private static final int GL_RGBA16UI  = 36214;
    private static final int GL_RGBA16I  = 36232;
    private static final int GL_RGBA_INTEGER  = 36249;
    private static final int GL_RGBA32I  = 36226;
    private static final int GL_RGB_INTEGER  = 36248;
    private static final int GL_RGB9_E5  = 35901;
    private static final int GL_R11F_G11F_B10F_EXT  = 35898;
    private static final int GL_UNSIGNED_INT_10F_11F_11F_REV_EXT  = 35899;
    private static final int GL_UNSIGNED_INT_5_9_9_9_REV  = 35902;
    private static final int GL_SRGB8_ALPHA8  = 35907;
    private static final int GL_SRGB8  = 35905;
    private static final int GL_RGBA8UI  = 36220;
    private static final int GL_DEPTH_COMPONENT32F  = 36012;

    private static final int GL_DEPTH_COMPONENT16  = 33189;

    private static final int GL_BGRA  = 32993;
    private static final int GL_BGR  = 32992;
    private static final int GL_UNSIGNED_INT_2_10_10_10_REV  = 33640;
    private static final int GL_UNSIGNED_SHORT_5_5_5_1  = 32820;
    private static final int GL_UNSIGNED_SHORT_5_6_5  = 33635;
    private static final int GL_UNSIGNED_INT_8_8_8_8  = 32821;
    private static final int GL_UNSIGNED_INT_10_10_10_2  = 32822;

    private static final int GL_RGBA  = 6408;
    private static final int GL_RGBA8  = 32856;
    private static final int GL_RGBA16  = 32859;
    private static final int GL_FLOAT  = 5126;
    private static final int GL_SHORT  = 5122;
    private static final int GL_UNSIGNED_INT  = 5125;
    private static final int GL_INT  = 5124;
    private static final int GL_RGB  = 6407;
    private static final int GL_RGB8  = 32849;
    private static final int GL_RGB16  = 32852;
    private static final int GL_RGB5_A1  = 32855;
    private static final int GL_RGB5  = 32848;
    private static final int GL_RED  = 6403;
    private static final int GL_UNSIGNED_SHORT  = 5123;
    private static final int GL_RGB10_A2  = 32857;
    private static final int GL_DEPTH  = 6145;
    private static final int GL_BYTE  = 5120;
    private static final int GL_UNSIGNED_BYTE  = 5121;
    private static final int GL_ALPHA8  = 32828;
    private static final int GL_ALPHA16  = 32830;
    private static final int GL_ALPHA  = 6406;
    private static final int GL_LUMINANCE_ALPHA  = 6410;
    private static final int GL_LUMINANCE  = 6409;
    private static final int GL_LUMINANCE8  = 32832;
    private static final int GL_LUMINANCE16  = 32834;
    private static final int GL_LUMINANCE8_ALPHA8  = 32837;
    private static final int GL_LUMINANCE16_ALPHA16  = 32840;

    private static final int GL_RGBA16_SNORM  = 36763;
    private static final int GL_RGBA8_SNORM  = 36759;
    private static final int GL_RG16_SNORM  = 36761;
    private static final int GL_RG8_SNORM  = 36757;
    private static final int GL_R16_SNORM  = 36760;
    private static final int GL_R8_SNORM  = 36756;

    private static final int GL_LUMINANCE16F_ARB  = 34846;
    private static final int GL_LUMINANCE32F_ARB  = 34840;
    private static final int GL_LUMINANCE_ALPHA32F_ARB  = 34841;
    private static final int GL_LUMINANCE_ALPHA16F_ARB  = 34847;

    private static final class DXTColBlock{
        short c010;
        short c011;

        byte[] row = new byte[4];

        int read(byte[] data, int offset){
            c010 = Numeric.getShort(data, offset);
            offset += 2;

            c011 = Numeric.getShort(data, offset);
            offset += 2;

            System.arraycopy(data, offset, row, 0, 4);
            offset += 4;

            return offset;
        }

        int write(byte[] data, int offset){
            int a = c010 & 0xFF;
            int b = (c010 >> 8) & 0xFF;
            data[offset ++] = (byte)a;
            data[offset ++] = (byte)b;

            a = c011 & 0xFF;
            b = (c011 >> 8) & 0xFF;
            data[offset ++] = (byte)a;
            data[offset ++] = (byte)b;

            System.arraycopy(row, 0, data, offset, 4);
            offset += 4;

            return offset;
        }

        void toAlpha5Block(DXT5AlphaBlock alphaBlock){
            int a = c010 &  255;
            int b = (c010 >> 8) & 255;

            alphaBlock.alpha0 = (byte)a;
            alphaBlock.alpha1 = (byte)b;

            a = c011 & 255;
            b = (c011 >> 8) & 255;
            alphaBlock.row[0] = (byte)a;
            alphaBlock.row[1] = (byte)b;

            alphaBlock.row[2] = row[0];
            alphaBlock.row[3] = row[1];
            alphaBlock.row[4] = row[2];
            alphaBlock.row[5] = row[3];
        }

        void fromAlpha5Block(DXT5AlphaBlock alphaBlock){
            int a = alphaBlock.alpha0 & 255;
            int b = alphaBlock.alpha1 & 255;
            c010 = (short)(a | b << 8);

            a = alphaBlock.row[0] & 255;
            b = alphaBlock.row[1] & 255;
            c011 = (short)(a | b << 8);

            row[0] = alphaBlock.row[2];
            row[1] = alphaBlock.row[3];
            row[2] = alphaBlock.row[4];
            row[3] = alphaBlock.row[5];
        }

        void toAlphaBlock(DXT3AlphaBlock alphaBlock){
            alphaBlock.row[0] = c010;
            alphaBlock.row[1] = c011;

            int a = row[0] & 255;
            int b = row[1] & 255;
            alphaBlock.row[2] = (short)(a|(b<<8));

            a = row[2] & 255;
            b = row[3] & 255;
            alphaBlock.row[3] = (short)(a|(b<<8));
        }

        void fromAlphaBlock(DXT3AlphaBlock alphaBlock) {
            c010 = alphaBlock.row[0];
            c011 = alphaBlock.row[1];

            int a = alphaBlock.row[2] & 255;
            int b = (alphaBlock.row[2] >> 8) & 255;
            row[0] = (byte)a;
            row[1] = (byte)b;

            a = alphaBlock.row[3] & 255;
            b = (alphaBlock.row[3] >> 8) & 255;
            row[2] = (byte)a;
            row[3] = (byte)b;
        }
    }

    private static final class DXT3AlphaBlock{
        short[] row = new short[4];
    }

    private static final class DXT5AlphaBlock{
        byte alpha0;
        byte alpha1;

        byte[] row = new byte[6];
    }

    private static final class DDS_PIXELFORMAT{
        int dwSize;
        int dwFlags;
        int dwFourCC;
        int dwRGBBitCount;
        int dwRBitMask;
        int dwGBitMask;
        int dwBBitMask;
        int dwABitMask;

        int size(){
            return 32; // 8 * 4
        }

        void read(DataInputStream in) throws IOException{
            dwSize = in.readInt();
            dwFlags = in.readInt();
            dwFourCC = in.readInt();
            dwRGBBitCount = in.readInt();
            dwRBitMask = in.readInt();
            dwGBitMask = in.readInt();
            dwBBitMask = in.readInt();
            dwABitMask = in.readInt();

            dwSize = Integer.reverseBytes(dwSize);
            dwFlags = Integer.reverseBytes(dwFlags);
            dwFourCC = Integer.reverseBytes(dwFourCC);
            dwRGBBitCount = Integer.reverseBytes(dwRGBBitCount);
            dwRBitMask = Integer.reverseBytes(dwRBitMask);
            dwGBitMask = Integer.reverseBytes(dwGBitMask);
            dwBBitMask = Integer.reverseBytes(dwBBitMask);
            dwABitMask = Integer.reverseBytes(dwABitMask);
        }

        void write(DataOutputStream fp) throws IOException {
            fp.writeInt(dwSize);
            fp.writeInt(dwFlags);
            fp.writeInt(dwFourCC);
            fp.writeInt(dwRGBBitCount);
            fp.writeInt(dwRBitMask);
            fp.writeInt(dwGBitMask);
            fp.writeInt(dwBBitMask);
            fp.writeInt(dwABitMask);
        }
    }

    private static final class DDS_HEADER{
        int dwSize;
        int dwFlags;
        int dwHeight;
        int dwWidth;
        int dwPitchOrLinearSize;
        int dwDepth;
        int dwMipMapCount;
        int[] dwReserved1 = new int[11];
        DDS_PIXELFORMAT ddspf = new DDS_PIXELFORMAT();
        int dwCaps1;
        int dwCaps2;
        int[] dwReserved2 = new int[3];

        int size(){
            return 23 * 4 + ddspf.size();
        }

        void read(DataInputStream in) throws IOException{
            dwSize = in.readInt();
            dwFlags = in.readInt();
            dwHeight = in.readInt();
            dwWidth = in.readInt();
            dwPitchOrLinearSize = in.readInt();
            dwDepth = in.readInt();
            dwMipMapCount = in.readInt();
            for(int i = 0; i < dwReserved1.length;i++){
                dwReserved1[i] = in.readInt();
            }
            ddspf.read(in);
            dwCaps1 = in.readInt();
            dwCaps2 = in.readInt();
            for(int i = 0; i < dwReserved2.length;i++){
                dwReserved2[i] = in.readInt();
            }

            // swap
            dwSize = Integer.reverseBytes(dwSize);
            dwFlags = Integer.reverseBytes(dwFlags);
            dwHeight = Integer.reverseBytes(dwHeight);
            dwWidth = Integer.reverseBytes(dwWidth);
            dwPitchOrLinearSize = Integer.reverseBytes(dwPitchOrLinearSize);
            dwDepth = Integer.reverseBytes(dwDepth);
            dwMipMapCount = Integer.reverseBytes(dwMipMapCount);

            for(int i = 0; i < dwReserved1.length;i++){
                dwReserved1[i] = Integer.reverseBytes(dwReserved1[i]);
            }
            dwCaps1 = Integer.reverseBytes(dwCaps1);
            dwCaps2 = Integer.reverseBytes(dwCaps2);
            for(int i = 0; i < dwReserved2.length;i++){
                dwReserved2[i] = Integer.reverseBytes(dwReserved2[i]);
            }
        }

        void write(DataOutputStream fp) throws IOException {
            fp.writeInt(dwSize);
            fp.writeInt(dwFlags);
            fp.writeInt(dwHeight);
            fp.writeInt(dwWidth);
            fp.writeInt(dwPitchOrLinearSize);
            fp.writeInt(dwDepth);
            fp.writeInt(dwMipMapCount);
            for(int i = 0; i < dwReserved1.length;i++)
                fp.writeInt(dwReserved1[i]);
            ddspf.write(fp);
            fp.writeInt(dwCaps1);
            fp.writeInt(dwCaps2);
            for(int i = 0; i < dwReserved2.length;i++)
                fp.writeInt(dwReserved2[i]);
        }
    }

    private static final class DDS_HEADER_10{
        int dxgiFormat;  // check type
        int resourceDimension; //check type
        int miscFlag;
        int arraySize;
        int reserved;

        public void read(DataInputStream fp) throws IOException {
            dxgiFormat = Integer.reverseBytes(fp.readInt());
            resourceDimension = Integer.reverseBytes(fp.readInt());
            miscFlag = Integer.reverseBytes(fp.readInt());
            arraySize = Integer.reverseBytes(fp.readInt());
            reserved = Integer.reverseBytes(fp.readInt());
        }
    }

    private static final class TempData{
        int bytesPerElement;
        boolean btcCompressed;
    }

    private static void set_type_info(int intf, int f, int t, int size, NvImage i, TempData p){
        i._internalFormat = f;
        i._format = f;
        i._type = t;

        p.bytesPerElement = size;
        p.btcCompressed = false;
    }

    private static void set_compressed_type_info(int intf, int f, int t, int size, NvImage i, TempData p){
        i._internalFormat = f;
        i._format = f;
        i._type = t;

        p.bytesPerElement = size;
        p.btcCompressed = true;
    }
}
