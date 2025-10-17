import numpy as np
import matplotlib.pyplot as plt
import matplotlib.animation as animation
import threading, time, pygad, Raia, config_parameters

# ==== Config ====
raia = config_parameters.raia
CENTRO = np.array([raia['posicao_referencial'], raia['posicao_referencial']])
VEL_SOM = raia['velocidade_do_som']
RESOLUCAO = raia['resolucao']
RUIDO = raia['ruido_deteccao']
NUM_BOIAS_MAX = raia['quant_max_boias']

def fitness_func(ga_instance, solution, sol_idx):
    genes = np.array(solution).reshape(-1, 3)
    cromossomo = [] # array de boias
    for (x, y, ativa) in genes:
        if not ativa:
            continue

        distancia_m = np.sqrt((x - CENTRO[0])**2 + (y - CENTRO[1])**2) * RESOLUCAO
        tempo_teorico_ms = distancia_m / VEL_SOM
        ruido = np.random.uniform(-RUIDO, RUIDO)
        tempo_medido_ms = tempo_teorico_ms + ruido
        cromossomo.append({
            "x": int(x),
            "y": int(y),
            "tempo_deteccao": int(tempo_medido_ms),
            "velocidade_do_som": VEL_SOM,
            "ativada": ativa
        })

    if len(cromossomo) < 3:
        return 1.e-9

    menor_custo, ponto_splash = Raia.calcular_custo(cromossomo)
    dist = np.linalg.norm(np.array(ponto_splash) - CENTRO)
    fitness = 1.0 / (menor_custo + 0.001 * dist + 1e-9)

    return fitness


gene_space = []
for _ in range(NUM_BOIAS_MAX):
    gene_space += [
        {'low': 233, 'high': 783}, # x
        {'low': 233, 'high': 783}, # y
        [0,1]
    ]
# ==== Inicialização do GA ====
ga = pygad.GA(
    num_generations=10,
    sol_per_pop=10,
    num_parents_mating=2,
    num_genes=len(gene_space),
    gene_space=gene_space,
    parent_selection_type="tournament",
    mutation_percent_genes=9,
    crossover_type="single_point",
    fitness_func=fitness_func,
    keep_parents=2,
    gene_type=int
)

ga.run()
print(f'BEST FITNESS: {ga.best_solutions_fitness}')
print(f'BEST INDIVIDUAL: {ga.best_solutions}')