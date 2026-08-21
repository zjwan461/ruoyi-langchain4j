#!/usr/bin/env python3
"""Fetch weather data from Open-Meteo API."""

import argparse
import json
import sys
from urllib.parse import urlencode
from urllib.request import Request, urlopen
from urllib.error import HTTPError, URLError

OPEN_METEO_URL = "https://api.open-meteo.com/v1/forecast"
TIMEOUT = 15
MAX_FORECAST_DAYS = 16
MAX_PAST_DAYS = 92

WMO_CODES = {
    0: "Clear sky", 1: "Mainly clear", 2: "Partly cloudy", 3: "Overcast",
    45: "Foggy", 48: "Rime fog",
    51: "Light drizzle", 53: "Moderate drizzle", 55: "Dense drizzle",
    56: "Light freezing drizzle", 57: "Dense freezing drizzle",
    61: "Slight rain", 63: "Moderate rain", 65: "Heavy rain",
    66: "Light freezing rain", 67: "Heavy freezing rain",
    71: "Slight snow", 73: "Moderate snow", 75: "Heavy snow", 77: "Snow grains",
    80: "Slight showers", 81: "Moderate showers", 82: "Violent showers",
    85: "Slight snow showers", 86: "Heavy snow showers",
    95: "Thunderstorm", 96: "Thunderstorm w/ hail", 99: "Thunderstorm w/ heavy hail",
}


class SkillError(Exception):
    """Normalized error used by the CLI layer."""

    def __init__(
        self,
        code: str,
        message: str,
        details: str,
        retryable: bool,
        hint: str,
        exit_code: int,
    ) -> None:
        super().__init__(details)
        self.code = code
        self.message = message
        self.details = details
        self.retryable = retryable
        self.hint = hint
        self.exit_code = exit_code

    def as_json(self) -> str:
        return json.dumps(
            {
                "error": {
                    "code": self.code,
                    "message": self.message,
                    "details": self.details,
                    "retryable": self.retryable,
                    "hint": self.hint,
                }
            },
            indent=2,
        )


def wmo_desc(code: int) -> str:
    """Convert WMO weather code to human-readable description."""
    return WMO_CODES.get(code, f"Unknown ({code})")


