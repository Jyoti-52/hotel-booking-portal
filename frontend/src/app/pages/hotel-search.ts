import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { ApiService } from '../core/api.service';
import { addDays, todayString, validateStay } from '../core/dates';
import { toFriendlyError } from '../core/errors';
import { Hotel } from '../core/models';
import { HotelImageFallbackDirective } from '../core/hotel-image-fallback.directive';

@Component({
  selector: 'app-hotel-search',
  imports: [FormsModule, RouterLink, CurrencyPipe, DatePipe, DecimalPipe, HotelImageFallbackDirective],
  template: `
    <section class="hero">
      <div class="hero-copy">
        <span class="eyebrow">A GOOD STAY STARTS HERE</span>
        <h1>Find a stay that feels like yours</h1>
        <p>Thoughtful places, memorable cities and a smoother way to plan your next trip.</p>
      </div>

      <form class="search-form" (ngSubmit)="submit()" #form="ngForm" novalidate>
        <label class="field">
          <span>Where to?</span>
          <input name="location" type="text" placeholder="e.g. Goa, Jaipur, Mumbai" [(ngModel)]="location" />
        </label>
        <label class="field">
          <span>Check-in</span>
          <input name="checkIn" type="date" [min]="today" [(ngModel)]="checkIn" (ngModelChange)="onCheckInChange()" />
        </label>
        <label class="field">
          <span>Check-out</span>
          <input name="checkOut" type="date" [min]="minCheckOut()" [(ngModel)]="checkOut" />
        </label>
        <button class="btn primary" type="submit">Search</button>
      </form>

      @if (formError()) {
        <p class="alert error" role="alert">{{ formError() }}</p>
      }
    </section>

    <section class="filter-panel" aria-label="Filter and sort hotels">
      <div class="filter-heading">
        <div>
          <span class="eyebrow">MAKE IT YOURS</span>
          <h2>Refine your stay</h2>
        </div>
        <button class="text-button" type="button" (click)="clearFilters()">Clear filters</button>
      </div>

      <div class="filter-controls">
        <label class="field">
          <span>Minimum price / night</span>
          <input
            name="minPrice"
            type="number"
            min="0"
            step="500"
            placeholder="No minimum"
            [ngModel]="minPrice()"
            (ngModelChange)="minPrice.set($event === '' ? null : $event)"
          />
        </label>
        <label class="field">
          <span>Maximum price / night</span>
          <input
            name="maxPrice"
            type="number"
            min="0"
            step="500"
            placeholder="No maximum"
            [ngModel]="maxPrice()"
            (ngModelChange)="maxPrice.set($event === '' ? null : $event)"
          />
        </label>
        <label class="field">
          <span>Guest rating</span>
          <select name="minRating" [ngModel]="minRating()" (ngModelChange)="minRating.set($event)">
            <option [ngValue]="null">Any rating</option>
            <option [ngValue]="4">4.0 and above</option>
            <option [ngValue]="4.5">4.5 and above</option>
            <option [ngValue]="4.7">4.7 and above</option>
          </select>
        </label>
        <label class="field">
          <span>Sort by</span>
          <select name="sortBy" [ngModel]="sortBy()" (ngModelChange)="sortBy.set($event)">
            <option value="recommended">Recommended</option>
            <option value="price-low">Price: low to high</option>
            <option value="price-high">Price: high to low</option>
            <option value="rating">Guest rating</option>
          </select>
        </label>
      </div>

      @if (availableAmenities().length > 0) {
        <div class="amenity-filter">
          <span class="filter-label">Popular facilities</span>
          <div class="amenity-options">
            @for (amenity of availableAmenities(); track amenity) {
              <button
                class="amenity-option"
                [class.selected]="selectedAmenities().includes(amenity)"
                [attr.aria-pressed]="selectedAmenities().includes(amenity)"
                type="button"
                (click)="toggleAmenity(amenity)"
              >
                {{ amenity }}
              </button>
            }
          </div>
        </div>
      }
    </section>

    <section aria-live="polite">
      @if (loading()) {
        <p class="loading-state"><span class="spinner" aria-hidden="true"></span> Finding lovely places to stay…</p>
      } @else if (loadError()) {
        <p class="alert error" role="alert">{{ loadError() }}</p>
      } @else {
        <div class="results-head results-toolbar">
          <div>
            <span class="eyebrow">CURATED FOR YOUR NEXT TRIP</span>
            <h2>
              @if (hasDates()) {
                Stays from {{ appliedCheckIn | date: 'd MMM' }} to {{ appliedCheckOut | date: 'd MMM y' }}
              } @else if (location.trim()) {
                Stays in {{ location }}
              } @else {
                Explore our stays
              }
            </h2>
          </div>
          <span class="result-count">
            <strong>{{ filteredHotels().length }}</strong>
            {{ filteredHotels().length === 1 ? 'stay' : 'stays' }}
            @if (filteredHotels().length !== hotels().length) {
              <span class="muted"> of {{ hotels().length }}</span>
            }
          </span>
        </div>

        @if (hotels().length === 0) {
          <div class="card empty">
            <span class="empty-icon" aria-hidden="true">⌕</span>
            <h3>No stays found just yet</h3>
            <p class="muted">Try another destination or clear your search to explore all of Stayfinder.</p>
            <button class="btn" type="button" (click)="clearSearch()">Explore all stays</button>
          </div>
        } @else if (filteredHotels().length === 0) {
          <div class="card empty">
            <span class="empty-icon" aria-hidden="true">⌕</span>
            <h3>No stays match these filters</h3>
            <p class="muted">Try a wider price range, a lower rating, or fewer selected facilities.</p>
            <button class="btn" type="button" (click)="clearFilters()">Clear filters</button>
          </div>
        } @else {
          <div class="grid">
            @for (h of filteredHotels(); track h.id) {
              <article class="card hotel-card" [class.soldout]="h.available === false">
                <a
                  class="hotel-media"
                  [routerLink]="['/hotels', h.id]"
                  [queryParams]="detailParams()"
                  [attr.aria-label]="'View ' + h.name"
                >
                  <img
                    [src]="h.imageUrl || '/hotel-placeholder.svg'"
                    [alt]="h.name + ' property'"
                    hotelImageFallback
                    loading="lazy"
                  />
                  @if (h.available === false) {
                    <span class="image-badge sold-out-badge">Sold out for these dates</span>
                  } @else {
                    <span class="image-badge">Stayfinder pick</span>
                  }
                </a>

                <div class="hotel-card-content">
                  <div class="hotel-top">
                    <div>
                      <p class="location">⌖ {{ h.location }}</p>
                      <h3>{{ h.name }}</h3>
                    </div>
                    @if (h.available === false) {
                      <span class="badge danger">Sold out</span>
                    } @else if (h.availableRooms !== undefined && h.availableRooms <= 2) {
                      <span class="badge warn">Only {{ h.availableRooms }} left</span>
                    } @else if (h.availableRooms !== undefined) {
                      <span class="badge ok">Available</span>
                    }
                  </div>

                  @if (h.rating != null && h.reviewCount != null) {
                    <div class="rating-line">
                      <span class="rating-score">{{ h.rating | number: '1.1-1' }}</span>
                      <span>
                        <strong>{{ ratingLabel(h.rating) }}</strong>
                        <small>Demo rating · {{ h.reviewCount }} sample reviews</small>
                      </span>
                    </div>
                  } @else {
                    <p class="muted small listing-note">New listing · no demo rating yet</p>
                  }

                  <p class="desc">{{ h.description }}</p>

                  <ul class="chips">
                    @for (a of h.amenities.slice(0, 4); track a) {
                      <li>{{ a }}</li>
                    }
                    @if (h.amenities.length > 4) {
                      <li>+{{ h.amenities.length - 4 }} more</li>
                    }
                  </ul>

                  <div class="hotel-bottom">
                    <div class="price">
                      <strong>{{ h.pricePerNight | currency: 'INR' : 'symbol' : '1.0-0' }}</strong>
                      <span class="muted"> / night</span>
                      @if (h.totalAmount !== undefined) {
                        <div class="muted small">
                          {{ h.totalAmount | currency: 'INR' : 'symbol' : '1.0-0' }} for {{ h.nights }}
                          {{ h.nights === 1 ? 'night' : 'nights' }}
                        </div>
                      }
                    </div>
                    <a
                      class="btn"
                      [class.primary]="h.available !== false"
                      [routerLink]="['/hotels', h.id]"
                      [queryParams]="detailParams()"
                    >
                      {{ h.available === false ? 'See nearby dates' : 'View stay' }}
                    </a>
                  </div>
                </div>
              </article>
            }
          </div>
        }
      }
    </section>
  `,
})
export class HotelSearchPage {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly today = todayString();

