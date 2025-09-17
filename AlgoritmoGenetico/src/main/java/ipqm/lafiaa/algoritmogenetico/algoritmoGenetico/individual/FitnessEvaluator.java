package ipqm.lafiaa.algoritmogenetico.algoritmoGenetico.individual;

import com.fasterxml.jackson.databind.ObjectMapper;
import ipqm.lafiaa.algoritmogenetico.algoritmoGenetico.Individual;
import ipqm.lafiaa.algoritmogenetico.domain.dto.BuoyDTO;
import ipqm.lafiaa.algoritmogenetico.domain.dto.PontoQuedaInput;
import ipqm.lafiaa.algoritmogenetico.domain.rvt.Buoy;
import ipqm.lafiaa.algoritmogenetico.domain.rvt.CoordenadaCartesianaRVT;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class FitnessEvaluator implements AutoCloseable{

    private static final URI ENDPOINT = URI.create("http://localhost:34080/api/raia/calcular");
    private final HttpClient client;
    private static final ObjectMapper mapper = new ObjectMapper();
    private final ExecutorService executor;
    private final ConcurrentMap<String, Double> cache = new ConcurrentHashMap<>();
    private boolean failOpen;

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
            json = mapper.writeValueAsString(boias);
        } catch (Exception e) {
            // serialização falhou
            if (failOpen) { ind.setFitness(Double.POSITIVE_INFINITY); return CompletableFuture.completedFuture(null); }
            return CompletableFuture.failedFuture(e);
        }

        HttpRequest req = HttpRequest.newBuilder(ENDPOINT)
                .timeout(Duration.ofSeconds(10))
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
                        double d = CoordenadaCartesianaRVT.calcularDistanciaEntreDoisPontos(
                                p.getCoordCartesiana().getX(), p.getCoordCartesiana().getY(), 500, 500);
                        return p.getCusto() + d;
                    } catch (Exception e) {
                        if (failOpen) return Double.POSITIVE_INFINITY;
                        throw new CompletionException(e);
                    }
                })
                .thenAccept(f -> {
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
