import { readFile } from 'node:fs/promises';
import type { RunSummary } from '../model/run';

/** The `schemaVersion` this extension reads. */
export const SUPPORTED_SCHEMA_VERSION = 1;

export type RunLoad = { ok: true; run: RunSummary } | { ok: false; reason: string };

export function parseAndValidateRunJson(text: string): RunLoad {
    let data: unknown;
    try {
        data = JSON.parse(text);
    } catch (e) {
        return { ok: false, reason: `run.json is not valid JSON: ${(e as Error).message}` };
    }
    if (!isObject(data)) {
        return { ok: false, reason: 'run.json is not a JSON object' };
    }

    const version = data.schemaVersion;
    if (version === undefined) {
        return { ok: false, reason: 'run.json has no schemaVersion' };
    }
    if (!Number.isInteger(version)) {
        return { ok: false, reason: 'schemaVersion in run.json is not an integer' };
    }
    if ((version as number) > SUPPORTED_SCHEMA_VERSION) {
        return {
            ok: false,
            reason: `This run was written by a newer backend (schemaVersion ${version}); update the extension`,
        };
    }
    if (version !== SUPPORTED_SCHEMA_VERSION) {
        return { ok: false, reason: `Unsupported schemaVersion ${version} in run.json` };
    }

    const checks: [string, boolean][] = [
        ['language', isNonEmptyString(data.language)],
        ['target', isObject(data.target) && isNonEmptyString(data.target.signature)],
        ['captured', Array.isArray(data.captured) && data.captured.every(isCapturedIdentifier)],
        ['status', data.status === 'ok' || data.status === 'failed'],
        ['createdAt', isNonEmptyString(data.createdAt)],
    ];
    for (const [key, valid] of checks) {
        if (!(key in data)) {
            return { ok: false, reason: `run.json is missing the required key "${key}"` };
        }
        if (!valid) {
            return { ok: false, reason: `"${key}" in run.json has the wrong shape` };
        }
    }
    return { ok: true, run: data as unknown as RunSummary };
}

export async function readRunJson(path: string): Promise<RunLoad> {
    let text: string;
    try {
        text = await readFile(path, 'utf8');
    } catch (e) {
        return { ok: false, reason: `Cannot read run.json: ${(e as Error).message}` };
    }
    return parseAndValidateRunJson(text);
}

function isObject(value: unknown): value is Record<string, unknown> {
    return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function isNonEmptyString(value: unknown): value is string {
    return typeof value === 'string' && value.length > 0;
}

function isCapturedIdentifier(value: unknown): boolean {
    return (
        isObject(value) &&
        Number.isInteger(value.captureId) &&
        (value.kind === 'argument' || value.kind === 'field') &&
        isNonEmptyString(value.name) &&
        isNonEmptyString(value.type)
    );
}
