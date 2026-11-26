import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { combineLatest } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { ApiService } from '../core/api.service';
import { addDays, todayString, validateStay } from '../core/dates';
import { toFriendlyError } from '../core/errors';
import { AlternativeStay, Hotel } from '../core/models';
import { HotelImageFallbackDirective } from '../core/hotel-image-fallback.directive';

@Component({
  selector: 'app-hotel-detail',
  imports: [FormsModule, RouterLink, CurrencyPipe, DatePipe, DecimalPipe, HotelImageFallbackDirective],
  template: `
    <a routerLink="/" [queryParams]="backParams()" class="back">← Back to results</a>

    @if (loading() && !hotel()) {
      <p class="muted center">Loading hotel…</p>
    } @else if (loadError() && !hotel()) {
      <p class="alert error" role="alert">{{ loadError() }}</p>
    } @else if (hotel(); as h) {
      <div class="detail-layout">
        <section class="card detail-main">
          <div class="detail-photo">
            <img
              [src]="h.imageUrl || '/hotel-placeholder.svg'"
              [alt]="h.name + ' property'"
              hotelImageFallback
            />
            <span class="image-badge">Stayfinder demo collection</span>
          </div>
          <h1>{{ h.name }}</h1>
          <p class="location">📍 {{ h.location }}</p>
          @if (h.rating != null && h.reviewCount != null) {
            <div class="rating-line detail-rating">
              <span class="rating-score">{{ h.rating | number: '1.1-1' }}</span>
              <span>
                <strong>{{ ratingLabel(h.rating) }}</strong>
                <small>Demo rating · {{ h.reviewCount }} sample reviews · not verified real-world reviews</small>
              </span>
            </div>
          } @else {
            <p class="muted small listing-note">New listing · no demo rating yet</p>
          }
          <p class="desc">{{ h.description }}</p>

          <h3>Amenities</h3>
          <ul class="chips">
            @for (a of h.amenities; track a) {
              <li>{{ a }}</li>
            }
          </ul>

          <h3>Room</h3>
          <p>
            Standard room · <strong>{{ h.pricePerNight | currency: 'INR' : 'symbol' : '1.0-0' }}</strong> per night ·
            {{ h.totalRooms }} rooms in total
          </p>
        </section>

        <aside class="card booking-panel">
          <h2>Your stay</h2>

          <form class="dates" (ngSubmit)="applyDates()" novalidate>
            <label class="field">
              <span>Check-in</span>
              <input name="checkIn" type="date" [min]="today" [(ngModel)]="checkIn" (ngModelChange)="onCheckInChange()" />
            </label>
            <label class="field">
              <span>Check-out</span>
              <input name="checkOut" type="date" [min]="minCheckOut()" [(ngModel)]="checkOut" />
            </label>
            <button class="btn" type="submit">Check availability</button>
          </form>

          @if (dateError()) {
            <p class="alert error" role="alert">{{ dateError() }}</p>
          }

          @if (h.availableRooms === undefined) {
            <p class="muted">Choose your dates to see availability and the total price.</p>
          } @else if (h.available) {
            <div class="summary" aria-live="polite">
              <div class="row">
                <span>{{ h.pricePerNight | currency: 'INR' : 'symbol' : '1.0-0' }} × {{ h.nights }}
                  {{ h.nights === 1 ? 'night' : 'nights' }}</span>
                <span>{{ h.totalAmount | currency: 'INR' : 'symbol' : '1.2-2' }}</span>
              </div>
              <div class="row total">
                <span>Total to pay at the hotel</span>
                <strong>{{ h.totalAmount | currency: 'INR' : 'symbol' : '1.2-2' }}</strong>
              </div>
              <p class="small" [class.warn-text]="h.availableRooms <= 2">
                {{ h.availableRooms }} {{ h.availableRooms === 1 ? 'room' : 'rooms' }} left for these dates
              </p>
            </div>

            <form class="guest-form" (ngSubmit)="book()" novalidate>
              <label class="field" [class.invalid]="fieldErrors()['guestName']">
                <span>Full name</span>
                <input name="guestName" type="text" autocomplete="name" [(ngModel)]="guestName" />
                @if (fieldErrors()['guestName']) {
                  <small class="field-error">{{ fieldErrors()['guestName'] }}</small>
                }
              </label>
              <label class="field" [class.invalid]="fieldErrors()['guestEmail']">
                <span>Email address</span>
                <input name="guestEmail" type="email" autocomplete="email" [(ngModel)]="guestEmail" />
                @if (fieldErrors()['guestEmail']) {
                  <small class="field-error">{{ fieldErrors()['guestEmail'] }}</small>
                }
              </label>

              @if (bookError()) {
                <p class="alert error" role="alert">{{ bookError() }}</p>
              }

              <button class="btn primary block" type="submit" [disabled]="booking()">
                {{ booking() ? 'Confirming…' : 'Confirm booking' }}
              </button>
              <p class="muted small">You will get a booking reference. No payment is taken in this demo.</p>
            </form>
          } @else {
            <div class="alert warn" role="status">
              <strong>Sold out for these dates.</strong>
              <span> Try one of these nearby stays instead.</span>
            </div>
          }

          @if (bookError() && h.available === false) {
            <p class="alert error" role="alert">{{ bookError() }}</p>
          }

          @if (h.available === false) {
            <div class="alternatives">
              <h3>Nearby dates with rooms</h3>
              @if (alternativesLoading()) {
                <p class="muted">Looking for nearby dates…</p>
              } @else if (alternatives().length === 0) {
                <p class="muted">No rooms found within two weeks of your dates. Try other dates or another hotel.</p>
              } @else {
                <ul class="alt-list">
                  @for (a of alternatives(); track a.checkIn) {
                    <li>
                      <div>
                        <strong>{{ a.checkIn | date: 'EEE d MMM' }} → {{ a.checkOut | date: 'EEE d MMM' }}</strong>
                        <div class="muted small">
                          {{ shiftLabel(a.shiftDays) }} · {{ a.availableRooms }}
                          {{ a.availableRooms === 1 ? 'room' : 'rooms' }} left ·
                          {{ a.totalAmount | currency: 'INR' : 'symbol' : '1.0-0' }}
                        </div>
                      </div>
                      <button class="btn small-btn" type="button" (click)="useAlternative(a)">Use these dates</button>
                    </li>
                  }
                </ul>
              }
            </div>
          }
        </aside>
      </div>
    }
  `,
})
export class HotelDetailPage {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly today = todayString();

