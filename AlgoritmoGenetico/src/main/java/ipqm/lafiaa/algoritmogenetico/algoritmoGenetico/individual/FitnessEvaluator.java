package ipqm.lafiaa.algoritmogenetico.algoritmoGenetico.individual;

import com.fasterxml.jackson.databind.ObjectMapper;
import ipqm.lafiaa.algoritmogenetico.algoritmoGenetico.Individual;
import ipqm.lafiaa.algoritmogenetico.domain.config.Parametros;
import ipqm.lafiaa.algoritmogenetico.domain.dto.BuoyDTO;
import ipqm.lafiaa.algoritmogenetico.domain.dto.IndividualDTO;
import ipqm.lafiaa.algoritmogenetico.domain.dto.PontoQuedaInput;
import ipqm.lafiaa.algoritmogenetico.domain.rvt.Buoy;
import ipqm.lafiaa.algoritmogenetico.domain.rvt.CoordenadaCartesianaRVT;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class FitnessEvaluator implements AutoCloseable{

    private static final URI ENDPOINT = URI.create("http://localhost:34085/api/raia/calcular");
    private final HttpClient client;
    private static final ObjectMapper mapper = new ObjectMapper();
    private final ExecutorService executor;
    private final ConcurrentMap<String, Double> cache = new ConcurrentHashMap<>();
    private final boolean failOpen;

    public FitnessEvaluator(int threads, boolean failOpen){
        this.failOpen = failOpen;

        this.executor = new ThreadPoolExecutor(
                threads, threads, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                r->{
                    Thread t = new Thread(r, "fitness-" + UUID.randomUUID());
                    t.setDaemon(true);
                    return t;
                }
        );
        this.client = HttpClient.newBuilder()
                .executor(executor)
                .version(HttpClient.Version.HTTP_1_1)
//                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }


    private static String genotypeKey(Individual ind){
        return ind.getGenes().stream()
                .sorted(Comparator.comparing(Buoy::getNome))
                .map(b -> b.getNome() + "@" + b.getPosX() + "," + b.getPosY())
                .collect(Collectors.joining("|"));
    }

    public CompletableFuture<Void> evaluateAsync(Individual ind) {
        String key = genotypeKey(ind);
        Double cached = cache.get(key);
        if (cached != null) {
            ind.setFitness(cached);
            return CompletableFuture.completedFuture(null);
        }

        final String json;
        try {
            List<BuoyDTO> boias = ind.getGenes().stream().map(BuoyDTO::new).toList();
            IndividualDTO individualDTO = new IndividualDTO(genotypeKey(ind), boias);
            json = mapper.writeValueAsString(individualDTO);
        } catch (Exception e) {
            // serialização falhou
            if (failOpen) { ind.setFitness(Double.POSITIVE_INFINITY); return CompletableFuture.completedFuture(null); }
            return CompletableFuture.failedFuture(e);
        }

        HttpRequest req = HttpRequest.newBuilder(ENDPOINT)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        return client.sendAsync(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .handle((resp, err) -> {
                    // converte qualquer falha/404/500 em "body == null" se failOpen, senão propaga
                    if (err != null || resp == null) {
                        if (failOpen) return (String) null;
                        throw new CompletionException(err);
                    }
                    if (resp.statusCode() != 200) {
                        if (failOpen) return (String) null;
                        throw new CompletionException(new RuntimeException("Cálculo não realizado: " + resp.statusCode()));
                    }
                    return resp.body();
                })
                .thenApply(body -> {
                    if (body == null) return Double.POSITIVE_INFINITY; // fail-open
                    try {
                        PontoQuedaInput p = mapper.readValue(body, PontoQuedaInput.class);
                        double d = CoordenadaCartesianaRVT.calcularDistanciaEntreDoisPontos(p.getCoordCartesiana().getX(), p.getCoordCartesiana().getY(), Parametros.DIMENSAO_RAIA/2, Parametros.DIMENSAO_RAIA/2); // Compara a posição do ponto de queda calculado com o centro da raia (Posição assumida pelo alvo)
                        return p.getCusto() + d;
                    } catch (Exception e) {
                        if (failOpen) return Double.POSITIVE_INFINITY;
                        throw new CompletionException(e);
                    }
                })
                .thenAccept(f -> {

                    double sum = 0.0;
                    if(ind.getGenes().size() < 4){
                        sum += 0.05; // Pune indivíduos com 3 boias, pois 3 é a quantidade com maior imprecisão no cálculo de triangulação
                    }

//                    ind.getGenes().forEach(g -> {
//                        double ponto = Double.parseDouble(g.getNome().substring(4));
//                        if(ponto > 5){
//                            sum.addAndGet(1);
//                        }
//                    });

                    f+=sum;
                    ind.setFitness(f);
                    cache.putIfAbsent(key, f);
                });
    }

    private CompletableFuture<Void> handleFailure(Individual ind, Throwable e) {
        if(failOpen){
            ind.setFitness(Double.POSITIVE_INFINITY);
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.failedFuture(e);
    }

    public void evaluateAllBlocking(List<Individual> inds){
        var futures = inds.stream()
                .map(this::evaluateAsync)
                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures).join();
    }

    @Override
    public void close() throws Exception {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) executor.shutdown();
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

    }
}
