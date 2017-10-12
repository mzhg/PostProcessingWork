package jet.opengl.android.common;

public class MainActivity extends BaseActivity {

	public static final String ACTION_SAMPLE = "jet.learning.opengl.action.sample";
	public static final String CATEGOTY_SUB = "jet.learning.opengl.category.sub";
	public static final String CATEGOTY_ES2 = "jet.learning.opengl.category.es2";
	public static final String CATEGOTY_ES3 = "jet.learning.opengl.category.es3";

	@Override
	protected String getAction() {
		return null;
	}

	@Override
	protected String getCategory() {
		return ACTION_SAMPLE;
	}

}
