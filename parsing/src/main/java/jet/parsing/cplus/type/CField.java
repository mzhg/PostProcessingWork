package jet.parsing.cplus.type;

import java.lang.reflect.Modifier;

import jet.parsing.cplus.expression.CExpression;

public class CField {
    private CType mType;  // The type of the field
    private String mName; // The Filed name
    private CExpression mValue; // The default value. can be null.

    private int mModifier;  // The modifier of the field.
    private CClass mParent;  // which class the filed belong to
}
