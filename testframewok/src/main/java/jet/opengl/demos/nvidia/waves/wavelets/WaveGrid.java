package jet.opengl.demos.nvidia.waves.wavelets;

import org.lwjgl.util.vector.ReadableVector;
import org.lwjgl.util.vector.ReadableVector4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;

import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.postprocessing.util.Pair;
import jet.opengl.postprocessing.util.StackFloat;

/**
 @ WaveGrid main class representing water surface.

 @section Usage


 @section Discretization

 explain that \theta_0 = 0.5*dtheta

 The location is determined by four numbers: x,y,theta,zeta
 $x \in [-size,size]$ - the first spatial coordinate
 $y \in [-size,size]$ - the second spatial coordinate
 $theta \in [0,2 \pi)$  - direction of wavevector, theta==0 corresponds to
 wavevector in +x direction
 $zeta \in [\log_2(minWavelength),\log_2(maxWavelength)]$ - zeta is log2 of
 wavelength

 The reason behind using zeta instead of wavenumber or wavelength is the
 nature of wavelengths and it has better discretization properties. We want
 to have a nice cascade of waves with exponentially increasing wavelengths.
 */
final class WaveGrid {
//    enum Coord { X , Y , Theta , Zeta  };

    static final int X =0, Y =1, Theta =2, Zeta=3;

    /** Select spectrum type. Currently only PiersonMoskowitz is supported. */
    enum SpectrumType {
        LinearBasis,
        PiersonMoskowitz
    }// spectrumType = PiersonMoskowitz;

    /**
     @brief Settings to set up @ref WaveGrid

     The physical size of the resulting domain is:
     [-size,size]x[-size,size]x[0,2pi)x[min_zeta,max_zeta]

     The final grid resolution is: n_x*n_x*n_theta*n_zeta
     */
    final class Settings {
        /** Spatial size of the domain will be [-size,size]x[-size,size] */
        float size = 50;
        /** Maximal zeta to simulate. */
        float max_zeta = 0.01f;
        /** Minimal zeta to simulate. */
        float min_zeta = 10;

        /** Number of nodes per spatial dimension. */
        int n_x = 100;
        /** Number of discrete wave directions. */
        int n_theta = 16;
        /** Number of nodes in zeta. This determines resolution in wave number @see
         * @ref Discretization. */
        int n_zeta = 1;

        /** Set up initial time. Default value is 100 because at time zero you get
         * wierd coherency patterns */
        float initial_time = 100;

        SpectrumType spectrumType = SpectrumType.PiersonMoskowitz;
    }

    private Grid m_amplitude = new Grid();
    private Grid m_newAmplitude = new Grid();
    private Spectrum m_spectrum;

    private ProfileBuffer[] m_profileBuffers;

    private final Vector4f m_xmin = new Vector4f();
    private final Vector4f m_xmax = new Vector4f();
    private final Vector4f m_dx = new Vector4f();
    private final Vector4f m_idx = new Vector4f();

    private float[] m_groupSpeeds;

    private float m_time;

    private Environment m_enviroment;

    /**
     * @brief Construct WaveGrid based on supplied @ref Settings
     * @param s settings to initialize WaveGrid
     */
    WaveGrid(Settings s){
        m_spectrum = new Spectrum(10);
        m_enviroment = new Environment(s.size);

        m_amplitude.resize(s.n_x, s.n_x, s.n_theta, s.n_zeta);
        m_newAmplitude.resize(s.n_x, s.n_x, s.n_theta, s.n_zeta);

        float zeta_min = (float) m_spectrum.minZeta();
        float zeta_max = (float) m_spectrum.maxZeta();

        m_xmin.set(-s.size, -s.size, 0.0f, zeta_min);
        m_xmax.set(s.size, s.size, tau, zeta_max);

        /*for (int i = 0; i < 4; i++) {
            m_dx[i]  = (m_xmax[i] - m_xmin[i]) / m_amplitude.dimension(i);
            m_idx[i] = 1.0 / m_dx[i];
        }*/

        Vector4f.sub(m_xmax, m_xmin, m_dx);
        m_dx.x /= m_amplitude.dimension(0);
        m_dx.y /= m_amplitude.dimension(1);
        m_dx.z /= m_amplitude.dimension(2);
        m_dx.w /= m_amplitude.dimension(3);

        Vector4f.div(1.0f, m_dx, m_idx);

        m_time = s.initial_time;

        m_profileBuffers = new ProfileBuffer[s.n_zeta];
        precomputeGroupSpeeds();
    }

