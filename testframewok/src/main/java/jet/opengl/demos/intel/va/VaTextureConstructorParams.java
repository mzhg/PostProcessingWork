package jet.opengl.demos.intel.va;

import java.util.UUID;

/**
 * Created by mazhen'gui on 2017/11/22.
 */

final class VaTextureConstructorParams implements VaConstructorParamsBase{
    UUID UID;
    VaTextureConstructorParams(UUID uid)  {UID = uid; }
    VaTextureConstructorParams() {UID = UUID.randomUUID();}
}
