package jet.opengl.demos.gpupro.fire;

/**
 * Created by mazhen'gui on 2017/10/28.
 */

final class Cloud {
    float[] d_position = new float[3];
    float d_horiz_spiral_speed;	// speed of spiral movement within horizontal plane
    float d_vert_speed;			// vertical speed
    float d_rotation;
    float d_rotation_speed;		// speed of rotation of cloud around its center
    float d_halfSize;			// half of the size of a cloud at the moment
    float d_lifetime_left;		// lifetime left

    public Cloud(float position_x, float position_y, float position_z, float horiz_spiral_speed, float vert_speed,
                 float rotation, float rotation_rate, float halfSize, float lifetime_left) {
        d_horiz_spiral_speed = horiz_spiral_speed;
        d_vert_speed = vert_speed;
        d_rotation = rotation;
        d_rotation_speed = rotation_rate;
        d_halfSize = halfSize;
        d_lifetime_left = lifetime_left;

        d_position[0] = position_x;
        d_position[1] = position_y;
        d_position[2] = position_z;
    }
}
