#!/system/bin/sh
# customize.sh - runs during Magisk module installation (as root)
# Generates a random seed used by the ID Changer LSPosed module.

SEED_DIR="/data/adb/idchanger"
SEED_FILE="$SEED_DIR/seed"

ui_print "- Setting up ID Changer seed..."

# Create storage directory
mkdir -p "$SEED_DIR"

if [ -f "$SEED_FILE" ] && [ -s "$SEED_FILE" ]; then
    ui_print "- Existing seed found, keeping it (IDs will remain stable)"
else
    # Generate 32 random hex chars as the seed
    SEED=$(cat /dev/urandom | tr -dc 'a-f0-9' | head -c 32 2>/dev/null)
    if [ -z "$SEED" ]; then
        # Fallback if /dev/urandom isn't available in this context
        SEED=$(date +%s%N | sha256sum | head -c 32)
    fi
    echo "$SEED" > "$SEED_FILE"
    ui_print "- New random seed generated"
fi

# World-readable so the LSPosed module (running as app UID) can read it
chmod 644 "$SEED_FILE"
chown root:root "$SEED_FILE"

ui_print "- Seed stored at $SEED_FILE"
ui_print "- Install the ID Changer APK via LSPosed Manager to complete setup"
ui_print "- Done!"
