import json

def apply_custom_config(payload_str):
    try:
        data = json.loads(payload_str)
        return f"Success: Engine applied config {data.get('preset')}"
    except Exception as e:
        return f"Engine Error: {str(e)}"
