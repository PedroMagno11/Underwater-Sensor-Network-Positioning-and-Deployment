# Underwater Sensor Network Positioning and Deployment

This project implements a **Genetic Algorithm (GA)** to optimize the placement of an underwater acoustic sensor (buoy + hydrophone) network for **projectile impact localization**.

The codebase is intentionally organized in small, focused packages:

- `acoustic/` – sound speed profile (SSP) and baseline acoustic arrival-time model
- `geometry/` – grid geometry and distance utilities
- `localization/` – MLE-based impact position estimation (grid search)
- `evaluation/` – chromosome decoding, penalties, and cost/report computation
- `genetic_algorithm/` – GA components (selection, crossover, mutation, population)
- `settings/` – configuration dataclasses
- `logging_utils/` – centralized logging configuration
- `executable/` – runnable entry points

## Running

Run the main experiment script:

```bash
python -m executable.run_experiment
```

This will run the GA for different sensor counts (e.g., 3, 4, 5) and print the final best sensor positions.

## Logs

Execution logs are written to:

- Console (default)
- `execution.log` (rotating file)

The GA logs include per-generation statistics such as:

- min/avg/median/p90 cost
- average no-coverage rate
- average localization error
- global best updates

To increase verbosity, change the `log_level` to `logging.DEBUG` in `executable/run_expirement.py`.

## Sound Speed Profile (SSP)

You can use either:

- A constant profile: `SoundSpeedProfile.create_constant_profile(1500.0)`
- A CSV file: `SoundSpeedProfile.csv_loader(...)`

CSV format example:

| depth | speed |
|------:|------:|
| 0.0   | 1500  |
| 8.0   | 1520  |