    private static int pos_modulo(int n, int d) { return (n % d + d) % d; }

    private static float tau = 6.28318530718f; // https://tauday.com/tau-manifesto

    /** @brief Preform one time step.
     * @param dt time step
     * @param fullUpdate If true preform standard time step. If false, update only profile buffers.
     *
     * One time step consists of doing an advection step, diffusion step and
     * computation of profile buffers.
     *
     * To choose a reasonable dt we provide function @ref cflTimeStep()
     */
    void timeStep(float dt, boolean fullUpdate /*=true*/){
        if (fullUpdate) {
            advectionStep(dt);
            diffusionStep(dt);
        }
        precomputeProfileBuffers();
        m_time += dt;
    }

    void timeStep(float dt){ timeStep(dt, true);}

    /**
     * @brief Position and normal of water surface
     * @param pos Position where you want to know position and normal
     * @return A pair: position, normal
     *
     * Returned position is not only a vertical displacement but because we use
     * Gerstner waves we get also a horizontal displacement.
     */
    Pair<Vector3f, Vector3f> waterSurface(Vector2f pos) {
        Vector3f surface = new Vector3f();
        Vector3f tx      = new Vector3f();
        Vector3f ty      = new Vector3f();

        for (int izeta = 0; izeta < gridDim(Zeta); izeta++) {
            float  zeta    = idxToPos(izeta, Zeta);
            ProfileBuffer profile = m_profileBuffers[izeta];

            int  DIR_NUM = gridDim(Theta);
            int  N       = 4 * DIR_NUM;
            float da      = 1.0f / N;
            float dx      = DIR_NUM * tau / N;
            for (float a = 0; a < 1; a += da) {

                float angle  = a * tau;
                Vector2f kdir = new Vector2f((float)Math.cos(angle), (float)Math.sin(angle));
//                Real kdir_x = kdir * pos;
                float kdir_x = Vector2f.dot(kdir, pos);

                /*Vector4f wave_data =
                        dx * amplitude(new Vector4f(pos.x, pos.y, angle, zeta)) * profile.get(kdir_x);*/
                float factor = dx * amplitude(new Vector4f(pos.x, pos.y, angle, zeta));
                Vector4f wave_data = profile.get(kdir_x);
                wave_data.scale(factor);

                /*surface +=
                        Vec3{kdir[0] * wave_data[0], kdir[1] * wave_data[0], wave_data[1]};*/

                surface.x += kdir.x * wave_data.x;
                surface.y += kdir.y * wave_data.x;
                surface.z += wave_data.y;

                /*tx += kdir.x * Vec3{wave_data[2], 0, wave_data[3]};
                ty += kdir.y * Vec3{0, wave_data[2], wave_data[3]};*/

                tx.x += kdir.x * wave_data.z;
                tx.z += kdir.x * wave_data.w;

                ty.y += kdir.y * wave_data.z;
                ty.z += kdir.y * wave_data.w;
            }
        }

//        Vec3 normal = normalized(cross(tx, ty));
        Vector3f normal = Vector3f.cross(tx, ty, null);

//        return {surface, normal};
        return new Pair<>(surface, normal);
    }

    /**
     * @brief Time step based on CFL conditions
     *
     * Returns time in which the fastest waves move one grid cell across.
     * It is usefull for setting reasonable time step in @ref timeStep()
     */
    float cflTimeStep() {
        return Math.min(m_dx.getX(), m_dx.getY()) / groupSpeed(gridDim(Zeta) - 1);
    }

    /**
     * @brief Amplitude at a point
     * @param pos4 Position in physical coordinates
     * @return Returns interpolated amplitude at a given point
     *
     * The point pos4 has to be physical coordinates: i.e. in box
     * [-size,size]x[-size,size]x[0,2pi)x[min_zeta,max_zeta] @ref Settings
     * Default value(@ref defaultAmplitude()) is returned is point is outside of
     * this box.
     */
    float amplitude(ReadableVector4f pos4) {
        return interpolatedAmplitude().eval(pos4);
    }

