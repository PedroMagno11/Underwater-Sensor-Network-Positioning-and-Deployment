package ipqm.lafiaa.algoritmogenetico.algoritmoGenetico.crossover;

import ipqm.lafiaa.algoritmogenetico.algoritmoGenetico.Individual;
import ipqm.lafiaa.algoritmogenetico.domain.rvt.Buoy;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class Crossover implements CrossoverOperator{
    @Override
    public Individual[] crossover(Individual p1, Individual p2, ThreadLocalRandom rand) throws Exception {
        Comparator<Buoy> byName = Comparator
                .comparing((Buoy b) -> b.getNome().replaceAll("\\d+$", ""), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(b -> {
                    String n = b.getNome();
                    String m = n.replaceAll("^.*?(\\d+)$", "$1");
                    return m.equals(n) ? 0 : Integer.parseInt(m);
                })
                .thenComparing(Buoy::getNome);

        List<Buoy> l1 = new ArrayList<>(p1.getGenes());
        l1.sort(byName);

        List<Buoy> l2 = new ArrayList<>(p2.getGenes());
        l2.sort(byName);

        // Decide quem é o maior (A) e o menor (B)
        List<Buoy> A = l1.size() >= l2.size() ? l1 : l2;
        List<Buoy> B = l1.size() >= l2.size() ? l2 : l1;
        int targetSize = A.size();


        // child1 começa com Todos de A, depois injeta de B por substituição se não existirem
        List<Buoy> child1 = new ArrayList<>(A); // já no tamanho máximo
        Set<String> used1 = child1.stream().map(Buoy::getNome).collect(Collectors.toSet());
        for (Buoy g : B) {
            if (!used1.contains(g.getNome())) {
                // substitui posição aleatória para manter tamanho e misturar genes
                int idx = rand.nextInt(child1.size());
                used1.remove(child1.get(idx).getNome());
                child1.set(idx, g);
                used1.add(g.getNome());
            }
        }

        // child2  começa vazio, adiciona B preservando ordem, completa com genes de A únicos
        List<Buoy> child2 = new ArrayList<>(targetSize);
        Set<String> used2 = new HashSet<>();
        // adiciona B
        for (Buoy g : B) {
            if (child2.size() == targetSize) break;
            if (used2.add(g.getNome())) child2.add(g);
        }
        // completa com A até targetSize
        for (Buoy g : A) {
            if (child2.size() == targetSize) break;
            if (used2.add(g.getNome())) child2.add(g);
        }
        // se por algum motivo ainda faltou (colisões de nomes), preenche com A por substituição
        while (child2.size() < targetSize) {
            Buoy g = A.get(rand.nextInt(A.size()));
            if (used2.add(g.getNome())) child2.add(g);
            else {
                int idx = rand.nextInt(child2.size());
                used2.remove(child2.get(idx).getNome());
                child2.set(idx, g);
                used2.add(g.getNome());
            }
        }

        // Converte para Map (mantendo último em caso de chave duplicada)
        Map<String, Buoy> genesChild1 = child1.stream()
                .collect(Collectors.toMap(Buoy::getNome, b -> b, (a,b)->b, HashMap::new));
        Map<String, Buoy> genesChild2 = child2.stream()
                .collect(Collectors.toMap(Buoy::getNome, b -> b, (a,b)->b, HashMap::new));

        return new Individual[] { new Individual(genesChild1), new Individual(genesChild2) };
    }
}

