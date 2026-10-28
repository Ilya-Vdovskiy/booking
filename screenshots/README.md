# Device screenshots

- `01-empty-form.png`, `02-validation-errors.png`, `draft-restored.png`, `valid-form.png`, `confirmation-dialog.png` and `booking-complete.png` were captured from the Java build on a connected Samsung SM-A515F (Android 10).
- `06-permission-explanation.png` and `07-notification-permission.png` were captured from the Java build on the existing Pixel 6 Android 15 emulator. The system permission dialog uses the emulator's English locale.

The connected-device flow is reproducible with `python tools/device_smoke.py <ADB_SERIAL> --skip-permission` after choosing valid dates in the app. Run without `--skip-permission` on Android 13+ to capture its notification dialog.
