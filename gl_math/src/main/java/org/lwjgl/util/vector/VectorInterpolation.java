package org.lwjgl.util.vector;

/**
 * Created by mazhen'gui on 2017/5/24.
 */

public class VectorInterpolation {
    public static final Vector2f lerp(ReadableVector2f start, ReadableVector2f stop, float amt, Vector2f dest){
        if(dest == null) dest = new Vector2f();

        dest.x = start.getX() + (stop.getX() - start.getX()) * amt;
        dest.y = start.getY() + (stop.getY() - start.getY()) * amt;
        return dest;
    }

    public static final Vector2f homeLerp(ReadableVector2f start, ReadableVector2f stop, float amt, Vector2f dest){
        if(dest == null) dest = new Vector2f();

        float w = start.getY() + (stop.getY() - start.getY()) * amt;
        dest.x = (start.getX() + (stop.getX() - start.getX()) * amt)/w;
        dest.y = 1.0f;
        return dest;
    }

    public static final Vector3f lerp(ReadableVector3f start, ReadableVector3f stop, float amt, Vector3f dest){
        if(dest == null) dest = new Vector3f();

        dest.x = start.getX() + (stop.getX() - start.getX()) * amt;
        dest.y = start.getY() + (stop.getY() - start.getY()) * amt;
        dest.z = start.getZ() + (stop.getZ() - start.getZ()) * amt;
        return dest;
    }

    public static final Vector3f homoLerp(ReadableVector3f start, ReadableVector3f stop, float amt, Vector3f dest){
        if(dest == null) dest = new Vector3f();

        float w = start.getZ() + (stop.getZ() - start.getZ()) * amt;
        dest.x = (start.getX() + (stop.getX() - start.getX()) * amt)/w;
        dest.y = (start.getY() + (stop.getY() - start.getY()) * amt)/w;
        dest.z = 1.0f;
        return dest;
    }

    public static final Vector4f lerp(ReadableVector4f start, ReadableVector4f stop, float amt, Vector4f dest){
        if(dest == null) dest = new Vector4f();

        dest.x = start.getX() + (stop.getX() - start.getX()) * amt;
        dest.y = start.getY() + (stop.getY() - start.getY()) * amt;
        dest.z = start.getZ() + (stop.getZ() - start.getZ()) * amt;
        dest.w = start.getW() + (stop.getW() - start.getW()) * amt;
        return dest;
    }

    public static final Vector4f homeLerp(ReadableVector4f start, ReadableVector4f stop, float amt, Vector4f dest){
        if(dest == null) dest = new Vector4f();

        float w = start.getW() + (stop.getW() - start.getW()) * amt;
        dest.x = (start.getX() + (stop.getX() - start.getX()) * amt)/w;
        dest.y = (start.getY() + (stop.getY() - start.getY()) * amt)/w;
        dest.z = (start.getZ() + (stop.getZ() - start.getZ()) * amt)/w;
        dest.w = 1.0f;
        return dest;
    }

    public static final float lerpInvert(ReadableVector2f start, ReadableVector2f stop, ReadableVector2f value){
        float diffX = value.getX() - start.getX();
        float diffY = value.getY() - start.getY();

        float dx = stop.getX() - start.getX();
        float dy = stop.getY() - start.getY();

        return (diffX * dx + diffY * dy)/ (dx * dx + dy * dy);
    }

    public static final float lerpInvert(ReadableVector3f start, ReadableVector3f stop, ReadableVector3f value){
        float diffX = value.getX() - start.getX();
        float diffY = value.getY() - start.getY();
        float diffZ = value.getZ() - start.getZ();

        float dx = stop.getX() - start.getX();
        float dy = stop.getY() - start.getY();
        float dz = stop.getZ() - start.getZ();

        return (diffX * dx + diffY * dy + diffZ * dz)/ (dx * dx + dy * dy + dz * dz);
    }

    public static final float lerpInvert(ReadableVector4f start, ReadableVector4f stop, ReadableVector4f value){
        float diffX = value.getX() - start.getX();
        float diffY = value.getY() - start.getY();
        float diffZ = value.getZ() - start.getZ();
        float diffW = value.getW() - start.getW();

        float dx = stop.getX() - start.getX();
        float dy = stop.getY() - start.getY();
        float dz = stop.getZ() - start.getZ();
        float dw = stop.getW() - start.getW();

        return (diffX * dx + diffY * dy + diffZ * dz + diffW * dw)/ (dx * dx + dy * dy + dz * dz + dw * dw);
    }

