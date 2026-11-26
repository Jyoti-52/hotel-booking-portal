import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { Hotel } from '../core/models';
import { HotelSearchPage } from './hotel-search';

describe('HotelSearchPage filters', () => {
  let fixture: ComponentFixture<HotelSearchPage>;
  let http: HttpTestingController;

  const hotels: Hotel[] = [
    {
      id: 1,
      name: 'Palm Grove',
      location: 'Goa',
      description: 'A demo beach stay.',
      amenities: ['WiFi', 'Pool'],
      pricePerNight: 6500,
      totalRooms: 6,
      rating: 4.7,
      reviewCount: 284,
    },
    {
      id: 2,
      name: 'Heritage Haveli',
      location: 'Jaipur',
      description: 'A demo city stay.',
      amenities: ['WiFi', 'Breakfast'],
      pricePerNight: 4800,
      totalRooms: 4,
      rating: 4.4,
      reviewCount: 176,
    },
    {
      id: 3,
      name: 'Garden Suites',
      location: 'Bengaluru',
      description: 'A demo garden stay.',
      amenities: ['WiFi', 'Pool'],
      pricePerNight: 5400,
      totalRooms: 5,
      rating: 4.6,
      reviewCount: 213,
    },
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HotelSearchPage],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(HotelSearchPage);
    fixture.detectChanges();
    http.expectOne('/api/hotels').flush(hotels);
  });

  afterEach(() => http.verify());

  it('filters by price, rating, and selected facilities', () => {
    const page = fixture.componentInstance;
    page.minPrice.set(5000);
    page.minRating.set(4.5);
    page.toggleAmenity('Pool');

    expect(page.filteredHotels().map((hotel) => hotel.id)).toEqual([1, 3]);
  });

  it('sorts matching hotels by price in both directions and by rating', () => {
    const page = fixture.componentInstance;

    page.sortBy.set('price-low');
    expect(page.filteredHotels().map((hotel) => hotel.id)).toEqual([2, 3, 1]);

    page.sortBy.set('price-high');
    expect(page.filteredHotels().map((hotel) => hotel.id)).toEqual([1, 3, 2]);

    page.sortBy.set('rating');
    expect(page.filteredHotels().map((hotel) => hotel.id)).toEqual([1, 3, 2]);
  });
});
