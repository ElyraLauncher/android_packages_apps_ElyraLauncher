#!/usr/bin/env bash
#
# Copyright 2026, The Elyra Launcher Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Developer Certificate of Origin (DCO) check.
#
# Verifies that every commit introduced by a branch / pull request carries a
# valid "Signed-off-by:" trailer (as produced by `git commit -s`). It compares
# the head against the merge base with the target branch, so only the commits
# actually introduced by the change are inspected -- existing history is never
# rewritten or judged.
#
# Usage:
#   scripts/ci/check-dco.sh [BASE_REF] [HEAD_REF]
#
# Defaults:
#   BASE_REF = origin/main (or GITHUB_BASE_REF when set by Actions)
#   HEAD_REF = HEAD        (or GITHUB_SHA when set by Actions)
#
# Exit status: 0 when all introduced commits are signed off, 1 otherwise.

set -euo pipefail

# Resolve the base reference.
if [ "${1:-}" != "" ]; then
  BASE_REF="$1"
elif [ "${GITHUB_BASE_REF:-}" != "" ]; then
  BASE_REF="origin/${GITHUB_BASE_REF}"
else
  BASE_REF="origin/main"
fi

# Resolve the head reference.
if [ "${2:-}" != "" ]; then
  HEAD_REF="$2"
elif [ "${GITHUB_SHA:-}" != "" ]; then
  HEAD_REF="${GITHUB_SHA}"
else
  HEAD_REF="HEAD"
fi

# Determine the merge base so we only inspect commits introduced on top of base.
if ! MERGE_BASE="$(git merge-base "$BASE_REF" "$HEAD_REF" 2>/dev/null)"; then
  # No common ancestor resolvable (e.g. shallow clone without base); fall back to
  # inspecting the head commit only rather than silently passing everything.
  echo "warning: could not compute merge-base of '$BASE_REF' and '$HEAD_REF';" \
       "inspecting '$HEAD_REF' only." >&2
  MERGE_BASE="${HEAD_REF}~1"
fi

RANGE="${MERGE_BASE}..${HEAD_REF}"

# List commit hashes introduced in the range (empty when nothing new).
mapfile -t COMMITS < <(git rev-list --no-merges "$RANGE" 2>/dev/null || true)

if [ "${#COMMITS[@]}" -eq 0 ]; then
  echo "DCO: no new non-merge commits to check in range ${RANGE}."
  exit 0
fi

echo "DCO: checking ${#COMMITS[@]} commit(s) in range ${RANGE}"

FAILED=0
for sha in "${COMMITS[@]}"; do
  subject="$(git show -s --format='%s' "$sha")"
  # A valid trailer looks like: "Signed-off-by: Full Name <email@example>".
  # We check for presence and basic shape; we intentionally do NOT print emails
  # from the trailer to avoid leaking contributor addresses into public logs.
  if git show -s --format='%B' "$sha" \
      | grep -Eiq '^Signed-off-by: .+ <[^>]+>[[:space:]]*$'; then
    echo "  PASS  ${sha:0:12}  ${subject}"
  else
    echo "  FAIL  ${sha:0:12}  ${subject}  (missing valid Signed-off-by trailer)"
    FAILED=1
  fi
done

if [ "$FAILED" -ne 0 ]; then
  cat >&2 <<'EOF'

DCO check failed: one or more commits are missing a valid "Signed-off-by:"
trailer. Sign off your commits with:

    git commit -s

To fix existing commits on your branch, amend or rebase to add the trailer, then
force-push your own branch:

    git rebase --signoff <base>

By signing off you certify the Developer Certificate of Origin (https://developercertificate.org/).
EOF
  exit 1
fi

echo "DCO: all introduced commits are signed off."
exit 0