    public static final Vector2f baryLerp(ReadableVector2f x, ReadableVector2f y, ReadableVector2f z, Vector3f amt, Vector2f dest){
        if(dest == null) dest = new Vector2f();

        dest.x = x.getX() * amt.x + y.getX() * amt.y + z.getX() * amt.z;
        dest.y = x.getY() * amt.x + y.getY() * amt.y + z.getY() * amt.z;
        return dest;
    }

    public static final Vector3f baryLerp(ReadableVector3f x, ReadableVector3f y, ReadableVector3f z, Vector3f amt, Vector3f dest){
        if(dest == null) dest = new Vector3f();

        dest.x = x.getX() * amt.x + y.getX() * amt.y + z.getX() * amt.z;
        dest.y = x.getY() * amt.x + y.getY() * amt.y + z.getY() * amt.z;
        dest.z = x.getZ() * amt.x + y.getZ() * amt.y + z.getZ() * amt.z;
        return dest;
    }

    public static final Vector4f baryLerp(ReadableVector4f x, ReadableVector4f y, ReadableVector4f z, Vector3f amt, Vector4f dest){
        if(dest == null) dest = new Vector4f();

        dest.x = x.getX() * amt.x + y.getX() * amt.y + z.getX() * amt.z;
        dest.y = x.getY() * amt.x + y.getY() * amt.y + z.getY() * amt.z;
        dest.z = x.getZ() * amt.x + y.getZ() * amt.y + z.getZ() * amt.z;
        dest.w = x.getW() * amt.x + y.getW() * amt.y + z.getW() * amt.z;
        return dest;
    }

    public static final Vector2f baryHomoLerp(ReadableVector2f x, ReadableVector2f y, ReadableVector2f z, Vector3f amt, Vector2f dest){
        if(dest == null) dest = new Vector2f();

        float w = x.getY() * amt.x + y.getY() * amt.y + z.getY() * amt.z;
        dest.x = (x.getX() * amt.x + y.getX() * amt.y + z.getX() * amt.z)/w;
        dest.y = 1.0f;
        return dest;
    }

    public static final Vector3f baryHomoLerp(ReadableVector3f x, ReadableVector3f y, ReadableVector3f z, Vector3f amt, Vector3f dest){
        if(dest == null) dest = new Vector3f();

        float w = x.getZ() * amt.x + y.getZ() * amt.y + z.getZ() * amt.z;
        dest.x = (x.getX() * amt.x + y.getX() * amt.y + z.getX() * amt.z)/w;
        dest.y = (x.getY() * amt.x + y.getY() * amt.y + z.getY() * amt.z)/w;
        dest.z = 1.0f;
        return dest;
    }

    public static final Vector4f baryHomoLerp(ReadableVector4f x, ReadableVector4f y, ReadableVector4f z, Vector3f amt, Vector4f dest){
        if(dest == null) dest = new Vector4f();

        float w = x.getW() * amt.x + y.getW() * amt.y + z.getW() * amt.z;
        dest.x = (x.getX() * amt.x + y.getX() * amt.y + z.getX() * amt.z)/w;
        dest.y = (x.getY() * amt.x + y.getY() * amt.y + z.getY() * amt.z)/w;
        dest.z = (x.getZ() * amt.x + y.getZ() * amt.y + z.getZ() * amt.z)/w;
        dest.w = 1.0f;
        return dest;
    }

    public static final Vector3f baryLerpInvert(ReadableVector2f x, ReadableVector2f y, ReadableVector2f z, ReadableVector2f u, Vector3f amt){
        final float fx = u.getX() - x.getX();
        final float fy = u.getY() - x.getY();

        final float e1x = y.getX() - x.getX();
        final float e1y = y.getY() - x.getY();
        final float e2x = z.getX() - x.getX();
        final float e2y = z.getY() - x.getY();

        float e22 = e2x * e2x + e2y * e2y;
        final float e12 = e1x * e2x + e1y * e2y;

        float up = (e22 * e1x - e12 * e2x) * fx + (e22 * e1y - e12 * e2y) * fy;
        float down = e22 * (e1x * e1x + e1y * e1y) - e12 * e12;

        if(amt == null) amt = new Vector3f();
        amt.y = up/down;

        e22 = e1x * e1x + e1y * e1y;
        up = (e22 * e2x - e12 * e1x) * fx + (e22 * e2y - e12 * e1y) * fy;
        amt.z = up/down;
        amt.x = 1.0f - amt.y - amt.z;

        return amt;
    }

