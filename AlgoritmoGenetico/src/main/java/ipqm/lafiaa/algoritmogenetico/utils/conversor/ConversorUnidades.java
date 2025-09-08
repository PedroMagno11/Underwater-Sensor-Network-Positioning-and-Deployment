package ipqm.lafiaa.algoritmogenetico.utils.conversor;

/**
 *
 * @author Vere Callidus
 */
public class ConversorUnidades {
 private ConversorUnidades() {
    }

    private static final double MN2JD = 2025.37182852143482064742;   // milhas náuticas para jardas
    private static final double JD2MN = 1.0 / ConversorUnidades.MN2JD; // jardas para milhas náuticas
    private static final double MN2M = 1852.0;                      // milhas náuticas para metros
    private static final double M2MN = 1.0 / ConversorUnidades.MN2M;  // metros para milhas náuticas
    private static final double M2JD = 1.09361;                        // metros para jardas
    private static final double JD2M = 0.9144; // jardas para metros
    private static final double MM2POL = 0.03937;
    private static final double POL2MM = 25.4;
    private static final double M2FT = 3.281; // metros para pés
    private static final double FT2M = 0.3048; // pés para metros
    private static final double MPS2KT = 1.9438444924574; // metros por segundo para nós
    private static final double KT2MPS = 0.514444444; //nós para metros por segundo
    private static final double FT2MN = 0.00016457883; // pés para milhas náuticas
    private static final double MN2FT = 1.0 / FT2MN; // pés para milhas náuticas
    private static final double KN2KMH = 1.852; // nós para quilômetros por hora
    private static final double KMH2KN = 0.540; // quilômetros por hora para noś
    private static final double KMH2MPS = 0.277778; // quilômetros por hora para metros por segundo
    private static final double MPS2KMH = 3.6; // metros por segundo para quilômetros por hora
    private static final double MN2KM = 1.852;   // milhas náuticas para quilômetros
    private static final double KM2MN = 0.539957;   // milhas náuticas para quilômetros
    private static final double KM2JD = 1093.613;   // jardas para quilômetros

    /**
     * Utilitário para converter milhas náuticas para jardas.
     *
     * @param milhas valor em milhas náuticas
     * @return valor em jardas.
     */
    public static double milhasNauticasParaJardas(double milhas) {
        return milhas * ConversorUnidades.MN2JD;
    }

    /**
     * Utilitário para converter jardas para milhas náuticas.
     *
     * @param jardas valor em jardas
     * @return valor em milhas náuticas
     */
    public static double jardasParaMilhasNauticas(double jardas) {
        return jardas * ConversorUnidades.JD2MN;
    }

    /**
     * Utilitário para converter milhas náuticas para metros.
     *
     * @param milhas valor em milhas náuticas
     * @return valor em metros
     */
    public static double milhasNauticasParaMetros(double milhas) {
        return milhas * ConversorUnidades.MN2M;
    }

    /**
     * Utilitário para converter metros para milhas náuticas.
     *
     * @param metros valor em metros.
     * @return valor em milhas nauticas.
     */
    public static double metrosParaMilhas(double metros) {
        return metros * ConversorUnidades.M2MN;
    }

    /**
     * Utilitário para converter metros para jardas.
     *
     * @param metros valor em metros.
     * @return valor em jardas.
     */
    public static double metrosParaJardas(double metros) {
        return metros * ConversorUnidades.M2JD;
    }

    /**
     * Utilitário para converter jardas para metros.
     *
     * @param jardas valor em jardas
     * @return valor em metros
     */
    public static double jardasParaMetros(double jardas) {
        return jardas * ConversorUnidades.JD2M;
    }

    /**
     * Utilitário para converter milímetros em polegadas.
     *
     * @param valorMm valor em mílimetros.
     * @return valor em polegadas.
     */
    public static float mmToPol(float valorMm) {
        return (float) (valorMm * MM2POL);
    }

