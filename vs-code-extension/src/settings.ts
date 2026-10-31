import { homedir } from 'node:os';
import { isAbsolute, join } from 'node:path';
import * as vscode from 'vscode';

const section = 'autoDebugger';

export const outputRootSetting = `${section}.outputRoot`;

/** `autoDebugger.outputRoot`, with `~` expanded and a relative path resolved against the first workspace folder. */
export function configuredOutputRoot(): string | undefined {
    const value = vscode.workspace.getConfiguration(section).get<string>('outputRoot', '').trim();
    if (!value) {
        return undefined;
    }
    const isInHome = value === '~' || value.startsWith('~/') || value.startsWith('~\\');
    const path = isInHome ? join(homedir(), value.slice(1)) : value;
    const workspace = vscode.workspace.workspaceFolders?.[0];
    return isAbsolute(path) || !workspace ? path : join(workspace.uri.fsPath, path);
}

/** Stores `path` as `autoDebugger.outputRoot`: in the workspace if a folder is open, else in the user settings. */
export async function saveOutputRoot(path: string): Promise<void> {
    const target = vscode.workspace.workspaceFolders
        ? vscode.ConfigurationTarget.Workspace
        : vscode.ConfigurationTarget.Global;
    await vscode.workspace.getConfiguration(section).update('outputRoot', path, target);
}
