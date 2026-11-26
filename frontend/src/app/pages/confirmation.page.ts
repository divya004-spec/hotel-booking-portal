import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { ApiService } from '../api.service';
import { InrPipe } from '../inr.pipe';
import { AppError, Booking } from '../models';
import { plural } from '../date-utils';

@Component({
  selector: 'app-confirmation',
  imports: [RouterLink, DatePipe, InrPipe],
  template: `
    @if (loading()) {
      <p class="muted">Loading your booking...</p>
    } @else if (error()) {
      <div class="alert alert-error" role="alert">{{ error() }}</div>
      <a class="btn" routerLink="/manage">Look up a booking</a>
    } @else if (booking(); as b) {
      <section class="confirmation">
        <div class="success-banner" role="status">
          <h1>Booking confirmed</h1>
          <p>Thank you, {{ b.guestName }}. Keep your reference safe: you need it, with your email, to view or cancel this booking.</p>
        </div>

        <div class="reference-box">
          <span class="muted">Booking reference</span>
          <strong class="reference">{{ b.reference }}</strong>
          <button type="button" class="btn btn-small no-print" (click)="copy(b.reference)">
            {{ copied() ? 'Copied' : 'Copy' }}
          </button>
        </div>

        <dl class="details">
          <div><dt>Hotel</dt><dd>{{ b.hotelName }}, {{ b.hotelLocation }}</dd></div>
          <div><dt>Check-in</dt><dd>{{ b.checkIn | date: 'EEEE, d MMMM y' }}</dd></div>
          <div><dt>Check-out</dt><dd>{{ b.checkOut | date: 'EEEE, d MMMM y' }}</dd></div>
          <div><dt>Length of stay</dt><dd>{{ plural(b.nights, 'night') }} at {{ b.pricePerNight | inr }}</dd></div>
          <div><dt>Guest</dt><dd>{{ b.guestName }} ({{ b.guestEmail }})</dd></div>
          <div class="total"><dt>Total amount</dt><dd>{{ b.totalAmount | inr }}</dd></div>
        </dl>

        <div class="actions no-print">
          <a class="btn btn-primary" routerLink="/">Book another stay</a>
          <a class="btn" routerLink="/manage" [queryParams]="{ reference: b.reference, email: b.guestEmail }">
            View or cancel this booking
          </a>
          <button type="button" class="btn" (click)="print()">Print</button>
        </div>
      </section>
    }
  `,
})
export class ConfirmationPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);

  readonly booking = signal<Booking | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly copied = signal(false);

  protected readonly plural = plural;

  ngOnInit(): void {
    const reference = this.route.snapshot.paramMap.get('reference') ?? '';
    const email = this.route.snapshot.queryParamMap.get('email') ?? '';

    this.api.getBooking(reference, email).subscribe({
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

  copy(reference: string): void {
    navigator.clipboard
      ?.writeText(reference)
      .then(() => {
        this.copied.set(true);
        setTimeout(() => this.copied.set(false), 2000);
      })
      .catch(() => {
        // Copying is a convenience only; ignore failures.
      });
  }

  print(): void {
    window.print();
  }
}