    public static final Vector3f baryLerpInvert(ReadableVector3f x, ReadableVector3f y, ReadableVector3f z, ReadableVector3f u, Vector3f amt){
        final float fx = u.getX() - x.getX();
        final float fy = u.getY() - x.getY();
        final float fz = u.getZ() - x.getZ();

        final float e1x = y.getX() - x.getX();
        final float e1y = y.getY() - x.getY();
        final float e1z = y.getZ() - x.getZ();
        final float e2x = z.getX() - x.getX();
        final float e2y = z.getY() - x.getY();
        final float e2z = z.getZ() - x.getZ();

        float e22 = e2x * e2x + e2y * e2y + e2z * e2z;
        final float e12 = e1x * e2x + e1y * e2y + e1z * e2z;

        float up = (e22 * e1x - e12 * e2x) * fx + (e22 * e1y - e12 * e2y) * fy + (e22 * e1z - e12 * e2z) * fz;
        float down = e22 * (e1x * e1x + e1y * e1y + e1z * e1z) - e12 * e12;

        if(amt == null) amt = new Vector3f();
        amt.y = up/down;

        e22 = e1x * e1x + e1y * e1y + e1z * e1z;
        up = (e22 * e2x - e12 * e1x) * fx + (e22 * e2y - e12 * e1y) * fy + (e22 * e2z - e12 * e1z) * fz;
        amt.z = up/down;
        amt.x = 1.0f - amt.y - amt.z;

        return amt;
    }

    public static final Vector3f baryLerpInvert(ReadableVector4f x, ReadableVector4f y, ReadableVector4f z, ReadableVector4f u, Vector3f amt){
        final float fx = u.getX() - x.getX();
        final float fy = u.getY() - x.getY();
        final float fz = u.getZ() - x.getZ();
        final float fw = u.getW() - x.getW();

        final float e1x = y.getX() - x.getX();
        final float e1y = y.getY() - x.getY();
        final float e1z = y.getZ() - x.getZ();
        final float e1w = y.getW() - x.getW();
        final float e2x = z.getX() - x.getX();
        final float e2y = z.getY() - x.getY();
        final float e2z = z.getZ() - x.getZ();
        final float e2w = z.getW() - x.getW();

        final float e22 = e2x * e2x + e2y * e2y + e2z * e2z + e2w * e2w;
        final float e11 = e1x * e1x + e1y * e1y + e1z * e1z + e1w * e1w;
        final float e12 = e1x * e2x + e1y * e2y + e1z * e2z + e1w * e2w;

        float up = (e22 * e1x - e12 * e2x) * fx + (e22 * e1y - e12 * e2y) * fy + (e22 * e1z - e12 * e2z) * fz + (e22 * e1w - e12 * e2w) * fw;
        float down = e22 * e11 - e12 * e12;

        if(amt == null) amt = new Vector3f();
        amt.y = up/down;

        up = (e11 * e2x - e12 * e1x) * fx + (e11 * e2y - e12 * e1y) * fy + (e11 * e2z - e12 * e1z) * fz + (e11 * e2w - e12 * e2w) * fw;
        amt.z = up/down;
        amt.x = 1.0f - amt.y - amt.z;

        return amt;
    }

    public static Vector2f bilinear(ReadableVector2f x, ReadableVector2f y, ReadableVector2f z, ReadableVector2f w, float a, float b, Vector2f dest){
        final float _a = 1-a;
        final float _b = 1-b;

        if(dest == null) dest = new Vector2f();

        dest.x = _a * _b * x.getX() + a * _b * y.getX() + a * b * z.getX() + _a * b * w.getX();
        dest.y = _a * _b * x.getY() + a * _b * y.getY() + a * b * z.getY() + _a * b * w.getY();

        return dest;
    }

