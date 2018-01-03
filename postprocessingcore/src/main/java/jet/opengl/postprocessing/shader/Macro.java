package jet.opengl.postprocessing.shader;

import jet.opengl.postprocessing.util.CommonUtil;

/**
 * Represents a pre-defined macro for the shader source building.<p></p>
 * Created by mazhen'gui on 2017-04-18 17:08:55.
 */

public class Macro {
    public String key;
    public Object value;

    public Macro(){}

    public Macro(String key, Object value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public String toString() {
        return "Macro{" +
                "key='" + key + '\'' +
                ", value=" + value +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Macro macro = (Macro) o;
        return CommonUtil.equals(key, macro.key) && CommonUtil.equals(value, macro.value);
    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }
}
