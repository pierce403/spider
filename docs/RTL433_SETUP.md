# rtl_433 Android Setup

Spider can receive TPMS and POCSAG via **rtl_433**. Two modes are supported: Network (easiest)
and USB (on-device). ADS-B uses **dump1090**, which is bundled in the APK.

---

## Mode 1: Network bridge (recommended for getting started)

Run rtl_433 on any host reachable from the Android device (laptop, Raspberry Pi, etc.):

```
rtl_433 -F json -S 0 -M level
```

Then in Spider:

1. Open the filter panel (hamburger icon).
2. Select **Network bridge** as the source.
3. Enter the host IP and port (default `1234`).
4. Tap **Start**.

The simulator at `scripts/sdr-simulator.py` can substitute for real hardware during
development:

```
python3 scripts/sdr-simulator.py --port 1234
adb forward tcp:1234 tcp:1234   # if testing on a physical device
```

---

## Mode 2: USB on-device

Spider launches `rtl_433` as a subprocess when USB mode is selected. The binary is expected in
the app's native library directory:

- `<nativeLibraryDir>/librtl_433.so`  ← preferred (packaged as NDK library)
- `<nativeLibraryDir>/rtl_433`         ← fallback

The native library directory is under `/data/app/ninja.spider-*/lib/<abi>/` and is not directly
user-writable; the binary must be bundled in the APK.

### Option A: NDK build (advanced)

Build rtl_433 and its dependencies for Android:

**Dependencies:**
- [libusb-android](https://github.com/libusb/libusb) — USB I/O for rtl-sdr
- [librtlsdr](https://github.com/osmocom/rtl-sdr) — RTL-SDR driver
- [rtl_433](https://github.com/merbanan/rtl_433) — protocol decoder

**Steps (arm64-v8a target):**

1. Set up NDK 27+ (already included in the Spider build):

    ```
    export ANDROID_NDK=$ANDROID_SDK_ROOT/ndk/27.2.12479018
    export TOOLCHAIN=$ANDROID_NDK/toolchains/llvm/prebuilt/linux-x86_64
    export CC=$TOOLCHAIN/bin/aarch64-linux-android24-clang
    export AR=$TOOLCHAIN/bin/llvm-ar
    ```

2. Cross-compile libusb with `--host=aarch64-linux-android`.
3. Cross-compile librtlsdr against the libusb output.
4. Build rtl_433 (cmake) with `CMAKE_TOOLCHAIN_FILE` pointing to the NDK toolchain file.
5. Copy the resulting binary into `app/src/main/jniLibs/arm64-v8a/librtl_433.so`.
6. Add to `app/src/main/cpp/CMakeLists.txt` as a prebuilt shared library
   (see the existing dump1090 section as a reference).

### Option B: Termux-assisted sideload (development/testing only)

This approach works on rooted or developer devices. It does **not** make the binary available
to Spider's subprocess launcher because Android blocks execution from user-writable paths on
non-rooted devices.

For reference only:

```
# On a device with Termux installed:
pkg install rtl-433
# Binary is at /data/data/com.termux/files/usr/bin/rtl_433
```

---

## HackRF One notes

- Pass `-d driver=hackrf` to rtl_433 in network mode.
- HackRF One covers 1 MHz–6 GHz, so TPMS (315/433 MHz), POCSAG (929 MHz),
  and ADS-B (1090 MHz) are all in range.
- Single-dongle frequency hopping is handled automatically by Spider when multiple
  protocols are enabled.

## RTL-SDR notes

- RTL-SDR V3/V4 covers ~500 kHz–1766 MHz (TPMS + POCSAG reachable, ADS-B borderline).
- For ADS-B with RTL-SDR: use a second dongle or enable Network bridge for ADS-B on a
  separate host running dump1090.

---

## Diagnostics

Open **Diagnostics** from the Spider menu to see:
- Current state (idle / scanning / error)
- Hardware detected
- rtl_433 callback count and last reading timestamp
- Per-state setup guidance

The "Copy diagnostics" button copies the full report to clipboard for bug reports.
