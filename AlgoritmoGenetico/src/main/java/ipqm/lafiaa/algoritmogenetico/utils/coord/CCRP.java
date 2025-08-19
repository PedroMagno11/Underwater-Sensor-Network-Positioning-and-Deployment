package ipqm.gsa.rvt.algoritmogenetico.utils.coord;


/**
 * Common Consistent Reference Point Converter. Permite converter a posição de
 * um ponto qualquer, descrito em coordenadas geográficas, para uma determinada
 * posição de referência dentro do navio. Utilizado para transformar todas as
 * posições, marcações e distâncias proveniente de sensores (GPS, radar/ARPA),
 * cujas antenas podem estar posicionadas em pontos bem distintos, para um
 * sistema de referência único dentro do navio (CCRP).
 *
 * @author Marcelo Medeiros <medeiros@ipqm.mar.mil.br>
 */
public class CCRP {

    private double ccrpX = 0.0;
    private double ccrpY = 0.0;
    private final double sensorX;
    private final double sensorY;

    /**
     * Cria um conversor de coordenadas para um sensor particular. Deve ser
     * definido um sistema cartesiano local de coordenadas para especificar a
     * posição do sensor. Esse sistema normalmente é definido pelos eixos
     * principais do navio (distância até a proa, popa, boreste e bombordo).
     *
     * @param ccrpX posição X (metros) do CCRP no sistema de referência
     * @param ccrpY posição Y (metros) do CCRP no sistema de referência
     * @param sensorX posição X (metros) do sensor no sistema de referência
     * @param sensorY posição Y (metros) do sensor no sistema de referência
     */
    public CCRP(double ccrpX, double ccrpY, double sensorX, double sensorY) {
        this.ccrpX = ccrpX;
        this.ccrpY = ccrpY;
        this.sensorX = sensorX;
        this.sensorY = sensorY;
    }

    /**
     * Cria um conversor de coordenadas para um sensor particular. Deve ser
     * definido um sistema cartesiano local de coordenadas para especificar a
     * posição do sensor. Esse sistema normalmente é definido pelos eixos
     * principais do navio (distância até a proa, popa, boreste e bombordo). O
     * CCRP está localizado na posição padrão (0,0).
     *
     * @param sensorX posição X (metros) do sensor no sistema de referência
     * @param sensorY posição Y (metros) do sensor no sistema de referência
     */
    public CCRP(double sensorX, double sensorY) {
        this(0.0, 0.0, sensorX, sensorY);
    }

    /**
     * Define a posição do ponto de referência no sistema local de referência.
     * Normalmente o ponto de referência está localizado no centro do sistema
     * local de coordenadas (0,0).
     *
     * @param ccrpX posição X (metros) do CCRP no sistema de referência
     * @param ccrpY posição Y (metros) do CCRP no sistema de referência
     */
    public void setCCRP(double ccrpX, double ccrpY) {
        this.ccrpX = ccrpX;
        this.ccrpY = ccrpY;
    }

    /**
     * Define a posição X do CCRP.
     *
     * @param ccrpX posição X (metros) do CCRP no sistema de referência local
     */
    public void setCCRPX(double ccrpX) {
        this.ccrpX = ccrpX;
    }

    /**
     * Define a posição Y do CCRP.
     *
     * @param ccrpY posição Y (metros) do CCRP no sistema de referência local
     */
    public void setCCRPY(double ccrpY) {
        this.ccrpY = ccrpY;
    }

    /**
     * Retorna a posição X do CCRP.
     *
     * @return posição X (metros) do CCRP no sistema de referência local
     */
    public double getCCRPX() {
        return ccrpX;
    }

    /**
     * Retorna a posição Y do CCRP.
     *
     * @return posição Y (metros) do CCRP no sistema de referência local
     */
    public double getCCRPY() {
        return ccrpY;
    }

    /**
     * Move um determinado ponto proveniente de um sensor para o CCRP. Este
     * método é utilizado para, por exemplo, converter a posição informada pelo
     * sensor GPS (cuja antena pode estar localizada em qualquer ponto do navio)
     * para o sistema de referência único do navio (CCRP).
     *
     * @param longitude longitude do ponto desejado
     * @param latitude latitude do ponto desejado
     * @param heading rumo de superfície do navio
     * @return posição do sensor no CCRP
     */
    public GeoCoord moveTo(double longitude, double latitude, double heading) {
        return move(longitude, latitude, heading + 180.0);
    }

    /**
     * Move um determinado ponto proveniente de um sensor para o CCRP. Este
     * método é utilizado para, por exemplo, converter a posição informada pelo
     * sensor GPS (cuja antena pode estar localizada em qualquer ponto do navio)
     * para o sistema de referência único do navio (CCRP).
     *
     * @param p posição do ponto desejado
     * @param heading rumo de superfície do navio
     * @return posição do sensor no CCRP
     */
    public GeoCoord moveTo(GeoCoord p, double heading) {
        return moveTo(p.getLongitude(), p.getLatitude(), heading);
    }

    /**
     * Move a posição do CCRP para a posição do sensor. Este método é utilizado
     * para encontrar a posição geográfica do sensor baseado na posição
     * geográfica do CCRP (que deve ser conhecida).
     *
     * @param longitude longitude do CCRP
     * @param latitude latitude do CCRP
     * @param heading rumo de superfície do navio
     * @return posição do sensor
     */
    public GeoCoord moveFrom(double longitude, double latitude, double heading) {
        return move(longitude, latitude, heading);
    }

    /**
     * Move a posição do CCRP para a posição do sensor. Este método é utilizado
     * para encontrar a posição geográfica do sensor baseado na posição
     * geográfica do CCRP (que deve ser conhecida).
     *
     * @param p posição geográfica do CCRP
     * @param heading rumo de superfície do navio
     * @return posição do sensor
     */
    public GeoCoord moveFrom(GeoCoord p, double heading) {
        return moveFrom(p.getLongitude(), p.getLatitude(), heading);
    }
    
    private GeoCoord move(double longitude, double latitude, double delta) {
        double dX = (this.sensorX - this.ccrpX) / 1852.0;
        double dY = (this.sensorY - this.ccrpY) / 1852.0;
        double distance = Math.sqrt(dX * dX + dY * dY);
        double bearing = Math.toDegrees(Math.atan2(dX, dY)) + delta;
        if (bearing > 360.0) {
            bearing -= 360.0;
        }
        return Geodesics.destination(longitude, latitude, bearing, distance);
    }
}
