import { copyFileSync, mkdirSync, mkdtempSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { afterAll, beforeAll, describe, expect, it } from 'vitest';
import { contractFileNames } from '../src/model/constants';
import { DEFAULT_MAX_DEPTH, findRuns, type RunNode } from '../src/runs/discovery';
import { examplesDir } from './helpers/schemaValidation';

const fixturesDir = join(__dirname, 'fixtures', 'runs');

/** Names in tree order, nested as `folder/child`; broken runs are marked with `!`. */
function flattenToRunPaths(nodes: RunNode[], prefix = ''): string[] {
    return nodes.flatMap((n) =>
        n.kind === 'folder'
            ? flattenToRunPaths(n.children, `${prefix}${n.name}/`)
            : [`${prefix}${n.name}${n.load.ok ? '' : '!'}`],
    );
}

describe('findRuns', () => {
    let root: string;

    /** Creates `dir` under the root with `run.json` copied from a fixture (or the given text). */
    function addRun(dir: string, fixture: string, text?: string): void {
        mkdirSync(join(root, dir), { recursive: true });
        const target = join(root, dir, contractFileNames.run);
        if (text === undefined) {
            copyFileSync(join(fixturesDir, `${fixture}.json`), target);
        } else {
            writeFileSync(target, text);
        }
    }

    beforeAll(() => {
        root = mkdtempSync(join(tmpdir(), 'auto-debugger-runs-'));
        addRun('linecount-naive', 'linecount-naive');
        addRun('calc/failed-ai', 'calc-failed-ai');
        addRun('a/b/c/deep', 'linecount-naive');
        addRun('broken/newer', 'newer-version');
        addRun('broken/not-json', '', 'not json');
        // Same folder, different createdAt.
        addRun('dated/older', 'linecount-naive');
        addRun('dated/newer', 'calc-failed-ai');
        // A run directory isn't searched further.
        addRun('linecount-naive/compiled/inner', 'linecount-naive');
        // Skipped folders and folders without runs.
        addRun('node_modules/pkg', 'linecount-naive');
        addRun('.hidden/run', 'linecount-naive');
        mkdirSync(join(root, 'empty/nested'), { recursive: true });
        writeFileSync(join(root, 'linecount-naive', contractFileNames.trace), '{}');
    });

    afterAll(() => rmSync(root, { recursive: true, force: true }));

    it('mirrors the folders: folders first, then runs newest first, then broken runs', async () => {
        expect(flattenToRunPaths(await findRuns(root))).toEqual([
            'a/b/c/deep',
            'broken/newer!',
            'broken/not-json!',
            'calc/failed-ai',
            'dated/newer',
            'dated/older',
            'linecount-naive',
        ]);
    });

    it('stops at the depth limit', async () => {
        expect(flattenToRunPaths(await findRuns(root, 2))).toEqual([
            'broken/newer!',
            'broken/not-json!',
            'calc/failed-ai',
            'dated/newer',
            'dated/older',
            'linecount-naive',
        ]);
    });

    it('records which other files a run has', async () => {
        const run = (await findRuns(root)).find((n) => n.name === 'linecount-naive');
        expect(run?.kind === 'run' && run.files).toEqual({
            trace: true,
            tests: false,
            results: false,
        });
    });

    it('keeps the reason of a broken run', async () => {
        const broken = (await findRuns(root)).find((n) => n.name === 'broken');
        const newer = broken?.kind === 'folder' ? broken.children[0] : undefined;
        expect(newer?.kind === 'run' && !newer.load.ok && newer.load.reason).toContain(
            'newer backend',
        );
    });

    it('sorts runs in one folder newest first', async () => {
        const dated = (await findRuns(root)).find((n) => n.name === 'dated');
        expect(dated?.kind === 'folder' && flattenToRunPaths(dated.children)).toEqual([
            'newer',
            'older',
        ]);
    });

    it('treats a root that is itself a run as the only run', async () => {
        expect(flattenToRunPaths(await findRuns(join(root, 'linecount-naive')))).toEqual([
            'linecount-naive',
        ]);
    });

    it('reads the contract examples with all their files', async () => {
        const runs = await findRuns(examplesDir);
        expect(flattenToRunPaths(runs)).toEqual(['objects', 'divider']);
        for (const run of runs) {
            expect(run.kind === 'run' && run.files).toEqual({
                trace: true,
                tests: true,
                results: true,
            });
        }
    });

    it('returns nothing for a missing root', async () => {
        expect(await findRuns(join(root, 'does-not-exist'))).toEqual([]);
    });

    it('reads no folders once the scan is aborted', async () => {
        const aborted = AbortSignal.abort();
        expect(await findRuns(root, DEFAULT_MAX_DEPTH, aborted)).toEqual([]);
    });
});