    public static Vector3f bilinear(ReadableVector3f x, ReadableVector3f y, ReadableVector3f z, ReadableVector3f w, float a, float b, Vector3f dest){
        final float _a = 1-a;
        final float _b = 1-b;

        if(dest == null) dest = new Vector3f();

        dest.x = _a * _b * x.getX() + a * _b * y.getX() + a * b * z.getX() + _a * b * w.getX();
        dest.y = _a * _b * x.getY() + a * _b * y.getY() + a * b * z.getY() + _a * b * w.getY();
        dest.z = _a * _b * x.getZ() + a * _b * y.getZ() + a * b * z.getZ() + _a * b * w.getZ();

        return dest;
    }

    public static Vector4f bilinear(ReadableVector4f x, ReadableVector4f y, ReadableVector4f z, ReadableVector4f w, float a, float b, Vector4f dest){
        final float _a = 1-a;
        final float _b = 1-b;

        if(dest == null) dest = new Vector4f();

        dest.x = _a * _b * x.getX() + a * _b * y.getX() + a * b * z.getX() + _a * b * w.getX();
        dest.y = _a * _b * x.getY() + a * _b * y.getY() + a * b * z.getY() + _a * b * w.getY();
        dest.z = _a * _b * x.getZ() + a * _b * y.getZ() + a * b * z.getZ() + _a * b * w.getZ();
        dest.w = _a * _b * x.getW() + a * _b * y.getW() + a * b * z.getW() + _a * b * w.getW();

        return dest;
    }

    public static Vector2f homoBilinear(ReadableVector2f x, ReadableVector2f y, ReadableVector2f z, ReadableVector2f w, float a, float b, Vector2f dest){
        final float _a = 1-a;
        final float _b = 1-b;

        if(dest == null) dest = new Vector2f();

        float wei = _a * _b * x.getY() + a * _b * y.getY() + a * b * z.getY() + _a * b * w.getY();
        dest.x = (_a * _b * x.getX() + a * _b * y.getX() + a * b * z.getX() + _a * b * w.getX())/wei;
        dest.y = 1.0f;

        return dest;
    }

    public static Vector3f homoBilinear(ReadableVector3f x, ReadableVector3f y, ReadableVector3f z, ReadableVector3f w, float a, float b, Vector3f dest){
        final float _a = 1-a;
        final float _b = 1-b;

        if(dest == null) dest = new Vector3f();

        float wei = _a * _b * x.getZ() + a * _b * y.getZ() + a * b * z.getZ() + _a * b * w.getZ();
        dest.x = (_a * _b * x.getX() + a * _b * y.getX() + a * b * z.getX() + _a * b * w.getX())/wei;
        dest.y = (_a * _b * x.getY() + a * _b * y.getY() + a * b * z.getY() + _a * b * w.getY())/wei;
        dest.z = 1.0f;

        return dest;
    }

    public static Vector4f homoBilinear(ReadableVector4f x, ReadableVector4f y, ReadableVector4f z, ReadableVector4f w, float a, float b, Vector4f dest){
        final float _a = 1-a;
        final float _b = 1-b;

        if(dest == null) dest = new Vector4f();

        float wei = _a * _b * x.getW() + a * _b * y.getW() + a * b * z.getW() + _a * b * w.getW();
        dest.x = (_a * _b * x.getX() + a * _b * y.getX() + a * b * z.getX() + _a * b * w.getX())/wei;
        dest.y = (_a * _b * x.getY() + a * _b * y.getY() + a * b * z.getY() + _a * b * w.getY())/wei;
        dest.z = (_a * _b * x.getZ() + a * _b * y.getZ() + a * b * z.getZ() + _a * b * w.getZ())/wei;
        dest.w = 1.0f;

        return dest;
    }

