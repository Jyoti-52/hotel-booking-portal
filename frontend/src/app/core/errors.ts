import { HttpErrorResponse } from '@angular/common/http';

import { ApiError } from './models';

export interface FriendlyError {
  message: string;
  code?: string;
  fields: Record<string, string>;
}

/** Converts any HTTP failure into a message that is safe to show to the user. */
export function toFriendlyError(err: unknown): FriendlyError {
  if (err instanceof HttpErrorResponse) {
    if (err.status === 0) {
      return {
        message: 'Cannot reach the server. Please check that the backend is running and try again.',
        fields: {},
      };
    }
    const body = err.error as Partial<ApiError> | null;
    if (body && typeof body === 'object' && body.message) {
      const fields: Record<string, string> = {};
      for (const f of body.fieldErrors ?? []) {
        fields[f.field] = f.message;
      }
      return { message: body.message, code: body.code, fields };
    }
  }
  return { message: 'Something went wrong. Please try again.', fields: {} };
}
