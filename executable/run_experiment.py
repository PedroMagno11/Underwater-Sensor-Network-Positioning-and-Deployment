"""Entry-point script (correct spelling).

The original file was named `run_expirement.py`. This wrapper keeps the same
runtime behavior while providing the correct spelling for new users.
"""

from __future__ import annotations

import runpy


if __name__ == "__main__":
    runpy.run_module("executable.run_expirement", run_name="__main__")
