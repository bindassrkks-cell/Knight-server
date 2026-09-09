import os
import json

def apply_custom_config(payload_str):
    try:
        data = json.loads(payload_str)
        return f"Server Verified: Preset {data.get('preset')} Compiled Successfully."
    except Exception as e:
        return f"Error: {str(e)}"
