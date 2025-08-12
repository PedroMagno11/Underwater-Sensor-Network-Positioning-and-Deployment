package ipqm.gsa.rvt.algoritmogenetico.service;

import ipqm.gsa.rvt.algoritmogenetico.domain.Alvo;

import java.util.HashSet;
import java.util.Set;

public class AlvoService {
    private Set<Alvo> alvos;
    private static AlvoService instance;

    private AlvoService() {
        alvos = new HashSet<>();
    }

    public static AlvoService getInstance() {
        if (instance == null) {
            instance = new AlvoService();
        }
        return instance;
    }

    public Alvo addAlvo(Alvo alvo){
        alvos.add(alvo);
        return alvo;
    }

    public Alvo getAlvo(){
        return alvos.iterator().next();
    }

    public void removeAlvo(Alvo alvo){
        alvos.remove(alvo);
    }

}
