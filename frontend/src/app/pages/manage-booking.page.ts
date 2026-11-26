import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { ApiService } from '../api.service';
import { plural, todayIso } from '../date-utils';
import { InrPipe } from '../inr.pipe';
import { AppError, Booking } from '../models';

@Component({
  selector: 'app-manage-booking',
  imports: [FormsModule, RouterLink, DatePipe, InrPipe],
  template: `
    <section class="hero">
      <h1>Manage your booking</h1>
      <p class="muted">Enter your booking reference and the email address you booked with.</p>

      <form class="lookup-form" (ngSubmit)="lookup()" novalidate>
        <div class="field">
          <label for="reference">Booking reference</label>
          <input
            id="reference"
            name="reference"
            type="text"
            placeholder="HB-ABCD2345"
            autocomplete="off"
            [ngModel]="reference()"
            (ngModelChange)="reference.set($event)"
          />
        </div>
        <div class="field">
          <label for="email">Email address</label>
          <input
            id="email"
            name="email"
            type="email"
            autocomplete="email"
            [ngModel]="email()"
            (ngModelChange)="email.set($event)"
          />
        </div>
        <button class="btn btn-primary" type="submit" [disabled]="loading()">
          {{ loading() ? 'Searching...' : 'Find booking' }}
        </button>
      </form>
    </section>

    <section aria-live="polite">
      @if (error()) {
        <div class="alert alert-error" role="alert">{{ error() }}</div>
      }

      @if (notice()) {
        <div class="alert alert-ok" role="status">{{ notice() }}</div>
      }

      @if (booking(); as b) {
        <article class="booking-card">
          <header class="booking-card-header">
            <div>
              <span class="muted">Reference</span>
              <strong class="reference">{{ b.reference }}</strong>
            </div>
            <span class="badge" [class.badge-ok]="b.status === 'CONFIRMED'" [class.badge-danger]="b.status === 'CANCELLED'">
              {{ b.status === 'CONFIRMED' ? 'Confirmed' : 'Cancelled' }}
            </span>
          </header>

          <dl class="details">
            <div><dt>Hotel</dt><dd>{{ b.hotelName }}, {{ b.hotelLocation }}</dd></div>
            <div><dt>Check-in</dt><dd>{{ b.checkIn | date: 'EEEE, d MMMM y' }}</dd></div>
            <div><dt>Check-out</dt><dd>{{ b.checkOut | date: 'EEEE, d MMMM y' }}</dd></div>
            <div><dt>Length of stay</dt><dd>{{ plural(b.nights, 'night') }} at {{ b.pricePerNight | inr }}</dd></div>
            <div><dt>Guest</dt><dd>{{ b.guestName }} ({{ b.guestEmail }})</dd></div>
            <div class="total"><dt>Total amount</dt><dd>{{ b.totalAmount | inr }}</dd></div>
            @if (b.cancelledAt) {
              <div><dt>Cancelled on</dt><dd>{{ b.cancelledAt | date: 'd MMM y, h:mm a' }}</dd></div>
            }
          </dl>

          @if (canCancel()) {
            @if (confirmingCancel()) {
              <div class="alert alert-warn" role="alertdialog" aria-label="Confirm cancellation">
                <p><strong>Cancel this booking?</strong> The room will be released and this cannot be undone.</p>
                <div class="actions">
                  <button type="button" class="btn btn-danger" (click)="cancel()" [disabled]="cancelling()">
                    {{ cancelling() ? 'Cancelling...' : 'Yes, cancel booking' }}
                  </button>
                  <button type="button" class="btn" (click)="confirmingCancel.set(false)" [disabled]="cancelling()">
                    Keep booking
                  </button>
                </div>
              </div>
            } @else {
              <div class="actions">
                <button type="button" class="btn btn-danger-outline" (click)="confirmingCancel.set(true)">
                  Cancel booking
                </button>
              </div>
            }
          } @else if (b.status === 'CONFIRMED') {
            <p class="muted">This stay has already started, so it can no longer be cancelled online.</p>
          } @else {
            <div class="actions">
              <a class="btn btn-primary" [routerLink]="['/hotels', b.hotelId]">Book this hotel again</a>
            </div>
          }
        </article>
      }
    </section>
  `,
})
export class ManageBookingPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);

  readonly reference = signal('');
  readonly email = signal('');

  readonly booking = signal<Booking | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly notice = signal<string | null>(null);

  readonly confirmingCancel = signal(false);
  readonly cancelling = signal(false);

  /** A confirmed booking can be cancelled up to its check-in date. */
  readonly canCancel = computed(() => {
    const booking = this.booking();
    return !!booking && booking.status === 'CONFIRMED' && booking.checkIn >= todayIso();
  });

  protected readonly plural = plural;

  ngOnInit(): void {
    const params = this.route.snapshot.queryParamMap;
    this.reference.set(params.get('reference') ?? '');
    this.email.set(params.get('email') ?? '');
    if (this.reference() && this.email()) {
      this.lookup();
    }
  }

  lookup(): void {
    this.error.set(null);
    this.notice.set(null);
    this.booking.set(null);
    this.confirmingCancel.set(false);

    if (!this.reference().trim() || !this.email().trim()) {
      this.error.set('Please enter both your booking reference and your email address.');
      return;
    }

    this.loading.set(true);
    this.api.getBooking(this.reference(), this.email()).subscribe({
      next: (booking) => {
        this.booking.set(booking);
        this.loading.set(false);
      },
      error: (err: AppError) => {
        this.error.set(err.message);
        this.loading.set(false);
      },
    });
  }

  cancel(): void {
    const booking = this.booking();
    if (!booking) {
      return;
    }
    this.cancelling.set(true);
    this.error.set(null);
    this.api.cancelBooking(booking.reference, booking.guestEmail).subscribe({
      next: (updated) => {
        this.booking.set(updated);
        this.notice.set('Your booking has been cancelled and the room has been released.');
        this.confirmingCancel.set(false);
        this.cancelling.set(false);
      },
      error: (err: AppError) => {
        this.error.set(err.message);
        this.confirmingCancel.set(false);
        this.cancelling.set(false);
      },
    });
  }
}
