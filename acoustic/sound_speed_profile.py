from __future__ import annotations
from typing import List, Sequence
import csv
import numpy as np

class SoundSpeedProfile:
    """
    Sound Speed Profile (SSP): maps depth (meters) -> sound speed (m/s).

    Stores (depth, speed) samples and provides 1D linear interpolation.
    """

    def __init__(self, depths: np.ndarray, sound_speeds: np.ndarray):
        if len(depths) < 2:
            raise ValueError("The SSP needs at least 2 points for interpolation.")
        if len(depths) != len(sound_speeds):
            raise ValueError("Depths and sound speeds need to be the same size.")

        sorted_indexes = np.argsort(depths)
        self.depths = np.asarray(depths, dtype=float)[sorted_indexes]
        self.sound_speeds = np.asarray(sound_speeds, dtype=float)[sorted_indexes]

        # Optional: ensure strictly increasing depths (avoids weird interpolation)
        if np.any(np.diff(self.depths) <= 0):
            raise ValueError("Depths must be strictly increasing after sorting.")

    # -----------------------------
    # Loaders / builders
    # -----------------------------

    @staticmethod
    def csv_loader(file_path: str,
                   column_name_depth: str = "depth",
                   column_name_speed: str = "sound_speed") -> SoundSpeedProfile:
        """
        Loads an SSP from a CSV file with (at least) two columns:
          - depth (default: 'depth')
          - sound speed (default: 'sound_speed')
        """
        depths: List[float] = []
        sound_speeds: List[float] = []

        with open(file_path, "r", newline="", encoding="utf-8") as file:
            reader = csv.DictReader(file)

            if reader.fieldnames is None:
                raise ValueError("The CSV is invalid. Header not found.")
            if column_name_depth not in reader.fieldnames or column_name_speed not in reader.fieldnames:
                raise ValueError(
                    f"The CSV does not have the required columns '{column_name_depth}' and '{column_name_speed}'."
                )

            for line in reader:
                depths.append(float(line[column_name_depth]))
                sound_speeds.append(float(line[column_name_speed]))

        return SoundSpeedProfile(np.array(depths, dtype=float), np.array(sound_speeds, dtype=float))

    @staticmethod
    def create_constant_profile(constant_speed: float = 1500.0) -> SoundSpeedProfile:
        """
        Creates a constant SSP (baseline model).
        """
        depths = np.array([0.0, 100.0], dtype=float)
        sound_speeds = np.array([constant_speed, constant_speed], dtype=float)
        return SoundSpeedProfile(depths, sound_speeds)

    # -----------------------------
    # Mackenzie (1981) + factories
    # -----------------------------

    @staticmethod
    def _mackenzie_sound_speed(T_c: float, S_psu: float, z_m: float) -> float:
        """
        Mackenzie (1981) sound speed in seawater, c(T,S,z).

        T_c: temperature (°C)
        S_psu: salinity (PSU)
        z_m: depth (m)
        returns: sound speed (m/s)
        """
        T = float(T_c)
        S = float(S_psu)
        z = float(z_m)

        c = (1448.96
             + 4.591 * T
             - 5.304e-2 * T**2
             + 2.374e-4 * T**3
             + 1.340 * (S - 35.0)
             + 1.630e-2 * z
             + 1.675e-7 * z**2
             - 1.025e-2 * T * (S - 35.0)
             - 7.139e-13 * T * z**3)
        return float(c)

    @staticmethod
    def from_temperature_salinity_profiles(
            depths_in_meters: Sequence[float],
            temperatures_celsius: Sequence[float],
            salinity_psu: Sequence[float],
    ) -> SoundSpeedProfile:
        """
        Builds a Sound Speed Profile (SSP) from discrete temperature and salinity
        samples as a function of depth, using the Mackenzie (1981) empirical model.

        Parameters
        ----------
        depths_in_meters : Sequence[float]
            Depth samples (meters), positive downward.
        temperatures_celsius : Sequence[float]
            Water temperature at each depth (°C).
        salinity_psu : Sequence[float]
            Water salinity at each depth (PSU).

        Returns
        -------
        SoundSpeedProfile
            Interpolable sound speed profile c(z).
        """

        depth_array = np.asarray(depths_in_meters, dtype=float)
        temperature_array = np.asarray(temperatures_celsius, dtype=float)
        salinity_array = np.asarray(salinity_psu, dtype=float)

        if depth_array.ndim != 1 or temperature_array.ndim != 1 or salinity_array.ndim != 1:
            raise ValueError("Depth, temperature, and salinity inputs must be one-dimensional sequences.")

        if not (
                len(depth_array) == len(temperature_array) == len(salinity_array)
        ):
            raise ValueError(
                "Depth, temperature, and salinity sequences must have the same length."
            )

        if len(depth_array) < 2:
            raise ValueError(
                "At least two depth points are required to construct a sound speed profile."
            )

        # Ensure monotonic ordering by depth
        sorting_indices = np.argsort(depth_array)

        sorted_depths = depth_array[sorting_indices]
        sorted_temperatures = temperature_array[sorting_indices]
        sorted_salinities = salinity_array[sorting_indices]

        sound_speeds = np.array(
            [
                SoundSpeedProfile._mackenzie_sound_speed(
                    temperature, salinity, depth
                )
                for temperature, salinity, depth
                in zip(sorted_temperatures, sorted_salinities, sorted_depths)
            ],
            dtype=float,
        )

        return SoundSpeedProfile(sorted_depths, sound_speeds)

    @staticmethod
    def from_shallow_water_TS(
        z_min: float,
        z_max: float,
        T_c: float,
        S_psu: float,
        n_points: int = 4
    ) -> SoundSpeedProfile:
        """
        Shallow-water SSP builder when you only have a representative T and S.

        Good for your case (0.5–8 m): you still avoid "chutar c", because c is derived
        from measured/estimated T and S via Mackenzie, with a small depth dependence.
        """
        if n_points < 2:
            raise ValueError("n_points must be >= 2.")
        depths = np.linspace(float(z_min), float(z_max), int(n_points), dtype=float)
        speeds = np.array([SoundSpeedProfile._mackenzie_sound_speed(T_c, S_psu, z)
                           for z in depths], dtype=float)
        return SoundSpeedProfile(depths, speeds)

    # -----------------------------
    # Query
    # -----------------------------

    def sound_speed(self, depth: float) -> float:
        """
        Returns interpolated sound speed (m/s) at a given depth (m).
        Clamps depth to the SSP range.
        """
        depth_clamped = float(np.clip(depth, self.depths[0], self.depths[-1]))
        return float(np.interp(depth_clamped, self.depths, self.sound_speeds))
