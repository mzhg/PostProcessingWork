package jet.opengl.demos.intel.fluid.render;

/**
 * Created by Administrator on 2018/4/5 0005.
 */

public enum CompareFuncE {
    CMP_FUNC_NEVER          ,   ///< Never pass the comparision.
    CMP_FUNC_LESS           ,   ///< If current/source is less than new/destination, comparison passes.
    CMP_FUNC_GREATER_EQUAL  ,   ///< If current/source is greater than or equal to than new/destination, comparison passes.
    CMP_FUNC_ALWAYS         ,   ///< Always pass the comparision.
}