    /**
     * @brief Amplitude value at a grid not
     * @param idx4 Node index to get amplitude at
     * @return Amplitude value at the give node.
     *
     * If idx4 is outside of discretized grid
     * {0,...,n_x-1}x{0,...,n_x-1}x{0,...,n_theta}x{0,...,n_zeta} then the default
     * value is returned,@ref defaultAmplitude().
     */
    float gridValue(int[] idx4) {
        return extendedGrid().eval(idx4[X], idx4[Y], idx4[Theta], idx4[Zeta]);
    }

    /**
     * @brief Wave trajectory
     * @param pos4 Starting location
     * @param length Trajectory length
     * @return Trajectory of a wave starting at position pos4.
     *
     * This method was used for debugin purposes, mainly checking that boudary
     * reflection works correctly.
     */
    StackFloat trajectory(Vector4f pos4, float length) {
        StackFloat trajectory = new StackFloat();

        for (float dist = 0; dist <= length;) {

            trajectory.push(pos4);

            Vector2f vel = groupVelocity(pos4);
            float dt  = dx(X) / /*norm(vel)*/vel.length();

            pos4.x += dt * vel.x;
            pos4.y += dt * vel.y;

            pos4 = boundaryReflection(pos4);

            dist += dt * /*norm(vel)*/vel.length();
        }
        trajectory.push(pos4);
        return trajectory;
    }

    /**
     * @brief Adds point disturbance to a point
     * @param pos Position of disturbance, in physical coordinates.
     * @val Strength of disturbance
     *
     * This function basically increases amplitude for all directions at a certain
     * point.
     */
    void addPointDisturbance(Vector2f pos, float val){
        // Find the closest point on the grid to the point `pos`
        int ix = posToIdx(pos.x, X);
        int iy = posToIdx(pos.y, Y);
        if (ix >= 0 && ix < gridDim(X) && iy >= 0 && iy < gridDim(Y)) {

            for (int itheta = 0; itheta < gridDim(Theta); itheta++) {
//                m_amplitude(ix, iy, itheta, 0) += val;
                m_amplitude.incre(ix, iy, itheta, 0, val);
            }
        }
    }

    /**
     * @brief Preforms advection step
     * @param dt Time for one step.
     */
    void advectionStep(float dt){
        AmplitudeInterpolate amplitude = interpolatedAmplitude();

//#pragma omp parallel for collapse(2)
        for (int ix = 0; ix < gridDim(X); ix++) {
            for (int iy = 0; iy < gridDim(Y); iy++) {

                Vector2f pos = nodePosition(ix, iy);

                // update only points in the domain
                if (m_enviroment.inDomain(pos.x, pos.y)) {

                    for (int itheta = 0; itheta < gridDim(Theta); itheta++) {
                        for (int izeta = 0; izeta < gridDim(Zeta); izeta++) {

                            Vector4f pos4 = idxToPos(new int[]{ix, iy, itheta, izeta});
                            Vector2f vel  = groupVelocity(pos4);

                            // Tracing back in Semi-Lagrangian
                            Vector4f trace_back_pos4 = pos4;
                            trace_back_pos4.x -= dt * vel.x;
                            trace_back_pos4.y -= dt * vel.y;

                            // Take care of boundaries
                            trace_back_pos4 = boundaryReflection(trace_back_pos4);

//                            m_newAmplitude(ix, iy, itheta, izeta) = amplitude(trace_back_pos4);
                            m_newAmplitude.set(ix, iy, itheta, izeta, amplitude(trace_back_pos4));
                        }
                    }
                }
            }
        }

//        std::swap(m_newAmplitude, m_amplitude);
        Grid tmp = m_newAmplitude;
        m_newAmplitude = m_amplitude;
        m_amplitude = tmp;
    }

