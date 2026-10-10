#!/usr/bin/env python3
"""PreToolUse hook: allow only Bash commands that fully match one of the given regexes.

Usage: guard_bash.py <regex> [<regex> ...]
Command chaining, redirection, and substitution are always rejected so an
allowed prefix cannot be used to smuggle in a write.
Exit code 2 blocks the tool call and shows stderr to the agent.
"""
import json
import re
import sys

allowed = sys.argv[1:]
event = json.load(sys.stdin)
command = event.get("tool_input", {}).get("command", "").strip()

forbidden = re.compile(r"[;&|<>`\n]|\$\(|--output")
if forbidden.search(command) or not any(re.fullmatch(pattern, command) for pattern in allowed):
    print(
        f"Blocked: this agent may only run read-only commands matching {allowed}, "
        "without pipes, redirection, chaining, or substitution. Use Read, Grep, or Glob instead.",
        file=sys.stderr,
    )
    sys.exit(2)
