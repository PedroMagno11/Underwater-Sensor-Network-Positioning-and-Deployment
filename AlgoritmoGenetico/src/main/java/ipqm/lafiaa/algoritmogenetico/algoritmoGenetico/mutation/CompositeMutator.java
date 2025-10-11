package ipqm.lafiaa.algoritmogenetico.algoritmoGenetico.mutation;

import ipqm.lafiaa.algoritmogenetico.algoritmoGenetico.Individual;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class CompositeMutator implements MutationOperator{

    private static class Entry{
        final MutationOperator operator;
        final double weight;

        Entry(MutationOperator op, double weight){
            this.operator = op;
            this.weight = weight;
        }
    }

    private final List<Entry> table = new ArrayList<>();
    private final double total;

    public CompositeMutator(Map<MutationOperator, Double> weights){
        double sum = 0.0;
        for(var e: weights.entrySet()){
            if(e.getValue() <= 0) continue;
            sum+=e.getValue();
            table.add(new Entry(e.getKey(), sum));
        }

        if(table.isEmpty()) throw new IllegalArgumentException("No positive weights");
        this.total = sum;
    }

    @Override
    public Individual mutate(Individual parent, Random rand) throws Exception {

        double r = rand.nextDouble()*total;
        for(Entry e : table){
            if(r <= e.weight){
//                System.out.println("CLASSE USADA: " + e.operator.getClass());
                return e.operator.mutate(parent, rand);
            }
        }
        return table.getLast().operator.mutate(parent, rand);
    }

    @Override
    public String getName(){
        return "Composite Mutator";
    }
}
