package ipqm.lafiaa.algoritmogenetico.algoritmoGenetico.individual;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ipqm.lafiaa.algoritmogenetico.algoritmoGenetico.Individual;
import ipqm.lafiaa.algoritmogenetico.domain.Alvo;
import ipqm.lafiaa.algoritmogenetico.domain.config.Parametros;
import ipqm.lafiaa.algoritmogenetico.domain.dto.BuoyDTO;
import ipqm.lafiaa.algoritmogenetico.domain.dto.IndividualDTO;
import ipqm.lafiaa.algoritmogenetico.domain.dto.PontoQuedaInput;
import ipqm.lafiaa.algoritmogenetico.domain.rvt.Buoy;
import ipqm.lafiaa.algoritmogenetico.domain.rvt.CoordenadaCartesianaRVT;
import ipqm.lafiaa.algoritmogenetico.utils.HTTPRequest;
import ipqm.lafiaa.algoritmogenetico.utils.Response;
import ipqm.lafiaa.algoritmogenetico.utils.cinematica.coordenada.CoordenadaGeografica;
import ipqm.lafiaa.algoritmogenetico.utils.conversor.ConversorUnidades;
import ipqm.lafiaa.algoritmogenetico.utils.coord.Geodesics;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class CircularIndividualEvaluator {
    private final double radius;
    private final int buoyCount;
    private final Random rand = new Random();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Alvo TARGET = Alvo.getInstance();
    private static final String ENDPOINT = "http://localhost:34085/api/raia/calcular";


    public CircularIndividualEvaluator(double radius, int geneCount) {
        if(radius <= 0 || radius >= 1000){
            throw new IllegalArgumentException("invalid radius " + radius);
        }

        this.radius = radius;
        this.buoyCount = geneCount;
    }

    public Individual generate() throws IOException, URISyntaxException, InterruptedException {
        Map<String, Buoy> genes = new LinkedHashMap<>();

        double angleStep = 2 * Math.PI/buoyCount;

        for (int i = 0; i < buoyCount; i++) {
            double angle = i * angleStep;

            double x = ((double) Parametros.DIMENSAO_RAIA / 2) + radius * Math.cos(angle);
            double y = ((double) Parametros.DIMENSAO_RAIA / 2) + radius * Math.sin(angle);

//            x += rand.nextInt(2 * noise + 1) - noise;
//            y += rand.nextInt(2 * noise + 1) - noise;

            int posX = (int) Math.round(x);
            int posY = (int) Math.round(y);

            Buoy buoy = new Buoy("buoy" + (i + 1), posX, posY);

            CoordenadaGeografica coordGeoBoia = CoordenadaCartesianaRVT.converterCoordenadaCartesianaParaGeografica(new CoordenadaCartesianaRVT(buoy.getPosX(), buoy.getPosY()), TARGET.getPosicao());

            buoy.setLatGeo(coordGeoBoia.getLatitude());
            buoy.setLonGeo(coordGeoBoia.getLongitude());

            double distanciaEmMilhasNauticas = Geodesics.distance(buoy.getLonGeo(), buoy.getLatGeo(), TARGET.getCoordenadaGeografica().getLongitude(), TARGET.getCoordenadaGeografica().getLatitude());
            double distanciaEmMetros = ConversorUnidades.milhasNauticasParaMetros(distanciaEmMilhasNauticas);
            double tempo = distanciaEmMetros / Parametros.VELOC_SOM;

            // De maneira aleatória, adiciona um atraso na detecção de até 3 ms
            if(rand.nextDouble() < 0.3){
                tempo += rand.nextDouble() * Parametros.RUIDO_TEMPO_DETECCAO;
            }else if (rand.nextDouble() > 0.7){
                tempo -= rand.nextDouble() * Parametros.RUIDO_TEMPO_DETECCAO;
            }

            long t = Math.max(0, Math.round(tempo));
            buoy.setTempoDeteccao(t);

            genes.put(buoy.getNome(), buoy);
        }

        return new Individual(genes);
    }

    public static void evaluate(Individual ind) {
        final String json;
        try {
            List<BuoyDTO> boias = ind.getGenes().stream().map(BuoyDTO::new).toList();
            IndividualDTO individualDTO = new IndividualDTO("circularIndividual", boias);
            json = mapper.writeValueAsString(individualDTO);
            Response resp = HTTPRequest.post(ENDPOINT, json);
            PontoQuedaInput pontoQuedaInput = mapper.readValue(resp.getBody().toString(), PontoQuedaInput.class);
            ind.setFitness(pontoQuedaInput.getCusto());
        } catch (JsonProcessingException e) {
            System.err.println("Json parsing error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("General error: " + e.getMessage());
        }
    }



}
