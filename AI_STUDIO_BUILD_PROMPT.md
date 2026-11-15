# PlayPulse — AI Studio Product Brief and Build Prompt

> **Purpose:** Use this as a project brief/prompt in Google AI Studio to continue building the existing PlayPulse Android app. It is documentation, not executable code. Give AI Studio access to the project files through the supported workflow, then paste or attach this document as context.
>
> **Privacy:** This brief intentionally contains no API keys, passwords, Firebase credentials, signing files, debug tokens, private user data, or production data. Never add those to prompts, source code, screenshots, logs, or commits.

---

## 1. Product in one page

### Product

**PlayPulse is app-store opportunity intelligence for deciding what to build, improve, or pursue next.** It helps app founders and operators assess overlooked mobile-app opportunities, model recurring revenue, monitor a portfolio, and turn assumptions into an actionable plan.

### One-line pitch

> PlayPulse helps app teams find overlooked opportunities, understand revenue risk, and decide what to build, improve, or pursue next.

### Short product description

PlayPulse is an Android portfolio and analytics workspace for app founders, indie developers, and small app studios. It brings opportunity research, subscription and churn modeling, portfolio tracking, AI-assisted strategy, and shareable reporting together in one place. It should make assumptions visible and support decisions—not imply that forecasts or AI responses are guaranteed facts.

### Target users

Start with **indie app founders and small app studios** who need a lightweight way to evaluate ideas and manage a portfolio. A secondary audience is app operators evaluating products to improve or acquire. Avoid building for every possible audience at once.

### User problem

> “I have app ideas or products to evaluate, but it is hard to compare their potential, understand the revenue assumptions, and turn scattered research into a practical next step.”

### Core user outcome

A user can capture an opportunity, review its evidence and assumptions, model a few business scenarios, choose a next action, and revisit the result as the portfolio changes.

### Differentiation

PlayPulse should be a **decision-support workflow**, not a generic dashboard or an “AI does everything” app:

1. Discover or record an opportunity.
2. Review available signals, their source, freshness, and limitations.
3. Model conservative, base, and upside scenarios with editable assumptions.
4. Get a concise, clearly labeled AI-assisted strategy draft.
5. Save a decision and track its progress.
6. Export a report that distinguishes observed data from estimates.

### Trust principles

- Never present fabricated store metrics, rankings, downloads, or competitor data as live or verified facts.
- Clearly label curated/demo data, estimates, user-entered values, and AI-generated content.
- Show assumptions behind revenue, LTV, churn, and forecast calculations.
- Let users edit assumptions and understand which inputs affect a result.
- Treat AI as an assistant: users must review and verify recommendations.
- Do not request confidential business or personally identifying information unless essential.

---

## 2. Current app context and engineering constraints

PlayPulse is an existing Kotlin Android app using Jetpack Compose and Material 3. Continue the current project; do not replace it with a web app or create a separate starter application.

The repository's documented architecture includes:

- MVVM with a `PlayPulseViewModel`, repositories, and immutable `StateFlow` UI state.
- Compose navigation and auth-gated feature screens.
- Firebase Authentication with Google sign-in through Jetpack Credential Manager.
- A named Cloud Firestore database and user-scoped data under `/users/{userId}`.
- Firebase AI Logic for AI features; use the existing repository integration rather than direct client REST calls or a hard-coded model API key.
- App Check initialization before Firebase-dependent app work.
- Existing opportunity, analytics, subscription, payment gateway, alert, saved-report, and strategy-brief models.
- CSV export through Android's Storage Access Framework.
- Unit, Robolectric/Compose UI, and Firestore rules tests.

### Mandatory implementation guardrails

Before editing, inspect the repository and follow its actual current code and tests. Do not assume this brief overrides source code or repository instructions.

