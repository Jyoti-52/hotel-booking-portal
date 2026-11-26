import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { AlternativeStay, Booking, CreateBookingRequest, Hotel } from './models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api';

  searchHotels(location: string, checkIn: string, checkOut: string): Observable<Hotel[]> {
    let params = new HttpParams();
    if (location.trim()) params = params.set('location', location.trim());
    if (checkIn) params = params.set('checkIn', checkIn);
    if (checkOut) params = params.set('checkOut', checkOut);
    return this.http.get<Hotel[]>(`${this.base}/hotels`, { params });
  }

  getHotel(id: number, checkIn: string, checkOut: string): Observable<Hotel> {
    let params = new HttpParams();
    if (checkIn) params = params.set('checkIn', checkIn);
    if (checkOut) params = params.set('checkOut', checkOut);
    return this.http.get<Hotel>(`${this.base}/hotels/${id}`, { params });
  }

  getAlternatives(id: number, checkIn: string, checkOut: string): Observable<AlternativeStay[]> {
    const params = new HttpParams().set('checkIn', checkIn).set('checkOut', checkOut);
    return this.http.get<AlternativeStay[]>(`${this.base}/hotels/${id}/alternatives`, { params });
  }

  createBooking(request: CreateBookingRequest): Observable<Booking> {
    return this.http.post<Booking>(`${this.base}/bookings`, request);
  }

  lookupBooking(reference: string, email: string): Observable<Booking> {
    return this.http.post<Booking>(`${this.base}/bookings/lookup`, { reference, email });
  }

  cancelBooking(reference: string, email: string): Observable<Booking> {
    return this.http.post<Booking>(`${this.base}/bookings/cancel`, { reference, email });
  }
}
