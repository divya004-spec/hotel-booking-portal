import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, throwError } from 'rxjs';

import { AppError, Booking, BookingRequest, DateOption, Hotel } from './models';

/** Turns any HTTP failure into one friendly, predictable shape. */
function toAppError(error: HttpErrorResponse): Observable<never> {
  let appError: AppError;

  if (error.status === 0) {
    appError = {
      status: 0,
      code: 'NETWORK',
      message: 'Cannot reach the server. Please check that the backend is running and try again.',
      fieldErrors: [],
    };
  } else if (error.error && typeof error.error === 'object' && error.error.message) {
    appError = {
      status: error.status,
      code: error.error.code ?? 'ERROR',
      message: error.error.message,
      fieldErrors: error.error.fieldErrors ?? [],
    };
  } else {
    appError = {
      status: error.status,
      code: 'ERROR',
      message: 'Something went wrong. Please try again.',
      fieldErrors: [],
    };
  }
  return throwError(() => appError);
}

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api';

  hotels(location: string, checkIn: string, checkOut: string): Observable<Hotel[]> {
    let params = this.dateParams(checkIn, checkOut);
    if (location.trim()) {
      params = params.set('location', location.trim());
    }
    return this.http.get<Hotel[]>(`${this.base}/hotels`, { params }).pipe(catchError(toAppError));
  }

  locations(): Observable<string[]> {
    return this.http.get<string[]>(`${this.base}/hotels/locations`).pipe(catchError(toAppError));
  }

  hotel(id: number, checkIn: string, checkOut: string): Observable<Hotel> {
    const params = this.dateParams(checkIn, checkOut);
    return this.http.get<Hotel>(`${this.base}/hotels/${id}`, { params }).pipe(catchError(toAppError));
  }

  alternatives(id: number, checkIn: string, checkOut: string): Observable<DateOption[]> {
    const params = this.dateParams(checkIn, checkOut);
    return this.http
      .get<DateOption[]>(`${this.base}/hotels/${id}/alternatives`, { params })
      .pipe(catchError(toAppError));
  }

  createBooking(request: BookingRequest): Observable<Booking> {
    return this.http.post<Booking>(`${this.base}/bookings`, request).pipe(catchError(toAppError));
  }

  getBooking(reference: string, email: string): Observable<Booking> {
    const params = new HttpParams().set('email', email.trim());
    return this.http
      .get<Booking>(`${this.base}/bookings/${encodeURIComponent(reference.trim())}`, { params })
      .pipe(catchError(toAppError));
  }

  cancelBooking(reference: string, email: string): Observable<Booking> {
    return this.http
      .post<Booking>(`${this.base}/bookings/${encodeURIComponent(reference.trim())}/cancel`, {
        email: email.trim(),
      })
      .pipe(catchError(toAppError));
  }

  /** Dates are only sent when both are present. */
  private dateParams(checkIn: string, checkOut: string): HttpParams {
    let params = new HttpParams();
    if (checkIn && checkOut) {
      params = params.set('checkIn', checkIn).set('checkOut', checkOut);
    }
    return params;
  }
}
