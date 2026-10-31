import * as vscode from 'vscode';

export const icons = {
    ok: new vscode.ThemeIcon('pass', new vscode.ThemeColor('testing.iconPassed')),
    failed: new vscode.ThemeIcon('error', new vscode.ThemeColor('testing.iconFailed')),
    broken: new vscode.ThemeIcon('warning', new vscode.ThemeColor('list.warningForeground')),
    missingFile: new vscode.ThemeIcon('info'),
    argument: new vscode.ThemeIcon('symbol-parameter'),
    field: new vscode.ThemeIcon('symbol-field'),
};
