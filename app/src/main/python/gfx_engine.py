import json

def apply_custom_config(payload_str):
    try:
        data = json.loads(payload_str)
        return f"Success: Built config with Aim={data.get('aimValue')}, Spread={data.get('bulletSpread')}"
    except Exception as e:
        return f"Engine Error: {str(e)}"
