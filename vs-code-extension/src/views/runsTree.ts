import { stat } from 'node:fs/promises';
import * as vscode from 'vscode';
import { formatDate } from '../helpers';
import { contractFileNames } from '../model/constants';
import { DEFAULT_MAX_DEPTH, findRuns, type RunFiles, type RunNode } from '../runs/discovery';
import { displayType, type CapturedIdentifier, type RunSummary } from '../model/run';
import { configuredOutputRoot } from '../settings';
import { icons } from './icons';

/** A row inside an expanded run: one line of its summary. */
interface DetailNode {
    kind: 'detail';
    label: string;
    description?: string;
    tooltip?: string;
    icon?: vscode.ThemeIcon;
    children?: DetailNode[];
}

type Node = RunNode | DetailNode;

type ViewState = 'noRoot' | 'missingRoot' | 'empty' | 'runs';

/** Lists runs from the configured output root. */
export class RunsTreeProvider implements vscode.TreeDataProvider<Node> {
    private readonly changed = new vscode.EventEmitter<void>();
    readonly onDidChangeTreeData = this.changed.event;

    /** Items directly under the output root. Loaded on first use, cleared by refresh(). */
    private topLevelRunsAndFolders: Promise<RunNode[]> | undefined;

    /** Stops the scan that `topLevelRunsAndFolders` is waiting for. */
    private scanAbort: AbortController | undefined;

    refresh(): void {
        this.scanAbort?.abort();
        this.scanAbort = undefined;
        this.topLevelRunsAndFolders = undefined;
        this.changed.fire();
    }

    getTreeItem(node: Node): vscode.TreeItem {
        switch (node.kind) {
            case 'folder':
                return folderItem(node);
            case 'run':
                return runItem(node);
            case 'detail':
                return detailItem(node);
        }
    }

    getChildren(node?: Node): Node[] | Promise<Node[]> {
        if (!node) {
            if (!this.topLevelRunsAndFolders) {
                this.scanAbort = new AbortController();
                this.topLevelRunsAndFolders = this.loadTopLevelRunsAndFolders(
                    this.scanAbort.signal,
                );
            }
            return this.topLevelRunsAndFolders;
        }

        switch (node.kind) {
            case 'folder':
                return node.children;
            case 'run':
                return node.load.ok ? runDetails(node.load.run, node.files) : [];
            case 'detail':
                return node.children ?? [];
        }
    }

    private async loadTopLevelRunsAndFolders(signal: AbortSignal): Promise<RunNode[]> {
        const root = configuredOutputRoot();
        let state: ViewState;
        let runs: RunNode[] = [];

        if (!root) {
            state = 'noRoot';
        } else if (!(await isDirectory(root))) {
            state = 'missingRoot';
        } else {
            runs = await findRuns(root, DEFAULT_MAX_DEPTH, signal);
            state = runs.length > 0 ? 'runs' : 'empty';
        }

        if (!signal.aborted) {
            void vscode.commands.executeCommand('setContext', 'autoDebugger.runsView', state);
        }
        return runs;
    }
}

async function isDirectory(path: string): Promise<boolean> {
    try {
        return (await stat(path)).isDirectory();
    } catch {
        return false;
    }
}

function folderItem(node: Extract<RunNode, { kind: 'folder' }>): vscode.TreeItem {
    const item = new vscode.TreeItem(node.name, vscode.TreeItemCollapsibleState.Expanded);
    item.iconPath = vscode.ThemeIcon.Folder;
    item.tooltip = node.path;
    return item;
}

function runItem(node: Extract<RunNode, { kind: 'run' }>): vscode.TreeItem {
    const item = new vscode.TreeItem(node.name, vscode.TreeItemCollapsibleState.Collapsed);
    item.contextValue = 'run';

    if (!node.load.ok) {
        item.collapsibleState = vscode.TreeItemCollapsibleState.None;
        item.iconPath = icons.broken;
        item.description = node.load.reason;
        item.tooltip = `${node.path}\n\nCannot open this run: ${node.load.reason}`;
        return item;
    }

    const run = node.load.run;
    item.iconPath = run.status === 'ok' ? icons.ok : icons.failed;
    item.description = `${run.target.signature} · ${formatDate(run.createdAt)}`;
    item.tooltip = runTooltip(run, node.path);
    return item;
}

function detailItem(node: DetailNode): vscode.TreeItem {
    const item = new vscode.TreeItem(
        node.label,
        node.children?.length
            ? vscode.TreeItemCollapsibleState.Expanded
            : vscode.TreeItemCollapsibleState.None,
    );
    item.description = node.description;
    item.tooltip = node.tooltip;
    item.iconPath = node.icon;
    return item;
}

function runTooltip(run: RunSummary, path: string): string {
    const lines = [
        `Target: ${run.target.signature}`,
        `Language: ${run.language}`,
        `Calls: ${run.counts?.calls ?? 'unknown'}`,
        `Tests: ${run.counts?.tests ?? 'unknown'}`,
        `Trace mode: ${run.traceMode ?? 'not given'}`,
        `Status: ${run.status}`,
        `Created: ${formatDate(run.createdAt)}`,
        `Directory: ${path}`,
    ];
    if (run.error) {
        lines.push('', `Error: ${run.error}`);
    }
    return lines.join('\n');
}

function runDetails(run: RunSummary, files: RunFiles): DetailNode[] {
    const details: DetailNode[] = [];
    if (run.status === 'failed') {
        details.push({
            kind: 'detail',
            label: 'Failed',
            description: run.error ?? 'no reason given',
            tooltip: run.error,
            icon: icons.failed,
        });
    }
    details.push(
        { kind: 'detail', label: 'Target', description: run.target.signature },
        { kind: 'detail', label: 'Language', description: run.language },
        { kind: 'detail', label: 'Trace mode', description: run.traceMode ?? 'not given' },
        { kind: 'detail', label: 'Created', description: formatDate(run.createdAt) },
    );
    // TODO: change when trace.json exporting is implemented
    if (run.counts?.calls !== undefined) {
        details.push({ kind: 'detail', label: 'Calls', description: String(run.counts.calls) });
    }
    if (run.counts?.tests !== undefined) {
        details.push({ kind: 'detail', label: 'Tests', description: String(run.counts.tests) });
    }

    const captured = (kind: CapturedIdentifier['kind'], label: string) => {
        const items = run.captured.filter((c) => c.kind === kind);
        if (items.length > 0) {
            details.push({
                kind: 'detail',
                label,
                children: items.map((c) => ({
                    kind: 'detail',
                    label: c.name,
                    description: displayType(run, c.type),
                    tooltip: c.type,
                    icon: kind === 'argument' ? icons.argument : icons.field,
                })),
            });
        }
    };
    captured('argument', 'Arguments');
    captured('field', 'Fields');

    const missing: [boolean, string, string][] = [
        [files.trace, contractFileNames.trace, 'The recorded calls cannot be shown.'],
        [files.tests, contractFileNames.tests, 'No generated tests are listed for this run.'],
        [files.results, contractFileNames.results, 'No test results are available for this run.'],
    ];
    for (const [present, file, effect] of missing) {
        if (!present) {
            details.push({
                kind: 'detail',
                label: `No ${file}`,
                description: effect,
                tooltip: `The run directory has no ${file}. ${effect}`,
                icon: icons.missingFile,
            });
        }
    }
    return details;
}
