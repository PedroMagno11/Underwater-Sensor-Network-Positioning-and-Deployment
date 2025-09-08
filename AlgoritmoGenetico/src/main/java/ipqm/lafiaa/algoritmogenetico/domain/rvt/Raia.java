package ipqm.lafiaa.algoritmogenetico.domain.rvt;

import ipqm.lafiaa.algoritmogenetico.utils.PropertyReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Classe da raia de tiro, que é um grid quadrado 1000x1000 em coordenadas
 * cartesianas, composto por "pontos" [X;Y]. O centro da raia é a posição
 * [500;500]), que é considerado como o ponto em que está posicionado o alvo a
 * ser atingido. As boias devem estar dentro da raia e suas posições na raia, no
 * momento da detecção do impacto, são consideradas no cálculo do ponto de
 * impacto. O método de cálculo do ponto de queda do projetil terá sucesso a
 * partir da existência de pelo menos três boias ativas dentro da raia,
 * retornando as coordenadas cartesianas do mais provavél ponto de queda do
 * projetil a partir dos tempos de detecção recebidos das boias Caso não haja
 * pelo menos três boias ativas o método retorna uma exceção indicando que não
 * foi possível executar o cálculo. A classe Raia é um Singleton (o que
 * significa que só pode haver uma raia criada em cada execução). O cliente não
 * pode comandar a iniciação da raia, pois o construtor é privado e é chamado
 * apenas pelo método estático getRaia() que deve ser chamado pelo cliente para
 * adquirir a referência para a raia que foi anteriormente criada ou que será
 * criada nessa chamada. Os demais métodos públicos da classe são: 1) void
 * atualizarBoia (String nome, int x, int y, long tempoDeteccao) que é chamado
 * para criar ou atualizar uma boia na raia, e 2) CoordenadaCartesiana
 * calcularPontoQueda () que retorna o ponto de queda mais provavel
 *
 * @author José Gomes de Carvalho Jr
 * @version 1.0
 * @since Release 02 da aplicação
 */
public class Raia {
    
    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());


    /**
     * @return the arrayBoias
     */
    public Buoy[] getArrayBoias() {
        return arrayBuoys;
    }

    /**
     * @param arrayBuoys the arrayBoias to set
     */
    public void setArrayBoias(Buoy[] arrayBuoys) {
        this.arrayBuoys = arrayBuoys;
    }

    /**
     * Quantidade máxima de boias na raia
     */
    public static final int MAXBOIAS = 5;

    /**
     * Largura da raia em pontos de resolução da raia
     */
    public static int DIMMAX;

    /**
     * Resolução de cada ponto da raia em metros
     */
    public static int RESOLUCAOGRID;

    /**
     * Largura total da raia em metros
     */
    public static int LARGURAGRID;

    /**
     * Velocidade do som na água do mar em m/ms
     */
    public static double VELOCSOM;

    /**
     * Quantidade mínima de boias para executar o cálculo
     */
    public static final int QUANTMINBOIAS = 3;

    /**
     * Classe auxiliar que define um vetor de valores que armazenam uma...
     * ...informação genérica real de dupla precisão para cada uma das boias.
     * Atributo: double [] valores; valores quaisquer a serem guardados para
     * cada boia
     */
    private class VetBoia {

        private double[] valores;

        /**
         * O construtor define o tamanho do vetor de acordo com a quantidade de
         * boias que é passada como parâmetro.
         *
         * @param quantBoias - quantidade de boias para dimensionar o vetor
         */
        public VetBoia(int quantBoias) {
            valores = new double[quantBoias];  // cria o vetor
        }

        /**
         * Método para gravar um valor genérico no vetor de boias
         *
         * @param boia - indice da boia no vetor de boias
         * @param valor - valor genérico a ser guardado no vetor de boias
         */
        public void setValores(int boia, double valor) {
            if ((boia >= 0) && (boia < valores.length)) {
                valores[boia] = valor;
            }
        }

        /**
         * Método para ler um valor genérico no vetor de boias
         *
         * @param boia
         * @return
         */
        public double getValores(int boia) {
            if ((boia >= 0) && (boia < valores.length)) {
                return valores[boia];
            } else {
                return 0.00;
            }
        }
    }
    /**
     * Atributos:
     */
    private Buoy[] arrayBuoys;        // Vetor com as boias existentes da raia

    private VetBoia[][] distBoia;// Para cada ponto da raia há um vetor contendo as
    // distâncias deste ponto a cada uma das boias.

    private VetBoia[][] tempoBoia; // Para cada ponto da raia há um vetor contendo 
    // os tempos deste ponto a cada uma das boias.

    private VetBoia[][] erroTempoBoia; // Para cada ponto da raia há um vetor com 
    // as diferenças entre os tempos deste
    // ponto e os tempos de cada uma das boias.

