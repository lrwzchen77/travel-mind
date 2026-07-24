from app.travel_comfort import extract_features, predict_comfort, reset_model_cache


def test_feature_contract_counts_density_weather_and_budget():
    features = extract_features({
        "days": [
            {"attractions": ["a", "b", "c", "d"], "weather": "高温有雨", "transfer": True},
            {"attractions": ["e", "f"], "weather": "晴", "transfer": False},
        ],
        "city_transfers": 1,
        "preferences": ["轻松"],
        "transportation": "flight",
        "budget": 1200,
    })

    assert features == {
        "day_count": 2.0,
        "total_attractions": 6.0,
        "average_attractions": 3.0,
        "max_attractions": 4.0,
        "dense_days": 1.0,
        "transfer_days": 1.0,
        "city_transfers": 1.0,
        "adverse_weather_days": 1.0,
        "relaxed_preference": 1.0,
        "budget_per_day": 600.0,
        "budget_pressure": 1.0,
        "transport_stress": 1.0,
    }


def test_bundled_model_separates_relaxed_and_intense_scenarios():
    reset_model_cache()
    relaxed = predict_comfort({
        "days": [{"attractions": ["a", "b"], "weather": "晴", "transfer": False}] * 3,
        "city_transfers": 0,
        "preferences": ["轻松"],
        "transportation": "train",
        "budget": 5000,
    })
    intense = predict_comfort({
        "days": [{"attractions": ["a", "b", "c", "d", "e"], "weather": "高温有雨", "transfer": True}] * 3,
        "city_transfers": 2,
        "preferences": ["轻松"],
        "transportation": "flight",
        "budget": 1500,
    })

    assert relaxed["model_mode"] == "trained_travel_comfort"
    assert relaxed["comfort_class"] == "relaxed"
    assert intense["comfort_class"] == "intense"
    assert intense["confidence"] > 0.9
