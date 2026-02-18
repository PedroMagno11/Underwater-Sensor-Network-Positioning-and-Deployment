from pathlib import Path
import numpy as np
import matplotlib.pyplot as plt

def plot_impacts(impacts_dir: str, generation: int):
    p = Path(impacts_dir) / f"impact_points_gen_{generation:04d}.npy"
    pts = np.load(p)  # shape (N,2)

    plt.figure()
    plt.scatter(pts[:,0], pts[:,1], s=12)
    plt.gca().set_aspect("equal", adjustable="box")
    plt.title(f"Impact points - gen {generation}")
    plt.xlabel("x (m)")
    plt.ylabel("y (m)")
    plt.grid(True)
    plt.show()

if __name__ == "__main__":
    plot_impacts("outputs/sensors_4/impacts", generation=0)
