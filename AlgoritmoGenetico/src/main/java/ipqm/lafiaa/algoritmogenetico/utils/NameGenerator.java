package ipqm.lafiaa.algoritmogenetico.utils;

import java.util.Set;

public class NameGenerator {

    private String prefix;

    public NameGenerator(String prefix){
        this.prefix = prefix;
    }

    public String nextName(Set<String> existing){
        int max = 0;
        for(String s: existing){
            if(s.startsWith(prefix)){
                String numPart = s.substring(prefix.length());
                try {
                    int n = Integer.parseInt(numPart);
                    max = Math.max(max, n);
                } catch (NumberFormatException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return prefix + (max + 1);
    }
}