    /**
     * @brief Preforms diffusion step
     * @param dt Time for one step.
     */
    void diffusionStep(float dt){
        Amplitude grid = extendedGrid();

//#pragma omp parallel for collapse(2)
        for (int ix = 0; ix < gridDim(X); ix++) {
            for (int iy = 0; iy < gridDim(Y); iy++) {

                float ls = m_enviroment.levelset(nodePosition(ix, iy));

                for (int itheta = 0; itheta < gridDim(Theta); itheta++) {
                    for (int izeta = 0; izeta < gridDim(Zeta); izeta++) {

                        Vector4f pos4  = idxToPos(new int[]{ix, iy, itheta, izeta});
                        float gamma = 2*0.025f * groupSpeed(izeta) * dt * m_idx.get(X);

                        // do diffusion only if you are 2 grid nodes away from boudnary
                        if (ls >= 4 * dx(X)) {
                            /*m_newAmplitude(ix, iy, itheta, izeta) =
                                    (1 - gamma) * grid(ix, iy, itheta, izeta) +
                                            gamma * 0.5 *
                                                    (grid(ix, iy, itheta + 1, izeta) +
                                                            grid(ix, iy, itheta - 1, izeta));*/
                            m_newAmplitude.set(ix, iy, itheta, izeta, (1 - gamma) * grid.eval(ix, iy, itheta, izeta) +
                                    gamma * 0.5f *
                                            (grid.eval(ix, iy, itheta + 1, izeta) +
                                                    grid.eval(ix, iy, itheta - 1, izeta)));
                        } else {
//                            m_newAmplitude(ix, iy, itheta, izeta) = grid(ix, iy, itheta, izeta);
                            m_newAmplitude.set(ix, iy, itheta, izeta, grid.eval(ix, iy, itheta, izeta));
                        }
                        // auto dispersion = [](int i) { return 1.0; };
                        // Real delta =
                        //     1e-5 * dt * pow(m_dx[3], 2) * dispersion(waveNumber(izeta));
                        // 0.5 * delta *
                        //     (m_amplitude(ix, iy, itheta, izeta + 1) +
                        //      m_amplitude(ix, iy, itheta, izeta + 1));
                    }
                }
            }
        }
//        std::swap(m_newAmplitude, m_amplitude);
        Grid tmp = m_newAmplitude;
        m_newAmplitude = m_amplitude;
        m_amplitude = tmp;
    }

    /**
     * @brief Precomputes profile buffers
     *
     * The "parameter" to this function is the internal time(@ref m_time) at which
     * the profile buffers are precomputed.
     */
    void precomputeProfileBuffers(){
        for (int izeta = 0; izeta < gridDim(Zeta); izeta++) {

            float zeta_min = idxToPos(izeta, Zeta) - 0.5f * dx(Zeta);
            float zeta_max = idxToPos(izeta, Zeta) + 0.5f * dx(Zeta);

            // define spectrum

            m_profileBuffers[izeta].precompute(m_spectrum, m_time, zeta_min, zeta_max, 4096, 2, 100);
        }
    }

    private interface Function{
        Vector2f eval(float x);
    }

    private static Vector2f integrate(int integration_nodes, double x_min, double x_max, Function fun) {

        float dx = (float) ((x_max - x_min) / integration_nodes);
        float x  = (float) (x_min + 0.5f * dx);

        Vector2f result =  fun.eval(x); // the first integration node
        result.scale(dx);
        for (int i = 1; i < integration_nodes; i++) { // proceed with other nodes, notice `int i= 1`
            x += dx;
//            result += dx * fun.eval(x);
            Vector2f.linear(result, fun.eval(x), dx, result);
        }

        return result;
    }

    /**
     * @brief Precomputed group speed.
     *
     * This basically computes the "expected group speed" for the currently chosen
     * wave spectrum.
     */
    void precomputeGroupSpeeds(){
        m_groupSpeeds = new float[gridDim(Zeta)];
        for (int izeta = 0; izeta < gridDim(Zeta); izeta++) {

            float zeta_min = idxToPos(izeta, Zeta) - 0.5f * dx(Zeta);
            float zeta_max = idxToPos(izeta, Zeta) + 0.5f * dx(Zeta);

            Vector2f result = integrate(100, zeta_min, zeta_max, (float zeta) -> {
                float waveLength = (float) Math.pow(2, zeta);
                float waveNumber = tau / waveLength;
                float cg         = (float) (0.5 * Math.sqrt(9.81 / waveNumber));
                float density    = (float) m_spectrum.get(zeta);
                return new Vector2f(cg * density, density);
            });

            m_groupSpeeds[izeta] =
                    3 /*the 3 should not be here !!!*/ * result.x / result.y;

        }
    }

