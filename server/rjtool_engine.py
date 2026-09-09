import os
import sys
import shutil
import zipfile

BASE = os.path.join(os.path.dirname(__file__), "RJTOOL")
PAK_ORIGINAL = os.path.join(BASE, "PAK_ORIGINAL")
PAK_UNPACK = os.path.join(BASE, "PAK_UNPACK")
EDITTED = os.path.join(BASE, "EDITTED")
RESULT_PAK = os.path.join(BASE, "RESULT_PAK")

def execute_pipeline(data_payload):
    print("[SOCKET] Step 1: Inspecting PAK_ORIGINAL for base pak archive...")
    shutil.rmtree(PAK_UNPACK, ignore_errors=True)
    os.makedirs(PAK_UNPACK, exist_ok=True)
    os.makedirs(RESULT_PAK, exist_ok=True)

    orig_files = [f for f in os.listdir(PAK_ORIGINAL) if f.endswith(".pak") or f.endswith(".zip")]
    if orig_files:
        orig_pak = os.path.join(PAK_ORIGINAL, orig_files[0])
        print(f"[SOCKET] Found base: {orig_files[0]}. Unpacking into /PAK_UNPACK...")
        if zipfile.is_zipfile(orig_pak):
            with zipfile.ZipFile(orig_pak, 'r') as zf:
                zf.extractall(PAK_UNPACK)
        else:
            shutil.copytree(PAK_ORIGINAL, PAK_UNPACK, dirs_exist_ok=True)
    else:
        print("[SOCKET] Notice: No pak found in PAK_ORIGINAL. Initializing base asset skeleton...")
        os.makedirs(os.path.join(PAK_UNPACK, "ShadowTrackerExtra/Saved/Config"), exist_ok=True)

    print("[SOCKET] Step 2: Overwriting base with EDITTED files (.uasset, .uexp, .lua)...")
    replaced = 0
    for root, _, files in os.walk(EDITTED):
        for file in files:
            src_file = os.path.join(root, file)
            rel = os.path.relpath(src_file, EDITTED)
            dst_file = os.path.join(PAK_UNPACK, rel)
            os.makedirs(os.path.dirname(dst_file), exist_ok=True)
            shutil.copy2(src_file, dst_file)
            replaced += 1
            print(f"[SOCKET] Injected EDITTED asset: {rel}")

    print(f"[SOCKET] Total {replaced} assets replaced.")

    out_pak = os.path.join(RESULT_PAK, "game_patch_4.5.0.21370.pak")
    print("[SOCKET] Step 3: Compiling & Repacking into RESULT_PAK/game_patch_4.5.0.21370.pak...")
    with zipfile.ZipFile(out_pak, 'w', zipfile.ZIP_DEFLATED) as z_out:
        for root, _, files in os.walk(PAK_UNPACK):
            for file in files:
                f_path = os.path.join(root, file)
                rel_p = os.path.relpath(f_path, PAK_UNPACK)
                z_out.write(f_path, rel_p)

    print("[DONE] 100% Repack finished. Available for app download!")

if __name__ == "__main__":
    execute_pipeline(sys.argv[1] if len(sys.argv) > 1 else "{}")
