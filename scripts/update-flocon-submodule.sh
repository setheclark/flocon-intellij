#!/usr/bin/env bash

set -e

SUBMODULE_PATH="flocon-upstream"

echo "Updating Flocon submodule to latest release tag..."

# Navigate to submodule directory
cd "$SUBMODULE_PATH"

# Fetch all tags from remote
echo "Fetching tags from remote..."
git fetch --tags

# Get the latest tag (sorted by version)
LATEST_TAG=$(git tag -l | grep -E '^[0-9]+\.[0-9]+\.[0-9]+$' | sort -V | tail -n 1)

if [ -z "$LATEST_TAG" ]; then
    echo "Error: No release tags found"
    exit 1
fi

echo "Latest release tag: $LATEST_TAG"

# Get current tag/commit for comparison
CURRENT_REF=$(git describe --tags --exact-match 2>/dev/null || git rev-parse --short HEAD)
echo "Current ref: $CURRENT_REF"

if [ "$CURRENT_REF" = "$LATEST_TAG" ]; then
    echo "Already at latest release tag ($LATEST_TAG)"
    cd ..
    exit 0
fi

# Checkout the latest tag
echo "Checking out $LATEST_TAG..."
git checkout "$LATEST_TAG"

# Return to parent directory
cd ..

# Stage the submodule update
echo "Staging submodule update..."
git add "$SUBMODULE_PATH"

echo ""
echo "✓ Submodule updated from $CURRENT_REF to $LATEST_TAG"
echo ""
echo "To commit this change, run:"
echo "  git commit -m \"Update Flocon submodule to $LATEST_TAG\""
