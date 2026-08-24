#!/usr/bin/env python3
"""Create a project-local Aether copy that works with ModDevGradle DevLaunch.

Only the Accessories mixin whose lambda target cannot be remapped in a
development run is removed. Release artifacts and Gradle's downloaded JAR stay
untouched.
"""

from __future__ import annotations

import io
import json
import sys
import zipfile
from pathlib import Path


ACCESSORIES_JAR_PREFIX = "META-INF/jarjar/accessories-neoforge-"
MIXIN_CONFIG = "accessories-common.mixins.json"
BROKEN_MIXIN = "client.model.EntityRenderersMixin"
SIGNATURE_SUFFIXES = (".SF", ".RSA", ".DSA", ".EC")


def without_broken_mixin(payload: bytes) -> bytes:
    """Return the nested Accessories JAR with only the DevLaunch-broken mixin removed."""
    result = io.BytesIO()
    with zipfile.ZipFile(io.BytesIO(payload)) as source, zipfile.ZipFile(
        result, "w", compression=zipfile.ZIP_DEFLATED
    ) as target:
        for info in source.infolist():
            name = info.filename
            if name.upper().startswith("META-INF/") and name.upper().endswith(SIGNATURE_SUFFIXES):
                continue
            data = source.read(info)
            if name == MIXIN_CONFIG:
                config = json.loads(data.decode("utf-8"))
                client = config.get("client")
                if not isinstance(client, list) or BROKEN_MIXIN not in client:
                    raise RuntimeError("Accessories mixin configuration does not contain the expected target")
                config["client"] = [entry for entry in client if entry != BROKEN_MIXIN]
                data = (json.dumps(config, indent=2) + "\n").encode("utf-8")
            target.writestr(info, data)
    return result.getvalue()


def patch_aether(source_path: Path, destination_path: Path) -> None:
    destination_path.parent.mkdir(parents=True, exist_ok=True)
    patched = False
    temporary_path = destination_path.with_suffix(destination_path.suffix + ".tmp")
    with zipfile.ZipFile(source_path) as source, zipfile.ZipFile(
        temporary_path, "w", compression=zipfile.ZIP_DEFLATED
    ) as target:
        for info in source.infolist():
            name = info.filename
            if name.upper().startswith("META-INF/") and name.upper().endswith(SIGNATURE_SUFFIXES):
                continue
            data = source.read(info)
            if name.startswith(ACCESSORIES_JAR_PREFIX) and name.endswith(".jar"):
                data = without_broken_mixin(data)
                patched = True
            target.writestr(info, data)
    if not patched:
        temporary_path.unlink(missing_ok=True)
        raise RuntimeError("Aether does not contain the expected embedded Accessories JAR")
    temporary_path.replace(destination_path)


if __name__ == "__main__":
    if len(sys.argv) != 3:
        raise SystemExit("Usage: patch_aether_devlaunch.py SOURCE_JAR DESTINATION_JAR")
    patch_aether(Path(sys.argv[1]), Path(sys.argv[2]))
