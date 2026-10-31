import { describe, expect, it } from 'vitest';
import { formatDate } from '../src/helpers';

describe('formatDate', () => {
    it('formats a date in local time', () => {
        expect(formatDate('2026-03-05T07:04:09')).toBe('2026-03-05 07:04');
    });

    it('returns text that is not a date as it is', () => {
        expect(formatDate('yesterday')).toBe('yesterday');
    });
});
