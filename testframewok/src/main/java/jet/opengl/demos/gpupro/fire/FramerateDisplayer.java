package jet.opengl.demos.gpupro.fire;

/**
 * Created by mazhen'gui on 2017/10/28.
 */

final class FramerateDisplayer {
    private int d_frames;		// no of frames since the previous fps estimation
    private float d_timeStep;	// interval used to collect fps statistics
    private String d_text;	// table containing fps information to be displayed in the window
    private int d_pointer_p;	// position within d_text where fps should be written
    private int d_fonts_p;	// glut fonts
    private float d_red, d_green, d_blue, d_x, d_y; // color of fps information
    // and its location within the window

    FramerateDisplayer(){
//			d_text[0]='\0';
//			sprintf(d_text,"Framerate: ");
//			d_pointer_p = d_text+11;
//			d_timer.Initialize();

        d_text = "Framerate: ";
        d_pointer_p = 11;
    }

    void init(float timeStep, int fonts,float red, float green, float blue, float x, float y){
        d_frames = 0;
        d_timeStep = timeStep;
        d_fonts_p = fonts;
        d_red = red;
        d_green = green;
        d_blue = blue;
        d_x = x;
        d_y = y;
//			d_timer.SetReference();
    }

    void displayFramerate(){
        int matrixMode;
        boolean lightingOn;

        /*lightingOn= GL11.glIsEnabled(GL11.GL_LIGHTING);
        if (lightingOn) GL11.glDisable(GL11.GL_LIGHTING);

        matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        {
            GL11.glLoadIdentity();
            GLU.gluOrtho2D(0.0f, 1.0f, 0.0f, 1.0f);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPushMatrix();
            {
                GL11.glLoadIdentity();
                GL11.glPushAttrib(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT);
                GL11.glColor3f(d_red, d_green, d_blue);
                GL11.glRasterPos3f(d_x, d_y, 0.0f);
//					for(char *ch = d_text; *ch; ch++) {
//						glutBitmapCharacter(d_fonts_p, (int)*ch);
//					}
                Glut.bitmapString(d_fonts_p, d_text);
                GL11.glPopAttrib();
            }
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_PROJECTION);
        }
        GL11.glPopMatrix();

        GL11.glMatrixMode(matrixMode);
        if (lightingOn) GL11.glEnable(GL11.GL_LIGHTING);*/
    }

    void anotherFrameExecuted(int fps){
//			d_frames++;
//			float temp_time = d_timer.SinceReference();
//			if( temp_time > d_timeStep ){
//				sprintf(d_pointer_p,"%d",(int)(d_frames/temp_time));
//				d_frames = 0;
//				d_timer.SetReference();
//			}

        d_text = "Framerate: " + fps;
    }
}
