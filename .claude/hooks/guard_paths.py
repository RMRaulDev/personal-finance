#!/usr/bin/env python3
"""PreToolUse hook: allow Edit/Write only on files matching the given globs.

Usage: guard_paths.py <glob> [<glob> ...]
Globs are relative to the project root. `*` also matches `/`.
Exit code 2 blocks the tool call and shows stderr to the agent.
"""
import json
import os
import sys
from fnmatch import fnmatch

allowed = sys.argv[1:]
event = json.load(sys.stdin)
file_path = event.get("tool_input", {}).get("file_path") or event.get("tool_input", {}).get("notebook_path")
if not file_path:
    sys.exit(0)

project_dir = os.environ.get("CLAUDE_PROJECT_DIR") or event.get("cwd") or os.getcwd()
relative = os.path.relpath(os.path.realpath(file_path), os.path.realpath(project_dir))

if relative.startswith("..") or not any(fnmatch(relative, pattern) for pattern in allowed):
    print(
        f"Blocked: this agent may only edit files matching {', '.join(allowed)}. "
        f"'{relative}' is outside that scope. Report the needed change instead.",
        file=sys.stderr,
    )
    sys.exit(2)
