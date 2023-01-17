#!/usr/bin/env bash



# Shell setup
# -----------

BASEPATH="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"  # Absolute canonical path
# shellcheck source=bash.conf.sh
source "$BASEPATH/bash.conf.sh" || exit 1

log "==================== BEGIN ===================="

# Trace all commands
set -o xtrace



# Don't build from experimental branches. Otherwise we'd need to have some
# mechanism to publish experimental module builds, which we don't have for
# Java and JavaScript modules.
#
# Maybe in the future we might have something like experimental Maven or NPM
# repositories, then we'd want to build experimental branches for them. But
# for now, just skip and avoid polluting the default builds repositories.
JOB_GIT_NAME="${JOB_GIT_NAME:-main}"
GIT_DEFAULT="$(kurento_git_default_branch.sh)"
if [[ "$JOB_GIT_NAME" != "$GIT_DEFAULT" ]]; then
  log "Skip building from experimental branch '$JOB_GIT_NAME'"
  exit 0
fi

rm -rf build
mkdir build ; cd build/
cmake .. -DGENERATE_JS_CLIENT_PROJECT=TRUE -DDISABLE_LIBRARIES_GENERATION=TRUE || {
  log "ERROR: Command failed: cmake"
  exit 1
}

[ -d js ] || {
  log "ERROR: Expected directory doesn't exist: $PWD/js"
  exit 1
}

# FIXME - When generating for the kurento-module-filters module, the JSON keys in
# 'js/src/filters.kmd.json' change ordering each time this job runs. So the git
# commit history is polluted with meaningless variations in key order.
# I haven't found the cause for this issue, and it is really a time sink without
# much payoff, so the easy way is to force a sorting order right here
for FILE in js/src/*.kmd.json; do
    jq '.remoteClasses? |= sort_by(.name)' "$FILE" >"${FILE}.tmp"
    mv "${FILE}.tmp" "$FILE"
done

JS_PROJECT="$(cat js_project_name)-js"
log "Generated sources: $JS_PROJECT"

kurento_clone_repo.sh "$JS_PROJECT" "$JOB_GIT_NAME" || {
  log "ERROR: Command failed: kurento_clone_repo $JS_PROJECT $JOB_GIT_NAME"
  exit 1
}

rm -rf "${JS_PROJECT:?}"/*
cp -a js/* "${JS_PROJECT:?}"/

log "Commit and push changes to repo: $JS_PROJECT"
GIT_COMMIT="$(git rev-parse --short HEAD)"
{
    pushd "$JS_PROJECT"

    git status

    git diff-index --quiet HEAD || {
      # `--all` to include possibly deleted files.
      git add --all .
      git commit -m "Code autogenerated from Kurento/${KURENTO_PROJECT}@${GIT_COMMIT}"

      # Use the repo default branch.
      GIT_DEFAULT="$(kurento_git_default_branch.sh)"

      git push origin "$GIT_DEFAULT" || {
        log "ERROR: Command failed: git push ($JS_PROJECT)"
        exit 1
      }
    }

    popd  # $JS_PROJECT
}



log "==================== END ===================="
