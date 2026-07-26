#!/bin/bash
# Sincroniza skills de diseño para Atlas:
# - https://github.com/emilkowalski/skill → .skills/emil-design-eng/
# - https://github.com/Leonxlnx/taste-skill → .cursor/skills/* (+ taste/redesign en .skills/)
# - https://github.com/pbakaus/impeccable → .cursor/skills/impeccable/
# Fallback sin red: .skills-vendor/ y .cursor/skills-vendor/
#
# Nota: a diferencia de Autotube, no se sincronizan flutter-ai-rules (Atlas no es Flutter).

set -euo pipefail

emil_repo="https://github.com/emilkowalski/skill.git"
taste_repo="https://github.com/Leonxlnx/taste-skill.git"
impeccable_repo="https://github.com/pbakaus/impeccable.git"
emil_cache=".skills/.emil-skill-head"
taste_cache=".cursor/.taste-skill-head"
impeccable_cache=".cursor/.impeccable-skill-head"
tmp_dir=""

taste_skills_in_dot_skills=(
  taste-skill
  redesign-skill
  gpt-tasteskill
)

cleanup() {
  if [[ -n "${tmp_dir:-}" && -d "$tmp_dir" ]]; then
    rm -rf "$tmp_dir"
  fi
}
trap cleanup EXIT

mkdir -p .skills .cursor/skills

remote_head() {
  git ls-remote "$1" HEAD 2>/dev/null | awk '{print $1}'
}

cached_head() {
  local f="$1"
  if [[ -f "$f" ]]; then
    tr -d '\n\r' < "$f"
  fi
}

needs_sync() {
  local repo="$1" cache="$2"
  local remote local
  remote="$(remote_head "$repo")"
  if [[ -z "${remote:-}" ]]; then
    return 1
  fi
  local="$(cached_head "$cache")"
  [[ "$remote" != "$local" ]]
}

ensure_tmp() {
  if [[ -z "${tmp_dir:-}" ]]; then
    tmp_dir="$(mktemp -d)"
  fi
}

sync_emil_design_eng() {
  local remote
  remote="$(remote_head "$emil_repo")"
  [[ -n "${remote:-}" ]] || return 0

  ensure_tmp
  git -c core.hooksPath=/dev/null clone --depth 1 "$emil_repo" "$tmp_dir/emil"
  rm -rf .skills/emil-design-eng
  cp -R "$tmp_dir/emil/skills/emil-design-eng" .skills/emil-design-eng
  printf '%s\n' "$remote" > "$emil_cache"
}

sync_taste_skills() {
  local remote name skill_dir
  remote="$(remote_head "$taste_repo")"
  [[ -n "${remote:-}" ]] || return 0

  ensure_tmp
  if [[ ! -d "$tmp_dir/taste" ]]; then
    git -c core.hooksPath=/dev/null clone --depth 1 "$taste_repo" "$tmp_dir/taste"
  fi
  if [[ ! -d "$tmp_dir/taste/skills" ]]; then
    echo "sync-skills: taste-skill sin carpeta skills/" >&2
    return 1
  fi

  for skill_dir in "$tmp_dir/taste/skills"/*/; do
    [[ -d "$skill_dir" ]] || continue
    [[ -f "$skill_dir/SKILL.md" ]] || continue
    name="$(basename "$skill_dir")"
    rm -rf ".cursor/skills/$name"
    cp -R "$skill_dir" ".cursor/skills/$name"
  done

  for name in "${taste_skills_in_dot_skills[@]}"; do
    if [[ -d "$tmp_dir/taste/skills/$name" ]]; then
      rm -rf ".skills/$name"
      cp -R "$tmp_dir/taste/skills/$name" ".skills/$name"
    fi
  done

  printf '%s\n' "$remote" > "$taste_cache"
}

sync_impeccable() {
  local remote
  remote="$(remote_head "$impeccable_repo")"
  [[ -n "${remote:-}" ]] || return 0

  ensure_tmp
  if [[ ! -d "$tmp_dir/impeccable" ]]; then
    git -c core.hooksPath=/dev/null clone --depth 1 "$impeccable_repo" "$tmp_dir/impeccable"
  fi
  if [[ ! -d "$tmp_dir/impeccable/.cursor/skills/impeccable" ]]; then
    echo "sync-skills: impeccable sin .cursor/skills/impeccable" >&2
    return 1
  fi
  rm -rf .cursor/skills/impeccable
  cp -R "$tmp_dir/impeccable/.cursor/skills/impeccable" .cursor/skills/impeccable
  printf '%s\n' "$remote" > "$impeccable_cache"
}

sync_dot_skills_vendor_fallback() {
  [[ -d .skills-vendor ]] || return 0
  for vendored in .skills-vendor/*/; do
    [[ -d "$vendored" ]] || continue
    local name
    name="$(basename "$vendored")"
    [[ -f "$vendored/SKILL.md" ]] || continue
    if [[ ! -f ".skills/$name/SKILL.md" ]]; then
      rm -rf ".skills/$name"
      cp -R "$vendored" ".skills/$name"
    fi
  done
}

sync_cursor_skills_vendor_fallback() {
  local src
  for src in .cursor/skills-vendor/*/; do
    [[ -d "$src" ]] || continue
    local name
    name="$(basename "$src")"
    [[ -f "$src/SKILL.md" ]] || continue
    if [[ ! -f ".cursor/skills/$name/SKILL.md" ]]; then
      rm -rf ".cursor/skills/$name"
      cp -R "$src" ".cursor/skills/$name"
    fi
  done
}

emil_stale=0
taste_stale=0
impeccable_stale=0
if needs_sync "$emil_repo" "$emil_cache"; then emil_stale=1; fi
if needs_sync "$taste_repo" "$taste_cache"; then taste_stale=1; fi
if needs_sync "$impeccable_repo" "$impeccable_cache"; then impeccable_stale=1; fi

if [[ "$emil_stale" -eq 0 && "$taste_stale" -eq 0 && "$impeccable_stale" -eq 0 ]]; then
  exit 0
fi

if [[ "$emil_stale" -eq 1 ]]; then
  sync_emil_design_eng || true
fi

if [[ "$taste_stale" -eq 1 ]]; then
  sync_taste_skills || true
fi

if [[ "$impeccable_stale" -eq 1 ]]; then
  sync_impeccable || true
fi

# Mantener espejo de taste skills en .skills/
for name in "${taste_skills_in_dot_skills[@]}"; do
  if [[ -d ".cursor/skills/$name" ]]; then
    rm -rf ".skills/$name"
    cp -R ".cursor/skills/$name" ".skills/$name"
  fi
done

sync_dot_skills_vendor_fallback
sync_cursor_skills_vendor_fallback
