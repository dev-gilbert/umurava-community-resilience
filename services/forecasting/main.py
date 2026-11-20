from datetime import datetime, timedelta, timezone
from math import sqrt
from typing import Literal
from uuid import UUID

import numpy as np
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field, model_validator
from sklearn.linear_model import LinearRegression
from sklearn.metrics import mean_squared_error

app = FastAPI(title="UMURAVA Forecasting Service", version="0.1.0")

class Observation(BaseModel):
    observed_at: datetime
    value: float = Field(ge=0, le=1_000_000_000)

class ForecastRequest(BaseModel):
    community_id: UUID
    resource_category: Literal["FOOD", "WATER", "ENERGY"]
    unit: str = Field(min_length=1, max_length=30)
    observations: list[Observation] = Field(min_length=5, max_length=365)
    horizon_days: int = Field(default=7, ge=1, le=90)

    @model_validator(mode="after")
    def validate_order(self):
        stamps = [item.observed_at for item in self.observations]
        if any(stamps[i] >= stamps[i + 1] for i in range(len(stamps) - 1)):
            raise ValueError("observations must be strictly ordered by observed_at")
        if len({stamp.date() for stamp in stamps}) < 5:
            raise ValueError("observations must cover at least five distinct dates")
        return self

class ForecastPoint(BaseModel):
    forecast_date: datetime
    predicted_value: float

class ForecastResponse(BaseModel):
    community_id: UUID
    resource_category: str
    unit: str
    model: str
    horizon_days: int
    training_points: int
    rmse: float
    generated_at: datetime
    forecasts: list[ForecastPoint]
    warning: str

@app.get("/health")
def health():
    return {"status": "UP", "service": "umurava-forecasting"}

@app.post("/v1/forecast", response_model=ForecastResponse)
def forecast(request: ForecastRequest):
    first = request.observations[0].observed_at
    x = np.array([[(item.observed_at - first).total_seconds() / 86400.0] for item in request.observations])
    y = np.array([item.value for item in request.observations], dtype=float)
    if not np.isfinite(x).all() or not np.isfinite(y).all():
        raise HTTPException(status_code=422, detail="observations must contain finite values")

    model = LinearRegression()
    model.fit(x, y)
    fitted = model.predict(x)
    rmse = sqrt(mean_squared_error(y, fitted))
    last = request.observations[-1].observed_at
    future_x = np.array([[x[-1][0] + day] for day in range(1, request.horizon_days + 1)])
    predictions = np.maximum(model.predict(future_x), 0.0)
    points = [
        ForecastPoint(
            forecast_date=(last + timedelta(days=day)).astimezone(timezone.utc),
            predicted_value=round(float(predictions[day - 1]), 3),
        )
        for day in range(1, request.horizon_days + 1)
    ]
    return ForecastResponse(
        community_id=request.community_id,
        resource_category=request.resource_category,
        unit=request.unit,
        model="LINEAR_TREND_BASELINE",
        horizon_days=request.horizon_days,
        training_points=len(request.observations),
        rmse=round(float(rmse), 3),
        generated_at=datetime.now(timezone.utc),
        forecasts=points,
        warning=("Experimental baseline only. Not an operational allocation decision; verify source data and local conditions before acting."),
    )
