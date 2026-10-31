# DevxyzClone — Virtual Engine Research (2026-11-01)

Question: is there a maintained open-source virtual-app engine that can run
cloned apps (multiple instances, isolated data per clone) inside a host app on
**Android 15** (Honor 400 5G), for arbitrary apps like Muse or Mobile Legends?

## Verdict (honest)

**No.** As of 2026-11-01, no open-source engine credibly supports Android 15 /
arbitrary modern apps. The only product that publicly claims full Android 15
cloning is **Parallel Space itself — closed-source** (its proprietary
"multiDroid" engine; Play listing changelog says "Fully compatible with
Android 15"). That engine is not available as a library.

## Candidates checked

| Engine | Source / license | Last activity (checked 2026-11-01) | Claimed Android support | Multiple instances / isolated data | Verdict |
|---|---|---|---|---|---|
| **BlackBox** (FBlackBox/BlackBox) | GitHub API reports **no license** (`license: null`; README carries only "Copyright 2022 BlackBox") — legally unsafe to ship without a license grant | Last push **2024-04-12** (a README edit; last code ~2023-08); last release **2.1.0, 2022-04-18** | README: **Android 5.0–12.0**; suggests downgrading targetSdk to ≤28 for compatibility | Yes on paper: `BlackBoxCore`, virtual users (`installPackageAsUser(apk, userId)`, per-user virtual storage) — the right API shape for Muse 2/3/4 | **Best-shaped API, but dead for modern Android.** No Android 13/14/15 support claimed anywhere; 64-bit/split-APK era issues unaddressed upstream. |
| **VirtualApp** (asLody/VirtualApp) | No license shown; public repo is the old open-sourced snapshot | Public code effectively frozen since ~2017 (repo metadata bumps are not code releases) | README: **"Support 14.0 in business version"** — i.e. modern support is **closed-source / paid** | Yes (the original multi-instance virtual engine; forks add per-instance spaces) | Open version unusable on Android 15; modern version not available to us. |
| **VirtualApp forks** (ServenScorpion et al.) | Various, unclear licensing | Some forks still pushed to in 2026 | Fork READMEs claim up to **Android 12** (app-clone / virtual multi-open features) | Yes | Ceiling is Android 12; no credible Android 15 claim; provenance/licensing murky. |
| **VirtualXposed** (android-hacker/VirtualXposed) | **GPL-3.0** | Last push **2024-03-08** | Built on the old VirtualApp; Xposed-module host, not a general app cloner | Runs apps in its container, but designed around Xposed modules | Wrong tool, stale base, GPL-3 copyleft on the whole host. |
| **Parallel Space** (LBE Tech) | **Closed source**, proprietary multiDroid | Actively updated on Play (2026 listings) | **Claims Android 15**; also documents limits: apps declaring `REQUIRE_SECURE_ENV` are not supported | Yes — this is the reference UX | Proof it's possible commercially; **not integrable** (no SDK/library). |

## What a real integration would require (BlackBox-class)

1. Add the engine as a Gradle module (`:Bcore` + `black-fake` + native
   `armeabi-v7a`/`arm64-v8a` libs), not a Maven dependency — there is no
   maintained AAR.
2. Host `Application` must init the engine before anything else, declare the
   engine's large block of stub activities/services/providers in the manifest,
   and proxy every guest launch through `BlackBoxCore.launchApk(pkg, userId)`.
3. Per-clone isolation = one **virtual userId per clone**; the engine maps
   each userId to its own virtual data directory — which is exactly the
   `CloneEngine.dataDirFor()` boundary this project already models.
4. Then the real work starts: Android 13–15 breakage in the engine's hooks
   (hidden-API restrictions, `ActivityThread` internals, package-visibility,
   split APKs, 64-bit-only apps) would have to be fixed **in the engine
   itself**. That is an engine-maintenance project, measured in weeks, with
   no guarantee for signature-checked apps.

## Clean-design note (the "anti-detect" scope we agreed)

DevxyzClone's design keeps the target app's **original APK completely
unmodified** — no repackaging, no re-signing, no patching, no root. That
matters because the most common clone-detection signal is a **changed
signature** on a re-signed clone; a virtual container running the untouched
original does not trip that particular signal the way a re-signed clone does.
**Limits, stated plainly:** this does NOT make clones undetectable. Apps can
still detect a virtual environment (unusual process/package context, hook
artifacts, Play Integrity verdicts), games with anti-cheat (e.g. Mobile
Legends) can still flag and **ban accounts** run in any virtual space, and
apps with signature/Play Integrity checks may simply refuse to run. No
Play Integrity / SafetyNet spoofing and no anti-cheat bypass is built or
planned — see BUILD_NOTES.md.

## Fallback used in this build

The host app is fully built behind the `CloneEngine` interface.
`LocalCloneEngine` manages clone records + truly isolated per-clone data
directories + numbering today, and its `launchClone()` honestly reports that
the virtual runtime is not wired yet. Wiring a real engine later = write a
`BlackBoxCloneEngine implements CloneEngine` and swap one line in
`MainActivity` / `AppPickerActivity`.

Sources: github.com/FBlackBox/BlackBox (+ GitHub API repo/releases/commits,
checked 2026-11-01), github.com/asLody/VirtualApp, github.com/android-hacker/VirtualXposed,
Parallel Space Play listing (com.lbe.parallel.intl) changelog, DeepWiki BlackBox architecture pages.