  location = '';
  checkIn = '';
  checkOut = '';

  // the values that were actually searched (the form can change without searching)
  appliedCheckIn = '';
  appliedCheckOut = '';

  readonly hotels = signal<Hotel[]>([]);
  readonly loading = signal(false);
  readonly loadError = signal('');
  readonly formError = signal('');
  readonly minPrice = signal<number | null>(null);
  readonly maxPrice = signal<number | null>(null);
  readonly minRating = signal<number | null>(null);
  readonly sortBy = signal<'recommended' | 'price-low' | 'price-high' | 'rating'>('recommended');
  readonly selectedAmenities = signal<string[]>([]);
  readonly availableAmenities = computed(() =>
    [...new Set(this.hotels().flatMap((hotel) => hotel.amenities))].sort((a, b) => a.localeCompare(b)),
  );
  readonly filteredHotels = computed(() => {
    const minPrice = this.minPrice();
    const maxPrice = this.maxPrice();
    const minRating = this.minRating();
    const amenities = this.selectedAmenities();
    const result = this.hotels().filter(
      (hotel) =>
        (minPrice === null || hotel.pricePerNight >= minPrice) &&
        (maxPrice === null || hotel.pricePerNight <= maxPrice) &&
        (minRating === null || (hotel.rating != null && hotel.rating >= minRating)) &&
        amenities.every((amenity) => hotel.amenities.includes(amenity)),
    );

    switch (this.sortBy()) {
      case 'price-low':
        return result.sort((a, b) => a.pricePerNight - b.pricePerNight);
      case 'price-high':
        return result.sort((a, b) => b.pricePerNight - a.pricePerNight);
      case 'rating':
        return result.sort((a, b) => (b.rating ?? -1) - (a.rating ?? -1));
      default:
        return result;
    }
  });

