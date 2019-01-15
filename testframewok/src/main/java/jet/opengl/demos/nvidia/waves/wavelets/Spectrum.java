package jet.opengl.demos.nvidia.waves.wavelets;

final class Spectrum {
    double m_windSpeed = 1;

    Spectrum(double windSpeed){
        m_windSpeed = windSpeed;
    }

    /*
     * Maximal reasonable value of zeta to consider
     */
    double maxZeta() { return Math.log(10)/Math.log(2);}

    /*
     * Minamal resonable value of zeta to consider
     */
    double minZeta() { return Math.log(0.03)/ Math.log(2);}

    /*
     * Returns density of wave for given zeta(=log2(wavelength))
     */
    double get(double zeta){
        double A = Math.pow(1.1, 1.5 * zeta); // original pow(2, 1.5*zeta)
        double B = Math.exp(-1.8038897788076411 * Math.pow(4, zeta) / Math.pow(m_windSpeed, 4));
        return 0.139098 * Math.sqrt(A * B);
    }
}