    public static Vector2f bilinearInvert(ReadableVector2f x, ReadableVector2f y, ReadableVector2f z, ReadableVector2f w, ReadableVector2f u, Vector2f amt){
        float wxx = w.getX() - x.getX();
        float wxy = w.getY() - x.getY();

        float zyx = w.getX() - y.getX();
        float zyy = w.getY() - y.getY();

        float uxx = u.getX() - x.getX();
        float uxy = u.getY() - x.getY();

        float uyx = u.getX() - y.getX();
        float uyy = u.getY() - y.getY();

        float a = cross(wxx, wxy, zyx, zyy);
        float b = cross(zyx, zyy, uxx, uxy) - cross(wxx, wxy, uyx, uyy);
        float c = cross(uxx, uxy, uyx, uyy);
        float _b;

        if(Math.abs(a) < 1e-6f){
            _b = -c/b;
        }else{
            if(b > 0){
                _b = (float) ((-b - Math.sqrt(b * b - 4 * a * c))/(2 * a));
            }else{
                _b = (float) (2 * c / (-b + Math.sqrt(b * b - 4 * a * c)));
            }
        }

        float s1bx = (1 - _b) * x.getX() + _b * w.getX();
        float s1by = (1 - _b) * x.getY() + _b * w.getY();
        float s2bx = (1 - _b) * y.getX() + _b * z.getX();
        float s2by = (1 - _b) * y.getY() + _b * z.getY();

        float dx = s2bx - s1bx, dy = s2by - s1by;
        float _a = ((u.getX() - s1bx) * dx + (u.getY() - s1by) * dy)/ (dx * dx + dy * dy);

        if(amt == null)
            amt = new Vector2f(_a, _b);
        else
            amt.set(_a, _b);
        return amt;
    }

    public static Vector2f bilinearInvert(ReadableVector3f x, ReadableVector3f y, ReadableVector3f z, ReadableVector3f w, ReadableVector3f u, Vector2f amt){
        final Vector3f n = new Vector3f();
        final Vector3f tmp = new Vector3f();
        final Vector3f zx = Vector3f.sub(z, x, null);
        final Vector3f wy = Vector3f.sub(w, y, null);
        final Vector3f wx = Vector3f.sub(w, x, null);
        final Vector3f zy = Vector3f.sub(z, y, null);
        final Vector3f ux = Vector3f.sub(u, x, null);
        final Vector3f uy = Vector3f.sub(u, y, null);

        Vector3f.cross(zx, wy, n);

        Vector3f.cross(wx, zy, tmp);
        float a = Vector3f.dot(n, tmp);

        Vector3f.cross(zy, ux, tmp);
        Vector3f.cross(wx, uy, wx);
        Vector3f.sub(tmp, wx, tmp);
        float b = Vector3f.dot(n, tmp);

        Vector3f.cross(ux, uy, tmp);
        float c = Vector3f.dot(n, tmp);

        float _b;

        if(Math.abs(a) < 1e-6f){
            _b = -c/b;
        }else{
            if(b > 0){
                _b = (float) ((-b - Math.sqrt(b * b - 4 * a * c))/(2 * a));
            }else{
                _b = (float) (2 * c / (-b + Math.sqrt(b * b - 4 * a * c)));
            }
        }

        Vector3f s1b = lerp(x, w, _b, tmp);
        Vector3f s2b = lerp(y, z, _b, zx);
        Vector3f diff = Vector3f.sub(s2b, s1b, wy);
        Vector3f f = Vector3f.sub(u, s1b, wx);

        float _a = Vector3f.dot(f, diff)/diff.lengthSquared();
        if(amt == null)
            amt = new Vector2f(_a, _b);
        else
            amt.set(_a, _b);

        return amt;
    }

    public static Vector2f slerp(ReadableVector2f x, ReadableVector2f y, float amt, Vector2f dest){
        float c = Vector2f.dot(x, y);  // cosine of x,y
        double angle = Math.acos(c);
        double a,b;

        if(angle < 1e-9){
            a = 1.0 - amt;
            b = amt;
        }else{
            double s = Math.sin(angle);

            a = Math.sin((1.0-amt) * angle)/s;
            b = Math.sin(amt * angle)/s;
        }

        return Vector2f.linear(x, (float)a, y, (float)b, dest);
    }

    public static Vector3f slerp(ReadableVector3f x, ReadableVector3f y, float amt, Vector3f dest){
        float c = Vector3f.dot(x, y);  // cosine of x,y
        double angle = Math.acos(c);
        double a,b;

        if(angle < 1e-9){
            a = 1.0 - amt;
            b = amt;
        }else{
            double s = Math.sin(angle);

            a = Math.sin((1.0-amt) * angle)/s;
            b = Math.sin(amt * angle)/s;
        }

        return Vector3f.linear(x, (float)a, y, (float)b, dest);
    }

    /** The cross product of the two vector2d. */
    static final float cross(float x1, float y1, float x2, float y2) {
        return x1 * y2 - x2 * y1;
    }

    private VectorInterpolation(){}
}
