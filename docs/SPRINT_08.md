# Sprint 08 — Forecasting service foundation

- Adds an isolated FastAPI forecasting service, enabled through the Docker Compose `ai` profile and not published on a host port.
- Adds a transparent scikit-learn linear-trend baseline for FOOD, WATER and ENERGY observations.
- Validates ordered observations, a minimum five distinct dates, bounded horizons and non-negative finite measurements.
- Returns forecast points, training sample count and training RMSE with an explicit experimental-use warning.
- Adds automated API tests and a dedicated forecasting CI job.

## Run locally
- `docker compose --profile ai up --build`
- The forecasting service is internal to the Compose network at `http://forecasting:8000`; it is not exposed to the public host.
- API schema for the internal service is available at `/docs` inside that network.

## Limitations
This is a baseline regression service, not a validated production AI model. It is not connected to authenticated community data yet, has no model registry or drift monitoring, and must not make automatic allocation or emergency decisions. Next work: secure service-to-service authentication, a governed historical dataset, temporal backtesting, seasonal baselines, model comparison, explainability and human approval.
