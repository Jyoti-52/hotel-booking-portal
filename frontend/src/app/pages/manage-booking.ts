import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { ApiService } from '../core/api.service';
import { toFriendlyError } from '../core/errors';
import { Booking } from '../core/models';

@Component({
  selector: 'app-manage-booking',
  imports: [FormsModule, RouterLink, CurrencyPipe, DatePipe],
  template: `
    <section class="narrow">
      <h1>Manage your booking</h1>
      <p class="muted">Enter the booking reference from your confirmation and the email address you booked with.</p>

      <form class="card stack" (ngSubmit)="lookup()" novalidate>
        <label class="field" [class.invalid]="fieldErrors()['reference']">
          <span>Booking reference</span>
          <input name="reference" type="text" placeholder="HB-XXXXXXXX" autocomplete="off" [(ngModel)]="reference" />
          @if (fieldErrors()['reference']) {
            <small class="field-error">{{ fieldErrors()['reference'] }}</small>
          }
        </label>
        <label class="field" [class.invalid]="fieldErrors()['email']">
          <span>Email address</span>
          <input name="email" type="email" autocomplete="email" [(ngModel)]="email" />
          @if (fieldErrors()['email']) {
            <small class="field-error">{{ fieldErrors()['email'] }}</small>
          }
        </label>
        <button class="btn primary" type="submit" [disabled]="busy()">{{ busy() ? 'Looking up…' : 'Find booking' }}</button>
      </form>

      @if (error()) {
        <p class="alert error" role="alert">{{ error() }}</p>
      }
      @if (notice()) {
        <p class="alert ok" role="status">{{ notice() }}</p>
      }

      @if (booking(); as b) {
        <article class="card booking-card" [class.cancelled]="b.status === 'CANCELLED'">
          <div class="hotel-top">
            <div>
              <h2>{{ b.hotelName }}</h2>
              <p class="location">📍 {{ b.hotelLocation }}</p>
            </div>
            <span class="badge" [class.ok]="b.status === 'CONFIRMED'" [class.danger]="b.status === 'CANCELLED'">
              {{ b.status === 'CONFIRMED' ? 'Confirmed' : 'Cancelled' }}
            </span>
          </div>

          <dl class="details">
            <div><dt>Reference</dt><dd><strong>{{ b.reference }}</strong></dd></div>
            <div><dt>Guest</dt><dd>{{ b.guestName }}</dd></div>
            <div><dt>Check-in</dt><dd>{{ b.checkIn | date: 'EEE, d MMM y' }}</dd></div>
            <div><dt>Check-out</dt><dd>{{ b.checkOut | date: 'EEE, d MMM y' }}</dd></div>
            <div><dt>Nights</dt><dd>{{ b.nights }} × {{ b.pricePerNight | currency: 'INR' : 'symbol' : '1.0-0' }}</dd></div>
            <div class="total"><dt>Total</dt><dd>{{ b.totalAmount | currency: 'INR' : 'symbol' : '1.2-2' }}</dd></div>
            @if (b.cancelledAt) {
              <div><dt>Cancelled on</dt><dd>{{ b.cancelledAt | date: 'd MMM y, h:mm a' }}</dd></div>
            }
          </dl>

          @if (b.status === 'CONFIRMED') {
            @if (b.cancellable) {
              @if (!confirmingCancel()) {
                <button class="btn danger" type="button" (click)="confirmingCancel.set(true)">Cancel booking</button>
              } @else {
                <div class="alert warn" role="alertdialog" aria-label="Confirm cancellation">
                  <p><strong>Cancel this booking?</strong> The room will be released and this cannot be undone.</p>
                  <div class="actions">
                    <button class="btn danger" type="button" (click)="cancel()" [disabled]="busy()">
                      {{ busy() ? 'Cancelling…' : 'Yes, cancel booking' }}
                    </button>
                    <button class="btn" type="button" (click)="confirmingCancel.set(false)">Keep booking</button>
                  </div>
                </div>
              }
            } @else {
              <p class="muted small">This stay has already started, so it can no longer be cancelled online.</p>
            }
          } @else {
            <p class="muted small">This booking is cancelled and its room has been released.</p>
          }
        </article>

        <p class="center"><a routerLink="/">Find another hotel</a></p>
      }
    </section>
  `,
})
export class ManageBookingPage {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);

  reference = this.route.snapshot.queryParamMap.get('reference') ?? '';
  email = '';

  readonly booking = signal<Booking | null>(null);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly notice = signal('');
  readonly fieldErrors = signal<Record<string, string>>({});
  readonly confirmingCancel = signal(false);

  lookup(): void {
    this.reset();
    const local: Record<string, string> = {};
    if (!this.reference.trim()) local['reference'] = 'Booking reference is required.';
    if (!/^\S+@\S+\.\S+$/.test(this.email.trim())) local['email'] = 'Please enter a valid email address.';
    if (Object.keys(local).length > 0) {
      this.fieldErrors.set(local);
      return;
    }
    this.busy.set(true);
    this.api.lookupBooking(this.reference.trim(), this.email.trim()).subscribe({
      next: (b) => {
        this.booking.set(b);
        this.busy.set(false);
      },
      error: (err) => this.fail(err),
    });
  }

  cancel(): void {
    this.error.set('');
    this.notice.set('');
    this.busy.set(true);
    this.api.cancelBooking(this.reference.trim(), this.email.trim()).subscribe({
      next: (b) => {
        this.booking.set(b);
        this.confirmingCancel.set(false);
        this.busy.set(false);
        this.notice.set('Your booking has been cancelled and the room has been released.');
      },
      error: (err) => {
        this.confirmingCancel.set(false);
        this.fail(err);
        // refresh the card so it shows the true state (e.g. already cancelled elsewhere)
        this.api.lookupBooking(this.reference.trim(), this.email.trim()).subscribe({
          next: (b) => this.booking.set(b),
          error: () => undefined,
        });
      },
    });
  }

  private reset(): void {
    this.error.set('');
    this.notice.set('');
    this.fieldErrors.set({});
    this.booking.set(null);
    this.confirmingCancel.set(false);
  }

  private fail(err: unknown): void {
    const e = toFriendlyError(err);
    this.error.set(e.message);
    this.fieldErrors.set(e.fields);
    this.busy.set(false);
  }
}
