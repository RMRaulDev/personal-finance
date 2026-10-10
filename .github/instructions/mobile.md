# Mobile Guidelines

## Stack

* **App:** React Native with **Expo SDK 57**, in `mobile/`, for Android and iOS from a single codebase.
* **Language:** TypeScript in strict mode (`mobile/tsconfig.json` extends `expo/tsconfig.base`).
* **Runtime:** Node 24 (installed with nvm).
* **Quality tools:** ESLint 9 (`eslint-config-expo` + `eslint-plugin-jsdoc`), Prettier 3, and `expo-doctor`.
* **Navigation and server data:** Expo Router and TanStack Query (see Architecture). Neither is installed yet; add them with `npx expo install` when the first screen needs them.
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
  App.tsx                Root component (until Expo Router is installed): mounts the providers and the first screen
  index.ts               Expo entry point (registerRootComponent)
  src/
    app/                 Routes (Expo Router). Thin screens that only compose features
    features/<feature>/  One folder per product area: dashboard, operations, obligations, accounts
    api/                 Typed HTTP client, API types, ApiError, TanStack Query client and keys
    lib/                 Pure helpers: money (cents → MXN), ISO dates
    design-system/       Tokens, theme, and base components (see Design System)
```

```text
app → features → api / lib / design-system
          api  → lib
```

* **`app/`** holds routes only. A route file renders a screen exported by a feature and wires navigation; it holds no data fetching, business logic, or styling of its own. Nothing imports from `app/`.
* **`features/<feature>/`** holds the screens, components, and hooks of one product area. A feature never imports another feature: shared code moves down to `lib/` or `api/`. A new folder under `features/` is picked up by the lint rules automatically.
* **`api/`** knows nothing about the UI. It may import only `lib/`.
* **`lib/`** holds pure helpers with no React Native, UI, or API code. It imports nothing from the other layers.
* **`design-system/`** is self-contained. It imports nothing from the other layers.
* **Public entry points.** `design-system/`, `api/`, `lib/`, and every feature expose a public `index.ts`. Code outside a module imports only that index (`../../api`), never its inner files (`../../api/client`). Inside a module, files import each other directly.
* Keep one component per file, named after the component (`Button.tsx`).

### Navigation

* Use **Expo Router**, as `mobile/AGENTS.md` describes; routes live in `src/app/`, and `_layout.tsx` files define navigators.
* Planned navigation: tabs for Home (dashboard), Operations (history), Obligations, and Accounts; registering an operation opens as a modal sheet. The screen list is final once the "Dashboard Finanzas" design from Claude Design is available.

### Server data (TanStack Query)

* All server data goes through **TanStack Query**. Screens never call `fetch` or the API client directly: each feature exposes hooks (`useDashboard`, `usePayOccurrence`, ...) built on the API functions in `api/`.
* Query keys are defined in one place in `api/` and start with the resource (`['dashboard']`, `['operations', filters]`, `['obligations']`, `['accounts']`, `['categories']`).
* Every mutation invalidates the queries whose data it changes; for example, paying an occurrence invalidates the dashboard, obligations, operations, and accounts.
* **Never retry a mutation.** Registrations and payments are not idempotent (`docs/backlog.md` §3): set `retry: 0` on mutations, and disable the submit button while a mutation is pending. Only queries (GET) may be retried.
* Every screen handles the loading, empty, and error states of its queries, and supports pull-to-refresh.

### API client

* `api/` exposes one function per endpoint, typed with hand-written TypeScript types that mirror `architecture.md` → Entry Layer. There is no runtime validation (no Zod); when an OpenAPI contract exists (`docs/backlog.md` §7), the types are generated from it.
* Request and response types use the API's names (`OperationResponse`, `amountCents`). Enums are string literal unions (`'INCOME' | 'EXPENSE'`).
* Errors are `application/problem+json`. The client turns every non-2xx response into an `ApiError` with `status`, `title`, `detail`, and the stable `code` of a 409. UI copy for an error is chosen from `status` and `code`, never from the server's `detail`.
* The base URL comes from `EXPO_PUBLIC_API_URL` (in `mobile/.env.local`, outside git). A phone needs the machine's LAN address (`http://192.168.x.x:8080`), not `localhost`.

### Money, dates, and forms

* Money stays in integer cents everywhere in the app. `lib/` formats it for display with `Intl.NumberFormat('es-MX', { style: 'currency', currency: 'MXN' })`, and parses user input into cents without floating-point arithmetic.
* API dates are ISO-8601 calendar dates (`2026-10-10`) without a time zone. Keep them as strings; never build them through `new Date(...)` in UTC. "Today" is the server's: use `horizon.from` from the dashboard (`docs/backlog.md` §9.6).
* Forms use `useState` and the design system `Field`. No form library.
* There is no global client state store. React context is only for cross-cutting providers (theme, query client).

---

## TypeScript

* No `any`. Use `unknown` and narrow it when a type is genuinely unknown.
* No non-null assertions (`!`) and no `as` casts, except to type a literal (`as const`) or when the type system cannot express a value that has already been checked; justify that case with a comment.
* Use `interface` for component props and object contracts, and `type` for unions and aliases.
* Name props interfaces `<Component>Props` and export them next to the component.
* Use function components and hooks. No class components.
* Use named exports. `App.tsx` keeps its default export because Expo requires it.
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
* Build screens with `Root`, `Stack`, `Row`, `Text`, `Code`, `Button`, `Field`, `Card`, and `Badge` before writing custom views.
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
  * Text that users read and text inputs are never smaller than 16 px.
* Anything the design system marks as **pending** (destructive button, icon-only button, loading state, select, textarea, clickable card, icons) does not exist yet. Do not invent it; report that it is missing.
* A new token or component must stay faithful to the source design system. If it does not exist there, propose it to the developer instead of adding it.

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
