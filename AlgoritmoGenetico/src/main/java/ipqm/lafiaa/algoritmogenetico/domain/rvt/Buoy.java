package ipqm.lafiaa.algoritmogenetico.domain.rvt;

import ipqm.lafiaa.algoritmogenetico.domain.config.Parametros;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Classe que contém as informações de posição, detecção e estado da boia
 *
 * @author José Gomes de Carvalho Jr.
 */
public class Buoy {

    /**
     * @return the latGeo
     */
    public double getLatGeo() {
        return latGeo;
    }

    /**
     * @param latGeo the latGeo to set
     */
    public void setLatGeo(double latGeo) {
        this.latGeo = latGeo;
    }

    /**
     * @return the LonGeo
     */
    public double getLonGeo() {
        return LonGeo;
    }

    /**
     * @param LonGeo the LonGeo to set
     */
    public void setLonGeo(double LonGeo) {
        this.LonGeo = LonGeo;
    }

    private String nome;  // Nome da boia (recebido nas mensagens de cada boia)
    private int posX = 0; // Posição x da boia dentro da raia
    private int posY = 0; // Posição y da boia dentro da raia 
    private long tempoDeteccao; // Instante em que o splash foi detectado
    private boolean ativada; // Informação se a boia está ativada ou não
    private double latGeo = 0.0; // Latitude da boia coordenada geografica DG
    private double LonGeo = 0.0; // Longitude da boia coordenada geografica DG
    private LocalDateTime timestamp; // data e hora do recebimento do sinal
    /**
     * Cria a boia inicialmente desativada e sem um nome
     *
     */
    public Buoy() {
        nome = "";
        ativada = false;
    }

    public Buoy(String nome, int posX, int posY){
        this.nome = nome;
        this.posX = posX;
        this.posY = posY;
        ativada = false;
    }

    /**
     * Retorna a posição x da boia na raia
     *
     * @return - x da posição da boia na raia
     */
    public int getPosX() {
        return posX;
    }

    /**
     * Retorna a posição y da boia na raia
     *
     * @return - y da posição da boia na raia
     */
    public int getPosY() {
        return posY;
    }

    /**
     * Ajusta a posição x da boia na raia
     *
     * @param x - posição x da boia na raia
     * @throws java.lang.Exception caso a posição x não esteja dentro da raia
     */
    public void setPosX(int x) throws Exception {
        if ((x >= 0) && (x < Parametros.DIMENSAO_RAIA)) {
            posX = x;
        } else {
            throw new Exception("Dimensão X da boia fora da raia (x=" + x + ").");
        }
    }

    /**
     * Ajusta a posição y da boia na raia
     *
     * @param y - posição y da boia na raia
     * @throws java.lang.Exception caso a posição y não esteja dentro da raia
     */
    public void setPosY(int y) throws Exception {
        if ((y >= 0) && (y < Parametros.DIMENSAO_RAIA)) {
            posY = y;
        } else {
            throw new Exception("Dimensão Y da boia fora da raia.");
        }
    }

    /**
     * Retorna o nome da boia
     *
     * @return - nome da boia
     */
    public String getNome() {
        return nome;
    }

    /**
     * Altera o nome da boia
     *
     * @param nome - nome da boia
     */
    public void setNome(String nome) {
        this.nome = nome;
    }

    /**
     * Retorna o estado da boia ("ativada":true ou "não ativada":false)
     *
     * @return - estado da ativação (true ou false)
     */
    public boolean getAtivada() {
        return ativada;
    }

    /**
     * Altera o estado da boia (ativa ou desativa)
     *
     * @param ativada - ativa (true) ou desativa (false) a boia
     */
    public void setAtivada(boolean ativada) {
        this.ativada = ativada;
    }

    /**
     * Retorna o tempo de detecção da boia.
     *
     * @return - tempo de detecção salvo para a boia
     */
    public long getTempoDeteccao() {
        return tempoDeteccao;
    }

    /**
     * Ajusta o tempo de detecção da boia.
     *
     * @param tempoDeteccao - tempo de detecção enviado pela mensagem da boia
     */
    public void setTempoDeteccao(long tempoDeteccao) {
        this.tempoDeteccao = tempoDeteccao;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }


    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(obj == null || getClass() != obj.getClass()) return false;
        Buoy buoy = (Buoy) obj;
        return Objects.equals(nome, buoy.nome);
    }

    @Override
    public String toString() {
    return "NAME: " + nome + " POS X: " + posX + " POS Y: " + posY + " LAT: " + latGeo + " LNG: " + LonGeo + " TEMPO_DETECCAO: " + tempoDeteccao;
    }
}
