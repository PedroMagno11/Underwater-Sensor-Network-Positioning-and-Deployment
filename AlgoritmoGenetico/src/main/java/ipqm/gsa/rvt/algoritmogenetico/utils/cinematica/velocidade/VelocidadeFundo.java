package ipqm.gsa.rvt.algoritmogenetico.utils.cinematica.velocidade;


import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Classe que representa a velocidade de fundo em termos de pontos cartesianos
 * (vetor velocidade). A unidade adotada é as milhas náuticas por hora (Nós).
 *
 * @author Pablo Rangel
 * @since 11/03/2011
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VelocidadeFundo extends TipoVelocidade {

    /**
     * método responsável por calcular a velocidade de fundo do veículo
     * @param velocidadeVeiculo
     * @param velocidadeCorrente
     * @param direcao
     * @return 
     */
    public static double calcularVelocidadeFundo(double velocidadeVeiculo, int velocidadeCorrente, double direcao) {
        double direcaoRadianos = Math.toRadians(direcao);
        return Math.sqrt( Math.pow(velocidadeCorrente, 2) + Math.pow(velocidadeVeiculo , 2) + (2 * velocidadeCorrente * velocidadeVeiculo * Math.cos(direcaoRadianos)) );
    }

}
