package jet.opengl.demos.intel.fluid.render;

/**
 * Texture base class.
 * Created by Administrator on 2018/3/13 0013.
 */

public abstract class TextureBase {
    /** Format of texture data.
     */
    enum FormatE
    {
        TEX_FORMAT_R8G8B8   ,
        TEX_FORMAT_A8R8G8B8 ,
        TEX_FORMAT_D16      ,
        TEX_FORMAT_D32      ,
        TEX_FORMAT_D24S8    ,
        TEX_FORMAT_UNDEFINED    ///< Texture format not assigned yet.
    } ;


    /** Texture memory usage flags. Combine with bitwise-OR.
     */
    public static final int
//    enum UsageFlagsE
//    {
        TEX_USAGE_DEFAULT       = 0x0 ,
        TEX_USAGE_DYNAMIC       = 0x1 , ///< Texture is writable.  Default is read-only.
        TEX_USAGE_RENDER_TARGET = 0x2 ; ///< Texture will be used as a render target.
//    } ;


    /** Topological configuration of texture.
     */
    enum ShapeE
    {
        TEX_SHAPE_1D        ,   ///< Line texture.
        TEX_SHAPE_2D        ,   ///< Planar texture.
        TEX_SHAPE_ARRAY     ,   ///< Array of planar textures.
        TEX_SHAPE_3D        ,   ///< Volume texture.
        TEX_SHAPE_CUBEMAP   ,   ///< 6 square planar faces.
        TEX_SHAPE_UNDEFINED ,   ///< Shape not assigned yet.
    } ;

    protected int         mWidth          ;   ///< Texture width, in pixels.
    protected int         mHeight         ;   ///< Texture height, in pixels.
    protected int         mNumPlanes      ;   ///< Number of planes for a texture array or volume texture.
    protected int         mNumMipLevels   ;   ///< Number of MIPmap levels.
    protected FormatE     mFormat         ;   ///< Format of texture data.
    protected int         mUsageFlags     ;   ///< Memory usage of texture data.
    protected ShapeE      mShape          ;   ///< Topological configuration of texture.

    public TextureBase(){
        mFormat = FormatE.TEX_FORMAT_UNDEFINED;
        mUsageFlags = TEX_USAGE_DEFAULT;
        mShape = ShapeE.TEX_SHAPE_UNDEFINED;
    }

    public TextureBase( int width , int height , int numPlanes , int numMipLevels , FormatE format , int usageFlags , ShapeE shape ){
        setWidth( width ) ;
        setHeight( height ) ;
        setNumPlanes( numPlanes ) ;
        setNumMipLevels( numMipLevels ) ;
        setFormat( format ) ;
        setUsageFlags( usageFlags ) ;
        setShape( shape ) ;
    }

    public void setWidth( int width ){
        assert ( 0 == mWidth ) ; // Not allowed to change after assigned
        assert ( width > 0 ) ;
        mWidth = width ;
    }

    public void setHeight( int height ){
        assert ( 0 == mHeight ) ; // Not allowed to change after assigned
        assert ( height > 0 ) ;
        assert ( ( ShapeE.TEX_SHAPE_1D != getShape() ) || ( 1 == height ) ) ; // If 1D texture, height must be 1.
        mHeight = height ;
    }

    public void setNumPlanes( int numPlanes ){
        assert ( 0 == mNumPlanes ) ; // Not allowed to change after assigned
        assert ( numPlanes > 0 ) ;
        assert (    ( 1 == numPlanes )
                ||  ( ShapeE.TEX_SHAPE_UNDEFINED == getShape() )
                ||  ( ShapeE.TEX_SHAPE_ARRAY     == getShape() )
                ||  ( ShapeE.TEX_SHAPE_3D        == getShape() ) ) ;
    }

    public void setNumMipLevels( int numMipLevels ){
        assert ( 0 == mNumMipLevels ) ; // Not allowed to change after assigned
        assert ( numMipLevels > 0 ) ;
        mNumMipLevels = numMipLevels ;
    }

    public void setFormat( FormatE format){
        assert ( FormatE.TEX_FORMAT_UNDEFINED == mFormat ) ; // Not allowed to change after assigned.
        assert ( format != FormatE.TEX_FORMAT_UNDEFINED ) ;
        mFormat = format ;
    }

    public void setUsageFlags( int usageFlags ){
        assert ( 0 == mUsageFlags ) ; // Not allowed to change after assigned.
        mUsageFlags = usageFlags ;
    }

    public void setShape( ShapeE shape ){
        assert ( ShapeE.TEX_SHAPE_UNDEFINED == mShape ) ; // Not allowed to change after assigned.
        assert ( shape != ShapeE.TEX_SHAPE_UNDEFINED ) ;
        assert ( ( shape != ShapeE.TEX_SHAPE_1D      ) || ( ( 0 == getHeight()    ) || ( 1 == getHeight()    ) ) )      ; // If 1D texture, either height must be unassigned or 1.
        assert ( ( shape != ShapeE.TEX_SHAPE_2D      ) || ( ( 0 == getNumPlanes() ) || ( 1 == getNumPlanes() ) ) )      ; // If 2D texture, either depth must be unassigned or 1.
        assert ( ( shape != ShapeE.TEX_SHAPE_CUBEMAP ) || ( ( 0 == getNumPlanes() ) || ( 1 == getNumPlanes() ) ) )      ; // If cubemap texture, either depth must be unassigned or 1.
        assert ( ( shape != ShapeE.TEX_SHAPE_CUBEMAP ) || ( ( 0 == getWidth() )     || ( getWidth() == getHeight() ) ) ) ; // If cubemap texture, width must equal height (square faces).
    }

    public int getWidth()         { return mWidth ; }
    public int getHeight()        { return mHeight ; }
    public int getNumPlanes()     { return mNumPlanes ; }
    public int getNumMipLevels()  { return mNumMipLevels ; }
    public FormatE getFormat()    { return mFormat ; }
    public int getUsageFlags()    { return mUsageFlags ; }
    public ShapeE getShape()      { return mShape ; }

    public abstract void bind( ApiBase renderApi , SamplerState samplerState );
    /*public abstract void CreateFromImages( const Image * images , size_t numImages );
    public abstract void CopyToImage( Image & image );*/
}
