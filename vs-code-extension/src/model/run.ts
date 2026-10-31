export interface CapturedIdentifier {
    captureId: number;
    kind: 'argument' | 'field';
    name: string;
    type: string;
}

/** Contents of `run.json`. Optional keys are the ones the contract allows to be missing. */
export interface RunSummary {
    schemaVersion: number;
    language: string;
    target: { signature: string };
    captured: CapturedIdentifier[];
    typeNames?: Record<string, string>;
    traceMode?: string;
    counts?: { calls?: number; tests?: number };
    status: 'ok' | 'failed';
    error?: string;
    createdAt: string;
}

/** The short display name of a type, from the run's `typeNames`, or the type as it is. */
export function displayType(run: RunSummary, type: string): string {
    return run.typeNames?.[type] ?? type;
}
