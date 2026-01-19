from __future__ import annotations

import logging
from logging.handlers import RotatingFileHandler
from typing import Optional


def setup_logging(
    log_level: int = logging.INFO,
    log_file_path: Optional[str] = "execution.log",
    log_to_console: bool = True,
) -> None:
    """Configure global logging.

    This function configures:
    - Console logging (optional)
    - Rotating file logging (optional)
    - A timestamped, reproducible log format

    Notes:
        It also clears existing handlers to avoid duplicated logs when running
        from IDEs or notebooks.
    """

    log_format = "%(asctime)s | %(levelname)s | %(name)s | %(message)s"
    date_format = "%Y-%m-%d %H:%M:%S"

    root_logger = logging.getLogger()
    root_logger.setLevel(log_level)

    # Prevent duplicated handlers in IDE / notebooks
    if root_logger.handlers:
        root_logger.handlers.clear()

    if log_to_console:
        console_handler = logging.StreamHandler()
        console_handler.setLevel(log_level)
        console_handler.setFormatter(logging.Formatter(log_format, datefmt=date_format))
        root_logger.addHandler(console_handler)

    if log_file_path:
        file_handler = RotatingFileHandler(
            log_file_path,
            maxBytes=5_000_000,  # 5 MB
            backupCount=3,
            encoding="utf-8",
        )
        file_handler.setLevel(log_level)
        file_handler.setFormatter(logging.Formatter(log_format, datefmt=date_format))
        root_logger.addHandler(file_handler)