//private double [][] varianciaErros; // Para cada ponto da raia guarda a 
    // variância dos valores no vetor 
    // de erroTempoBoia.
    private double[][] custo; // Para cada ponto da raia há um valor   
    // representativo do "custo" daquele ponto, que é 
    // proporcional à diferença entre os tempos reais 
    // medidos para as boias e o tempo teórico que 
    // levaria deste ponto à boia.  

    private static Raia raia;   // Referência para a raia única criada

    /**
     * O Construtor cria uma raia Singleton ao ser chamado pelo método
     * getRaia().
     */
    private Raia() {
        reiniciarRaia();
    }

    public void reiniciarRaia(){
       try{
            DIMMAX = Integer.parseInt(PropertyReader.getProp().getProperty("dimensao_maxima"));

            /**
             * Resolução de cada ponto da raia em metros
             */
            RESOLUCAOGRID = Integer.parseInt(PropertyReader.getProp().getProperty("resolucao_do_grid"));

            /**
             * Largura total da raia em metros
             */
            LARGURAGRID = DIMMAX * RESOLUCAOGRID;

            /**
             * Velocidade do som na água do mar em m/ms
             */
            VELOCSOM = Double.parseDouble(PropertyReader.getProp().getProperty("velocidade_do_som"));
            /**
             * Cria o vetor para guardar as boias possíveis de existirem na raia
             * e, em seguida, cria as boias e as guarda no vetor:
             */
            arrayBuoys = new Buoy[MAXBOIAS];
            for (int i = 0; i < arrayBuoys.length; i++) {
                arrayBuoys[i] = new Buoy();
            }

            /**
             * Cria as matrizes da dimensão da raia para armazenar as
             * distâncias, os tempos e os erros de cada ponto da raia para cada
             * uma das boias:
             */
            distBoia = new VetBoia[DIMMAX][DIMMAX];
            tempoBoia = new VetBoia[DIMMAX][DIMMAX];
            erroTempoBoia = new VetBoia[DIMMAX][DIMMAX];
            //varianciaErros  = new double  [DIMMAX] [DIMMAX];
            custo = new double[DIMMAX][DIMMAX];

            /**
             * Cria vetores para guardar as distâncias, os tempos e os erros de
             * cada ponto da raia para cada uma das boias:
             */
            for (int x = 0; x < DIMMAX; x++) {
                for (int y = 0; y < DIMMAX; y++) {
                    distBoia[x][y] = new VetBoia(MAXBOIAS);
                    tempoBoia[x][y] = new VetBoia(MAXBOIAS);
                    erroTempoBoia[x][y] = new VetBoia(MAXBOIAS);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Ocorreu algum erro na criação da raia");
        }
    }
    
    /**
     * Cria uma raia Singleton caso não haja nenhuma ou retorna uma já criada
     *
     * @return - referência para a raia Singleton criada
     */
    public static Raia getRaia() {
        if (raia == null) {
            raia = new Raia(); //Cria a raia propriamente dita
        }
        return raia;
    }

    public void rearmarBoia(String nome) {
        for (Buoy buoy : getArrayBoias()) {
            if (buoy.getNome().equals(nome)) {
                buoy.setAtivada(true);
            }
        }
    }

    /**
     * Atualiza a posição e o tempo de uma boia referenciada pelo seu nome. Caso
     * uma boia com esse nome ainda não exista, cria e atualiza a mesma.
     *
     * @param nome - Nome da boia que se deseja atualizar ou criar
     * @param x - posição X em coordenadas cartesianas na raia de 1000 x 1000
     * pontos
     * @param y - posição Y em coordenadas cartesianas na raia de 1000 x 1000
     * pontos
     * @param lat
     * @param lon
     * @param tempoDeteccao - tempo absoluto em que a boia detectou o splash
     * @throws Exception - caso a boia não exista e já haja cinco boias
     * criadas... ...ou caso a posição informada esteja fora da raia.
     */
    public void atualizarBoia(String nome,
            int x, int y,
            double lat, double lon,
            long tempoDeteccao) throws Exception {

        boolean achou = false;
        boolean criou = false;
        try {
            for (Buoy buoy : getArrayBoias()) {
                if (buoy.getNome().equals(nome)) {
                    buoy.setPosX(x);
                    buoy.setPosY(y);
                    buoy.setLatGeo(lat);
                    buoy.setLonGeo(lon);
                    buoy.setTempoDeteccao(tempoDeteccao);
                    buoy.setAtivada(true);
                    achou = true;
                }
            }
            if (!achou) {
                for (int i = 0; (i < getArrayBoias().length) && (!criou); i++) {
                    if (!(arrayBuoys[i].getAtivada())) {
                        getArrayBoias()[i].setNome(nome);
                        getArrayBoias()[i].setPosX(x);
                        getArrayBoias()[i].setPosY(y);
                        getArrayBoias()[i].setLatGeo(lat);
                        getArrayBoias()[i].setLonGeo(lon);
                        getArrayBoias()[i].setTempoDeteccao(tempoDeteccao);
                        getArrayBoias()[i].setAtivada(true);
                        criou = true;
                    }
                }
            }
            if (!achou && !criou) {
                throw new Exception("Existem cinco boias criadas e nenhuma tem o nome '" + nome + "'.");
            }
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * Calcula a posiçao da raia mais provável para a queda do projetil.
     *
     * @return CoordenadaCartesiana do ponto de queda do projetil caso haja pelo
     * menos três boias ativas para possibilitar a execução dos cálculos.
     *
     * @throws Exception - caso não haja ao menos três boias ativas.
     */
    public PontoQueda calcularPontoQueda() throws Exception {

        double distancia;  // Distância do ponto sendo correntemente calculado
        double tempo;      // Tempo do ponto sendo correntemente calculado 
        double menorCusto; // Menor custo encontrado até o momento
        int numBoiasAtivas = 0; // Quantidade de boias ativas (mínimo são 3)
        int quantMinimos = 0;   // Pode haver mais de um ponto de mínimo na raia
        CoordenadaCartesianaRVT pontoQueda; // Valor de retorno calculado no método
        long menorTempo = 0;  // Tempo da Boia Base (primeira boia a enviar tempo)
        int indBoiaBase = 0;    // Índice da Boia Base no vetor de boias 

        try {
            /**
             * Antes de fazer a busca pelo ponto de menor custo, verifica se há
             * um mínimo de boias ativas (que a quantidade mínima para haver
             * precisão aceitável). Caso não haja o mínimo aceitável de boias
             * ativas, gera uma exceção informando quantas boias estão ativas:
             */
            for (Buoy buoy : getArrayBoias()) {
                if (buoy.getAtivada()) {
                    numBoiasAtivas++;
                    menorTempo = buoy.getTempoDeteccao();//Inicia com um dos valores
                }
            }
            if (numBoiasAtivas < QUANTMINBOIAS) {
                String erro;
                /**
                 * Como não será calculado, desativa as bóias não utilizadas
                 */
                for (Buoy buoy : getArrayBoias()) {
                    buoy.setAtivada(false);
                }
                erro = String.format("%s%d%s", "Cálculo inválido, pois só há ",
                        numBoiasAtivas, " boias ativas.");
                LOGGER.error(erro, this);
                throw new Exception(erro);
            }

            /**
             * A seguir calcula qual é a "Boia Base", ou seja a boia que
             * detectou primeiramente o ruído do splash. O tempo da boia base
             * será descontado do tempo das demais boias tornando o tempo dessa
             * boia a base de referência para cálculo dos tempos das boias e
             * ocasionando o descarte da posição da boia base (pois essa passa a
             * ter tempo zero). Isso é necessário pois não sabemos o tempo
             * absoluto do splash para que o mesmo possa ser usado como
             * referencia.
             */
            for (int i = 0; i < getArrayBoias().length; i++) {
                if (getArrayBoias()[i].getAtivada()) {
                    if (menorTempo >= getArrayBoias()[i].getTempoDeteccao()) {
                        menorTempo = getArrayBoias()[i].getTempoDeteccao();
                        indBoiaBase = i;
                    }
                }
            }
            //boias[indBoiaBase].setAtivada(false);  // desativa a boia base
            for (Buoy buoy : getArrayBoias()) {
                if (buoy.getAtivada()) {
                    buoy.setTempoDeteccao(buoy.getTempoDeteccao() - menorTempo);
                }
            }

            /**
             * Calcula as distâncias e os tempos teóricos de cada ponto da raia
             * para cada uma das boias usando as últimas posições informadas
             * para cada boia ativa.
             */
            for (int x = 0; x < DIMMAX; x++) {
                for (int y = 0; y < DIMMAX; y++) {
                    for (int b = 0; b < getArrayBoias().length; b++) {
                        if (getArrayBoias()[b].getAtivada()) {
                            distancia = Math.sqrt(Math.pow((getArrayBoias()[b].getPosX() * RESOLUCAOGRID - x * RESOLUCAOGRID), 2) + Math.pow((getArrayBoias()[b].getPosY() * RESOLUCAOGRID - y * RESOLUCAOGRID), 2));
                            
                            /**
                             * O tempo deve ser utilizado em milisegundos, mas a
                             * constante VELOCSOM já está em m/ms
                             */
                            tempo = (distancia / VELOCSOM);
                            distBoia[x][y].setValores(b, distancia);
                            tempoBoia[x][y].setValores(b, tempo);

                            /**
                             * A seguir calcula o erro de cada ponto da raia
                             * como sendo a diferenças entre o tempo teórico de
                             * cada ponto do grid às boias e o tempo informado
                             * por cada uma das boias ativas (descontado do
                             * tempo da boia base):
                             */
                            erroTempoBoia[x][y].setValores(b,
                                    (tempo - getArrayBoias()[b].getTempoDeteccao()));
                        }
                    }
                }
            }

            /**
             * A seguir calcula o "custo" de cada ponto da raia como sendo a
             * variância dos erros desse ponto a cada boia. Essa será a medida
             * de similaridade empregada, pois as diferenças entre o tempo que
             * deveria levar do ponto a cada boia e o tempo efetivo informado
             * por cada boia deveriam ser muito semelhantes, pois representam a
             * parte desconhecida do tempo de cada boia que inclui o tempo
             * (desconhecido) do instante do splash até alcançar a Boia Base, ou
             * seja, a VARIÂNCIA desse valor de tempo desconhecido deve ser
             * mínima para os pontos da grade exatamente no ponto de splash:
             */
            double somatorioErros;// Somatório dos erros do ponto para cada boia
            double mediaErros;    // Média dos dos erros do ponto
            double varianciaErros;// Variância dos erros do ponto para cada boia
            double desvioPadraoErros;// Desvio Padrão dos erros 

            for (int x = 0; x < DIMMAX; x++) {
                for (int y = 0; y < DIMMAX; y++) {
                    somatorioErros = 0; //[x][y] = 0; 
                    for (int b = 0; b < getArrayBoias().length; b++) {
                        if (getArrayBoias()[b].getAtivada()) {
                            somatorioErros
                                    += Math.abs(erroTempoBoia[x][y].getValores(b));
                        }
                    }
                    mediaErros = somatorioErros / numBoiasAtivas;
                    // Cálculo da variância:
                    double quadrDif = 0;
                    for (int b = 0; b < getArrayBoias().length; b++) {
                        if (getArrayBoias()[b].getAtivada()) {
                            quadrDif += Math.pow((erroTempoBoia[x][y].getValores(b)
                                    - mediaErros), 2);
                        }
                    }// aplicando a fórmula de variância populacional: 
                    varianciaErros = quadrDif / numBoiasAtivas;
                    desvioPadraoErros = Math.sqrt(varianciaErros);
                    custo[x][y] = varianciaErros;
                }
            }

            /**
             * Por fim, executa a busca pelo ponto de menor custo iniciando com
             * o ponto do centro da raia, haja vista que esse é o ponto esperado
             * para queda do projetil (alvo) e que deveria ser o de menor custo
             * real (caso o alvo tenha sido atingido).
             */
            double custoPonto; //custo do ponto sendo analisado 
            pontoQueda = new CoordenadaCartesianaRVT(500, 500);
            menorCusto = custo[500][500];

            /**
             * Realiza a busca por toda a raia do ponto com o menor custo:
             */
            for (int x = 0; x < DIMMAX; x++) {         // Percorre dimensão X da raia
                for (int y = 0; y < DIMMAX; y++) {     // Percorre a dimensão y da raia
                    // Pega o custo do próximo ponto 
                    if (custo[x][y] < menorCusto) {  // e atualiza se for menor 
                        menorCusto = custo[x][y];   // que o melhor já encontrado.
                        quantMinimos = 1;         //Anota que achou só um mínimo 
                        pontoQueda.setX(x);     // e as coordenadas do    
                        pontoQueda.setY(y);     // ponto que é o novo mínimo.                    
                    } else if (custo[x][y] == menorCusto) {//  Se custo for igual ao                        
                        quantMinimos++;       // mínimo, incrementa quantidade.
                        pontoQueda.setX(x);   //   Guarda as coordenadas do     
                        pontoQueda.setY(y);   // novo mínimo encontrado.
                    }
                }
            }
        } catch (Exception e) {
            throw e;
        } finally {
            /**
             * Executando ou não o cálculo, desativa todas as boias. Um novo
             * cálculo necessita que novas posições de cada boia sejam recebidas
             * para... pelo menos três boias: .
             */
            for (Buoy buoy : getArrayBoias()) {
                buoy.setAtivada(false);
            }
        }
        
        return new PontoQueda(menorCusto, pontoQueda);
    }

}
