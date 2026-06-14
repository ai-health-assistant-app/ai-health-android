# HealthTwin — Technical Case Study

> A walkthrough of the **why** and the **how** behind HealthTwin: the product decision that shaped it, the on-device signal-processing engine that is its core, the privacy architecture, and an honest reflection on building it with AI agents under human direction.

---

## 1. The problem

Tens of millions of people own a mid-range fitness band (Xiaomi, Amazfit, Huawei, etc.). These devices capture a surprising amount of physiology — nocturnal resting heart rate, sleep stages, activity — but the companion apps mostly show **raw numbers**, not **interpretation**. "Your RHR was 58" doesn't tell you whether today is a day to push or to rest.

**Goal:** turn that already-collected passive data into a single, honest, daily signal — a **Readiness score** — plus a coach that explains it in plain language. No new hardware, no behaviour change asked of the user.

**Target user:** someone who already wears a budget band on Android and wants *meaning*, not more dashboards — without paying for a Whoop/Oura subscription.

## 2. The pivot that defined the project

The original concept included **active HRV measurement via video photoplethysmography (vPPG)** — finger on the camera + flash for 60–180 seconds. I killed that module. The reasoning:

1. **Retention.** Any measurement that demands the user stop and hold still for a minute is the single biggest drop-off point in this category. "Frictionless" beats "accurate-but-annoying" for a consumer wellness app.
2. **Engineering cost.** vPPG means fighting Android camera fragmentation (Camera2/CameraX, ISO calibration, flash control) and DSP (Butterworth/bandpass, peak detection) — hundreds of hours of maintenance.
3. **Regulatory surface.** Capturing a blood-flow video stream pushes you toward medical-device territory (GDPR/MDR). Dropping it shrinks the attack surface and the compliance burden.

So the project pivoted to **passive-only**: lean entirely on what wearables already write to Health Connect, and move the differentiation from *computer vision* to *signal processing*.

**The honest trade-off** (and a real weakness, discussed in §6): passive mid-range data does **not** include reliable beat-to-beat HRV. The bet is that **nocturnal RHR trends + sleep architecture** are good-enough surrogates for autonomic state at the population level the app targets. That bet is defensible for wellness, but it's a bet — and it sits in tension with one of the metrics I still compute (the Baevsky stress index).

## 3. The engine (the core IP)

Everything important happens **on the device**, in pure Kotlin (`core:domain`), orchestrated by `BiometricEngineUseCase` over `BiometricMathUtils`. The pipeline:

### 3.1 Pre-processing — cubic-spline interpolation
Wearable HR arrives irregularly sampled. A **natural cubic spline** reconstructs a smooth, uniformly-spaced (1-min) curve, but **only across gaps ≤ 5 minutes** — larger gaps are left as holes rather than inventing data. Interpolated values are clamped to a physiological `[30, 220]` bpm.

### 3.2 Training load — Banister TRIMP
Per-session load using heart-rate reserve and sex-specific weighting:

```
ΔHRr = (HR − HRrest) / (HRmax − HRrest)
Male:   y = 0.64 · e^(1.92·ΔHRr)
Female: y = 0.86 · e^(1.67·ΔHRr)
TRIMP  = Σ ( Δt_min · ΔHRr · y )
```

### 3.3 Fitness–Fatigue (Performance Manager)
Two EWMAs over daily TRIMP, with `α = 2/(period+1)`:
- **CTL** ("fitness") = 42-day EWMA
- **ATL** ("fatigue") = 7-day EWMA
- **TSB** ("form") = CTL − ATL

TSB drives the training component of Readiness (fresh vs. overreached).

### 3.4 Autonomic state — RHR Z-score
Today's RHR vs. a **30-day baseline**:

```
Z = (RHR_today − μ_30d) / σ_30d        (σ floored at 0.5 to avoid blow-ups)
Z < 1.5  → GREEN     1.5 ≤ Z < 2.5 → YELLOW     Z ≥ 2.5 → RED
```

A sustained RHR rise is a classic early signal of infection / overtraining — so a RED Z **overrides** the Readiness score down to ≤ 30 ("rest day").

### 3.5 Supporting metrics
- **Sleep HR dipping** — nocturnal vs. daytime HR drop, classified Reverse/Non/Normal/Extreme dipper.
- **Sleep-quality score** — from architecture (deep %, REM %, WASO).
- **Baevsky stress index** — histogram-based autonomic stress proxy (see §6 for the caveat).

