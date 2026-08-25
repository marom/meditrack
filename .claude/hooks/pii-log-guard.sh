#!/bin/bash
input=$(cat)
content=$(printf '%s' "$input" | jq -r '.tool_input.content // .tool_input.new_string // empty')

if [ -z "$content" ]; then
  exit 0
fi

# Collapse to one line, then split into ';'-terminated statements so a log call
# whose arguments span multiple lines is still checked as a single unit.
violation=$(printf '%s;' "$content" | tr '\n' ' ' \
  | grep -oE '[^;]*;' \
  | grep -inE '(log|logger)\.(info|debug|warn|error|trace)\s*\(|System\.out\.print(ln)?\s*\(' \
  | grep -iE 'email|phone')

if [ -n "$violation" ]; then
  echo "BLOCKED: log statement appears to expose patient email or phone. Remove PII from log output." >&2
  echo "$violation" >&2
  exit 2
fi

exit 0
