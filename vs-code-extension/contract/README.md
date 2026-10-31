# Backend contract (version 1)

How a backend (Java, .NET, …) hands a recorded run to the Auto Debugger extension, and how the extension asks it to run
tests. The protocol is file-based: the backend writes JSON files into a **run directory**, and the extension reads them.
The extension can also start one backend command, `run-tests`.

The JSON shapes are defined by the [JSON Schema](https://json-schema.org/) files in [`schemas/`](schemas/).
This page covers what the schemas can't express: the rules that span files, the command line, and versioning.
[`examples/`](examples/) holds two complete runs that validate against the schemas.

| File           | Schema                                                       | Written by                     |
| -------------- | ------------------------------------------------------------ | ------------------------------ |
| `run.json`     | [`run.schema.json`](schemas/run.schema.json)                 | full run                       |
| `trace.json`   | [`trace.schema.json`](schemas/trace.schema.json)             | full run                       |
| `tests.json`   | [`tests.schema.json`](schemas/tests.schema.json)             | full run                       |
| `results.json` | [`results.schema.json`](schemas/results.schema.json)         | full run, updated by run-tests |
| —              | [`value.schema.json`](schemas/value.schema.json) (snapshots) | referenced by `trace.json`     |

All files are UTF-8 JSON.

## Concepts

- **Run**: one execution of the backend pipeline on one target procedure. One run per directory; a directory is a run
  directory when it contains `run.json`.
- **Call**: one call of the target procedure, with an **entry** snapshot and, if the backend records them, an **exit**
  snapshot and an **outcome** (returned, or threw an exception). `callIndex` numbers the calls 0, 1, 2, … in the order
  they started.
- **Snapshot**: the values of the captured arguments and fields, keyed by `captureId` from `run.json`.
- **Test**: a generated test, identified by a `testId`, reproducing zero or more calls (`callIndices`). A test
  with no calls is **synthetic**: an input combination that never occurred.
- **Result**: the last execution of a test. A test with no result hasn't been executed yet.

## Value kinds

Every value has a `kind` and a `type` (the language's type name as text, shown as it comes).

| `kind`      | Extra keys                   | Meaning                                                                                                                 |
| ----------- | ---------------------------- | ----------------------------------------------------------------------------------------------------------------------- |
| `primitive` | `value`                      | Number, boolean or character. Large integers and NaN / ±Infinity are strings                                            |
| `string`    | `value`                      | A string                                                                                                                |
| `null`      | —                            | A null reference; `type` is the declared type                                                                           |
| `object`    | `fields`, `id`?              | An object with its recorded fields (field name → value)                                                                 |
| `array`     | `elements`, `id`?, `length`? | An array, or a list recorded element by element. `length` is the real size when only the first `elements` were recorded |
| `text`      | `text`, `id`?                | An object recorded only as text, e.g. a library collection through its `toString()`                                     |
| `ref`       | `id`                         | The same object as the `object` / `array` / `text` value with that `id`, elsewhere in the snapshot                      |
| `cycle`     | `id`                         | A reference back to an enclosing `object` / `array` with that `id`                                                      |
| `truncated` | —                            | Cut off by the backend's depth or size limits                                                                           |
| `skipped`   | —                            | Not recorded, e.g. a type the backend doesn't serialize                                                                 |

`id` values are scoped to one snapshot.

### Display names for types

`type` is the full name, e.g. `System.Int32` or `java.util.ArrayList`. A backend can give shorter names for display in
`run.json` `typeNames`, a map from full name to display name:

```json
"typeNames": { "System.Int32": "int", "com.example.Node": "Node" }
```

The UI shows `typeNames[type]` when present, otherwise `type`, and shows the full `type` on hover. The map applies to
every type name in the run: values, `captured[].type` and exception types in `outcome`. It is optional, and only types
that have a shorter name need an entry. This keeps `trace.json` small: each name is listed once per run, not on every
value.

## Rules across files

A backend must also meet these rules, which the schemas don't check:

1. `trace.json` `calls[i].callIndex` is `i`.
2. Every key of a snapshot is a `captureId` listed in `run.json` `captured`. Every captured identifier should appear in
   every snapshot; a value that couldn't be recorded is written as `skipped`.
3. `recorded.exit: false` in `run.json` → no call has `exit`. `recorded.outcome: false` → no call has `outcome`. Both
   default to `true`, and then every call has both, except that a call that didn't finish has `exit: null` and no
   `outcome`. With `recorded.exit: false`, a missing `outcome` is the only sign that a call didn't finish; with both
   `false`, unfinished calls can't be told apart.
4. A `callIndex` appears in at most one test's `callIndices`, and every one is a valid index into `trace.json`. A call in
   no test has no test.
5. `testId`s are unique and stable: the same test gets the same `testId` when it is run again.
6. Every `testId` in `results.json` appears in `tests.json`; at most one result per `testId`.
7. `counts` in `run.json`, when present, equals the number of calls and tests.
8. `run.json` `status` covers only the full run's own work: recording the calls and generating the tests. It is written
   even when that fails; `trace.json` / `tests.json` may then be missing or incomplete. Failures while executing tests (e.g. the tests don't compile) are reported in `results.json` only, and
   `run-tests` never changes `run.json`.

`traceMode` is shown in the run summary only; consumers must not change behaviour based on it or on `language`, except
that the extension uses `language` to pick the backend command.

## Command: `run-tests`

```
<backend command> run-tests --run-dir <absolute run dir> [--test <testId>]…
```

- Without `--test`, runs every test of the run. `--test` may be repeated.
- Reads only the run directory. Anything the backend needs for this (e.g. a Java classpath or a .NET assembly path) is
  stored there by the full run, in files of the backend's own choosing that are not part of this contract.
- Updates `results.json`: results of the tests that ran are replaced, results of other tests are kept. A failure of the
  whole execution (e.g. the tests don't compile) is written as the top-level
  `error`, which the next successful execution removes.
- **Exit code** `0` when the tests were executed, whatever their results. Non-zero when the backend itself failed (bad
  arguments, missing run directory, compile failure); `results.json` then has `error` if it could be written.
- Progress and errors go to stdout / stderr as plain text for the user; the extension doesn't parse them.
- Must not need the instrumentation used for recording, so it can run while another analysis is running.

The extension starts the command with an argument array (no shell), waits for it to exit, then re-reads `results.json`.

## Versioning

- Every file has `"schemaVersion": 1`.
- Within version 1, changes are **additive** only: new optional keys, new files. Existing keys keep their meaning, and
  required keys stay required. Readers ignore keys they don't know, so the schemas don't forbid extra keys.
- Anything else (removing or renaming a key, changing a meaning, a new value `kind`) raises `schemaVersion`. The extension
  refuses runs with a newer version than it supports and says so.

## Validating a backend's output

The schemas use JSON Schema draft 2020-12, so any validator that supports it works, e.g.
[Ajv](https://ajv.js.org/) (JavaScript), [networknt json-schema-validator](https://github.com/networknt/json-schema-validator)
(Java) or [JsonSchema.Net](https://docs.json-everything.net/schema/basics/) (.NET).

Each schema's `$id` is a URN, `urn:auto-debugger:contract:v1:<file>` (e.g. `urn:auto-debugger:contract:v1:trace`). It
names the schema and can't be downloaded, so register all five files with the validator before validating:
`trace.schema.json` refers to `urn:auto-debugger:contract:v1:value`. The extension's unit tests ([`test/contract.test.ts`](../test/contract.test.ts))
validate the examples with Ajv in strict mode.
