/** Local date and time as `YYYY-MM-DD HH:mm`, or the text as it is if it isn't a date. */
export function formatDate(text: string): string {
    const date = new Date(text);
    if (Number.isNaN(date.getTime())) {
        return text;
    }
    const pad = (n: number) => String(n).padStart(2, '0');
    return (
        `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ` +
        `${pad(date.getHours())}:${pad(date.getMinutes())}`
    );
}
