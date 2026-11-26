/** Dates travel as plain "YYYY-MM-DD" strings, exactly like the API expects. */

export const MAX_NIGHTS = 30;

export function toIso(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function parseIso(iso: string): Date {
  const [year, month, day] = iso.split('-').map(Number);
  return new Date(year, month - 1, day);
}

/** Today in the visitor's own time zone (not UTC). */
export function todayIso(): string {
  return toIso(new Date());
}

export function addDaysIso(iso: string, days: number): string {
  const date = parseIso(iso);
  date.setDate(date.getDate() + days);
  return toIso(date);
}

export function nightsBetween(checkIn: string, checkOut: string): number {
  if (!checkIn || !checkOut) {
    return 0;
  }
  const millisPerDay = 24 * 60 * 60 * 1000;
  return Math.round((parseIso(checkOut).getTime() - parseIso(checkIn).getTime()) / millisPerDay);
}

/**
 * Friendly checks that run in the browser before calling the API.
 * The backend repeats every rule, because the browser can never be trusted.
 * Returns an error message, or null when the dates are fine (or both empty).
 */
export function validateDates(checkIn: string, checkOut: string): string | null {
  if (!checkIn && !checkOut) {
    return null;
  }
  if (!checkIn || !checkOut) {
    return 'Please choose both a check-in and a check-out date.';
  }
  if (checkIn < todayIso()) {
    return 'Check-in date cannot be in the past.';
  }
  if (checkOut <= checkIn) {
    return 'Check-out date must be after the check-in date.';
  }
  if (nightsBetween(checkIn, checkOut) > MAX_NIGHTS) {
    return `You can book at most ${MAX_NIGHTS} nights at a time.`;
  }
  return null;
}

/** A stable colour for each hotel's banner. */
export function hueFor(id: number): number {
  return (id * 47) % 360;
}

export function plural(count: number, word: string): string {
  return `${count} ${word}${count === 1 ? '' : 's'}`;
}
