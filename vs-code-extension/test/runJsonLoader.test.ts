import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';
import { contractFileNames } from '../src/model/constants';
import { displayType } from '../src/model/run';
import { parseAndValidateRunJson, type RunLoad } from '../src/runs/runJsonLoader';
import { errorsOf, examplesDir, readJson } from './helpers/schemaValidation';

const fixturesDir = join(__dirname, 'fixtures', 'runs');

const parseFile = (path: string): RunLoad => parseAndValidateRunJson(readFileSync(path, 'utf8'));
const reasonOf = (load: RunLoad) => (load.ok ? '' : load.reason);

describe('run.json fixtures', () => {
    // Fixtures from real backend runs must match the contract; broken ones must not.
    it.each(['linecount-naive', 'calc-failed-ai'])('%s matches the schema', (name) => {
        expect(errorsOf('run', readJson(join(fixturesDir, `${name}.json`)))).toBe('');
    });

    it.each(['newer-version', 'missing-target'])('%s does not match the schema', (name) => {
        expect(errorsOf('run', readJson(join(fixturesDir, `${name}.json`)))).not.toBe('');
    });
});

describe('parseAndValidateRunJson', () => {
    it.each(['divider', 'objects'])('reads the contract example %s', (example) => {
        const load = parseFile(join(examplesDir, example, contractFileNames.run));
        expect(load.ok).toBe(true);
    });

    it('reads a successful run', () => {
        const load = parseFile(join(fixturesDir, 'linecount-naive.json'));
        expect(load).toMatchObject({
            ok: true,
            run: {
                language: 'java',
                target: { signature: 'com.example.LineCounter.lineCount(java.lang.String,int)' },
                traceMode: 'naive',
                status: 'ok',
            },
        });
        expect(load.ok && load.run.captured.map((c) => c.name)).toEqual(['arg0', 'arg1']);
    });

    it('reads a failed run with its error', () => {
        const load = parseFile(join(fixturesDir, 'calc-failed-ai.json'));
        expect(load.ok && load.run.status).toBe('failed');
        expect(load.ok && load.run.error).toContain('authentication_error');
    });

    it('rejects a newer schemaVersion with a hint to update', () => {
        const load = parseFile(join(fixturesDir, 'newer-version.json'));
        expect(reasonOf(load)).toBe(
            'This run was written by a newer backend (schemaVersion 2); update the extension',
        );
    });

    it('names a missing required key', () => {
        const load = parseFile(join(fixturesDir, 'missing-target.json'));
        expect(reasonOf(load)).toBe('run.json is missing the required key "target"');
    });

    it.each([
        ['invalid JSON', '{"schemaVersion": 1,', /^run\.json is not valid JSON/],
        ['a non-object', '[1, 2]', /^run\.json is not a JSON object$/],
        ['no schemaVersion', '{}', /^run\.json has no schemaVersion$/],
        ['an older schemaVersion', '{"schemaVersion": 0}', /^Unsupported schemaVersion 0/],
    ])('rejects %s', (_name, text, reason) => {
        expect(reasonOf(parseAndValidateRunJson(text))).toMatch(reason);
    });

    it('rejects a key with the wrong shape', () => {
        const run = readJson(join(fixturesDir, 'linecount-naive.json')) as Record<string, unknown>;
        const load = parseAndValidateRunJson(JSON.stringify({ ...run, status: 'done' }));
        expect(reasonOf(load)).toBe('"status" in run.json has the wrong shape');
    });
});

describe('displayType', () => {
    it('uses typeNames and falls back to the full name', () => {
        const load = parseFile(join(examplesDir, 'objects', contractFileNames.run));
        if (!load.ok) throw new Error(load.reason);
        expect(displayType(load.run, 'com.example.Node')).toBe('Node');
        expect(displayType(load.run, 'int')).toBe('int');
    });
});
