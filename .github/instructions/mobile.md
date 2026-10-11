# Mobile Guidelines

## Stack

* **App:** React Native with **Expo SDK 57**, in `mobile/`, for Android and iOS from a single codebase.
* **Language:** TypeScript in strict mode (`mobile/tsconfig.json` extends `expo/tsconfig.base`).
* **Runtime:** Node 24 (installed with nvm).
* **Quality tools:** ESLint 9 (`eslint-config-expo` + `eslint-plugin-jsdoc`), Prettier 3, and `expo-doctor`.
* **Navigation and server data:** Expo Router (entry `expo-router/entry`, scheme `personalfinance`) and TanStack Query (see Architecture). `react-native-svg` draws the few icons the designs use.
* **Tests:** none yet. Do not add a test framework or test files until the developer asks for it.
* `mobile/AGENTS.md` comes from the Expo template and holds Expo-specific rules (versioned docs, `npx expo install`, native folders). Read it before touching Expo APIs.

---

## Commands

Run them from `mobile/` (or with `npm --prefix mobile run <script>` from the repository root):

* `npm run check`: the quality gate. Runs, in order, `typecheck`, `lint`, `format:check`, and `expo-doctor`. It must pass before a change is done.
* `npm run typecheck`: `tsc --noEmit`.
* `npm run lint` / `npm run lint:fix`: ESLint through `expo lint`.
* `npm run format` / `npm run format:check`: Prettier.
* `npx expo start`: dev server. Scan the QR with Expo Go (the machine's firewall must allow TCP 8081).

---

## Dependencies

* Add packages with `npx expo install <package>` (dev dependencies: `npx expo install -- --save-dev <package>`), never with a bare `npm install <package>`. It resolves versions compatible with the SDK.
* Prefer Expo modules over third-party libraries. Do not add a dependency without a concrete need.
* The app must keep working in **Expo Go**. A library with native code that Expo Go does not bundle requires a development build; ask the developer first.
* Never create or edit `mobile/ios/` or `mobile/android/` by hand. Native configuration goes in `app.json` and config plugins.

---

## Architecture

The app is layered like the backend: dependencies point inward. ESLint enforces these rules (`import/no-restricted-paths` and `import/no-cycle` in `mobile/eslint.config.js`); `npm run lint` fails when one is broken.

```text
mobile/
  .env.example           Template for .env.local (EXPO_PUBLIC_API_URL)
  src/
    app/                 Routes (Expo Router). Thin screens that only compose features
    features/<feature>/  One folder per product area: its screens, components/, <feature>.model.ts
    shared/              App code that any feature can reuse: components/, hooks/, choices/, errors/
    api/                 client/ (HTTP client, ApiError, query client, invalidation) and resources/<resource>/
    lib/                 Pure helpers grouped by topic: money/, dates/, recurrence/
    design-system/       tokens/, theme/, and components/ (see Design System)
```

Inside every layer, files are **grouped by unit**: a component, together with everything that belongs only to it, lives in its own folder.

```text
design-system/
  tokens/        colors.ts · typography.ts · spacing.ts · index.ts
  theme/         ThemeProvider.tsx · useTheme.ts · useThemedStyles.ts · DesignSystemProvider.tsx · focusRing.ts · index.ts
  components/    Button/ (Button.tsx · Button.styles.ts · index.ts), Field/, AmountField/, ChoiceGroup/, DatePickerSheet/ (… · DatePickerSheet.model.ts), …
shared/
  components/    Screen/, BackHeader/, LoadState/, Toast/ (Toast.tsx · ToastProvider.tsx · ToastOutlet.tsx · useToast.ts · …), BottomSheet/, BottomBar/, OperationRow/, ListGroup/, Notice/, DetailRow/, SectionTitle/
  hooks/         useMutationLock.ts · useRefresh.ts · useKeyboardVisible.ts · useDelayedIndicator.ts · useDelayedLoading.ts · useIsMounted.ts
  choices/       choiceOptions.ts
  errors/        errorMessage.ts
features/dashboard/                    a feature with one screen
  DashboardScreen.tsx · DashboardScreen.styles.ts · dashboard.model.ts · index.ts
  components/    AvailableSummary/, CommitmentList/, RegistrationSheet/ (RegistrationSheet.tsx · .styles.ts · .model.ts · index.ts), …
features/obligations/                  a feature with several screens
  ObligationsListScreen/ · ObligationDetailScreen/ · ObligationFormScreen/   each a unit folder (Screen.tsx · .styles.ts · .model.ts · index.ts)
  obligations.model.ts · index.ts
  components/    ObligationRow/, PaySheet/, ResolvedList/, …   sections used by more than one of its screens
api/
  client/        client.ts · errors.ts · queryClient.ts · invalidation.ts · index.ts
  resources/     common/ · dashboard/ · obligations/ · operations/ · accounts/ · categories/ · setup/
                 each: types.ts · endpoints.ts · queries.ts · mutations.ts (when it has any) · index.ts
lib/
  money/         format.ts · input.ts · index.ts
  dates/         parse.ts · format.ts · relative.ts · index.ts
  recurrence/    format.ts · index.ts
```

```text
app → features → shared → api / lib / design-system
                   api  → lib
```

* **`app/`** holds routes only. A route file renders a screen exported by a feature and wires navigation; it holds no data fetching, business logic, or styling of its own. Nothing imports from `app/`.
* **`features/<feature>/`** holds only what is specific to one product area: its screens, the sections that only those screens show (`components/`), and the mapping from API data to what those screens render (`<feature>.model.ts`, or the `.model.ts` of the component that uses it). A feature never imports another feature. A new folder under `features/` is picked up by the lint rules automatically.
* **`shared/`** holds app code that is not specific to one feature and that several screens use or will use: screen scaffolding (`Screen`, `ScreenLoading`, `ScreenError`, `BackHeader`, `LoadState`, `BottomBar`, and the `useRefresh` pull-to-refresh hook), the sheet (`BottomSheet`), the toast (`ToastProvider`, `ToastOutlet`, `useToast`), error copy for API errors (`errorMessage`, with `ACCOUNTS_NOT_LOADED_ERROR` and `CATEGORIES_NOT_LOADED_ERROR`), the account and category choices of a form (`choices/`), the mutation lock (`useMutationLock`), the keyboard state (`useKeyboardVisible`), the loading timing (`useDelayedIndicator`, `useDelayedLoading`), `useIsMounted`, and list and row patterns from the screen designs (`SectionTitle`, `ListGroup`, `Notice`, `DetailRow`, `OperationRow` with `operationDisplay`). `Screen` provides the background, safe area, scrolling sections, optional pull-to-refresh (only when `onRefresh` is given, so a form's typed values are never replaced), and a `scrollRef`, plus `header`, `footer`, and `overlay` slots; it does not pad the `header`, so each screen pads its own. `BackHeader` is the back button header of a screen; `LoadState` is a back-button screen before it has data: the delayed `ActivityBar` with its text, or the failure with "Reintentar". `Notice` has the tones `info`, `warning`, and `danger`, an optional title, and `details`; `DetailRow` has `muted`. The toast lives in one `ToastProvider` in the root layout, so a toast shown just before a screen closes stays on display on the screen it returns to; each screen draws it with `ToastOutlet`, which takes a `bottom` offset so it can sit above a footer. It may import `api/`, `lib/`, and `design-system/`, never `features/` or `app/`.
* **`api/`** knows nothing about the UI. It may import only `lib/`. It is organized by resource (see Server data).
* **`lib/`** holds pure helpers with no React Native, UI, or API code. It imports nothing from the other layers.
* **`design-system/`** is self-contained. It imports nothing from the other layers.
* **Public entry points.** `design-system/`, `shared/`, `api/`, `lib/`, and every feature expose a public `index.ts`. Code outside a module imports only that index (`../../api`), never its inner files (`../../api/client`). Inside a module, a **unit folder** (a component folder, a resource folder, a `lib` topic) also has an `index.ts`, and code outside that folder imports only it (`../components/Button`, never `../components/Button/Button.styles`). Files inside the same folder import each other directly.

### Files and naming

| Kind of file | Shape | Example |
|---|---|---|
| Component folder | `PascalCase/`, named like the component | `components/Button/` |
| Component | `Name.tsx` | `Button.tsx` |
| Styles of a component | `Name.styles.ts` | `Button.styles.ts` |
| Screen of a feature | `<Feature>Screen.tsx` at the feature root when the feature has one screen; `<Name>Screen/` unit folder when it has several | `DashboardScreen.tsx`, `ObligationDetailScreen/` |
| Non-React logic of a component or screen | `Name.model.ts` (`<feature>.model.ts` for logic several sections of a feature share) | `RegistrationSheet.model.ts`, `dashboard.model.ts` |
| Hook | `useName.ts` | `useToast.ts` |
| Grouping folder (not a component) | lowercase | `tokens/`, `hooks/`, `resources/`, `money/` |
| Entry point | `index.ts` in every unit folder | `components/Button/index.ts` |

* One exported component per file. A component's folder holds everything that belongs only to it: its styles, its model, its hook, and private subcomponents (a chip, an icon) in their own `.tsx` files. Anything another unit also needs moves out (see Where code goes).
* **Styles live in `Name.styles.ts`.** It exports `createNameStyles(theme)`, built with `StyleSheet.create` from tokens only, and the component reads it with `useThemedStyles(createNameStyles)` from `design-system/theme` (recomputed only when the theme changes). State-dependent styles (pressed, focused, invalid) are chosen in the component among styles defined in `.styles.ts`. A `.styles.ts` imports only from `design-system/`.
* **Moving or renaming tracked files is done with `git mv`**, never by creating a new file and deleting the old one, so git keeps each file's history.

### Where code goes (reuse first)

Before writing anything, look for it in `design-system/`, `shared/`, `lib/`, and `api/`, in that order, and reuse it. When it does not exist, place it by asking who else would use it:

| What it is | Where it goes |
|---|---|
| A pure function with no React (formatting, parsing, date math, text for a value) | `lib/` |
| Talking to the server: types, endpoints, query options, mutations, cache invalidation | `api/resources/<resource>/` (invalidation helpers in `api/client/`) |
| A component or hook that another screen would use too, even if only one screen uses it today | `shared/` |
| A section, text, or mapping that only makes sense on one feature's screens | `features/<feature>/` |
| A visual primitive from the source design system (Lucas DS) | `design-system/`, only when the design system defines it |

* A screen is composition: it wires queries and mutations to sections and shared scaffolding. Logic that does not depend on React (sorting, grouping, texts) goes in a `.model.ts` or in `lib/`, not in components.
* Do not copy code between features. When a second feature needs something that lives in a feature, move it to `shared/`, `lib/`, or `api/` in the same change.
* Do not create a shared abstraction for something only one screen needs and no other planned screen would use; keep it in the feature until a second use appears.

### Navigation

* Use **Expo Router**, as `mobile/AGENTS.md` describes; routes live in `src/app/`, and `_layout.tsx` files define navigators.
* `src/app/_layout.tsx` mounts the app-wide providers (`SafeAreaProvider` → `QueryClientProvider` → `DesignSystemProvider`) and a header-less `Stack`. `src/app/index.tsx` is the entry gate: it redirects (`<Redirect>`) to `/onboarding` when the user has no accounts and to `/dashboard` otherwise. `onboarding.tsx` and `dashboard.tsx` are the two screens; `_layout.tsx` fades into the dashboard.
* The commitments (obligations) routes, in `src/app/obligations/`: `/obligations` (list), `/obligations/new` (form), `/obligations/[id]` (detail; the `from=dashboard` param makes its back button read "‹ Inicio" instead of "‹ Compromisos"), and `/obligations/[id]/edit` (form). The screens are in `features/obligations/`: list, detail (pay, skip, reopen, undo a payment, archive), and the create/edit form. The routes wire navigation and the screens never import another feature.
* After the initial setup the app replaces the route, so Back never returns to the onboarding, and the dashboard shows the arrival toast "{nombre} está lista." once.
* Screens follow the Claude Design project "Dashboard Finanzas". It has no tabs: the home screen is the dashboard, with a bottom action bar (Ingreso, + Gasto) that opens the registration sheet ("Nuevo gasto" / "Nuevo ingreso": amount, optional category, and the account shown or chosen). Transferir and Revisión stay hidden until they have a design (and, for Revisión, an endpoint). The "Próximos compromisos" section leads to the commitments: "Ver todos los compromisos (n)" opens the list, a new user's invite has "Agregar compromiso", and a row whose payment is blocked shows "Revisar" instead of "Pagar", which opens the detail. That row uses the dashboard API's `paymentBlocked`; the list and detail compute blocked from the account's and category's statuses.
* The first run is the onboarding (`features/onboarding/`): two steps (account name, opening balance), a preparation view, and error screens for network and generic failures. It calls `POST /api/v1/setup`; a 409 `INITIAL_SETUP_ALREADY_COMPLETED` counts as success.

### Server data (TanStack Query)

* All server data goes through **TanStack Query**. Screens and components never call `fetch` or the endpoint functions directly.
* `api/` is organized by resource: `api/resources/<resource>/` (`accounts`, `categories`, `dashboard`, `obligations`, `operations`, `setup`; shared types in `common`) holds the resource's types (`types.ts`), its endpoint functions (`endpoints.ts`), a query options factory per query (`queries.ts`: `dashboardQuery()`, built with TanStack's `queryOptions`), and a hook per mutation (`mutations.ts`: `usePayOccurrence()`). The HTTP client, `ApiError`, the query client and keys, and the invalidation helpers live in `api/client/`. Features read data with `useQuery(dashboardQuery())` and change it with the mutation hooks.
* Query keys are defined in one place in `api/` and start with the resource (`queryKeys` in `api/client/queryClient.ts`: `['dashboard']`, `['obligations']`, `['operations']`, `['accounts']`, `['categories']`).
* **Invalidation lives with the mutation**, in `api/`, never in a feature: every mutation hook invalidates the queries whose data it changes. Mutations that move money (registrations, payments, cancellations) share one invalidation helper (`invalidateMoneyMovement` in `api/client/invalidation.ts`), so they always refresh the same data (dashboard, obligations, operations, accounts).
* **Never retry a mutation.** Registrations and payments are not idempotent (`docs/backlog.md` §3): mutations have `retry: 0`, and a screen runs them through the shared mutation lock, which blocks a second submission synchronously and keeps the submit buttons inactive until the mutation settles. Only queries (GET) may be retried.
* **A mutation whose result toast can outlive its screen** (the screen closes or navigates on success, or the user may leave while it runs) uses `mutateAsync` with `try`/`catch`/`finally`, not `mutate(variables, { onSuccess, … })`: TanStack Query v5 does not call the per-call callbacks of `mutate` once the component that called it has unmounted, so the toast and the lock release would be lost. Release the mutation lock in `finally`. Check `useIsMounted()` before navigating.
* Every screen handles the loading, empty, and error states of its queries, and supports pull-to-refresh (except a form, whose typed values must not be replaced), with the scaffolding in `shared/`.
* A screen that only needs the server's "today" reads `horizonQuery()` (`api/resources/dashboard/`): it shares the dashboard's cache entry.

### API client

* Each resource module exposes one function per endpoint, typed with hand-written TypeScript types that mirror `architecture.md` → Entry Layer. There is no runtime validation (no Zod); when an OpenAPI contract exists (`docs/backlog.md` §7), the types are generated from it.
* Request and response types use the API's names (`OperationResponse`, `amountCents`). Enums are string literal unions (`'INCOME' | 'EXPENSE'`).
* Errors are `application/problem+json`. The client turns every non-2xx response into an `ApiError` with `status`, `title`, `detail`, and the stable `code` of a 409. UI copy for an error is chosen from `status` and `code`, never from the server's `detail`, through the single error copy table in `shared/` (`errorMessage.ts`). Its texts are neutral, so any feature can reuse the table.
* The base URL comes from `EXPO_PUBLIC_API_URL` (in `mobile/.env.local`, outside git). A phone needs the machine's LAN address (`http://192.168.x.x:8080`), not `localhost`.

### Money, dates, and forms

* Money stays in integer cents everywhere in the app. `lib/` formats it for display with `Intl.NumberFormat('es-MX', { style: 'currency', currency: 'MXN' })`, and parses user input into cents without floating-point arithmetic.
* API dates are ISO-8601 calendar dates (`2026-10-10`) without a time zone. Keep them as strings; never build them through `new Date(...)` in UTC. "Today" is the server's: use `horizon.from` from the dashboard, through `horizonQuery()` (`docs/backlog.md` §9.6). `lib/dates/` does the calendar arithmetic and Spanish date texts on those strings, and `lib/recurrence/` the Spanish text of an obligation's calendar and the form's summary; the occurrences themselves are always the server's.
* Forms use `useState` and the design system `Field`. No form library.
* There is no global client state store. React context is only for cross-cutting providers (theme, query client).

---

## TypeScript

* No `any`. Use `unknown` and narrow it when a type is genuinely unknown.
* No non-null assertions (`!`) and no `as` casts, except to type a literal (`as const`) or when the type system cannot express a value that has already been checked; justify that case with a comment.
* Use `interface` for component props and object contracts, and `type` for unions and aliases.
* Name props interfaces `<Component>Props` and export them next to the component.
* Use function components and hooks. No class components.
* Use named exports. Route files in `src/app/` keep their default export because Expo Router requires it.
* Use `PascalCase` for components and types, `camelCase` for variables, functions, and hooks (`useTheme`), and `UPPER_SNAKE_CASE` for module-level constants that are not tokens.

---

## Comments

Code is always commented. Comments are written in **English**, like the Java code. Text shown to users is not a comment and stays in the app's language (Spanish).

* **Every exported declaration** (component, function, hook, interface, type, constant) has a JSDoc comment (`/** ... */`) that says what it is and what it is for. ESLint enforces this with `jsdoc/require-jsdoc`; empty JSDoc blocks are rejected.
* **Internal functions and components** also get a JSDoc comment, even though the linter does not enforce it.
* **Props** get a JSDoc comment whenever their name and type do not tell the whole story: defaults, units, valid ranges, side effects, or the design system rule they carry (for example, "only once per screen").
* **Inline comments** (`//`) explain *why*: a design system rule, a platform quirk (Android vs. iOS), a workaround, or a non-obvious decision. Do not restate what the code already says.
* When code changes, update its comments in the same change. A comment that no longer matches the code is a bug.

---

## Design System

`mobile/src/design-system/` translates the "Lucas DS" design system from claude.ai/design (`tokens.css` + `components.css`) to React Native. It is the only source of visual values.

* Wrap the app once in `<DesignSystemProvider>`. It loads Inter and JetBrains Mono and provides the theme. Read the theme with `useTheme()`.
* Build screens with `Root`, `Stack`, `Row`, `Text`, `Code`, `Button`, `Field`, `AmountField`, `ChoiceGroup`, `ActivityBar`, `Card`, `Badge`, `PressableRow`, `ConfirmDialog`, `DateField`, and `DatePickerSheet` before writing custom views.
* Proposals pending adoption in Lucas DS (approved by the developer for the app, not yet in the source project): the components `AmountField`, `ChoiceGroup`, `ActivityBar`, `PressableRow`, `ConfirmDialog`, `DateField`, and `DatePickerSheet`; the tokens `control.xl`, `control.touch`, the `amount` text style, and the semantic color `colors.scrim`; and the `size` and `fullWidth` props of `Field` and `fullWidth` of `Button`. `scrim` is the backdrop behind dialogs and sheets (neutral-900 at 32 % in light, neutral-950 at 60 % in dark); use it instead of a literal color.
* Decisions of the `PressableRow`, `ConfirmDialog`, `DateField`, and `DatePickerSheet` proposals:
  * They carry Spanish copy inside ("Cancelar", month and weekday names), because the app is Spanish-only for now.
  * `DatePickerSheet` keeps its own Spanish month and weekday tables and calendar arithmetic (`DatePickerSheet.model.ts`), because the design system cannot import `lib/`. It also repeats the sheet chrome of `shared/BottomSheet` (modal, scrim, handle), because the design system cannot import `shared/`; keep the two in step.
  * `ConfirmDialog` has no destructive variant (the design system's destructive button is pending), so it never uses red.
  * Their icons (chevron, calendar) are drawn as inline SVG inside each component, because icons are pending in Lucas DS.
  * Still pending in the date picker: keyboard navigation in the month grid, Esc to close, and returning focus to the button that opened it.
* **Tokens only.** Colors come from `useTheme().colors` (semantic tokens). Spacing, radii, control heights, and fonts come from `space`, `radius`, `control`, `borderWidth`, and `fontFamily`. No literal colors, sizes, radii, or spacing. The only literals allowed are `1` (border) and `2` (focus ring), through `borderWidth`. Never use `primitives` in UI code.
* Light and dark themes follow the system. Never branch on `scheme` to pick colors: if something needs a different value in dark mode, a semantic token is missing.
* The fluid styles (`display`, `title-1`, `title-2`, `spaceSection`) are for screen-level titles and section spacing only, never inside a component.
* Rules from the design system that are easy to break:
  * Only one `Button` with `variant="primary"` per screen. Card actions are `secondary` or `ghost` with `size="sm"`.
  * Cards are delimited by a border, never by a shadow. Shadows (`useTheme().shadows`) are only for floating elements.
  * Font weight 600 at most; never 700.
  * On `role.<role>.fill`, text uses `role.<role>.on`.
  * Color never carries meaning alone: every state has a word or an icon.
  * No decorative emoji, no gradients, no cards with a colored left border.
  * Body text and text inputs are never smaller than 16 px (`body`). Secondary and meta text may use `body-sm` (14 px), and badges and small links `caption` (12 px), the system's minimum.
* Anything the design system marks as **pending** (destructive button, icon-only button, loading state, select, textarea, clickable card, icons) does not exist yet. Do not invent it; report that it is missing.
* A new token or component must stay faithful to the source design system. If it does not exist there, propose it to the developer instead of adding it.
* **Screen designs may use sizes outside the scale** (e.g. the dashboard's 52 px amount). Such values live in the `.styles.ts` of the component that uses them, as named constants with a JSDoc that cites the design. Values that are not styles (durations, delays, limits) live next to the hook or model that uses them, also named and documented. Colors, spacing, radii, control heights, and fonts still come from the tokens.
* Use `Text`'s `weight` prop (400, 500, 600) for a weight other than the variant's; do not set `fontFamily` by hand in screens.

---

## Accessibility

* Every interactive element has an accessible role and label (`accessibilityRole`, `accessibilityLabel`, or a visible label).
* Never remove the focus ring (`focusRing(theme)`).
* An unavailable action uses `Button`'s `inactive` prop (equivalent to `aria-disabled`), so it stays focusable and announced. Avoid it when possible.
* Field errors go in `Field`'s `error` prop: what happened and what to do, in one sentence.

---

## Backend API

* The app consumes the REST API under `/api/v1` (see `architecture.md` → Entry Layer) through `api/` and TanStack Query (see Architecture). It never reimplements business rules: validation in the app only improves the user experience; the backend decides.
* Follow the API's JSON conventions (`architecture.md` → Entry Layer): camelCase, UUID ids, ISO-8601 dates, uppercase enum strings.
* Money is an integer number of cents in `...Cents` fields. Keep it in cents in the app and convert to MXN only to display it; never do money arithmetic with floating-point decimals.
* Show API errors to the user with the design system (for example, `Field`'s `error` or a `Badge` with `role="danger"` plus a word), never as raw messages or stack traces.
