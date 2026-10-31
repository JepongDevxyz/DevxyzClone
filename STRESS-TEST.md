# DevxyzClone — Overnight Stress Test (2026-11-01, LOCAL ONLY)

Scope: the HOST app in `~/workspace/devxyzclone/` only (the BlackBox spike copy
was not touched). Method: every source file read and traced, then real checks
run on this VM — XML parsing with a real parser, resource-reference integrity
scan, a real Java parser (javalang) for syntax validation of all 7 Java files,
and executable Python simulations that faithfully mirror the store's
numbering/persistence logic and the engine's delete logic (run against the
original logic first to prove each bug, then against the fixed logic).
No JDK exists on this VM, so a true compile remains CI's job (see "Not
verifiable locally" — this matches BUILD_NOTES.md's standing note).

## Checklist

| Area | Result |
|---|---|
| XML well-formedness (14 files: manifest, layouts, values, drawables, mipmaps) | PASS |
| Resource references (52 refs: every @string/@color/@layout/@style/R.* resolves) | PASS |
| Color parity values ↔ values-night (8/8, incl. new dc_error) | PASS |
| No hardcoded colors in layouts | PASS (after fix #10) |
| No default AlertDialog in code (comment mention only) | PASS |
| Java syntax — javalang real-parser parse, 7/7 files | PASS |
| Manifest: label DevxyzClone, both activities registered, `<queries>` for the picker intent, launcher = MainActivity | PASS |
| applicationId/namespace = com.jepongdevxyz.clone; gradlew executable; gradle-wrapper.jar present (43,504 B) | PASS |
| Numbering: 2,3,4 → delete → next is 5 (never reused), normal path | PASS (simulation, before and after) |
| Numbering under counter corruption / counter loss / process death | **FAILED before → PASS after** (fixes #1, #2) |
| Store resilience: one malformed record; top-level JSON corruption | **FAILED before → PASS after** (fix #3) |
| Delete: correct folder only, traversal-proof, symlink-safe | **FAILED before → PASS after** (fixes #4, #5; model 6/6) |
| Rename: empty/whitespace rejected, trim, emoji/unicode, null, length cap, reset-to-default, identity fields untouched | PASS (dialog trace + store guard + simulation; cap = fix #11) |
| Picker: dedupe by package, self excluded, sort, search filter, label-less apps, double-tap, UI-thread jank | PASS after fixes #6, #7, #13, #15 |
| Clone of uninstalled original: list + info handling | PASS after fix #12 |
| Dialogs: no stacking, no window leak on destroy/rotation, secondary buttons visually secondary | PASS after fixes #8, #9 |
| Dark/light: all screens via @color resources + DayNight theme | PASS after fix #10 |
| Double-tap on "+ Add Clone" | PASS after fix #14 |

## Bugs found and fixed (15)

### Critical / data-loss

1. **Corrupted or lost counters → clone numbers (and data folders) reused.**
   `nextNumber()` caught any JSON error and returned the constant `2`, and a
   missing counters entry defaulted the same way. With Muse 2/3 already
   existing, the next clone was again "Muse 2" with data dir
   `com_muse_clone2` — two clones silently sharing one data folder, and
   deleting either would delete the other's data. *Fix:* the next number is
   `max(stored counter, highest existing clone number for the package) + 1`;
   no constant fallback exists anymore. Simulation: FAIL → PASS.
2. **`apply()` write-skew → same reuse after process death.**
   Clone list and counters were written with two async `apply()` calls; a
   process death could persist one without the other. *Fix:* all writes use
   `commit()`, counters committed before the record save (a lost save can only
   skip a number, never reuse one). Simulation: FAIL → PASS.
3. **One malformed record wiped the whole clone list — permanently.**
   `all()` parsed all-or-nothing and returned an empty list on any error;
   the next `save()` then overwrote the disk copy with that short list.
   *Fix:* per-record parsing (bad records are skipped), plus `save()` keeps
   the previous good JSON under a backup key and `all()` falls back to it if
   the primary value is wholly unparseable. Simulation: FAIL → PASS (backup
   fallback restores the previous good state, i.e. last-but-one save).
4. **Delete path traversal via persisted `dataDirName`.**
   The delete target was built by joining the clones root with a string read
   back from stored JSON, with no validation — a corrupted/tampered value
   like `../../x` would have aimed the recursive delete outside the clone
   folder. *Fix:* names containing separators or dot-segments are refused,
   and non-symlink targets must canonical-resolve inside the clones root.
   Model test: traversal refused, target files intact.
5. **Recursive delete followed symlinks.**
   `deleteRec()` recursed into any directory `listFiles()` returned,
   including a symlinked directory — which would delete the *target's*
   contents. *Fix:* symlinks are unlinked, never recursed into, at every
   level of the tree. Model test: 6/6 incl. nested symlink inside a clone.

### Functional

6. **Picker crashed / produced nameless clones for label-less apps.**
   `ri.loadLabel(pm).toString()` NPEs when an app has no label (the parallel
   spike pass hit the same crash), and a blank label produced clones named
   `" 2"`. *Fix:* label load is guarded; null/blank labels fall back to the
   package name in the picker, and `CloneStore.createNext()` enforces the
   same fallback.
7. **Picker loaded every app's label + icon on the UI thread** in `onCreate`
   (query + per-app binder calls for the whole device) — multi-second freeze
   / ANR risk. *Fix:* loading runs on a worker thread; only the list swap and
   adapter notify touch the UI thread, with an isFinishing/isDestroyed guard.
8. **Dialog `filled` flag was ignored** — `addBtn()` built the same filled
   MaterialButton for every action, so Cancel / Reset / Delete looked like
   primary actions. *Fix:* non-filled actions now use the Material text-button
   style; filled stays contained.
9. **Dialogs could stack and leak their window.** A double-tap could open two
   dialogs, and any dialog open across rotation/destroy leaked (WindowLeaked)
   and lost its context. *Fix:* `DevxyzDialogs` tracks the current dialog,
   `base()` replaces a still-showing one instead of stacking, and
   `MainActivity.onDestroy()` dismisses it. (Residual: rename text typed
   before a rotation is still lost — the dialog closes cleanly instead of
   leaking; retyping is the cost.)

### Minor / robustness

10. **Hardcoded `#D32F2F`** for the rename error text in `dialog_shell.xml`
    (spec violation, weak contrast in dark mode). *Fix:* new `dc_error`
    color in both values (#D32F2F) and values-night (#EF9A9A); layouts now
    contain zero hex colors.
11. **Rename length unbounded** — a pasted mega-name would wreck list layout
    and bloat the store. *Fix:* 60-char `InputFilter` in the rename dialog;
    `CloneStore.rename()` also trims and ignores null/empty as defense in
    depth (dialog already rejects empty inline and stays open).
12. **Uninstalled originals showed a blank icon and no explanation.**
    *Fix:* placeholder system icon + "Original app uninstalled" appended in
    the list row and a note in Clone info.
13. **Picker double-tap created two clones** before `finish()` landed.
    *Fix:* one-shot `picked` guard — one pick creates exactly one clone.
14. **"+ Add Clone" double-tap stacked two picker activities**, the second
    surfacing after the first finished. *Fix:* open-guard flag in
    MainActivity, cleared in `onResume`.
15. **Locale-sensitive `toLowerCase()` in picker search** (classic Turkish-i
    bug). *Fix:* `Locale.ROOT` for filter comparisons.

## Checked and NOT bugs

- Normal-path numbering never reused numbers even before the fixes
  (simulation pass) — the reuse needed corruption/loss, which is exactly what
  fixes #1–#2 close.
- Two clones sharing one *display name* via rename is allowed by design;
  clone number in the list subtitle and Clone info disambiguates them.
- `gradle-wrapper.jar` present, `gradlew` executable; AGP 8.7.3 / Gradle
  8.11.1 pairing is valid; debug `.debug` applicationId suffix is deliberate
  (matches her other projects, per BUILD_NOTES.md).
- The two warnings from the automated reference scan are false positives,
  verified by hand: `@style/Widget.MaterialComponents.Button.TextButton` is a
  Material library style (present in the material 1.12 dependency), and
  `android.R.id.content` is the Dialog framework's own content view.
- Launch honesty is by design (BUILD_NOTES.md): no virtual runtime is wired;
  nothing in this pass changes that.

## Not verifiable locally (needs CI build / her phone)

- **Real compilation and APK build** — no JDK anywhere on this VM; javalang
  parse + brace balance is the strongest local proof. CI remains the build
  proof (push only on Jepong's word; workflow file via web UI, as noted in
  BUILD_NOTES.md).
- **On-device behavior**: programmatic text-button styling, RecyclerView
  recycling with real icons, DayNight rendering on the Honor 400 (Android
  16), dialog behavior across real rotation, picker load time with a full
  app drawer, and SharedPreferences commit() timing under real process death.
- **Backup/restore** of the store (allowBackup=true) — records vs. data
  folders restoring together can only be proven on a device.
- Anything about actually running cloned apps (Muse/ML) — unchanged engine
  verdict; see ENGINE-RESEARCH.md and the spike report.
