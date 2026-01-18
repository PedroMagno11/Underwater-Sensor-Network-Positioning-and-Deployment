from __future__ import annotations
from typing import List, Optional, Tuple
import random
import numpy as np
from numpy import dtype

from settings.environment_settings import EnvironmentSettings
from settings.genetic_algorithm_settings import GeneticAlgorithmSettings
from settings.simulation_settings import SimulationSettings
from geometry.grid_geometry import GridGeometry
from acoustic.sound_speed_profile import SoundSpeedProfile

from evaluation.cost_function import evaluate_chromosome_cost
from genetic_algorithm.selection import select_index_for_tournament
from genetic_algorithm.crossover import perform_crossover_per_sensor
from genetic_algorithm.mutation import apply_mutation
from genetic_algorithm.population import create_random_chromosome

def run_genetic_algorithm(
        number_of_sensors: int,
        environment_settings: EnvironmentSettings,
        genetic_algorithm_settings: GeneticAlgorithmSettings,
        simulation_settings: SimulationSettings,
        sound_speed_profile: SoundSpeedProfile
) -> Tuple[np.ndarray, float]:
    random_generator = random.Random(genetic_algorithm_settings.random_seed)
    grid_geometry = GridGeometry(environment_settings)

    population: List[np.ndarray] = [
        create_random_chromosome(number_of_sensors, grid_geometry, environment_settings, random_generator)
        for _ in range(genetic_algorithm_settings.population_size)
    ]

    print(f"Populacao gerada: {population}")

    best_chromosome: Optional[np.ndarray] = None
    best_cost: float = float('inf')

    for index_of_generation in range(genetic_algorithm_settings.number_of_generations):
        costs = [
            evaluate_chromosome_cost(chromosome=chromosome,
                                     number_of_sensors=number_of_sensors,
                                     grid_geometry=grid_geometry,
                                     environment_settings=environment_settings,
                                     simulation_settings=simulation_settings,
                                     sound_speed_profile=sound_speed_profile,
                                     random_generator=random_generator)
            for chromosome in population
        ]

        index_of_the_best = int(np.argmin(np.array(costs, dtype=float)))
        if costs[index_of_the_best] < best_cost:
            best_cost = float(costs[index_of_the_best])
            best_chromosome = np.array(population[index_of_the_best], dtype=float)

        sorted_indexes = list(np.argsort(np.array(costs, dtype=float)))
        elites = [np.array(population[i], dtype=float) for i in sorted_indexes[: genetic_algorithm_settings.elitism]]

        new_population: List[np.ndarray] = []
        new_population.extend(elites)

        while len(new_population) < genetic_algorithm_settings.population_size:
            father_index = select_index_for_tournament(costs, genetic_algorithm_settings.tournament_size, random_generator)
            mother_index = select_index_for_tournament(costs, genetic_algorithm_settings.tournament_size, random_generator)

            father_chromosome = population[father_index]
            mother_chromosome = population[mother_index]

            if random_generator.random() < genetic_algorithm_settings.crossover_probability:
                child = perform_crossover_per_sensor(father_chromosome=father_chromosome, mother_chromosome=mother_chromosome, number_of_sensors=number_of_sensors, random_generator=random_generator)
            else:
                child = np.array(father_chromosome, dtype=float)

            apply_mutation(chromosome=child,
                           number_of_sensors=number_of_sensors,
                           environment_settings=environment_settings,
                           grid_geometry=grid_geometry,
                           genetic_algorithm_settings=genetic_algorithm_settings,
                           random_generator=random_generator
            )

            new_population.append(child)

        population = new_population

        if (index_of_generation + 1) % 10 == 0:
            print(f"Generation {index_of_generation + 1}/{genetic_algorithm_settings.number_of_generations} | The best cost until this point: {best_cost:.3f}")

    if best_chromosome is None:
        raise RuntimeError("Unexpected error: best chromosome not found")

    return best_chromosome, best_cost
