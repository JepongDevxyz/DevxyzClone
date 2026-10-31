# DevxyzClone

Parallel Space-style app cloner for Android — by Jepong Devxyz.
Pick an installed app (e.g. Muse) and create numbered clones — **Muse 2, Muse 3, Muse 4** —
each with its own isolated data space, and each **renamable** to any custom name
("Muse clone", "Muse fake", ...). Clean design: the original app's APK is never
modified, repackaged or re-signed, and no root is needed.

**Status: first local build.** Clone records, numbering, rename, isolated data
folders and the app picker are real. Actually *running* a cloned app inside
DevxyzClone is **not wired yet** — see `ENGINE-RESEARCH.md` and `BUILD_NOTES.md`
for the honest verdict and next steps.

Package: `com.jepongdevxyz.clone` · minSdk 26 · target/compileSdk 34