- Preserve the current Kotlin/Compose architecture, navigation, theme, design system, and existing working features.
- Read and follow `.github/copilot-instructions.md` if present.
- Do not add a new Firebase project, database, authentication method, backend, or dependency without first explaining why it is needed.
- Do not introduce API keys, secrets, credentials, signing material, private Firebase config, debug tokens, or real user data into code, prompts, tests, logs, screenshots, or documentation.
- Never use anonymous or email/password authentication; preserve the app's Google Credential Manager flow.
- Always use the app's existing named Firestore database configuration; never silently use the default database.
- Keep Firestore reads/writes user-scoped and consistent with existing rules and model allowlists. Do not weaken security rules to make a feature work.
- Keep AI calls behind the existing Firebase AI Logic repository, use structured outputs where appropriate, respect existing timeouts, and propagate coroutine cancellation correctly.
- Put user-facing copy in Android string resources and provide accessible labels, meaningful content descriptions, test tags, and touch targets consistent with the project.
- Do not add permissions when a system picker or existing mechanism can do the job.
- Avoid broad refactors. Make small, reviewable changes and add or update tests for behavior you change.
- If a requested feature cannot be implemented safely or needs a credential, external data source, or product decision that is not available, stop at a safe interface/mock and clearly explain what is needed. Never invent credentials or claim an integration is live.

---

## 3. Product experience: home screen and onboarding plan

### Home / dashboard

The home screen should answer three questions quickly: **What needs attention? What is the portfolio doing? What can I do next?**

Recommended hierarchy:

1. **Welcome and primary action**
   - Brief greeting and concise portfolio context.
   - Primary CTA: “Add an opportunity” or “Scout an opportunity.”
   - Secondary CTA: “View portfolio.”
2. **Portfolio snapshot**
   - Show only a small set of useful KPIs, such as tracked opportunities, active pipeline items, modeled MRR, and alerts needing attention.
   - Explain empty or unavailable values; never display fake zeroes as if they were measured.
   - Distinguish user-entered/modelled values from measured values.
3. **Next best actions**
   - Examples: complete an opportunity profile, review a stale assumption, compare scenarios, or follow up on an alert.
   - Keep recommendations explainable and dismissible.
4. **Opportunity/pipeline preview**
   - Show a few recent or priority opportunities, their stage, last-updated date, and a concise reason for attention.
5. **Insights preview**
   - Show a short, clearly labeled AI insight only when one exists; otherwise offer a useful opt-in action to generate one.
6. **Navigation**
   - Preserve the project's existing adaptive navigation conventions and route structure.

### Onboarding flow

Make onboarding short, skippable, and useful without requiring sensitive information.

1. **Welcome:** Explain the product in plain language: evaluate opportunities, model economics, and track decisions.
2. **Choose a starting path:** “Explore a sample workspace,” “Add my first opportunity,” or “Set up my portfolio.”
3. **Privacy and AI explanation:** State what data is saved to the signed-in user's workspace and that AI output is generated assistance, not verified market research. Do not promise data handling the app cannot prove.
4. **Optional goals:** Let users choose priorities such as opportunity research, revenue modeling, or portfolio monitoring. Keep this optional and editable.
5. **First useful action:** Guide the user to create or inspect one opportunity, enter a few assumptions, and view a scenario. Provide a clear skip/exit path.
6. **Completion:** Show a small checklist and return to the dashboard. Do not require payment details, store credentials, or personal financial information.

If a sample workspace exists, clearly identify it as sample data and never mix it with the user's live data. Preserve auth gating: do not instantiate authenticated feature state or access private collections before a valid user session is established.

---

## 4. MVP priorities

### P0 — Make the core decision loop trustworthy

- Add or select an opportunity.
- Show an opportunity detail view with status, source/notes, last-updated information, and editable assumptions.
- Make revenue scenarios understandable, editable, and transparent about assumptions.
- Save a decision/next step and move an opportunity through the existing pipeline.
- Provide clear loading, empty, error, and retry states.
- Ensure security rules and repository tests cover user isolation and valid writes.

### P1 — Improve activation and repeat use

- Add the guided onboarding and helpful dashboard states described above.
- Add a concise “next action” section based on existing app state, not unsupported external data.
- Improve saved reports and explain export contents.
- Allow users to revisit and compare saved scenarios if compatible with the current schema.

### P2 — Add advanced research only when data is real and sourced

- Integrate approved data sources with clear provenance, freshness, usage terms, and failure handling.
- Add team sharing or acquisition workflows only after a clear target customer and access-control design are agreed.
- Do not scrape stores, bypass access controls, or imply official affiliation with Google Play, Galaxy Store, Amazon Appstore, or other providers.

---

## 5. Monetization hypotheses (validate before implementation)

