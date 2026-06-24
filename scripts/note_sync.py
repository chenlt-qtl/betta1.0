#!/usr/bin/env python3
"""
Sync a local Obsidian vault with the betta note API.

Config example:
{
  "server": "https://betta.example.com",
  "token": "your-jwt-token",
  "vault": "/Users/me/Documents/Obsidian/MyVault"
}
"""

import argparse
import hashlib
import json
import mimetypes
import os
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

STATE_FILE = ".betta-note-sync.json"
ALLOWED_EXTENSIONS = {".md", ".jpg", ".jpeg", ".png", ".gif", ".webp", ".svg"}


def is_ignored(path: Path, root: Path) -> bool:
    try:
        relative = path.relative_to(root)
    except ValueError:
        return True
    for part in relative.parts:
        if part.startswith(".") or part in {".obsidian", ".trash"}:
            return True
        if part.endswith(".tmp") or part.endswith(".swp") or part.endswith("~"):
            return True
    return False


def file_hash(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def rel_path(path: Path, root: Path) -> str:
    return path.relative_to(root).as_posix()


def scan_local(vault: Path) -> dict:
    manifest = {}
    for path in vault.rglob("*"):
        if not path.is_file() or is_ignored(path, vault) or path.suffix.lower() not in ALLOWED_EXTENSIONS:
            continue
        manifest[rel_path(path, vault)] = {
            "path": rel_path(path, vault),
            "hash": file_hash(path),
            "size": path.stat().st_size,
            "updateTime": int(path.stat().st_mtime * 1000),
        }
    return manifest


def load_state(vault: Path) -> dict:
    state_path = vault / STATE_FILE
    if not state_path.exists():
        return {}
    with state_path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def save_state(vault: Path, state: dict) -> None:
    with (vault / STATE_FILE).open("w", encoding="utf-8") as handle:
        json.dump(state, handle, ensure_ascii=False, indent=2, sort_keys=True)


class BettaClient:
    def __init__(self, server: str, token: str):
        self.server = server.rstrip("/")
        self.token = token

    def request(self, method: str, path: str, data=None, headers=None, raw=False):
        headers = headers or {}
        headers["Authorization"] = "Bearer " + self.token
        url = self.server + path
        request = urllib.request.Request(url, data=data, headers=headers, method=method)
        try:
            with urllib.request.urlopen(request, timeout=60) as response:
                body = response.read()
        except urllib.error.HTTPError as error:
            detail = error.read().decode("utf-8", errors="replace")
            raise RuntimeError(f"{method} {path} failed: {error.code} {detail}") from error
        if raw:
            return body
        if not body:
            return None
        payload = json.loads(body.decode("utf-8"))
        if payload.get("code") != 200:
            raise RuntimeError(payload.get("msg") or f"{method} {path} failed")
        return payload.get("data")

    def manifest(self) -> dict:
        data = self.request("GET", "/system/note/sync/manifest") or []
        return {item["path"]: item for item in data}

    def download(self, remote_path: str, target: Path) -> None:
        query = urllib.parse.urlencode({"path": remote_path})
        body = self.request("GET", f"/system/note/sync/download?{query}", raw=True)
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(body)

    def delete(self, remote_path: str) -> None:
        query = urllib.parse.urlencode({"path": remote_path})
        self.request("DELETE", f"/system/note/sync/delete?{query}")

    def upload(self, remote_path: str, local_path: Path, last_hash: str = "") -> None:
        boundary = "----betta-note-sync-" + hashlib.md5(str(time.time()).encode()).hexdigest()
        mime_type = mimetypes.guess_type(local_path.name)[0] or "application/octet-stream"
        fields = {"path": remote_path}
        if last_hash:
            fields["lastKnownHash"] = last_hash
        body = bytearray()
        for name, value in fields.items():
            body.extend(f"--{boundary}\r\n".encode())
            body.extend(f'Content-Disposition: form-data; name="{name}"\r\n\r\n'.encode())
            body.extend(str(value).encode())
            body.extend(b"\r\n")
        body.extend(f"--{boundary}\r\n".encode())
        body.extend(
            f'Content-Disposition: form-data; name="file"; filename="{local_path.name}"\r\n'
            f"Content-Type: {mime_type}\r\n\r\n".encode()
        )
        body.extend(local_path.read_bytes())
        body.extend(b"\r\n")
        body.extend(f"--{boundary}--\r\n".encode())
        self.request("POST", "/system/note/sync/upload", bytes(body), {
            "Content-Type": f"multipart/form-data; boundary={boundary}",
        })


def conflict_name(path: Path, side: str) -> Path:
    stamp = time.strftime("%Y%m%d%H%M%S")
    return path.with_name(f"{path.stem}.conflict-{side}-{stamp}{path.suffix}")


def sync(config: dict) -> None:
    vault = Path(config["vault"]).expanduser().resolve()
    vault.mkdir(parents=True, exist_ok=True)
    client = BettaClient(config["server"], config["token"])
    state = load_state(vault)
    local = scan_local(vault)
    remote = client.manifest()

    for path in sorted(set(local) | set(remote) | set(state)):
        local_entry = local.get(path)
        remote_entry = remote.get(path)
        last_entry = state.get(path)
        last_hash = (last_entry or {}).get("hash", "")
        local_path = vault / path

        if local_entry and remote_entry:
            if local_entry["hash"] == remote_entry["hash"]:
                continue
            local_changed = bool(last_hash and local_entry["hash"] != last_hash)
            remote_changed = bool(last_hash and remote_entry["hash"] != last_hash)
            if local_changed and remote_changed:
                server_conflict = conflict_name(local_path, "server")
                client.download(path, server_conflict)
                client.upload(rel_path(conflict_name(local_path, "local"), vault), local_path)
                print(f"conflict: kept local {path}, saved server copy {server_conflict}")
            elif local_changed or (not last_hash and local_entry["updateTime"] >= remote_entry["updateTime"]):
                client.upload(path, local_path, last_hash)
                print(f"uploaded: {path}")
            else:
                client.download(path, local_path)
                print(f"downloaded: {path}")
        elif local_entry and not remote_entry:
            if last_hash and local_entry["hash"] == last_hash:
                local_path.unlink()
                print(f"deleted local: {path}")
            else:
                client.upload(path, local_path, last_hash)
                print(f"uploaded new: {path}")
        elif remote_entry and not local_entry:
            if last_hash and remote_entry["hash"] == last_hash:
                client.delete(path)
                print(f"deleted remote: {path}")
            else:
                client.download(path, local_path)
                print(f"downloaded new: {path}")

    save_state(vault, client.manifest())


def main():
    parser = argparse.ArgumentParser(description="Sync local Obsidian vault with betta notes.")
    parser.add_argument("--config", required=True, help="Path to JSON config file.")
    args = parser.parse_args()
    with open(args.config, "r", encoding="utf-8") as handle:
        config = json.load(handle)
    sync(config)


if __name__ == "__main__":
    main()