  private hotelId = 0;
  checkIn = '';
  checkOut = '';
  guestName = '';
  guestEmail = '';

  readonly hotel = signal<Hotel | null>(null);
  readonly loading = signal(false);
  readonly loadError = signal('');
  readonly dateError = signal('');
  readonly bookError = signal('');
  readonly fieldErrors = signal<Record<string, string>>({});
  readonly booking = signal(false);
  readonly alternatives = signal<AlternativeStay[]>([]);
  readonly alternativesLoading = signal(false);

  constructor() {
    // Reload whenever the hotel or the selected dates in the URL change (emits once on start).
    combineLatest([this.route.paramMap, this.route.queryParamMap])
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.reload());
  }

  backParams() {
    return this.checkIn && this.checkOut ? { checkIn: this.checkIn, checkOut: this.checkOut } : {};
  }

  minCheckOut(): string {
    return this.checkIn ? addDays(this.checkIn, 1) : addDays(this.today, 1);
  }

  onCheckInChange(): void {
    if (this.checkIn && this.checkOut && this.checkOut <= this.checkIn) {
      this.checkOut = '';
    }
  }

  shiftLabel(shift: number): string {
    const n = Math.abs(shift);
    return `${n} ${n === 1 ? 'day' : 'days'} ${shift > 0 ? 'later' : 'earlier'}`;
  }

  ratingLabel(rating: number): string {
    if (rating >= 4.7) return 'Exceptional';
    if (rating >= 4.5) return 'Excellent';
    if (rating >= 4) return 'Very good';
    return 'Good';
  }

  applyDates(): void {
    this.dateError.set('');
    const problem = validateStay(this.checkIn, this.checkOut);
    if (problem) {
      this.dateError.set(problem);
      return;
    }
    this.router.navigate([], { queryParams: { checkIn: this.checkIn, checkOut: this.checkOut } });
  }

  useAlternative(a: AlternativeStay): void {
    this.router.navigate([], { queryParams: { checkIn: a.checkIn, checkOut: a.checkOut } });
  }

  book(): void {
    const h = this.hotel();
    if (!h || this.booking()) return;
    this.bookError.set('');
    this.fieldErrors.set({});

    // Quick client-side hints; the backend validates everything again.
    const local: Record<string, string> = {};
    if (this.guestName.trim().length < 2) local['guestName'] = 'Please enter your full name.';
    if (!/^\S+@\S+\.\S+$/.test(this.guestEmail.trim())) local['guestEmail'] = 'Please enter a valid email address.';
    if (Object.keys(local).length > 0) {
      this.fieldErrors.set(local);
      return;
    }

    this.booking.set(true);
    this.api
      .createBooking({
        hotelId: h.id,
        checkIn: this.checkIn,
        checkOut: this.checkOut,
        guestName: this.guestName.trim(),
        guestEmail: this.guestEmail.trim(),
      })
      .subscribe({
        next: (booking) => {
          this.booking.set(false);
          this.router.navigate(['/confirmation'], { state: { booking } });
        },
        error: (err) => {
          this.booking.set(false);
          const e = toFriendlyError(err);
          this.bookError.set(e.message);
          this.fieldErrors.set(e.fields);
          if (e.code === 'ROOMS_UNAVAILABLE') {
            // someone else took the last room: refresh availability and show alternatives
            this.reload(true);
          }
        },
      });
  }

  private reload(keepBookError = false): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    const params = this.route.snapshot.queryParamMap;
    this.hotelId = id;
    this.checkIn = params.get('checkIn') ?? '';
    this.checkOut = params.get('checkOut') ?? '';
    if (!keepBookError) this.bookError.set('');
    this.fieldErrors.set({});
    this.dateError.set('');
    this.alternatives.set([]);

    this.loading.set(true);
    this.loadError.set('');
    this.api.getHotel(id, this.checkIn, this.checkOut).subscribe({
      next: (hotel) => {
        this.hotel.set(hotel);
        this.loading.set(false);
        if (hotel.available === false) {
          this.loadAlternatives();
        }
      },
      error: (err) => {
        const e = toFriendlyError(err);
        this.loading.set(false);
        if (this.checkIn && e.code === 'INVALID_STAY') {
          // bad dates in the URL: fall back to the hotel without dates
          this.dateError.set(e.message);
          this.checkIn = '';
          this.checkOut = '';
          this.api.getHotel(id, '', '').subscribe({
            next: (hotel) => this.hotel.set(hotel),
            error: (err2) => this.loadError.set(toFriendlyError(err2).message),
          });
        } else {
          this.hotel.set(null);
          this.loadError.set(e.message);
        }
      },
    });
  }

  private loadAlternatives(): void {
    this.alternativesLoading.set(true);
    this.api.getAlternatives(this.hotelId, this.checkIn, this.checkOut).subscribe({
      next: (list) => {
        this.alternatives.set(list);
        this.alternativesLoading.set(false);
      },
      error: () => {
        this.alternatives.set([]);
        this.alternativesLoading.set(false);
      },
    });
  }
}
