package jet.opengl.demos.intel.fluid.render;

/**
 * Declaration of a vertex format.<p></p>

 This is a placeholder for a more sophisticated, flexible
 mechanism for declaring a vertex format.  Meanwhile it
 exists only in a preliminary state, in the hope that once
 it is fully implemented, the API will remain mostly unchanged.<p></p>

 Eventually, a generic "vertex declaration" will be a list of VertexElements.<p></p>

 (For now, this class merely wraps VertexFormatE.)
 * Created by Administrator on 2018/3/13 0013.
 */

public class VertexDeclaration {
    enum SemanticE
    {
        SEMANTIC_NONE   ,
        POSITION        ,
        NORMAL          ,
        BINORMAL        ,
        TANGENT         ,
        COLOR_AMBIENT   ,
        COLOR_DIFFUSE   ,
        COLOR_SPECULAR  ,
        TEXTURE_0       ,
        TEXTURE_1       ,
        BLEND_WEIGHTS   ,
        NUM_SEMANTICS   ,
    } ;

    enum DataTypeE
    {
        DATA_TYPE_NONE  ,
        BYTE            ,
        SHORT           ,
        INT             ,
        FLOAT           ,
        FLOAT2          ,
        FLOAT3          ,
        FLOAT4          ,
        NUM_DATA_TYPES
    } ;

    /** Element in a vertex format.
     The combination of offset and stride allows interleaved or contiguous data.
     */
    public class VertexElement
    {
        public SemanticE   mSemantic   ;   /// Semantic (meaning/purpose) for this vertex element.
        public DataTypeE   mDataType   ;   /// Primitive type of data used by this vertex element.
        public int         mOffset     ;   /// Offset, in bytes, from start of vertex buffer to first of this kind of element.
        public int         mStride     ;   /// Number of bytes between elements of this type.
    } ;


    /** Simple common vertex formats.

     In principle, the underlying API supports more formats than these,
     and the "vertex format" should be more flexible than this.  See VertexElement.

     Note that some API's, like DirectX8, impose restrictions on the
     ordering of vertex elements, so the ordering implied by these names
     might not represent the actual ordering within the vertex data
     for all platforms.
     */
    enum VertexFormatE
    {
        VERTEX_FORMAT_NONE              ,

        POSITION                        ,

        POSITION_NORMAL                 ,
        POSITION_COLOR                  ,
        POSITION_TEXTURE                ,

        POSITION_NORMAL_COLOR           ,
        POSITION_NORMAL_TEXTURE         ,
        POSITION_COLOR_TEXTURE          ,

        POSITION_NORMAL_COLOR_TEXTURE   ,

        GENERIC                         ,

        NUM_FORMATS
    } ;


    /// Initialize a vertex declaration object.
    public VertexDeclaration(){}


    /** Initialize a vertex declaration object.

     \param vertexFormat     Format to use to represent a vertex

     \note   VertexFormatE is a simplistic, inflexible way to declare a vertex format.
     It is convenient but a another mechanism could provide more flexible format declarations.
     */
    public VertexDeclaration( VertexFormatE vertexFormat ) {
        mVertexFormat = vertexFormat;
    }

    public void set(VertexDeclaration ohs){
        this.mVertexFormat = ohs.mVertexFormat;
    }

    public VertexFormatE getVertexFormat()
    {
        return mVertexFormat ;
    }


    /// Return whether this vertex declaration has normals, i.e. supports lighting/shading.
    public boolean hasNormals()
    {
        switch( mVertexFormat )
        {
            case POSITION_NORMAL:
            case POSITION_NORMAL_COLOR:
            case POSITION_NORMAL_TEXTURE:
            case POSITION_NORMAL_COLOR_TEXTURE:
                return true ;
        }
        return false ;
    }


    /// Return whether this vertex declaration has colors, i.e. should use per-vertex color.
    public boolean hasColors()
    {
        switch( mVertexFormat )
        {
            case POSITION_COLOR:
            case POSITION_NORMAL_COLOR:
            case POSITION_COLOR_TEXTURE:
            case POSITION_NORMAL_COLOR_TEXTURE:
                return true ;
        }
        return false ;
    }


    /// Return whether this vertex declaration supports textures.
    public boolean hasTextureCoordinates()
    {
        switch( mVertexFormat )
        {
            case POSITION_TEXTURE:
            case POSITION_NORMAL_TEXTURE:
            case POSITION_COLOR_TEXTURE:
            case POSITION_NORMAL_COLOR_TEXTURE:
                return true ;
        }
        return false ;
    }


    /// Return whether this vertex declaration is invalid.
    public boolean isInvalid()
    {
        return ( VertexFormatE.VERTEX_FORMAT_NONE == mVertexFormat ) ;
    }

    VertexFormatE   mVertexFormat = VertexFormatE.VERTEX_FORMAT_NONE; ///< Format used to represent a vertex
}
