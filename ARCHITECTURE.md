# Architecture

- `app/src/main/java/guru/urchin/sdr`
  Handles USB/network SDR ingestion, `rtl_433` process control, USB detection, and TPMS parsing.

- `app/src/main/java/guru/urchin/scan`
  Contains the shared observation pipeline and diagnostics store used by SDR capture.

- `app/src/main/java/guru/urchin/data`
  Room entities, DAOs, and repository logic for sensors and sightings.

- `app/src/main/java/guru/urchin/ui`
  Main list, detail, diagnostics, adapters, and simple UI preferences.

- `app/src/main/java/guru/urchin/util`
  Formatting, TPMS metadata presentation, window inset handling, and app version helpers.
