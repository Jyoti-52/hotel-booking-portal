import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { ApiService } from './api.service';

describe('ApiService', () => {
  let api: ApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ApiService, provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(ApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('searches hotels using the backend query parameters', () => {
    api.searchHotels(' Goa ', '2030-01-02', '2030-01-04').subscribe((hotels) => {
      expect(hotels).toEqual([]);
    });

    const request = http.expectOne((req) => req.url === '/api/hotels');
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('location')).toBe('Goa');
    expect(request.request.params.get('checkIn')).toBe('2030-01-02');
    expect(request.request.params.get('checkOut')).toBe('2030-01-04');
    request.flush([]);
  });

  it('uses the backend hotel detail and alternatives routes', () => {
    api.getHotel(7, '2030-01-02', '2030-01-04').subscribe();
    const hotelRequest = http.expectOne((req) => req.url === '/api/hotels/7');
    expect(hotelRequest.request.method).toBe('GET');
    expect(hotelRequest.request.params.get('checkIn')).toBe('2030-01-02');
    expect(hotelRequest.request.params.get('checkOut')).toBe('2030-01-04');
    hotelRequest.flush({});

    api.getAlternatives(7, '2030-01-02', '2030-01-04').subscribe((stays) => {
      expect(stays).toEqual([]);
    });
    const alternativesRequest = http.expectOne((req) => req.url === '/api/hotels/7/alternatives');
    expect(alternativesRequest.request.method).toBe('GET');
    expect(alternativesRequest.request.params.get('checkIn')).toBe('2030-01-02');
    expect(alternativesRequest.request.params.get('checkOut')).toBe('2030-01-04');
    alternativesRequest.flush([]);
  });

  it('uses the backend booking, lookup, and cancellation routes', () => {
    const bookingRequest = {
      hotelId: 7,
      checkIn: '2030-01-02',
      checkOut: '2030-01-04',
      guestName: 'Asha Rao',
      guestEmail: 'asha@example.com',
    };

    api.createBooking(bookingRequest).subscribe();
    const createRequest = http.expectOne('/api/bookings');
    expect(createRequest.request.method).toBe('POST');
    expect(createRequest.request.body).toEqual(bookingRequest);
    createRequest.flush({});

    api.lookupBooking('HB-ABCDEFGH', 'asha@example.com').subscribe();
    const lookupRequest = http.expectOne('/api/bookings/lookup');
    expect(lookupRequest.request.method).toBe('POST');
    expect(lookupRequest.request.body).toEqual({ reference: 'HB-ABCDEFGH', email: 'asha@example.com' });
    lookupRequest.flush({});

    api.cancelBooking('HB-ABCDEFGH', 'asha@example.com').subscribe();
    const cancelRequest = http.expectOne('/api/bookings/cancel');
    expect(cancelRequest.request.method).toBe('POST');
    expect(cancelRequest.request.body).toEqual({ reference: 'HB-ABCDEFGH', email: 'asha@example.com' });
    cancelRequest.flush({});
  });
});
