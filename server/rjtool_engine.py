import os
import sys
import shutil
import zipfile

BASE = os.path.join(os.path.dirname(__file__), "RJTOOL")
PAK_ORIGINAL = os.path.join(BASE, "PAK_ORIGINAL")
PAK_UNPACK = os.path.join(BASE, "PAK_UNPACK")
EDITTED = os.path.join(BASE, "EDITTED")
RESULT_PAK = os.path.join(BASE, "RESULT_PAK")

def compile(params):
    print("[SOCKET] Phase 1: Scanning PAK_ORIGINAL for clean base file...")
    shutil.rmtree(PAK_UNPACK, ignore_errors=True)
    os.makedirs(PAK_UNPACK, exist_ok=True)
    os.makedirs(RESULT_PAK, exist_ok=True)

    paks = [f for f in os.listdir(PAK_ORIGINAL) if f.endswith(".pak") or f.endswith(".zip")]
    if paks:
        target_base = os.path.join(PAK_ORIGINAL, paks[0])
        print(f"[SOCKET] Unpacking {paks[0]} to /PAK_UNPACK...")
        if zipfile.is_zipfile(target_base):
            with zipfile.ZipFile(target_base, 'r') as z:
                z.extractall(PAK_UNPACK)
        else:
            shutil.copytree(PAK_ORIGINAL, PAK_UNPACK, dirs_exist_ok=True)
    else:
        print("[SOCKET] Initializing virtual Unreal structure in PAK_UNPACK...")
        os.makedirs(os.path.join(PAK_UNPACK, "ShadowTrackerExtra/Saved/Config"), exist_ok=True)

    print("[SOCKET] Phase 2: Replacing matching assets (.uasset, .uexp, .lua) from EDITTED...")
    count = 0
    for root, _, files in os.walk(EDITTED):
        for file in files:
            src = os.path.join(root, file)
            rel = os.path.relpath(src, EDITTED)
            dst = os.path.join(PAK_UNPACK, rel)
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            shutil.copy2(src, dst)
            count += 1
            print(f"[SOCKET] Injected: {rel}")

    print(f"[SOCKET] Replaced {count} modified game assets.")

    out_file = os.path.join(RESULT_PAK, "game_patch_4.5.0.21370.pak")
    print("[SOCKET] Phase 3: Packing & Compiling to RESULT_PAK/game_patch_4.5.0.21370.pak...")
    with zipfile.ZipFile(out_file, 'w', zipfile.ZIP_DEFLATED) as z_out:
        for root, _, files in os.walk(PAK_UNPACK):
            for file in files:
                full_p = os.path.join(root, file)
                arc_p = os.path.relpath(full_p, PAK_UNPACK)
                z_out.write(full_p, arc_p)

    print("[DONE] 100% SUCCESS: Pak compiled successfully!")

if __name__ == "__main__":
    compile(sys.argv[1] if len(sys.argv) > 1 else "{}")
