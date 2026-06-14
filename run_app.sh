#!/bin/bash

# Find connected devices and emulators
devices=($(adb devices | grep -v "List of devices" | grep -v "^$" | awk '{print $1}'))

if [ ${#devices[@]} -eq 0 ]; then
  echo "No connected devices or emulators found."
  exit 1
fi

echo "Available devices:"
for i in "${!devices[@]}"; do
  echo "[$i] ${devices[$i]}"
done

read -p "Choose a device number [0-$(( ${#devices[@]} - 1 ))]: " choice

if [[ "$choice" =~ ^[0-9]+$ ]] && [ "$choice" -lt "${#devices[@]}" ]; then
  SELECTED_DEVICE="${devices[$choice]}"
  echo "Selected device: $SELECTED_DEVICE"
  
  echo "Building debug APK..."
  ./gradlew assembleDebug
  
  if [ $? -ne 0 ]; then
    echo "Build failed. Exiting."
    exit 1
  fi
  
  echo "Uninstalling existing app to prevent signature mismatch..."
  adb -s "$SELECTED_DEVICE" uninstall com.aistudio.cricketscorer.fymqpx 2>/dev/null
  
  echo "Deploying and running the application..."
  ~/.local/bin/android run --device="$SELECTED_DEVICE" --apks=app/build/outputs/apk/debug/app-debug.apk
  
  # If it is a physical device (not starting with "emulator"), automatically launch scrcpy screen mirroring
  if [[ ! "$SELECTED_DEVICE" =~ ^emulator- ]]; then
    if command -v scrcpy >/dev/null 2>&1 || [ -f /opt/homebrew/bin/scrcpy ]; then
      # Check if scrcpy is already running for this device
      if ! pgrep -f "scrcpy.*$SELECTED_DEVICE" >/dev/null; then
        echo "Launching screen mirroring (scrcpy) for physical device..."
        export PATH="/Users/shivakrishnareddy/Library/Android/sdk/platform-tools:$PATH"
        /opt/homebrew/bin/scrcpy -s "$SELECTED_DEVICE" >/dev/null 2>&1 &
      fi
    fi
  fi

  echo ""
  echo "======================================================================"
  echo "Streaming real-time log traces from the app (Press Ctrl+C to exit logs)"
  echo "======================================================================"
  echo ""
  
  # Clear previous log buffer
  adb -s "$SELECTED_DEVICE" logcat -c
  
  # Stream live logs for the application package in color
  adb -s "$SELECTED_DEVICE" logcat -v color --package=com.aistudio.cricketscorer.fymqpx
else
  echo "Invalid choice. Exiting."
  exit 1
fi
