package jet.opengl.demos.intel.va;

import java.io.File;
import java.util.UUID;

/**
 * Created by mazhen'gui on 2017/11/17.
 */

public final class VaCore {
    private VaCore(){}

    public static String GetWorkingDirectory(){
        return new File("").getAbsolutePath() + "\\";
    }

    public static UUID GUIDFromString( String str )
    {
        /*vaGUID ret;
        RPC_STATUS s = UuidFromStringW( (RPC_WSTR)str.c_str(), &ret );
        VA_ASSERT( s == RPC_S_OK, L"GUIDFromString failed" );
        return ret;*/

        return UUID.fromString(str);
    }

    public static UUID GUIDNull() {
        return null;
    }
}
