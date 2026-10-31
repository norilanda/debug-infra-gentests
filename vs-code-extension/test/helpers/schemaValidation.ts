import { readdirSync, readFileSync } from 'node:fs';
import { join } from 'node:path';
import Ajv2020, { type ValidateFunction } from 'ajv/dist/2020';
import addFormats from 'ajv-formats';

export const contractDir = join(__dirname, '..', '..', 'contract');
export const examplesDir = join(contractDir, 'examples');
const schemasDir = join(contractDir, 'schemas');

export const files = ['run', 'trace', 'tests', 'results'] as const;
export type ContractFile = (typeof files)[number];

export function readJson(path: string): unknown {
    return JSON.parse(readFileSync(path, 'utf8'));
}

function loadValidators(): Record<ContractFile, ValidateFunction> {
    const ajv = new Ajv2020({ strict: true, allowUnionTypes: true, allErrors: true });
    addFormats(ajv);
    for (const name of readdirSync(schemasDir)) {
        ajv.addSchema(readJson(join(schemasDir, name)) as object);
    }
    return Object.fromEntries(
        files.map((file) => [file, ajv.getSchema(`urn:auto-debugger:contract:v1:${file}`)!]),
    ) as Record<ContractFile, ValidateFunction>;
}

const validators = loadValidators();

/** The schema errors of `data` as JSON, or `''` if it is valid. */
export function errorsOf(file: ContractFile, data: unknown): string {
    const validate = validators[file];
    return validate(data) ? '' : JSON.stringify(validate.errors, null, 2);
}
