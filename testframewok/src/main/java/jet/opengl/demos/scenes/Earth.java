package jet.opengl.demos.scenes;

import com.nvidia.developer.opengl.app.NvCameraMotionType;
import com.nvidia.developer.opengl.app.NvInputTransformer;
import com.nvidia.developer.opengl.models.DrawMode;
import com.nvidia.developer.opengl.models.GLVAO;
import com.nvidia.developer.opengl.models.QuadricBuilder;
import com.nvidia.developer.opengl.models.QuadricMesh;
import com.nvidia.developer.opengl.models.QuadricSphere;

import org.lwjgl.util.vector.Quaternion;

import jet.opengl.demos.scene.BaseScene;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureUtils;

public class Earth extends BaseScene {
    private GLVAO mSphere;
    private Texture2D mEarthTex;

    public Earth(){

    }

    @Override
    protected void onCreate(Object prevSavedData) {
        mNVApp.getInputTransformer().setMotionMode(NvCameraMotionType.FIRST_PERSON);
        mNVApp.getInputTransformer().setTranslation(0, -10, -45);

        QuadricBuilder builder = new QuadricBuilder();
        builder.setXSteps(30).setYSteps(30);
        builder.setDrawMode(DrawMode.FILL);
        builder.setCenterToOrigin(true);
        builder.setPostionLocation(0);
        builder.setNormalLocation(1);
        builder.setTexCoordLocation(2);

        mSphere = new QuadricMesh(builder, new QuadricSphere(1)).getModel().genVAO();
//        mEarthTex = TextureUtils.createTexture2DFromFile();
    }

    @Override
    protected void update(float dt) {

    }

    @Override
    protected void onRender(boolean clearFBO) {

    }

    @Override
    protected void onDestroy() {

    }
}