These are hypotheses for user research, not commitments or promises:

- **Free tier:** A small personal portfolio, basic scenario modeling, and limited exports.
- **Pro tier:** More tracked opportunities, saved scenarios, richer reports, and higher AI usage limits.
- **Studio tier:** Multiple portfolios or seats, shared workflows, and team exports, only after access control and collaboration are implemented.

Do not add billing, paywalls, payment processing, or subscription entitlements as part of an unrelated build task. First validate willingness to pay with target users and define costs, limits, cancellation, and support expectations.

---

## 6. 90-day product roadmap

This is a sequence of validation and delivery goals, not a promise of dates or a requirement to build everything at once.

### Days 1–30: Focus and trust

- Interview a small set of indie developers and app-studio operators; test whether opportunity evaluation is a frequent, expensive problem.
- Pick one initial customer and one primary decision workflow.
- Audit current screens, Firestore schema/rules, AI flows, empty states, and existing tests.
- Make data provenance and estimate labels visible in opportunity and analytics views.
- Improve the add-opportunity → model → save-next-step loop.
- Add tests for calculations, input validation, user isolation, and UI empty/error states.

**Exit criteria:** A new user can understand the product, create or inspect an opportunity, and make a decision without mistaking estimates for verified data.

### Days 31–60: Activation and repeat use

- Implement lightweight onboarding and a useful empty dashboard.
- Improve saved scenarios/reports only where supported by the existing model and security rules.
- Make AI briefs actionable, editable, and clearly labeled as AI-generated drafts.
- Test usability with target users and record where they hesitate or lose trust.
- Fix the highest-impact reliability, accessibility, and performance issues.

**Exit criteria:** Test users can complete the core workflow with minimal guidance and can explain the assumptions behind their result.

### Days 61–90: Validate distribution and paid value

- Validate which audience has the strongest recurring need and refine positioning accordingly.
- Assess legitimate data-provider options before adding external market data.
- Test pricing and willingness-to-pay concepts with interviews or a non-binding waitlist.
- Prepare a privacy-safe demo, onboarding copy, and release checklist.
- Only implement a paid tier or team feature after the value proposition and secure entitlement model are understood.

**Exit criteria:** Evidence supports a focused audience, a repeatable user outcome, and a credible next release scope.

---

## 7. Short pitch for a demo or investor conversation

> App teams make expensive decisions with fragmented market research and uncertain revenue assumptions. PlayPulse brings opportunity evaluation, subscription scenario modeling, portfolio tracking, and AI-assisted planning into one workflow. We are starting with indie developers and small studios, helping them compare what to build or improve next while keeping assumptions visible and recommendations verifiable. Our next step is validating which part of this decision workflow users return to most and will pay to improve.

This is an early product positioning statement, not a claim about active customers, verified market coverage, proprietary data, revenue, or traction.

---

## 8. Suggested first implementation task for AI Studio

Start with a **small, high-value implementation** rather than attempting the entire roadmap in one pass:

1. Inspect the existing dashboard, navigation, ViewModel state, models, strings, and tests.
2. Report the current implementation and identify the smallest compatible change for a more helpful dashboard/empty state and first-opportunity action.
3. Implement that one scoped change using existing project patterns and theme.
4. Add or update tests for the changed state and interactions.
5. Run the relevant tests/build if the environment supports it. If not, list exactly what was not run and why.
6. Summarize changed files, behavior, tests, and remaining gaps. Do not claim tests passed unless they were actually run.

### Acceptance criteria for the first task

- Existing auth gating and Firestore access boundaries remain intact.
- The dashboard gives a new user a clear, accessible next step when their portfolio is empty.
- No fabricated live market values are introduced.
- User-facing text uses Android string resources.
- Existing navigation, theme, and architecture are reused.
- Tests cover the important new empty-state/action behavior.
- No secrets, permissions, unrelated dependencies, or broad refactors are added.

---

## 9. Instructions to AI Studio

Use this document as product context, then inspect the provided PlayPulse source repository before proposing or making edits. Work incrementally. First explain the current relevant implementation and a short plan; then implement only the agreed first task above. Preserve existing project conventions and security invariants. Ask for clarification instead of guessing when a product decision, sensitive configuration, or data source is missing. Return a concise summary of edits, test results, and any manual setup required.
