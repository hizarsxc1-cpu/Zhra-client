# Zhra Client

A client-side Fabric mod for Minecraft 1.21.0 with a Midnight Blue and Electric Cyan interface.

## Current features

Open the unified Zhra menu with **Right Shift**. The menu contains:

- HUD toggle with adjustable position and scale
- FPS, ping, and XYZ display
- WASD keystrokes
- Armor durability display
- Left-click CPS counter
- Zoom toggle (30 FOV)
- Toggle Sprint and Toggle Sneak
- Custom crosshair toggle, color, and size
- Performance Mode: reduces particles to Minimal and caps view distance at 6 chunks while enabled

Performance Mode restores the previous particle and view-distance settings when switched off.

## Build requirements

- Minecraft Java Edition: 1.21.0
- Fabric Loader: 0.15.11 or newer
- Fabric API: 0.102.0+1.21
- Java: 21

GitHub Actions builds the mod and uploads the JAR as a workflow artifact. Compilation is checked by CI; in-game behavior and PojavLauncher compatibility still need testing on a device.