    /**
     * Utilitário para converter polegadas para milímetros.
     *
     * @param valorPol valor em polegadas.
     * @return valor em milímetros.
     */
    public static float polToMm(float valorPol) {
        return (float) (valorPol * POL2MM);
    }

    /**
     * Utilitário para converter metros para pés.
     *
     * @param valorM valor em metros.
     * @return valor em pés.
     */
    public static float mToFeet(double valorM) {
        return (float) (valorM * M2FT);
    }

    /**
     * Utilitário para converter pés para metros.
     *
     * @param valorPes valor em pés.
     * @return valor em metros.
     */
    public static float feetToM(double valorPes) {
        return (float) (valorPes * FT2M);
    }

    public static double metrosPorSegundoParaNos(double valorMetrosPorSegundo) {
        return valorMetrosPorSegundo * MPS2KT;
    }

    public static double nosParaMetrosPorSegundo(double valorNos) {
        return valorNos * KT2MPS;
    }

    /**
     * Utilitário para converter pés para milhas náuticas.
     *
     * @param valorPes valor em pés.
     * @return valor em milhas náuticas.
     */
    public static double feetToMN(double valorPes) {
        return valorPes * FT2MN;
    }

    /**
     * Utilitário para converter milhas náuticas para pés.
     *
     * @param valorMN valor em milhas náuticas.
     * @return valor em pés.
     */
    public static double MNToFeet(double valorMN) {
        return valorMN * MN2FT;
    }

    /**
     * Utilitário para converter nós em quilômetros por hora.
     *
     * @param valorKN valor em nós.
     * @return valor em quilômetros por hora.
     */
    public static double nosParaQuilometrosHora(double valorKN) {
        return valorKN * KN2KMH;
    }

    /**
     * Utilitário para converter quilômetros por hora para nós.
     *
     * @param valorKMH valor em quilômetro por hora.
     * @return valor em nós.
     */
    public static double quilometrosHoraParaNos(double valorKMH) {
        return valorKMH * KMH2KN;
    }

    /**
     * Utilitário para converter quilômetros por hora para metros por segundo.
     *
     * @param valorKMH valor em quilômetro por hora.
     * @return valor em metros por segundo.
     */
    public static double quilometrosHoraParaMetrosPorSegundo(double valorKMH) {
        return valorKMH * KMH2MPS;
    }

    /**
     * Utilitário para converter metros por segundo para quilômetros por hora.
     *
     * @param valorMPS valor em metros por segundo.
     * @return valor em quilometros por hora.
     */
    public static double metrosPorSegundoParaQuilometrosHora(double valorMPS) {
        return valorMPS * MPS2KMH;
    }

    /**
     * Utilitário para converter milhas náuticas para quilômetros.
     *
     * @param valorMN valor em milhas náuticas.
     * @return valor em quilometros.
     */
    public static double milhasNauticasParaQuilometros(double valorMN) {
        return valorMN * MN2KM;
    }

    /**
     * Utilitário para converter de quilômtros para milhas náuticas.
     *
     * @param valorKM valor em quilometros.
     * @return valor em milhas náuticas.
     */
    public static double quilometrosParaMilhasNauticas(double valorKM) {
        return valorKM * KM2MN;
    }

    /**
     * Utilitário para converter de quilômtros para jardas.
     *
     * @param valorKM valor em quilometros.
     * @return valor em jardas.
     */
    public static double quilometrosParaJardas(double valorKM) {
        return valorKM * KM2JD;
    }

    /**
     * Utilitário para converter de segundos por rotação para rotações por
     * minuto
     *
     * @param valor em spr
     * @return valor em rpm
     */
    public static double sprParaRpm(double valor) {
        return 1 / valor * 60;
    }

    /**
     * Utilitário para converter de rotações por minuto para segundos por  
     * rotação
     *
     * @param valor em rpm
     * @return valor em spr
     */
    public static double rpmParaSpr(double valor) {
        return 1 / (valor / 60);
    }   
}
