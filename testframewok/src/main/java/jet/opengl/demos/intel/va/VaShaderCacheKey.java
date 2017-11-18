package jet.opengl.demos.intel.va;

import jet.opengl.postprocessing.util.CommonUtil;

/**
 * Created by mazhen'gui on 2017/11/18.
 */

public class VaShaderCacheKey {
    public String                StringPart = "";

//    bool                       operator == ( const vaShaderCacheKey & cmp ) const    { return this->StringPart == cmp.StringPart; }
//    bool                       operator < ( const vaShaderCacheKey & cmp ) const     { return this->StringPart < cmp.StringPart; }
//    bool                       operator >( const vaShaderCacheKey & cmp ) const     { return this->StringPart > cmp.StringPart; }

    public void                       Save( VaStream outStream ){
        throw new UnsupportedOperationException();
    }
    public void                       Load( VaStream inStream ){
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VaShaderCacheKey that = (VaShaderCacheKey) o;

        return CommonUtil.equals(StringPart, that.StringPart);
    }

    @Override
    public int hashCode() {
        return StringPart != null ? StringPart.hashCode() : 0;
    }
}