def fetch_weather(
    lat: float,
    lon: float,
    past_days: int = 0,
    resolution: str = "hourly",
    timezone: str = "auto",
) -> dict:
    """
    Fetch weather from Open-Meteo and return normalized data.

    Args:
        lat, lon: Coordinates from geocode.py.
        past_days: Number of historical days to include (0..92).
        resolution: 'current', 'hourly', or 'daily'.
        timezone: Timezone string. 'auto' uses the location's timezone.

    Returns:
        Dict with keys: meta, current, hourly, daily.
    """
    if past_days < 0 or past_days > MAX_PAST_DAYS:
        raise SkillError(
            code="INVALID_INPUT",
            message="Invalid past days value.",
            details=f"past_days={past_days} is outside supported range.",
            retryable=False,
            hint=f"Use --past-days between 0 and {MAX_PAST_DAYS}.",
            exit_code=1,
        )

    params = {
        "latitude": lat, "longitude": lon, "timezone": timezone,
        "forecast_days": MAX_FORECAST_DAYS,
        "current": "temperature_2m,apparent_temperature,weather_code,wind_speed_10m,relative_humidity_2m,precipitation",
    }
    if past_days > 0:
        params["past_days"] = past_days
    if resolution in ("hourly", "current"):
        params["hourly"] = "temperature_2m,apparent_temperature,precipitation_probability,precipitation,weather_code,wind_speed_10m"
    if resolution in ("daily", "hourly"):
        params["daily"] = "temperature_2m_max,temperature_2m_min,apparent_temperature_max,apparent_temperature_min,precipitation_sum,precipitation_probability_max,weather_code,wind_speed_10m_max"

    url = f"{OPEN_METEO_URL}?{urlencode(params)}"
    req = Request(url, headers={"User-Agent": "WeatherSkill/1.0"})
    try:
        with urlopen(req, timeout=TIMEOUT) as resp:
            raw = json.loads(resp.read().decode("utf-8"))
    except HTTPError as e:
        raise SkillError(
            code="UPSTREAM_API_ERROR",
            message="Weather service returned an error response.",
            details=f"HTTPError: {e.code} {e.reason}",
            retryable=True,
            hint="Retry shortly. If this persists, the upstream service may be degraded.",
            exit_code=2,
        ) from e
    except URLError as e:
        raise SkillError(
            code="NETWORK_UNAVAILABLE",
            message="Unable to reach remote weather service.",
            details=f"URLError: {e.reason}",
            retryable=True,
            hint="Enable outbound network access or rerun with elevated permissions.",
            exit_code=2,
        ) from e
    except json.JSONDecodeError as e:
        raise SkillError(
            code="UPSTREAM_API_ERROR",
            message="Weather service returned invalid data.",
            details=f"JSONDecodeError: {e.msg}",
            retryable=True,
            hint="Retry shortly. If this persists, the upstream service may be degraded.",
            exit_code=2,
        ) from e

    result = {
        "meta": {
            "lat": lat, "lon": lon,
            "timezone": raw.get("timezone", timezone),
            "timezone_abbreviation": raw.get("timezone_abbreviation", ""),
            "elevation": raw.get("elevation"),
            "resolution": resolution,
            "forecast_days": MAX_FORECAST_DAYS,
            "past_days": past_days,
            "forecast_clamped": False,
        },
        "current": None, "hourly": None, "daily": None,
    }

    # Parse current conditions
    if "current" in raw:
        c = raw["current"]
        result["current"] = {
            "time": c.get("time"),
            "temperature_c": c.get("temperature_2m"),
            "apparent_temperature_c": c.get("apparent_temperature"),
            "weather_code": c.get("weather_code"),
            "weather_description": wmo_desc(c.get("weather_code", -1)),
            "wind_speed_kmh": c.get("wind_speed_10m"),
            "humidity_pct": c.get("relative_humidity_2m"),
            "precipitation_mm": c.get("precipitation"),
        }

    # Parse hourly
    if "hourly" in raw:
        h = raw["hourly"]
        n = len(h["time"])
        result["hourly"] = [
            {
                "time": h["time"][i],
                "temperature_c": h.get("temperature_2m", [None]*n)[i],
                "apparent_temperature_c": h.get("apparent_temperature", [None]*n)[i],
                "precipitation_probability_pct": h.get("precipitation_probability", [None]*n)[i],
                "precipitation_mm": h.get("precipitation", [None]*n)[i],
                "weather_code": h.get("weather_code", [None]*n)[i],
                "weather_description": wmo_desc(h.get("weather_code", [None]*n)[i] or -1),
                "wind_speed_kmh": h.get("wind_speed_10m", [None]*n)[i],
            }
            for i in range(n)
        ]

    # Parse daily
    if "daily" in raw:
        d = raw["daily"]
        n = len(d["time"])
        result["daily"] = [
            {
                "date": d["time"][i],
                "temp_max_c": d.get("temperature_2m_max", [None]*n)[i],
                "temp_min_c": d.get("temperature_2m_min", [None]*n)[i],
                "apparent_temp_max_c": d.get("apparent_temperature_max", [None]*n)[i],
                "apparent_temp_min_c": d.get("apparent_temperature_min", [None]*n)[i],
                "precipitation_sum_mm": d.get("precipitation_sum", [None]*n)[i],
                "precipitation_probability_max_pct": d.get("precipitation_probability_max", [None]*n)[i],
                "weather_code": d.get("weather_code", [None]*n)[i],
                "weather_description": wmo_desc(d.get("weather_code", [None]*n)[i] or -1),
                "wind_speed_max_kmh": d.get("wind_speed_10m_max", [None]*n)[i],
            }
            for i in range(n)
        ]

    return result


if __name__ == "__main__":
    p = argparse.ArgumentParser(description="Fetch weather from Open-Meteo")
    p.add_argument("--lat", type=float, required=True, help="Latitude")
    p.add_argument("--lon", type=float, required=True, help="Longitude")
    p.add_argument("--past-days", type=int, default=0, help=f"Historical days to include (0-{MAX_PAST_DAYS})")
    p.add_argument("--resolution", default="hourly", choices=["current", "hourly", "daily"],
                   help="current = now only, hourly = hour-by-hour, daily = day-by-day")
    p.add_argument("--timezone", default="auto", help="Timezone (default: auto = location's TZ)")
    args = p.parse_args()
    if args.past_days < 0 or args.past_days > MAX_PAST_DAYS:
        err = SkillError(
            code="INVALID_INPUT",
            message="Invalid past days value.",
            details=f"past_days={args.past_days} is outside supported range.",
            retryable=False,
            hint=f"Use --past-days between 0 and {MAX_PAST_DAYS}.",
            exit_code=1,
        )
        print(err.as_json())
        sys.exit(err.exit_code)

    try:
        data = fetch_weather(
            args.lat,
            args.lon,
            args.past_days,
            args.resolution,
            args.timezone,
        )
        print(json.dumps(data, indent=2))
    except SkillError as e:
        print(e.as_json())
        sys.exit(e.exit_code)
