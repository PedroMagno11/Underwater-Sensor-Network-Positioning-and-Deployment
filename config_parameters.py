parameters={
    'num_generations': 100, # número total de gerações
    'sol_per_pop': 100, # número de indivíduos por geração
    'num_parents_mating': 10, # quantos indivíduos vão cruzar por geração
    'parent_selection_type': '', # método de seleção de pais('rws' -> roleta ,'tournament' -> seleciona os melhores de um grupo aleatoriamente, 'steady_state' -> mantém alguns pais fixos, 'rank' -> seleção proporcional à posição no ranking) BIZU: tournament (mais usado)
    'keep_elitism': '' # quantos melhores indivíduos permanecem intactos
}

mutation = {
    'mutation_percent_genes': '', # % de genes mutados em cada geração
    'mutation_type': '', # Tipo de mutação ('random' -> substitui genes por valor aleatorio, 'swap' -> troca dois genes de lugar , 'scramble' -> embaralha subconjunto de genes, 'adaptive' -> diminui a taxa de mutação conforme o fitness melhora) BIZU: random (mais usado)
}

gene = {
    'num_genes' : 5, # número de genes por indivíduo
    'gene_space': 10, # intervalo permitido para cada gene (ou lista de dicionário)
    'gene_type': int
}

crossover ={
    'crossover_type': '',    # Tipo de mutação ('single_point' -> divide o cromossomo em 1 ponto, 'two_points' -> divide em 2 pontos, 'uniform' -> cada gene tem chance independente de vir de cada pai, 'scattered' -> escolhe genes aleatoriamente) BIZU: single_point e uniform -são os mais usados
}

raia ={
    'dimensao': 1016,
    'velocidade_do_som': 1.531, # m/ms
    'resolucao': 9.0,
    'posicao_referencial': 508,
    'ruido_deteccao': 3.0, # [-3,3] ms
    'quant_max_boias': 5,
    'quant_min_boias': 3,
}