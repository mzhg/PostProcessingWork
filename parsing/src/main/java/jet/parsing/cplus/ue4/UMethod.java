package jet.parsing.cplus.ue4;

import java.util.List;

// A class represents the method in the ue4 class or struct.
final class UMethod {
    // The comments above the method, can be null
    String documents;
    // The returned type of the method, can't be null
    String returnType;

    // The modifer of the method, can't be null
    String modifier = "public";

    // Parameter Lists, they same as the UFiled, can be null
    List<Object> parameters;

    // can be null
    String body;

    boolean isVirtual;
}
