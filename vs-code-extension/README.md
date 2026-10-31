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

In the new window, set **Auto Debugger: Output Root** (`autoDebugger.outputRoot`) to the folder that holds the run
directories, e.g. `Java/auto-debugger/demo/output`, or use the folder button in the Runs view.

## Layout

| Path               | Content                                                                                      |
| ------------------ | -------------------------------------------------------------------------------------------- |
| `src/extension.ts` | Entry point, registers views                                                                 |
| `src/settings.ts`  | Reading and saving the extension settings                                                    |
| `src/model/`       | Types for the contract files (`run.json`)                                                    |
| `src/runs/`        | Reading `run.json` and finding run directories (no `vscode` import, unit tested)             |
| `src/views/`       | Tree views (Runs)                                                                            |
| `test/`            | Unit tests (`*.test.ts`), fixtures in `test/fixtures/`                                       |
| `contract/`        | Backend contract: JSON Schemas, examples, `run-tests` command ([README](contract/README.md)) |
