import * as vscode from 'vscode';
import { RunsTreeProvider } from './views/runsTree';

export function activate(context: vscode.ExtensionContext): void {
    context.subscriptions.push(
        vscode.window.registerTreeDataProvider('autoDebugger.runs', new RunsTreeProvider()),
    );
}

export function deactivate(): void {}
