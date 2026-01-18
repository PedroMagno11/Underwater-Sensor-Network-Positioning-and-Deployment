from __future__ import annotations
from typing import List
import csv
import numpy as np

class SoundSpeedProfile:
    """
    Sound Speed Profile (SSP): maps depth (meters) -> sound speed (m/s).

    This class stores a set of (depth, speed) samples and provides
    a simple 1D interpolation model to query sound speed at any depth.
    """

    def __init__(self, depths: np.ndarray, sound_speeds: np.ndarray):
        """
        Creates an SSP from arrays of depth and corresponding sound speeds.

        Requirements:
            - At least 2 points are needed for interpolation.
            - Depth and sound speed arrays must have the same length.
            - Data is stored by depth to guarantee monotonic depth ordering.
        """

        if len(depths) < 2:
            raise ValueError("The SSP needs at least 2 points for interpolation.")
        if len(depths) != len(sound_speeds):
            raise ValueError("Depths and sound speeds need to be the same size.")

        # Sort the samples by depth (ascending), so interpolation is well-defined
        sorted_indexes = np.argsort(depths)
        self.depths = depths[sorted_indexes].astype(float)
        self.sound_speeds = sound_speeds[sorted_indexes].astype(float)

    @staticmethod
    def csv_loader(file_path: str,
                   column_name_depth: str = "depth",
                   column_name_speed: str = "sound_speed") -> SoundSpeedProfile:

        """
        Loads an SSP from a CSV file with (at least) two columns:
            - depth column (default: 'depth')
            - sound speed column (default: 'speed')

        The CSV is parsed with DictReader to allow flexible column ordering.
        Values are converted to float and then used to build the SSP object.
    """
        depths: List[float] = []
        sound_speeds: List[float] = []

        with open(file_path, "r", newline="", encoding="utf-8") as file:
            reader = csv.DictReader(file)

            if reader.fieldnames is None:
                raise ValueError("The CSV is invalid. Header not found.")
            if column_name_depth not in reader.fieldnames or column_name_speed not in reader.fieldnames:
                raise ValueError(
                    f"The CSV does not have the required column {column_name_depth} and {column_name_speed}."
                )

            for line in reader:
                depths.append(float(line[column_name_depth]))
                sound_speeds.append(float(line[column_name_speed]))

        return SoundSpeedProfile(np.array(depths), np.array(sound_speeds))


    @staticmethod
    def create_constant_profile(constant_speed: float = 1500.0) -> SoundSpeedProfile:
        """
        Creates a constant SSP

        This is a convenient baseline model, often used when:
            - environmental measurements are not available, or
            the model sensitivity to SSP is not being studies yet.

        Note: We still provide two depth points so linear interpolation works.
        """

        depths = np.array([0.0, 100.0], dtype=float)
        sound_speeds = np.array([constant_speed, constant_speed], dtype=float)

        return SoundSpeedProfile(depths, sound_speeds)

    def sound_speed(self, depth: float) -> float:
        """
        Returns the interpolated sound speed (m/s) at a given depth (meters).

        Step:
        1 - Clamp the query depth to the known depth range, to avoid extrapolation.
        2 - Linearly interpolate within the sampled SSP points.
        """

        depth_clamped = float(np.clip(depth, self.depths[0], self.depths[-1]))

        return float(np.interp(depth_clamped, self.depths, self.sound_speeds))