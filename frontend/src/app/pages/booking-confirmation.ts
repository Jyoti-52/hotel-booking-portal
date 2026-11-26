import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { Booking } from '../core/models';

@Component({
  selector: 'app-booking-confirmation',
  imports: [RouterLink, CurrencyPipe, DatePipe],
  template: `
    @if (booking(); as b) {
      <section class="card confirmation">
        <div class="tick" aria-hidden="true">✓</div>
        <h1>Booking confirmed</h1>
        <p class="muted">Thank you, {{ b.guestName }}. Keep your reference safe: you need it, with your email, to view or cancel this booking.</p>

        <div class="reference" aria-label="Booking reference">
          <span class="muted small">Booking reference</span>
          <strong>{{ b.reference }}</strong>
          <button class="btn small-btn" type="button" (click)="copy(b.reference)">
            {{ copied() ? 'Copied ✓' : 'Copy' }}
          </button>
        </div>

        <dl class="details">
          <div><dt>Hotel</dt><dd>{{ b.hotelName }}, {{ b.hotelLocation }}</dd></div>
          <div><dt>Check-in</dt><dd>{{ b.checkIn | date: 'EEEE, d MMMM y' }}</dd></div>
          <div><dt>Check-out</dt><dd>{{ b.checkOut | date: 'EEEE, d MMMM y' }}</dd></div>
          <div><dt>Nights</dt><dd>{{ b.nights }} × {{ b.pricePerNight | currency: 'INR' : 'symbol' : '1.0-0' }}</dd></div>
          <div class="total"><dt>Total</dt><dd>{{ b.totalAmount | currency: 'INR' : 'symbol' : '1.2-2' }}</dd></div>
          <div><dt>Guest email</dt><dd>{{ b.guestEmail }}</dd></div>
        </dl>

        <div class="actions">
          <a class="btn primary" routerLink="/manage" [queryParams]="{ reference: b.reference }">Manage this booking</a>
          <a class="btn" routerLink="/">Book another stay</a>
        </div>
      </section>
    }
  `,
})
export class BookingConfirmationPage {
  private readonly router = inject(Router);

  readonly booking = signal<Booking | null>(null);
  readonly copied = signal(false);

  constructor() {
    // The booking is handed over through router state right after it is created.
    const state = this.router.getCurrentNavigation()?.extras.state as { booking?: Booking } | undefined;
    const booking = state?.booking ?? (history.state as { booking?: Booking } | null)?.booking;
    if (booking) {
      this.booking.set(booking);
    } else {
      // Opened directly (e.g. after a refresh): send the guest to the lookup page instead of showing nothing.
      this.router.navigate(['/manage']);
    }
  }

  copy(reference: string): void {
    navigator.clipboard
      ?.writeText(reference)
      .then(() => {
        this.copied.set(true);
        setTimeout(() => this.copied.set(false), 2000);
      })
      .catch(() => undefined);
  }
}
