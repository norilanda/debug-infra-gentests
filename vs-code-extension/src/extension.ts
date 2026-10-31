import * as vscode from 'vscode';
import { outputRootSetting, saveOutputRoot } from './settings';
import { RunsTreeProvider } from './views/runsTree';

export function activate(context: vscode.ExtensionContext): void {
    const runs = new RunsTreeProvider();
    context.subscriptions.push(
        vscode.window.registerTreeDataProvider('autoDebugger.runs', runs),
        vscode.commands.registerCommand('autoDebugger.refreshRuns', () => runs.refresh()),
        vscode.commands.registerCommand('autoDebugger.setOutputRoot', async () => {
            if (await setOutputRoot()) {
                runs.refresh();
            }
        }),
        vscode.workspace.onDidChangeConfiguration((e) => {
            if (e.affectsConfiguration(outputRootSetting)) {
                runs.refresh();
            }
        }),
    );
}

export function deactivate(): void {}

/** Asks for a folder and stores it as the output root. Returns whether a folder was picked. */
async function setOutputRoot(): Promise<boolean> {
    const picked = await vscode.window.showOpenDialog({
        canSelectFiles: false,
        canSelectFolders: true,
        canSelectMany: false,
        openLabel: 'Use as Output Root',
    });
    if (!picked?.[0]) {
        return false;
    }
    await saveOutputRoot(picked[0].fsPath);
    return true;
}
