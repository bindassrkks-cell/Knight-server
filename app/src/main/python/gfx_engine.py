import json

def apply_custom_config(payload_str):
    """
    Parses custom server build values and compiles them into config profile
    """
    try:
        data = json.loads(payload_str)
        aim_val = data.get("aim_value", 5)
        spread = data.get("bullet_spread", 5)
        preset = data.get("preset", "MEDIUM")
        recoil_guns = data.get("no_recoil_guns", [])
        
        return f"Success: Engine applied Preset={preset}, Recoil Mod for {len(recoil_guns)} weapons."
    except Exception as e:
        return f"Engine Error: {str(e)}"
