import type { Dirent } from 'node:fs';
import { access, readdir } from 'node:fs/promises';
import { basename, join } from 'node:path';
import { readRunJson, type RunLoad } from './runJsonLoader';
import { contractFileNames } from '../model/constants';

/** Which of the other contract files a run directory contains. */
export interface RunFiles {
    trace: boolean;
    tests: boolean;
    results: boolean;
}

export type RunNode =
    | { kind: 'folder'; name: string; path: string; children: RunNode[] }
    | { kind: 'run'; name: string; path: string; load: RunLoad; files: RunFiles };

/** How many folder levels below the root are searched for runs. */
export const DEFAULT_MAX_DEPTH = 5;

/**
 * Finds the run directories (those with a `run.json`) below `root`.
 */
export async function findRuns(
    root: string,
    maxDepth = DEFAULT_MAX_DEPTH,
    signal?: AbortSignal,
): Promise<RunNode[]> {
    if (await fileExists(join(root, contractFileNames.run))) {
        return [await loadRun(root)];
    }
    return scan(root, maxDepth, signal);
}

async function scan(dir: string, depthLeft: number, signal?: AbortSignal): Promise<RunNode[]> {
    if (depthLeft <= 0 || signal?.aborted) {
        return [];
    }
    let names: string[];
    try {
        names = (await readdir(dir, { withFileTypes: true }))
            .filter(isSearchedFolder)
            .map((e) => e.name);
    } catch {
        return [];
    }

    const nodes = await Promise.all(
        names.map(async (name): Promise<RunNode | undefined> => {
            const path = join(dir, name);
            if (await fileExists(join(path, contractFileNames.run))) {
                return loadRun(path);
            }
            const children = await scan(path, depthLeft - 1, signal);
            return children.length > 0 ? { kind: 'folder', name, path, children } : undefined;
        }),
    );
    return nodes.filter((n) => n !== undefined).sort(compareNodes);
}

function isSearchedFolder(entry: Dirent): boolean {
    return entry.isDirectory() && !entry.name.startsWith('.') && entry.name !== 'node_modules';
}

async function fileExists(path: string): Promise<boolean> {
    try {
        await access(path);
        return true;
    } catch {
        return false;
    }
}

async function loadRun(path: string): Promise<RunNode> {
    const [load, trace, tests, results] = await Promise.all([
        readRunJson(join(path, contractFileNames.run)),
        fileExists(join(path, contractFileNames.trace)),
        fileExists(join(path, contractFileNames.tests)),
        fileExists(join(path, contractFileNames.results)),
    ]);
    return { kind: 'run', name: basename(path), path, load, files: { trace, tests, results } };
}

function compareNodes(a: RunNode, b: RunNode): number {
    const rank = (n: RunNode) => (n.kind === 'folder' ? 0 : n.load.ok ? 1 : 2);
    if (rank(a) !== rank(b)) {
        return rank(a) - rank(b);
    }
    if (a.kind === 'run' && b.kind === 'run' && a.load.ok && b.load.ok) {
        const byDate = Date.parse(b.load.run.createdAt) - Date.parse(a.load.run.createdAt);
        if (byDate !== 0 && !Number.isNaN(byDate)) {
            return byDate;
        }
    }
    return a.name.localeCompare(b.name);
}
