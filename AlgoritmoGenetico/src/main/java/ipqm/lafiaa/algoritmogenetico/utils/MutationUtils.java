package ipqm.lafiaa.algoritmogenetico.utils;

import ipqm.lafiaa.algoritmogenetico.domain.rvt.Buoy;

import java.util.HashMap;
import java.util.Map;

public class MutationUtils {
    public static Map<String, Buoy> deepCopy(Map<String, Buoy> map) {
        Map<String, Buoy> copy = new HashMap<>();
        for(var e : map.entrySet()){
            copy.put(e.getKey(), e.getValue());
        }
        return copy;
    }

    public static int clamp(int v, int low, int high) {
        return Math.max(low, Math.min(high, v));
    }
}
