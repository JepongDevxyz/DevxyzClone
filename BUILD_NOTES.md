# DevxyzClone — Build Notes (v1, 2026-11-01, LOCAL ONLY — not pushed anywhere)

App: **DevxyzClone** · package `com.jepongdevxyz.clone` · minSdk 26 ·
target/compileSdk 34 · AGP 8.7.3 / Gradle 8.11.1 wrapper · plain Java + AndroidX
(no Kotlin) · debug builds get applicationId suffix `.debug` like her other projects.

## What was built (all with real handlers)

- **Home (`MainActivity`)** — list of clones: original app's icon, display name,
  package + clone number, empty state, **+ Add Clone** button, bottom
  "Powered by Jepong Devxyz" branding line. Tap a clone = Launch; ⋮ button or
  long-press = actions dialog (Launch / Rename / Clone info / Delete / Cancel).
- **App picker (`AppPickerActivity`)** — lists all launchable installed apps
  (icon + label + package, deduped, sorted, live search). Tap an app → creates
  the **next numbered clone** for it: first Muse clone is **"Muse 2"**, then
  **"Muse 3"**, **"Muse 4"**… Counters are persisted per package and numbers
  are never reused, even after a clone is deleted.
- **Rename (first-class feature, her 2026-11-01 order)** — Rename action opens a
  modern in-app dialog (`dialog_shell.xml`, no default AlertDialog) with the
  field **prefilled with the current display name**, real Save/Cancel handlers,
  and a **Reset to default name** button. Examples she gave: "Muse clone",
  "Muse fake". Rules enforced in code:
  - Custom name persists in the store and is shown **everywhere** the display
    name appears (home list, action/info dialogs).
  - Renaming changes **only** `displayName` — clone number, package link and
    isolated data dir (`dataDirName`) are immutable identity fields.
  - **Empty name is rejected** inline in the dialog (error text, dialog stays
    open) — a clone can never end up nameless.
- **Delete** — custom confirm dialog; removes the clone record **and**
  recursively deletes its isolated data folder. The original app is untouched.
- **Clone info** — shows clone #, package, default name, and the clone's exact
  isolated data path.
- **Persistence (`CloneStore`)** — clone records + counters in SharedPreferences
  as JSON; clones survive app restart.
- **Isolated data (`LocalCloneEngine`)** — every clone gets its own folder
  `files/clones/<package>_clone<N>/{data,cache,files}` at creation time. No two
  clones share a folder; the original app's data is never touched.
- **UI rules** — dark/light via `values` + `values-night` color resources and a
  DayNight theme (no hardcoded white/black screen backgrounds); **no**
  `alert()`/default dialogs anywhere — all dialogs are the in-app shell;
  feedback toasts are standard bottom toasts.
- **CI** — `.github/workflows/android.yml` (debug APK) exists **locally only**.
  ⚠️ Workflow files cannot be pushed via her GitHub App (no `workflows`
  permission) — if/when she approves a push, this file must be added by her
  via the GitHub web UI. **Nothing has been pushed; no repo was created.**

## Engine verdict (see ENGINE-RESEARCH.md for evidence)

No maintained open-source virtual engine credibly supports Android 15.
BlackBox has the right multi-user API shape but is unmaintained since
2022–2024, claims only Android 5–12, and shows **no license**. VirtualApp's
modern support is closed-source. So v1 ships the full host behind the
`CloneEngine` interface with `LocalCloneEngine` as the honest first
implementation.

## What does NOT work yet — read this before testing

- **Launching a clone does NOT run the app.** Launch (tap / menu) opens an
  honest in-app dialog: the clone and its separate data space are saved, but
  the virtual runtime is not wired, so Muse / Mobile Legends do **not** run
  inside DevxyzClone in this build. Nothing fakes success.
- Consequently the prank's full effect (opening "Muse 2" and using a second
  account) is **not yet possible** — the twin list, numbering, names and data
  spaces are real; the twin *execution* is the missing piece.

## Clean design / "anti-detect" — scope and honest limits

- Design rule, enforced as the `CloneEngine` contract: a future runtime must
  run the target's **original, unmodified APK** — no repackaging, no
  re-signing, no patching, no root. Re-signing is what most signature checks
  flag, so a clean virtual run avoids *that specific* detection signal.
- **Not built and not planned:** Play Integrity / SafetyNet attestation
  spoofing, signature-verification spoofing, anti-cheat bypass for games.
- **Not claimed:** that clones are undetectable. Virtual environments can be
  detected by other means; apps using Play Integrity may refuse to run;
  Mobile Legends' anti-cheat may flag virtual-space sessions and **ban the
  account** — that risk exists for *any* Parallel Space-style tool and stays
  real here. (Her stated ML use is dual-account play, not cheating — noted;
  the ban risk from detection exists regardless of intent.)

## Risks / known hard problems for the runtime step

- **Signature & Play Integrity checks** (banking, Meta apps, many games):
  guest apps may fail login or exit inside any virtual container.
- **Anti-cheat detection** in games (ML): virtual-space detection + bans.
- **Split APKs / app bundles**: most modern apps install as splits; a virtual
  engine must install *all* splits + native libs, not just `base.apk` — a
  known BlackBox-era weakness.
- **64-bit only** apps and Android 15 hidden-API / `ActivityThread` changes.
- **Notifications, Google services (microG vs real GMS)** inside the container.

## Exact next steps

1. Get CI green on this host (push only on Jepong's word; workflow via web UI).
2. Her phone test of the host UX: create Muse 2/3/4, rename one to
   "Muse fake", delete one, restart app — records must persist.
3. Engine spike in a branch: vendor BlackBox `Bcore` as a module behind a
   `BlackBoxCloneEngine`, target ONE simple 64-bit app first on her Honor 400;
   measure Android 15 breakage before promising Muse/ML.
4. Only after a simple app runs: try Muse (Play Integrity risk), then ML
   (anti-cheat/ban risk — her call, with a throwaway account first).

## Local validation done (2026-11-01)

- All XML files parsed with a real XML parser — well-formed.
- Manifest checked: label DevxyzClone, applicationId/namespace
  `com.jepongdevxyz.clone`.
- Java sources brace/paren balance-checked.
- Local Gradle build NOT attempted beyond policy: this VM has no JDK on PATH
  and the Gradle daemon is known-broken here (AGENTS.md) — **CI is the build
  proof**, as with her other Devxyz projects.