    /**
     * @brief Boundary checking.
     * @param pos4 Point to be checked
     * @return If the intput point was inside of a boudary then this function
     * returns reflected point.
     *
     * If the point pos4 is not inside of the boundary then it is returned.
     *
     * If the point is inside of the boundary then it is reflected. This means
     * that the spatial position and also the wave direction is reflected w.t.r.
     * to the boundary.
     */
    Vector4f boundaryReflection(Vector4f pos4) {
        Vector2f pos = new Vector2f(pos4.x, pos4.y);
        float ls  = m_enviroment.levelset(pos.x, pos.y);
        if (ls >= 0) // no reflection is needed if point is in the domain
            return pos4;

        // Boundary normal is approximatex by the levelset gradient
        Vector2f n = m_enviroment.levelsetGrad(pos.x, pos.y);

        float theta = pos4.get(Theta);
        Vector2f kdir  = new Vector2f((float)Math.cos(theta), (float)Math.sin(theta));

        // Reflect point and wave-vector direction around boundary
        // Here we rely that `ls` is equal to the signed distance from the boundary
        /*pos  = pos - 2.0 * ls * n;
        kdir = kdir - 2.0 * (kdir * n) * n;*/
        Vector2f.linear(pos, n, - 2.0f * ls, pos);
        Vector2f.reflection(kdir, n, kdir);

        float reflected_theta = (float) Math.atan2(kdir.y, kdir.x);

        // We are assuming that after one reflection you are back in the domain. This
        // assumption is valid if you boundary is not so crazy.
        // This assert tests this assumption.
        assert(m_enviroment.inDomain(pos.x, pos.y));
        return new Vector4f(pos.x, pos.y, reflected_theta, pos4.get(Zeta));
    }

    interface Amplitude{
        float eval(int x, int y, int theta, int zeta);
    }

    /**
     * @brief Extends discrete grid with default values
     * @return Returns a function with signature(Int × Int × Int × Int -> Real).
     *
     * We store amplitudes on a 4D grid of the size (n_x*n_x*n_theta*n_zeta)(@ref
     * Settings). Sometimes it is usefull not to worry about grid bounds and get
     * default value for points outside of the grid or it wrapps arround for the
     * theta-coordinate.
     *
     * This function is doing exactly this, it returns a function which accepts
     * four integers and returns an amplitude. You do not have to worry about
     * the bounds.
     */
    Amplitude extendedGrid() {
        return (int ix, int iy, int itheta, int izeta) ->
        {
            // wrap arround for angle
            itheta = pos_modulo(itheta, gridDim(Theta));

            // return zero for wavenumber outside of a domain
            if (izeta < 0 || izeta >= gridDim(Zeta)) {
                return 0.0f;
            }

            // return a default value for points outside of the simulation box
            if (ix < 0 || ix >= gridDim(X) || iy < 0 || iy >= gridDim(Y)) {
                return defaultAmplitude(itheta, izeta);
            }

            // if the point is in the domain the return the actual value of the grid
            return m_amplitude.get(ix, iy, itheta, izeta);
        };
    }

    private float domain(int ix, int iy, int itheta, int izeta){
        Vector2f pos = nodePosition(ix, iy);
        return m_enviroment.inDomain(pos.x, pos.y) ? 1: 0;
    }

    interface AmplitudeInterpolate{
        float eval(ReadableVector4f pos);
    }

