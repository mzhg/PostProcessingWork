package jet.opengl.demos.intel.fluid.utils;

import org.lwjgl.util.vector.Vector3f;

import jet.opengl.demos.intel.fluid.render.ApiBase;

/**
 * Field of text used to display diagnostic messages.<p></p>

 Technically, this is an Overlay, so it could (should?) be managed as an overlay,
 but (a) I have not yet implemented Overlays and (b) even if/when I do, diagnostic text
 would be a special overlay in that it would be the last one rendered, to guarantee it
 appears overtop everything else on the screen.  Still, it should eventually follow the same
 recipe (e.g. implement the same interface) as a regular Overlay.<p></p>
 * Created by mazhen'gui on 2018/3/12.
 */

public class DiagnosticTextOverlay implements IRenderable {
    private ApiBase            mRenderApi          ;   /// Non-owned address of low-level render system device
    private final StringBuilder mTextLines = new StringBuilder();   /// Text buffer
    private int                mMaxCharsPerLine    ;   /// Maximum number of characters per line
    private int                mNumLinesPopulated  ;   /// Number of text lines that have data
    private boolean            mDisplayEnabled     ;   /// Whether to display diagnostic text

    public DiagnosticTextOverlay( ApiBase renderApi , int numLines /*= 256*/, int maxCharactersPerLine /*= 256*/ ){
        mRenderApi = renderApi;
        mMaxCharsPerLine = maxCharactersPerLine;
        mNumLinesPopulated = 0;
        mDisplayEnabled = true;

        mTextLines.ensureCapacity((int)(Math.sqrt(numLines * maxCharactersPerLine)));
    }

    public void appendLine( String pattern, Object...args){
        if(args != null){
            mTextLines.append(String.format(pattern, args)).append('\n');
        }else{
            mTextLines.append(pattern).append('\n');
        }

        mNumLinesPopulated ++;
    }

    public void Clear(){
        mTextLines.setLength(0);
        mNumLinesPopulated = 0;
    }

    public boolean getDisplayEnabled()                   { return mDisplayEnabled ; }
    public void setDisplayEnabled( boolean displayEnabled )   { mDisplayEnabled = displayEnabled ; }

    @Override
    public void render() {
        // Render line by calling api->RenderSimpleText
        mRenderApi.renderSimpleText( mTextLines , new Vector3f( 0.0f , 0 , 0.0f ) , /* use screen space */ true ) ;
    }
}
