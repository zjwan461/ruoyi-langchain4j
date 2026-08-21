#!/usr/bin/env python3
"""Geocode locations using OpenStreetMap Nominatim API."""

import json
import sys
from typing import Optional
from urllib.parse import quote_plus
from urllib.request import Request, urlopen
from urllib.error import HTTPError, URLError

NOMINATIM_URL = "https://nominatim.openstreetmap.org/search"
USER_AGENT = "WeatherSkill/1.0 (claude-assistant)"
TIMEOUT = 10


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
            ensure_ascii=False,
        )


def geocode(location_query: str, max_results: int = 3) -> list[dict]:
    """Geocode a location string to lat/lon candidates."""
    url = f"{NOMINATIM_URL}?q={quote_plus(location_query)}&format=json&limit={max_results}&addressdetails=1"
    req = Request(url, headers={"User-Agent": USER_AGENT})
    try:
        with urlopen(req, timeout=TIMEOUT) as resp:
            data = json.loads(resp.read().decode("utf-8"))
    except HTTPError as e:
        raise SkillError(
            code="UPSTREAM_API_ERROR",
            message="Geocoding service returned an error response.",
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
            message="Geocoding service returned invalid data.",
            details=f"JSONDecodeError: {e.msg}",
            retryable=True,
            hint="Retry shortly. If this persists, the upstream service may be degraded.",
            exit_code=2,
        ) from e
    return [
        {
            "display_name": item.get("display_name", ""),
            "lat": float(item["lat"]),
            "lon": float(item["lon"]),
            "importance": float(item.get("importance", 0)),
            "type": item.get("type", ""),
        }
        for item in data
    ]


def best_match(location_query: str) -> Optional[dict]:
    """Return best geocoding match with ambiguity flag."""
    results = geocode(location_query)
    if not results:
        return None
    best = results[0]
    best["ambiguous"] = (
        len(results) >= 2
        and (best["importance"] - results[1]["importance"]) < 0.05
    )
    if best["ambiguous"]:
        best["alternatives"] = [r["display_name"] for r in results[1:]]
    return best


if __name__ == "__main__":
    if len(sys.argv) < 2:
        err = SkillError(
            code="INVALID_INPUT",
            message="Missing required location query.",
            details='Usage: python geocode.py "<location>"',
            retryable=False,
            hint='Provide a city or location string, for example: "Paris, France".',
            exit_code=1,
        )
        print(err.as_json())
        sys.exit(err.exit_code)
    try:
        result = best_match(" ".join(sys.argv[1:]))
        if result:
            print(json.dumps(result, indent=2, ensure_ascii=False))
            sys.exit(0)
        err = SkillError(
            code="INVALID_INPUT",
            message="Location not found.",
            details="No results found",
            retryable=False,
            hint="Use a more specific location (city, region, country).",
            exit_code=1,
        )
        print(err.as_json())
        sys.exit(err.exit_code)
    except SkillError as e:
        print(e.as_json())
        sys.exit(e.exit_code)