    /**
     * @brief Preforms linear interpolation on the grid
     * @return Returns a function(Vec4 -> float): accepting a point in physical
     * coordinates and returns interpolated amplitude.<p></p>
     *
     * This function preforms a linear interpolation on the computational grid in
     * all four coordinates. It returns a function accepting @ref Vec4 and returns
     * interpolated amplitude. Any input point is valid because the interpolation
     * is done on @ref extendedGrid().
     */
    AmplitudeInterpolate interpolatedAmplitude(){
        return (ReadableVector4f pos4)->
        {
            Vector4f ipos4 = posToGrid(pos4);
            Amplitude extended_grid = extendedGrid();

            int ix = (int)Math.floor(ipos4.x);
            float wx = ipos4.x - ix;

            int iy = (int)Math.floor(ipos4.y);
            float wy = ipos4.y - iy;

            int iz = (int)Math.floor(ipos4.z);
            float wz = ipos4.z - iz;

            int iw = Math.round(ipos4.w);

            float x0 = (wx != 0 ? wx * extended_grid.eval(ix + 1, iy, iz, iw) * domain(ix + 1, iy, iz, iw) : 0) +
                    (wx != 1 ? (1 - wx) * extended_grid.eval(ix, iy, iz, iw) * domain(ix, iy, iz, iw) : 0);

            float x1 = (wx != 0 ? wx * extended_grid.eval(ix + 1, iy+1, iz, iw) * domain(ix + 1, iy+1, iz, iw) : 0) +
                    (wx != 1 ? (1 - wx) * extended_grid.eval(ix, iy+1, iz, iw) * domain(ix, iy+1, iz, iw) : 0);

            float z0 = Numeric.mix(x0, x1, wy);

            x0 = (wx != 0 ? wx * extended_grid.eval(ix + 1, iy, iz + 1, iw) * domain(ix + 1, iy, iz + 1, iw) : 0) +
                    (wx != 1 ? (1 - wx) * extended_grid.eval(ix, iy, iz + 1, iw)*domain(ix, iy, iz + 1, iw) : 0);

            x1 = (wx != 0 ? wx * extended_grid.eval(ix + 1, iy+1, iz + 1, iw)*domain(ix + 1, iy+1, iz + 1, iw) : 0) +
                    (wx != 1 ? (1 - wx) * extended_grid.eval(ix, iy+1, iz + 1, iw)*domain(ix, iy+1, iz + 1, iw) : 0);

            float z1 = Numeric.mix(x0, x1, wy);

            float reuslt = Numeric.mix(z0, z1, wz);
            return reuslt;
        };
    }

    float idxToPos(int idx, int dim){
        return m_xmin.get(dim) + (idx + 0.5f) * m_dx.get(dim);
    }

    Vector4f idxToPos(int[] idx) {
        return new Vector4f(idxToPos(idx[X], X), idxToPos(idx[Y], Y),
                idxToPos(idx[Theta], Theta), idxToPos(idx[Zeta], Zeta));
    }

    float posToGrid(float pos, int dim) {
        return (pos - m_xmin.get(dim)) * m_idx.get(dim) - 0.5f;
    }
    Vector4f posToGrid(ReadableVector4f pos4) {
        return new Vector4f(posToGrid(pos4.getX(), X), posToGrid(pos4.getY(), Y),
                posToGrid(pos4.get(Theta), Theta), posToGrid(pos4.get(Zeta), Zeta));
    }

    int posToIdx(float pos, int dim) {
        return Math.round(posToGrid(pos, dim));
    }
    int[] posToIdx(ReadableVector4f pos4) {
        return new int[]{posToIdx(pos4.getX(), X), posToIdx(pos4.getY(), Y),
                posToIdx(pos4.get(Theta), Theta), posToIdx(pos4.get(Zeta), Zeta)};
    }

    Vector2f nodePosition(int ix, int iy) {
        return new Vector2f(idxToPos(ix, 0), idxToPos(iy, 1));
    }

    float waveLength(int izeta) {
        float zeta = idxToPos(izeta, Zeta);
        return (float) Math.pow(2, zeta);
    }

    float waveNumber(int izeta) {
        return tau / waveLength(izeta);
    }

    float dispersionRelation(float k) {
        float g = 9.81f;
        return (float) Math.sqrt(k * g);
    }
    float dispersionRelation(ReadableVector4f pos4) {
        float knum = waveNumber((int) pos4.get(Zeta));
        return dispersionRelation(knum);
    }

    float groupSpeed(int izeta) {
        return m_groupSpeeds[izeta];
    }
    // Real groupSpeed(Vec4 pos4) const;

    // Vec2 groupVelocity(int izeta) const;
    Vector2f groupVelocity(ReadableVector4f pos4) {
        int  izeta  = posToIdx(pos4.get(Zeta), Zeta);
        float cg    = groupSpeed(izeta);
        float theta = pos4.get(Theta);
        return  new Vector2f((float)Math.cos(theta) * cg, (float)Math.sin(theta) * cg);
    }

    float defaultAmplitude(int itheta, int izeta){
        if (itheta == 5 * gridDim(Theta) / 16)
            return 0.1f;
        return 0.0f;
    }

    int  gridDim(int dim) {
        return m_amplitude.dimension(dim);
    }

    float dx(int dim) { return m_dx.get(dim);}
}
