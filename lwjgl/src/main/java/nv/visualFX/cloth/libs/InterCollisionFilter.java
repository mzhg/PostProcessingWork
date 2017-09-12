package nv.visualFX.cloth.libs;

/**
 * Created by mazhen'gui on 2017/9/9.
 */

public interface InterCollisionFilter {
    /** called during inter-collision, user0 and user1 are the user data from each cloth */
    void onCollision(Object user0, Object user1);
}
