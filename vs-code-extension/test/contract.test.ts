import { readdirSync } from 'node:fs';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';
import {
    errorsOf,
    examplesDir,
    files,
    readJson,
    type ContractFile,
} from './helpers/schemaValidation';

type Example = {
    run: { captured: { captureId: number }[]; counts?: { calls?: number; tests?: number } };
    trace: { calls: { callIndex: number; entry: object; exit?: object | null }[] };
    tests: { tests: { testId: string; callIndices: number[] }[] };
    results: { results: { testId: string }[] };
};

const examples = readdirSync(examplesDir);

describe.each(examples)('example %s', (example) => {
    const data = Object.fromEntries(
        files.map((file) => [file, readJson(join(examplesDir, example, `${file}.json`))]),
    ) as Example;

    it.each(files)('%s.json matches its schema', (file) => {
        expect(errorsOf(file, data[file])).toBe('');
    });

    // Rules that span files, which JSON Schema can't express.
    it('follows the cross-file rules', () => {
        const captureIds = new Set(data.run.captured.map((c) => String(c.captureId)));
        data.trace.calls.forEach((call, i) => {
            expect(call.callIndex).toBe(i);
            for (const snapshot of [call.entry, call.exit ?? {}]) {
                expect(Object.keys(snapshot).filter((id) => !captureIds.has(id))).toEqual([]);
            }
        });

        const covered = data.tests.tests.flatMap((t) => t.callIndices);
        expect(new Set(covered).size).toBe(covered.length);
        expect(covered.every((i) => i < data.trace.calls.length)).toBe(true);

        const testIds = new Set(data.tests.tests.map((t) => t.testId));
        expect(testIds.size).toBe(data.tests.tests.length);
        expect(data.results.results.every((r) => testIds.has(r.testId))).toBe(true);

        expect(data.run.counts?.calls ?? data.trace.calls.length).toBe(data.trace.calls.length);
        expect(data.run.counts?.tests ?? data.tests.tests.length).toBe(data.tests.tests.length);
    });
});

describe('schemas reject', () => {
    const divider = (file: ContractFile) =>
        structuredClone(readJson(join(examplesDir, 'divider', `${file}.json`))) as Record<
            string,
            unknown
        >;

    it('a newer schemaVersion', () => {
        expect(errorsOf('run', { ...divider('run'), schemaVersion: 2 })).not.toBe('');
    });

    it('an empty display name for a type', () => {
        expect(errorsOf('run', { ...divider('run'), typeNames: { 'System.Int32': '' } })).not.toBe(
            '',
        );
    });

    it('a captured identifier that is neither an argument nor a field', () => {
        const run = divider('run');
        run.captured = [{ captureId: 0, kind: 'local', name: 'x', type: 'int' }];
        expect(errorsOf('run', run)).not.toBe('');
    });

    it('an unknown value kind', () => {
        const trace = {
            schemaVersion: 1,
            calls: [{ callIndex: 0, entry: { '0': { kind: 'blob', type: 'int' } } }],
        };
        expect(errorsOf('trace', trace)).not.toBe('');
    });

    it('an array without elements', () => {
        const trace = {
            schemaVersion: 1,
            calls: [{ callIndex: 0, entry: { '0': { kind: 'array', type: 'int[]', length: 3 } } }],
        };
        expect(errorsOf('trace', trace)).not.toBe('');
    });

    it('a non-numeric captureId key', () => {
        const value = { kind: 'primitive', type: 'int', value: 1 };
        const trace = { schemaVersion: 1, calls: [{ callIndex: 0, entry: { a: value } }] };
        expect(errorsOf('trace', trace)).not.toBe('');
    });

    it('a thrown outcome without the exception', () => {
        const trace = {
            schemaVersion: 1,
            calls: [{ callIndex: 0, entry: {}, outcome: { kind: 'threw' } }],
        };
        expect(errorsOf('trace', trace)).not.toBe('');
    });

    it('a test listing the same call twice', () => {
        const tests = { schemaVersion: 1, tests: [{ testId: 'T#a', callIndices: [1, 1] }] };
        expect(errorsOf('tests', tests)).not.toBe('');
    });

    it('an unknown result status', () => {
        const results = divider('results');
        results.results = [{ testId: 'T#a', status: 'flaky' }];
        expect(errorsOf('results', results)).not.toBe('');
    });
});