  constructor() {
    // The URL is the single source of truth, so results survive a refresh and the back button.
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      this.location = params.get('location') ?? '';
      this.checkIn = params.get('checkIn') ?? '';
      this.checkOut = params.get('checkOut') ?? '';
      this.appliedCheckIn = this.checkIn;
      this.appliedCheckOut = this.checkOut;
      this.load();
    });
  }

  hasDates(): boolean {
    return !!this.appliedCheckIn && !!this.appliedCheckOut;
  }

  detailParams() {
    return this.hasDates() ? { checkIn: this.appliedCheckIn, checkOut: this.appliedCheckOut } : {};
  }

  minCheckOut(): string {
    return this.checkIn ? addDays(this.checkIn, 1) : addDays(this.today, 1);
  }

  onCheckInChange(): void {
    if (this.checkIn && this.checkOut && this.checkOut <= this.checkIn) {
      this.checkOut = '';
    }
  }

  ratingLabel(rating: number): string {
    if (rating >= 4.7) return 'Exceptional';
    if (rating >= 4.5) return 'Excellent';
    if (rating >= 4) return 'Very good';
    return 'Good';
  }

  toggleAmenity(amenity: string): void {
    this.selectedAmenities.update((selected) =>
      selected.includes(amenity) ? selected.filter((item) => item !== amenity) : [...selected, amenity],
    );
  }

  clearFilters(): void {
    this.minPrice.set(null);
    this.maxPrice.set(null);
    this.minRating.set(null);
    this.sortBy.set('recommended');
    this.selectedAmenities.set([]);
  }

  clearSearch(): void {
    this.clearFilters();
    this.location = '';
    this.checkIn = '';
    this.checkOut = '';
    this.submit();
  }

  submit(): void {
    this.formError.set('');
    const anyDate = !!this.checkIn || !!this.checkOut;
    if (anyDate) {
      const problem = validateStay(this.checkIn, this.checkOut);
      if (problem) {
        this.formError.set(problem);
        return;
      }
    }
    this.router.navigate([], {
      queryParams: {
        location: this.location.trim() || null,
        checkIn: this.checkIn || null,
        checkOut: this.checkOut || null,
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.loadError.set('');
    this.api.searchHotels(this.location, this.checkIn, this.checkOut).subscribe({
      next: (hotels) => {
        this.hotels.set(hotels);
        this.loading.set(false);
      },
      error: (err) => {
        this.hotels.set([]);
        this.loadError.set(toFriendlyError(err).message);
        this.loading.set(false);
      },
    });
  }
}
