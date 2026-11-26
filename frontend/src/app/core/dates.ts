/** Date helpers. Dates travel as plain yyyy-MM-dd strings, same as the API. */

export function todayString(): string {
  const d = new Date();
  return formatLocal(d);
}

export function addDays(date: string, days: number): string {
  const [y, m, d] = date.split('-').map(Number);
  return formatLocal(new Date(y, m - 1, d + days));
}

function formatLocal(d: Date): string {
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${mm}-${dd}`;
}

/** Client-side check for fast feedback. The backend remains the source of truth. */
export function validateStay(checkIn: string, checkOut: string): string | null {
  if (!checkIn || !checkOut) return 'Please select both a check-in and a check-out date.';
  if (checkIn < todayString()) return 'Check-in date cannot be in the past.';
  if (checkOut <= checkIn) return 'Check-out date must be after the check-in date.';
  return null;
}
