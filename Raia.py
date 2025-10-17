import numpy as np

import config_parameters


def calcular_custo(boias):
    """
    Modelo RVT simplificado.
    Calcula somente o custo de variância dos erros de tempo.
    :param boias:
    :return: (menor_custo, (x_splash, y_splash))
    """

    dimensao = config_parameters.raia['dimensao']
    resolucao = config_parameters.raia['resolucao']

    # constroi uma matriz de 1016 por 1016
    custo = np.zeros((dimensao, dimensao))

    tempos_deteccao = np.array([b['tempo_deteccao'] for b in boias])
    menor_tempo_deteccao = np.min(tempos_deteccao)

    for b in boias:
        b['tempo_ref'] = b['tempo_deteccao'] - menor_tempo_deteccao # tempo relativo a boias base

    for x in range(dimensao):
        for y in range(dimensao):
            erros = []
            for b in boias:
                distancia = np.sqrt((b['x'] - x) ** 2 + (b['y'] - y) ** 2) * resolucao # em metros
                tempo_teorico = distancia / config_parameters.raia['velocidade_do_som']
                erro = tempo_teorico - b['tempo_ref']
                erros.append(erro)
            custo[x,y] = np.var(erros)

    menor_idx = np.argmin(custo)
    x_min, y_min = np.unravel_index(menor_idx, custo.shape)
    return float(np.min(custo)), (x_min, y_min)

