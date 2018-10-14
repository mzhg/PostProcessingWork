package jet.opengl.demos.nvidia.face.libs;

import org.lwjgl.util.vector.ReadableVector4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.security.Guard;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Administrator on 2018/10/13 0013.
 */

public class Gaussian {

    public static final List<Gaussian> SKIN;
    public static final List<Gaussian> MARBLE;

    static
    {
        {
            // We use the unblurred image as an aproximation to the first
            // gaussian because it is too narrow to be noticeable. The weight
            // of the unblurred image is the first one.
            Vector3f weights[] = {
                    new Vector3f(0.240516183695f, 0.447403391891f, 0.615796108321f),
                    new Vector3f(0.115857499765f, 0.366176401412f, 0.343917471552f),
                    new Vector3f(0.183619017698f, 0.186420206697f, 0.0f),
                    new Vector3f(0.460007298842f, 0.0f, 0.0402864201267f)
            };
            float variances[] = { 0.0516500425655f, 0.271928080903f, 2.00626388153f };

            SKIN = Collections.unmodifiableList(gaussianSum(variances, weights, 3));
        }

        {
            // In this case the first gaussian is wide and thus we cannot
            // approximate it with the unblurred image. For this reason the
            // first weight is set to zero.
            Vector3f weights[] = {
                    new Vector3f(0.0f, 0.0f, 0.0f),
                    new Vector3f(0.0544578254963f, 0.12454890956f, 0.217724878147f),
                    new Vector3f(0.243663230592f, 0.243532369381f, 0.18904245481f),
                    new Vector3f(0.310530428621f, 0.315816663292f, 0.374244725886f),
                    new Vector3f(0.391348515291f, 0.316102057768f, 0.218987941157f)
            };
            float variances[] = { 0.0362208693441f, 0.114450574559f, 0.455584392509f, 3.48331959682f };

            MARBLE = Collections.unmodifiableList(gaussianSum(variances, weights, 4));
        }
    }

    private float width;
    private final Vector4f weight = new Vector4f();

    private Gaussian() {}
    private Gaussian(float variance, Vector3f weights[], int n){
        width = (float) Math.sqrt(variance);

        Vector3f total = new Vector3f(0.0f, 0.0f, 0.0f);
        for (int i = 0; i < n + 2; i++) {
//            total += weights[i];
            Vector3f.add(total, weights[i], total);
        }

//        weight = D3DXVECTOR4(weights[n + 1], 1.0f);
        weight.set(weights[n + 1], 1.0f);
        weight.x *= 1.0f / total.x;
        weight.y *= 1.0f / total.y;
        weight.z *= 1.0f / total.z;
    }

    // This function builds a gaussian from the variances and  weights that
    // define it.
    // Two important notes:
    // - It will substract the previous variance to current one, so there
    //   is no need to do it manually.
    // - Because of optimization reasons, the first variance is implicitely
    //   0.0. So you must supply <n> variances and <n + 1> weights. If the
    //   sum of gaussians of your profile does not include a gaussian with
    //   this variance, you can set the first weight to zero.
    //   This implicit variance is useful for sum of gaussians that have a
    //   very narrow gaussian that can be approximated with the unblurred
    //   image (that is equal to 0.0 variance). See the provided gaussian
    //   sums or the README for examples.
    static List<Gaussian> gaussianSum(float variances[],
                                      Vector3f weights[],
                                      int nVariances){
        List<Gaussian> gaussians = new ArrayList<>();
        for (int i = 0; i < nVariances; i++) {
            float variance = i == 0? variances[i] : variances[i] - variances[i - 1];
            gaussians.add(new Gaussian(variance, weights, i));
        }
        return gaussians;
    }

    public float getWidth()  { return width; }
    public ReadableVector4f getWeight()  { return weight; }

//    static const std::vector<Gaussian> SKIN;
//    static const std::vector<Gaussian> MARBLE;

}
