package ipqm.gsa.rvt.algoritmogenetico;

import ipqm.gsa.rvt.algoritmogenetico.algoritmoGenetico.GeneticAlgorithm;
import ipqm.gsa.rvt.algoritmogenetico.algoritmoGenetico.Individual;
import ipqm.gsa.rvt.algoritmogenetico.domain.Alvo;
import ipqm.gsa.rvt.algoritmogenetico.domain.rvt.Raia;
import ipqm.gsa.rvt.algoritmogenetico.utils.cinematica.coordenada.CoordenadaGeografica;
import ipqm.gsa.rvt.algoritmogenetico.utils.cinematica.coordenada.Posicao;

import java.util.List;

/**
 * JavaFX App
 */
//public class App extends Application {
//
//    private static Scene scene;
public class App{
//    @Override
//    public void start(Stage stage) throws IOException {
//        
//        
//        scene = new Scene(loadFXML("teste"), 640, 480);
//        stage.setScene(scene);
//        stage.show();
//        
//     
//    }
//
//    static void setRoot(String fxml) throws IOException {
//        scene.setRoot(loadFXML(fxml));
//    }
//
//    private static Parent loadFXML(String fxml) throws IOException {
//        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("fxml/" + fxml + ".fxml"));
//        return fxmlLoader.load();
//    }

    public static void main(String[] args) throws Exception {
//        launch();
        int populationSize = 10;
        int generations = 100;

        Posicao pAlvo = new Posicao();
        pAlvo.setCoordenadaGeografica(new CoordenadaGeografica(-22.12343,-43.23423));
        Alvo alvo = new Alvo(pAlvo);

        GeneticAlgorithm ga = new GeneticAlgorithm(populationSize, generations, alvo);

        ga.executar(alvo);
        
       
        
//        Individual best = ga.run();
//        System.out.println("Melhor disposicao de boias: ");
//        double[] genes = best.getGenes();
//        for(int i = 0; i < genes.length; i++){
//            System.out.println("BOIA " + (i+1) +": (" + genes[2*i] + ", " + genes[2*i + 1] + ")");
//        }

    }

}