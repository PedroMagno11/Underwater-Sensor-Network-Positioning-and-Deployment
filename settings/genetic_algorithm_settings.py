from __future__ import annotations
from dataclasses import dataclass

@dataclass(frozen=True)
class GeneticAlgorithmSettings:
    number_of_generations: int = 150
    population_size: int = 60
    tournament_size: int = 3

    crossover_probability: float = 0.80
    mutation_probability: float = 0.30

    standard_deviation_of_xy_mutation: float = 150.0
    standard_deviation_of_z_mutation: float = 0.8

    elitism: int = 3
    random_seed: int = 42