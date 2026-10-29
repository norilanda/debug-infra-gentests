# Auto Debugger (VS Code extension)

Lets the user pick a run of [`auto-debugger`](../Java/auto-debugger/README.md), step forward or backward through the
recorded calls of the target procedure, and run the test generated for a call.

## Develop

Requires Node.js 20+.

```bash
npm install
npm run build       # bundle dist/extension.js with esbuild
npm run watch       # rebuild on change
npm run typecheck   # tsc, no emit
npm run lint        # eslint
npm run format      # prettier, rewrites files (format:check only reports)
npm test            # vitest unit tests (code that doesn't import `vscode`)
```

To try it, open this folder in VS Code and press F5 (**Run Extension**). It builds and opens an Extension Development
Host window.

## Layout

| Path               | Content                                                                                      |
| ------------------ | -------------------------------------------------------------------------------------------- |
| `src/extension.ts` | Entry point, registers views                                                                 |
| `src/views/`       | Tree views (Runs)                                                                            |
| `test/`            | Unit tests (`*.test.ts`)                                                                     |
| `contract/`        | Backend contract: JSON Schemas, examples, `run-tests` command ([README](contract/README.md)) |
