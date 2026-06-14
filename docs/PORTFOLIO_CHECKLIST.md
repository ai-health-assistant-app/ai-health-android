# Portfolio / Presentability Checklist

Concrete cleanup items to make HealthTwin a stronger, more credible showcase. Ordered by impact. None of these touch the biometric engine (the strong part). I (Claude) can execute any of these on request — just say which.

## A. Make it real (highest impact)
- [ ] **Deploy the backend.** Today `NetworkModule.CHAT_BASE_URL` points at `http://10.0.2.2:8001` and Firebase uses local emulators, so sign-in + chat don't work for a fresh install. Deploy the FastAPI service (e.g. Cloud Run), move secrets to a secret manager, switch to a real Firebase project.
- [ ] **Enforce HTTPS / disable cleartext for release.** `usesCleartextTraffic="true"` is fine for the LAN dev box, not for a shipped/demoed build — gate it to debug only.
- [ ] **Make the base URL build-configurable** (BuildConfig field / flavor) instead of a hardcoded constant.

## B. Correctness & honesty (cheap, high credibility)
- [ ] **Personalise the biometric profile.** `ChatViewModel` hardcodes `UserBiometricProfile(190, baselineRhr, true)` → HRmax=190 and sex=male for everyone, making TRIMP wrong for many users. Wire it to the real user profile (already persisted).
- [ ] **Label or drop the Baevsky stress index.** It's derived from minute-level HR, not true HRV — mark it "experimental, non-clinical" in the UI, or remove it. (Already disclosed in README/case study.)
- [ ] **Add an in-app medical disclaimer** ("not medical advice; consult a professional") on first run and near the chat — required for Play Store health declarations and EU.
- [ ] **Add a privacy policy** (even a short hosted one) — Health Connect apps need it.

## C. Repo hygiene (makes it look professional)
- [ ] **Remove dead code:** `core/data/.../worker/HealthSyncWorker.kt` is unused; the live one is `.../sync/HealthSyncWorker.kt`. Delete the duplicate.
- [ ] **Remove leftover CameraX dependencies** in `gradle/libs.versions.toml` (`cameraX`, `androidx-camera-*`, comment "Per acquisizione PPG") — dead since the vPPG pivot.
- [ ] **Prune branches.** Remote has `steps--unused`, `steps-refactor--unused`; local has unmerged `PPG-measurement`, `heart`, `steps`, `steps-refactor`. Delete or document them so the branch list tells a clean story.
- [ ] **Delete the stale local `core/domain/bin/` directory** (compiled leftovers; already gitignored, but remove it locally).
- [ ] **Finish the in-flight refactor** (UserRepository move to `core:domain`) and commit, so `git status` is clean.
- [ ] **Add a `LICENSE`** (e.g. MIT) so the repo's terms are explicit.

## D. Security note (low severity, do it anyway)
- [ ] **Firebase config (`app/google-services.json`) is committed.** These keys are identifiers, not secrets (security is enforced by Firebase **Security Rules** + **App Check** + SHA-256 signing-cert restrictions), so this is *not* a breach — but for a public portfolio: lock down Security Rules, enable App Check, restrict the API key in Google Cloud Console, and consider pointing the public repo at a throwaway Firebase project.

## E. Architecture polish (optional, nice-to-have)
- [ ] **Stop leaking data-layer entities into feature modules.** `feature:dashboard` references `core:data` entities directly; map to domain models so features depend only on `core:domain` + `core:ui`.
- [ ] **Add a few tests outside the engine** — at least the repositories / sync mapping — to balance the (currently engine-only) coverage.

> Skipped intentionally: screenshots/GIFs (author opted out).
