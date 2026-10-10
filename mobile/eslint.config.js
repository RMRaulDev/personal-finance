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