### 3.6 Synthesis — Readiness
```
Readiness = 0.40·sleep + 0.30·RHR + 0.20·training + 0.10·volatility   → 0..100
            (RED Z-score override caps it at 30)
GREEN ≥ 85   ·   YELLOW 60–84   ·   RED < 60
```

### 3.7 Design choices worth calling out
- **Graceful degradation:** every sub-result returns `null` when there isn't enough data (e.g. < 3 days of RHR, < 2 HR samples), instead of throwing or faking a number. The score appears only when it's meaningful.
- **Pure & deterministic:** no Android types in the math → **32 unit tests** run on the JVM, covering interpolation, TRIMP (both sexes), EWMA decay, CTL/ATL/TSB, Z-score bands, dipping profiles, Baevsky, and edge cases (empty/short/uniform input).

## 4. Architecture & data flow

**Multi-module Clean Architecture**, domain is pure Kotlin:

```
:app → :feature:dashboard, :feature:onboarding
:feature:dashboard → :core:domain, :core:data, :core:ui
:core:data → :core:domain, :core:health
:core:health → :core:domain
```

**Offline-first:** Room is the single source of truth; the UI observes the DB, never Health Connect directly. A `WorkManager` job ingests via the Health Connect **Changes API** (differential sync) and, importantly, **propagates deletions** so removing data upstream removes it locally too.

**Edge-first / "zero-knowledge" backend:** the device does all the physiology. The backend is a thin **insight receiver** that gets only a flat, validated metrics payload (`{readiness, rhr_deviation, sleep_balance_score, ...}`) plus the user's message, builds an LLM prompt, and returns coaching text. Raw time-series never leave the phone, and the local DB is encrypted (SQLCipher, key in the Android Keystore TEE).

## 5. Development methodology — AI-assisted, human-directed

This project is a deliberate example of **building with AI coding agents while staying the engineer in charge.**

| Human (me) | AI agents |
| --- | --- |
| Product scope, target user, the vPPG→passive **pivot** | Module/file scaffolding |
| Module boundaries, offline-first & edge-first design | First-draft implementations & boilerplate |
| Choice + specification of every algorithm and threshold | First-draft unit tests |
| Integration, debugging, testing on **real** wearable data | Refactors under review |
| Final review and acceptance of every change | — |

**How I steered it:** small, well-scoped tasks; explicit acceptance criteria (e.g. "TRIMP must match the Banister formula and pass these cases"); reading and reviewing diffs rather than trusting output. The biometric math was specified by me up front precisely because it's the part where a plausible-but-wrong implementation would be dangerous.

**Why disclose it:** the honest, marketable skill here is *direction* — decomposing an ambitious idea, making the hard product/architecture calls, and getting agents to execute them without losing control of quality. Hiding the AI involvement would be both dishonest and would undersell the actual competency.

## 6. Critical reflection — what I'd do differently

I'd rather show judgment than polish:

- **The Baevsky stress index is the weakest scientific link.** It computes `RR = 60/BPM` from minute-aggregated HR and treats the spread as autonomic stress. Real Baevsky / HRV needs millisecond beat-to-beat intervals. Worse, it **contradicts the passive-only pivot**: I removed the only sensor path that could produce true HRV, then kept a metric that wants it. The right move is to drop it or clearly label it "experimental, non-clinical."
- **Validate before building more.** I built a sophisticated engine before validating (a) that the Readiness number agrees with a reference device on real data over weeks, and (b) that anyone would pay. Engine-first, validation-later is the classic indie trap.
- **Test the unglamorous parts.** Coverage is excellent on the math and ~zero on sync/auth/networking — exactly the areas where Health Connect/OEM latency bugs actually bite users.
- **Ship the backend or cut it from the demo.** A differentiator that only runs on my LAN isn't a differentiator yet.
- **Personalise the biometric profile.** Hardcoded HRmax/sex in the chat path makes TRIMP wrong for many users — a small fix with outsized correctness impact.

## 7. What this project demonstrates

- Real **signal processing / applied math** in production Kotlin, properly tested.
- **Modern Android** at a senior level: multi-module Clean Architecture, Compose, Hilt, Room, WorkManager, Health Connect, coroutines/Flow.
- **Security & privacy** thinking: on-device encryption, edge-first data minimisation.
- **Product judgment**: a non-obvious, well-reasoned pivot — and the maturity to name the project's own weaknesses.
- **AI-orchestrated delivery** under human direction.

> See the root [README](../README.md) for setup and the [portfolio checklist](PORTFOLIO_CHECKLIST.md) for the cleanup roadmap.
