package jet.opengl.demos.labs.skylight;

import org.lwjgl.util.vector.Vector3f;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import jet.opengl.postprocessing.util.Numeric;

public class SkyImage {
    public static void main(String[] args){
        float albedo = 0.5f;
        float turbidity = 3.f;
        float elevation = (float)Math.toRadians(10);
        final int resolution = 1024;
        final int num_channels = 9;
        // Three wavelengths around red, three around green, and three around blue.
        double lambda[/*num_channels*/] = {630, 680, 710, 500, 530, 560, 460, 480, 490};

        System.out.println("Begin to generate images!");

        ArHosekSkyModelState []skymodel_state = new ArHosekSkyModelState[num_channels];
        for (int i = 0; i < num_channels; ++i) {
            skymodel_state[i] =
                    ArHosekSkyModel.arhosekskymodelstate_alloc_init(elevation, turbidity, albedo);
        }

        System.out.println("Inilize the ArHosekSkyModel done!");
        // Vector pointing at the sun. Note that elevation is measured from the
        // horizon--not the zenith, as it is elsewhere in pbrt.
        Vector3f sunDir = new Vector3f(0.f, (float)Math.sin(elevation), (float)Math.cos(elevation));

        Vector3f v = new Vector3f();
        final float Pi = Numeric.PI;
        int nTheta = resolution;
        int nPhi = 2 * nTheta;
        float[] img = new float[3 * nTheta * nPhi];
        for(int t = 0; t< nTheta; t++){
            Float theta = (float)(t + 0.5) / nTheta * Pi;
            if (theta > Pi / 2.) continue;
            for (int p = 0; p < nPhi; ++p) {
                float phi = (p + 0.5f) / nPhi * 2.f * Pi;

                // Vector corresponding to the direction for this pixel.
//                Vector3f v(std::cos(phi) * std::sin(theta), std::cos(theta),
//                        std::sin(phi) * std::sin(theta));
                v.x = (float) (Math.cos(phi) * Math.sin(theta));
                v.y = (float) Math.cos(theta);
                v.z = (float) (Math.sin(phi) * Math.sin(theta));
                // Compute the angle between the pixel's direction and the sun
                // direction.
                double gamma = Math.acos(Numeric.clamp(Vector3f.dot(v, sunDir), -1, 1));
                assert (gamma >= 0 && gamma <= Pi);

                for (int c = 0; c < num_channels; ++c) {
                    double val = ArHosekSkyModel.arhosekskymodel_solar_radiance(
                            skymodel_state[c], theta, gamma, lambda[c]);
                    // For each of red, green, and blue, average the three
                    // values for the three wavelengths for the color.
                    // TODO: do a better spectral->RGB conversion.
                    img[3 * (t * nPhi + p) + c / 3] += val / 3.f;
                }
            }
        }

        System.out.println("------------------");
        BufferedImage outImg = new BufferedImage(nTheta, nPhi, BufferedImage.TYPE_3BYTE_BGR);
        for(int w = 0; w < nTheta; w++){
            for(int h = 0; h < nPhi; h++){
                int index = (w * nPhi + h) * 3;

                float r = Tonemap(img[index + 0]);
                float g = Tonemap(img[index + 1]);
                float b = Tonemap(img[index + 2]);
                int rgb = Numeric.makeRGBA((int)(255. * b), (int)(255. * g), (int)(255.*r), 255);
                outImg.setRGB(w, h, rgb);
            }
        }

        try {
            File file = new File("D:\\textures\\skyimg.jpg");
            file.getParentFile().mkdirs();
            ImageIO.write(outImg, "jpg", file);
            System.out.println("Saved file!");
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*ParallelInit();
        ParallelFor([&](int64_t t) {

        }, nTheta, 32);*/
    }

    private static float Tonemap(float rgb)
    {
        // Apply exposure
//        rgb *= g_exposure;

        // Apply the magic Jim Hejl tonemapping curve (see:
        // http://www.slideshare.net/ozlael/hable-john-uncharted2-hdr-lighting, slide #140)
        rgb = Math.max(0, rgb - 0.004f);
        rgb = (rgb * (6.2f * rgb + 0.5f)) / (rgb * (6.2f * rgb + 1.7f) + 0.06f);

        return rgb;
    }




}
