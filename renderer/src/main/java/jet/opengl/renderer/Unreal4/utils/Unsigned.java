package jet.opengl.renderer.Unreal4.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.TYPE_PARAMETER})
@Retention( RetentionPolicy.SOURCE)
public @interface Unsigned {
}
