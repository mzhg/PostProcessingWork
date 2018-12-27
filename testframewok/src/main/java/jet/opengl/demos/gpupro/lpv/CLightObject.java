package jet.opengl.demos.gpupro.lpv;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

final class CLightObject {
    final Vector3f position = new Vector3f();
    final Vector3f direction = new Vector3f();
    final Vector3f up = new Vector3f();
    final Vector3f right = new Vector3f();
    final Matrix4f ViewMatrix = new Matrix4f();
    final Matrix4f ProjectionMatrix = new Matrix4f();
    float verticalAngle, horizontalAngle, aspect, fov;

    CLightObject(){
        direction.set(0,2,2);
        direction.normalise();

        up.set(0,1, 0);
    }
    CLightObject(Vector3f pos, Vector3f dir){
        position.set(pos);
        direction.set(dir);
        verticalAngle = (3.14f / 4.0f);
        fov = (90.0f);
        aspect = 1;
    }

    void computeMatrixes(){
        Matrix4f.ortho(-40,40,-40,40,-100,100, ProjectionMatrix);

        // Direction : Spherical coordinates to Cartesian coordinates conversion
        //Viz http://www.lighthouse3d.com/wp-content/uploads/2011/04/vfpoints2.gif
        direction.set(
                (float) (Math.cos(verticalAngle) * Math.sin(horizontalAngle)),
                (float) Math.sin(verticalAngle),
                (float) (Math.cos(verticalAngle) * Math.cos(horizontalAngle))
        );

        // Right vector
        right.set(
                (float) Math.sin(horizontalAngle - 3.14f / 2.0f),
        0,
                (float) Math.cos(horizontalAngle - 3.14f / 2.0f)
		);

        // Up vector - cross produktem dostanu kolm?vektor na tyto dva
//        this->up = glm::cross(right, direction);
        Vector3f.cross(right, direction, up);

        // Camera matrix
//        this->ViewMatrix = glm::lookAt(
//                this->position,
//                this->position + this->direction /*- this->position*/,
//                this->up
//		);

        Matrix4f.lookAt(position.x,position.y, position.z,
                    position.x+direction.x, position.y+direction.y, position.z+direction.z,
                    up.x, up.y, up.z, ViewMatrix);
    }

    Vector3f getPosition() {return position;}
    Vector3f getDirection() { return direction; }
    Matrix4f getProjMatrix() { return ProjectionMatrix;}
    Matrix4f getViewMatrix() { return ViewMatrix;}
    Vector3f getRight() { return right;}
    float getHorAngle() { return horizontalAngle;}
    float getVerAngle() { return verticalAngle;}
    float getAspectRatio() { return aspect;}
    float getFov() { return fov;}

//    void setPosition(ReadableVector3f pos) { position.set(pos);}
//    void setDirection(rea);
//    void setUp(glm::vec3);
    void setHorAngle(float angle) {horizontalAngle = angle;}
    void setVerAngle(float angle) {verticalAngle = angle;}
}
