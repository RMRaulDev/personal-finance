// https://docs.expo.dev/guides/using-eslint/
const fs = require('node:fs');
const path = require('node:path');
const { defineConfig } = require('eslint/config');
const expoConfig = require('eslint-config-expo/flat');
const jsdoc = require('eslint-plugin-jsdoc');
const prettierRecommended = require('eslint-plugin-prettier/recommended');

/**
 * Lists the feature folders under src/features. Read on every lint run, so a new feature is
 * covered by the architecture rules without touching this file.
 */
function listFeatures() {
  const featuresDir = path.join(__dirname, 'src/features');
  if (!fs.existsSync(featuresDir)) {
    return [];
  }
  return fs
    .readdirSync(featuresDir, { withFileTypes: true })
    .filter((entry) => entry.isDirectory())
    .map((entry) => entry.name);
}

/** Lists the names of the subfolders of a folder under mobile/ (none when it does not exist). */
function listDirs(relativeDir) {
  const dir = path.join(__dirname, relativeDir);
  if (!fs.existsSync(dir)) {
    return [];
  }
  return fs
    .readdirSync(dir, { withFileTypes: true })
    .filter((entry) => entry.isDirectory())
    .map((entry) => entry.name);
}

/**
 * Lists the unit folders (see .github/instructions/mobile.md → Architecture): every component
 * folder, every api resource and api/client, every lib topic, and the design system's tokens and
 * theme. Computed on every lint run, so a new unit is covered without touching this file.
 */
function listUnits() {
  const componentRoots = [
    './src/design-system/components',
    './src/shared/components',
    ...listFeatures().map((feature) => `./src/features/${feature}/components`),
  ];
  const units = [
    ...componentRoots.flatMap((root) => listDirs(root).map((name) => `${root}/${name}`)),
    ...listDirs('./src/api/resources').map((name) => `./src/api/resources/${name}`),
    ...listDirs('./src/lib').map((name) => `./src/lib/${name}`),
  ];
  for (const fixed of ['./src/api/client', './src/design-system/tokens', './src/design-system/theme']) {
    if (fs.existsSync(path.join(__dirname, fixed))) {
      units.push(fixed);
    }
  }
  return units;
}

/**
 * Everything under src except `unit` and the folders that contain it: the siblings of the unit
 * and of each of its ancestors. Together they are "outside the unit" for a restricted-path zone.
 */
function outsideOf(unit) {
  const parts = unit.replace('./', '').split('/');
  const targets = [];
  for (let depth = 1; depth < parts.length; depth += 1) {
    const parent = parts.slice(0, depth).join('/');
    for (const entry of fs.readdirSync(path.join(__dirname, parent))) {
      if (entry !== parts[depth]) {
        targets.push(`./${parent}/${entry}`);
      }
    }
  }
  return targets;
}

const features = listFeatures();
const featurePaths = features.map((feature) => `./src/features/${feature}`);

// Every module that exposes a public index.ts. Code outside a module may only import that index.
const modules = ['./src/design-system', './src/api', './src/lib', './src/shared', ...featurePaths];
// Code that consumes modules: the routes and the modules themselves.
const consumers = ['./src/app', ...modules];

/**
 * Builds the zones of `import/no-restricted-paths` that enforce the architecture in
 * .github/instructions/mobile.md → Architecture. Dependencies point inward:
 * app → features → shared → api / lib / design-system, and api → lib.
 */
function architectureZones() {
  const zones = [
    {
      target: './src/design-system',
      from: ['./src/app', './src/features', './src/shared', './src/api', './src/lib'],
      message: 'design-system is self-contained: it imports nothing from app, features, shared, api, or lib.',
    },
    {
      target: './src/lib',
      from: ['./src/app', './src/features', './src/shared', './src/api', './src/design-system'],
      message: 'lib holds pure helpers: it imports nothing from app, features, shared, api, or design-system.',
    },
    {
      target: './src/api',
      from: ['./src/app', './src/features', './src/shared', './src/design-system'],
      message: 'api knows nothing about the UI: it may only import lib.',
    },
    {
      target: './src/shared',
      from: ['./src/app', './src/features'],
      message: 'shared is reused by every feature: it never imports features or routes.',
    },
    {
      target: './src/features',
      from: './src/app',
      message: 'Features never import routes; routes compose features.',
    },
  ];

  // A feature never imports another feature. Shared code moves down to shared, lib, or api.
  for (const feature of featurePaths) {
    const others = featurePaths.filter((other) => other !== feature);
    if (others.length > 0) {
      zones.push({
        target: feature,
        from: others,
        message: 'A feature never imports another feature. Move the shared code to shared, lib, or api.',
      });
    }
  }

  // Outside a module, only its public index.ts may be imported.
  for (const module of modules) {
    zones.push({
      target: consumers.filter((consumer) => consumer !== module),
      from: module,
      except: ['./index.ts'],
      message: `Import ${module.replace('./', '')} through its public index.ts, not its inner files.`,
    });
  }

  // Inside a module, outside a unit folder only its index.ts may be imported.
  for (const unit of listUnits()) {
    zones.push({
      target: outsideOf(unit),
      from: unit,
      except: ['./index.ts'],
      message: `Import ${unit.replace('./src/', '')} through its index.ts, not its inner files.`,
    });
  }

  // A .styles.ts is built from design-system tokens only.
  zones.push(
    {
      target: './src/**/*.styles.ts',
      from: ['./src/app', './src/features', './src/shared', './src/api', './src/lib'],
      message: 'A .styles.ts imports only from design-system.',
    },
    {
      target: './src/design-system/**/*.styles.ts',
      from: './src/design-system/components',
      message: 'Inside design-system, a .styles.ts imports only from tokens and theme.',
    },
  );

  return zones;
}

module.exports = defineConfig([
  expoConfig,
  // Prettier formats the code; its differences are reported as lint errors.
  prettierRecommended,
  {
    // Every export has a JSDoc comment that says what it is and what it is for.
    files: ['**/*.{ts,tsx}'],
    plugins: { jsdoc },
    rules: {
      'jsdoc/require-jsdoc': [
        'error',
        {
          publicOnly: true,
          require: {
            FunctionDeclaration: true,
            ArrowFunctionExpression: true,
            ClassDeclaration: true,
          },
          contexts: [
            'ExportNamedDeclaration > TSInterfaceDeclaration',
            'ExportNamedDeclaration > TSTypeAliasDeclaration',
            'ExportNamedDeclaration > TSEnumDeclaration',
            'ExportNamedDeclaration > VariableDeclaration',
          ],
        },
      ],
      'jsdoc/no-blank-blocks': 'error',
    },
  },
  {
    // The layered architecture: allowed import directions and public entry points.
    files: ['**/*.{ts,tsx}'],
    rules: {
      'import/no-restricted-paths': ['error', { basePath: __dirname, zones: architectureZones() }],
      // Circular imports usually mean a layer is in the wrong place.
      'import/no-cycle': 'error',
    },
  },
  {
    ignores: ['dist/*', '.expo/*'],
  },
]);
