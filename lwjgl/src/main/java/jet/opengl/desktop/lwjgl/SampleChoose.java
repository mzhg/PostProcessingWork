package jet.opengl.desktop.lwjgl;

import java.util.Arrays;
import java.util.Random;

public class SampleChoose {

	public static void main(String[] args) {
		// The choosed sample: AMD_DepthOfField
		String[] samplers = 
		{
			"NV_Cloth",
//			"NV_WaveWork",
//			"NV_Flow",
			"NV_Hair",
//			"NV_HBAOPlus",
//			"NV_FaceWork",
//			"NV_VolumetricLighting",
			"NV_Blast",
//			"NV_SDK10.5",
//			"AMD_ShowdowFX",
//			"AMD_DepthOfField",
//			"AMD_TressFX",
//			"AMD_GeometryFX",
//			"AMD_AO",
//			"Inter_ASSAO",
//			"Inter_Cloud",
			"Inter_SAA",
			"Intel_Fluid",
			"Global Illumination",
			"PostProcessing"
		};

		final class Rate implements Comparable<Rate>{
			final int idx;
			int count;
			Rate(int i){idx = i;}

			void incr() {count++;}

			@Override
			public int compareTo(Rate rate) {
				return Integer.compare(count, rate.count);
			}
		}
		
		Random random = new Random();
		Rate[] rates = new Rate[samplers.length];
		for(int i = 0; i < rates.length; i++){
			rates[i] = new Rate(i);
		}

		for(int i = 0; i < samplers.length * 100; i++){
			int idx = random.nextInt(samplers.length);
			rates[idx].incr();
		}

		Arrays.sort(rates);

		System.out.println("The choosed sample: " + samplers[rates[rates.length - 1].idx]);
		
	}
}
