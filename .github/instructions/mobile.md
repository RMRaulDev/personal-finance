# Mobile Guidelines

## Stack

* **App:** React Native with **Expo SDK 57**, in `mobile/`, for Android and iOS from a single codebase.
* **Language:** TypeScript in strict mode (`mobile/tsconfig.json` extends `expo/tsconfig.base`).
* **Runtime:** Node 24 (installed with nvm).
* **Quality tools:** ESLint 9 (`eslint-config-expo` + `eslint-plugin-jsdoc`), Prettier 3, and `expo-doctor`.
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

## Structure

```text
mobile/
  App.tsx              Root component: mounts DesignSystemProvider and the first screen
  index.ts             Expo entry point (registerRootComponent)
  src/
    design-system/     Tokens, theme, and base components (see Design System)
```

* Application code lives in `mobile/src/`. Keep one component per file, named after the component (`Button.tsx`).
* Each folder that other code imports from exposes a public `index.ts`. Import from it (`./src/design-system`), not from inner files.
* Navigation is not set up yet. When it is needed, use Expo Router as described in `mobile/AGENTS.md`.

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

* The app consumes the REST API under `/api/v1` (see `architecture.md` → Entry Layer). It never reimplements business rules: validation in the app only improves the user experience; the backend decides.
* Follow the API's JSON conventions (`architecture.md` → Entry Layer): camelCase, UUID ids, ISO-8601 dates, uppercase enum strings.
* Money is an integer number of cents in `...Cents` fields. Keep it in cents in the app and convert to MXN only to display it; never do money arithmetic with floating-point decimals.
* Show API errors to the user with the design system (for example, `Field`'s `error` or a `Badge` with `role="danger"` plus a word), never as raw messages or stack traces.
