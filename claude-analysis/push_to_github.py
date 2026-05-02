#!/usr/bin/env python3
"""Push claude-analysis to GitHub via Git Data API (no git clone needed)."""
import json, subprocess, base64, os, sys

REPO = "mingtian6000/sandbox"
SRC = os.path.expanduser("~/vscodeProject/claude-analysis")

def gh(method, path, data=None):
    cmd = ["gh", "api", "-X", method, f"repos/{REPO}/{path}", "--jq", "."]
    stdin = None
    if data is not None:
        stdin = json.dumps(data).encode()
        cmd += ["--input", "-"]
    proc = subprocess.run(cmd, input=stdin, capture_output=True, timeout=30)
    if proc.returncode != 0:
        err = proc.stderr.decode() if proc.stderr else "unknown error"
        print(f"  ✗ {path}: {err.strip()}")
        return None
    out = proc.stdout.decode() if proc.stdout else "{}"
    return json.loads(out)

def main():
    # 1. Get current main branch HEAD
    print("→ Getting current HEAD...")
    ref = gh("GET", "git/refs/heads/main")
    if not ref:
        print("Failed to get HEAD ref")
        sys.exit(1)
    latest_sha = ref["object"]["sha"]
    print(f"  HEAD: {latest_sha}")

    # 2. Get base tree
    commit = gh("GET", f"git/commits/{latest_sha}")
    if not commit:
        sys.exit(1)
    base_tree = commit["tree"]["sha"]
    print(f"  Base tree: {base_tree}")

    # 3. Create blobs and build tree entries
    print("\n→ Creating blobs...")
    entries = []
    for root, dirs, files in os.walk(SRC):
        dirs[:] = [d for d in dirs if d not in ("node_modules", "dist", ".git")]
        for fname in files:
            fp = os.path.join(root, fname)
            rel = "claude-analysis/" + os.path.relpath(fp, SRC).replace("\\", "/")
            with open(fp, "rb") as f:
                b64 = base64.b64encode(f.read()).decode()
            blob = gh("POST", "git/blobs",
                       {"content": b64, "encoding": "base64"})
            if not blob:
                print(f"  ✗ {rel}")
                continue
            entries.append({
                "path": rel, "mode": "100644",
                "type": "blob", "sha": blob["sha"]
            })
            print(f"  ✓ {rel}")

    if not entries:
        print("No files to upload")
        sys.exit(1)

    # 4. Create tree with all files attached to root
    print(f"\n→ Creating tree ({len(entries)} files)...")
    tree = gh("POST", "git/trees",
              {"base_tree": base_tree, "tree": entries})
    if not tree:
        sys.exit(1)
    new_tree = tree["sha"]
    print(f"  New tree: {new_tree}")

    # 5. Create commit
    print("\n→ Creating commit...")
    new_commit = gh("POST", "git/commits", {
        "message": "Add claude-analysis architecture report website",
        "tree": new_tree,
        "parents": [latest_sha]
    })
    if not new_commit:
        sys.exit(1)
    cs = new_commit["sha"]
    print(f"  Commit: {cs}")

    # 6. Update main branch
    print("\n→ Updating main branch...")
    result = gh("PATCH", "git/refs/heads/main", {"sha": cs, "force": False})
    if result:
        print(f"\n✓ Success! Pushed to https://github.com/{REPO}")
    else:
        print("\n✗ Failed to update branch")

if __name__ == "__main__":
    main()
