# Weather Forecast Skill

An AI agent skill that fetches current weather conditions and forecasts for any location worldwide using free, open APIs — no API keys required.

## How It Works

1. **Geocode** a location name to coordinates using [Nominatim](https://nominatim.openstreetmap.org/) (OpenStreetMap)
2. **Fetch weather** data for those coordinates using [Open-Meteo](https://open-meteo.com/)
3. Respond with a formatted summary (current conditions, hourly/daily forecasts, clothing advice for travelers)

## Project Structure

```
scripts/
  geocode.py          # Location name → lat/lon coordinates
  fetch_weather.py    # Lat/lon → weather data (current, hourly, daily)
references/
  clothing_guide.md   # Clothing recommendations by weather condition
SKILL.md              # Full skill specification for the AI agent
```

## Quick Usage

```bash
# Geocode a location
python scripts/geocode.py "Tokyo"

# Current weather
python scripts/fetch_weather.py --lat 35.6764 --lon 139.65 --resolution current

# Hourly forecast
python scripts/fetch_weather.py --lat 35.6764 --lon 139.65 --resolution hourly

# Daily forecast
python scripts/fetch_weather.py --lat 35.6764 --lon 139.65 --resolution daily

# Past weather (last 7 days)
python scripts/fetch_weather.py --lat 35.6764 --lon 139.65 --resolution daily --past-days 7
```

## APIs

| API | Purpose | Auth |
|-----|---------|------|
| [Nominatim](https://nominatim.openstreetmap.org/) | Geocoding (location → coordinates) | None |
| [Open-Meteo](https://open-meteo.com/) | Weather data (current, hourly, daily forecasts up to 16 days) | None |

## Requirements

- Python 3 (standard library only, no external packages)
- Outbound internet access to reach the APIs
