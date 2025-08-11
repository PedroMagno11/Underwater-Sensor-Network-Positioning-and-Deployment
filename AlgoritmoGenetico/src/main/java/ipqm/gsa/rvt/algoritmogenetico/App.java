package ipqm.gsa.rvt.algoritmogenetico;

import ipqm.gsa.rvt.algoritmogenetico.domain.Alvo;
import ipqm.gsa.rvt.algoritmogenetico.algoritmoGenetico.GeneticAlgorithm;
import ipqm.gsa.rvt.algoritmogenetico.algoritmoGenetico.Individual;
import ipqm.gsa.rvt.algoritmogenetico.domain.rvt.Boia;
import ipqm.gsa.rvt.algoritmogenetico.domain.rvt.Raia;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
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
        int populationSize = 100;
        int generations = 100;
        int numBoias = 4;
        int areaSize = 1000;
        double sigma = 1.0;

        GeneticAlgorithm ga = new GeneticAlgorithm(populationSize, numBoias, areaSize, sigma, generations);

        List<Individual> populacao = ga.initializePopulation();
        for(Individual i : populacao){
            System.out.println(" - " + i);
        }
        
      
        
       
        
//        Individual best = ga.run();
//        System.out.println("Melhor disposicao de boias: ");
//        double[] genes = best.getGenes();
//        for(int i = 0; i < genes.length; i++){
//            System.out.println("BOIA " + (i+1) +": (" + genes[2*i] + ", " + genes[2*i + 1] + ")");
//        }

    }

}