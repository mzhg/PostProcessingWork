package jet.opengl.renderer.Unreal4.editor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention( RetentionPolicy.CLASS)
public @interface Meta {

    float UIMin() default 0;
    float UIMax() default 1;
    float Delta() default 0.001f;

    float ClampMin() default 0;
    float ClampMax() default 1;
    String ColorGradingMode() default "";
    String Tooltip() default "";
    String ForceUnits() default "cm";
    String Keywords() default "";
    float ShiftMouseMovePixelPerDelta() default 1;
    boolean SupportDynamicSliderMaxValue() default false;
    boolean SupportDynamicSliderMinValue() default false;

    String DisplayName() default "";
    int Hints() default 0;
    String Editcondition() default "";
}
