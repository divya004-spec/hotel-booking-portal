import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { ApiService } from '../api.service';
import { addDaysIso, hueFor, nightsBetween, plural, todayIso, validateDates } from '../date-utils';
import { InrPipe } from '../inr.pipe';
import { AppError, DateOption, Hotel } from '../models';

const EMAIL_PATTERN = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;

@Component({
  selector: 'app-hotel-detail',
  imports: [FormsModule, RouterLink, DatePipe, InrPipe],
  template: `
    @if (loading()) {
      <p class="muted">Loading hotel...</p>
    } @else if (loadError()) {
      <div class="alert alert-error" role="alert">{{ loadError() }}</div>
      <a class="btn" routerLink="/">Back to hotels</a>
    } @else if (hotel(); as h) {
      <a class="back-link" routerLink="/" [queryParams]="backParams()">&larr; Back to results</a>

      <div class="detail-layout">
        <section>
          <div class="banner banner-lg" [style.--hue]="hue(h.id)">
            <span>{{ h.location }}</span>
          </div>

          <h1>{{ h.name }}</h1>
          <p class="muted">{{ h.location }} &middot; Standard room &middot; {{ h.pricePerNight | inr }} per night</p>
          <p>{{ h.description }}</p>

          <h2>Amenities</h2>
          <ul class="chips" aria-label="Amenities">
            @for (amenity of h.amenities; track amenity) {
              <li>{{ amenity }}</li>
            }
          </ul>

          <h2>Your dates</h2>
          <form class="date-form" (ngSubmit)="updateDates()" novalidate>
            <div class="field">
              <label for="checkIn">Check-in</label>
              <input
                id="checkIn"
                name="checkIn"
                type="date"
                [min]="today"
                [ngModel]="checkIn()"
                (ngModelChange)="checkIn.set($event)"
              />
            </div>
            <div class="field">
              <label for="checkOut">Check-out</label>
              <input
                id="checkOut"
                name="checkOut"
                type="date"
                [min]="minCheckOut()"
                [ngModel]="checkOut()"
                (ngModelChange)="checkOut.set($event)"
              />
            </div>
            <button class="btn" type="submit">Check availability</button>
          </form>
          @if (dateError()) {
            <div class="alert alert-error" role="alert">{{ dateError() }}</div>
          }
        </section>

        <aside class="booking-panel" aria-live="polite">
          @if (stay(); as s) {
            <h2>Your stay</h2>
            <p class="stay-dates">
              {{ s.checkIn | date: 'EEE, d MMM y' }} to {{ s.checkOut | date: 'EEE, d MMM y' }}
            </p>

            <dl class="price-breakdown">
              <div>
                <dt>{{ h.pricePerNight | inr }} x {{ plural(s.nights, 'night') }}</dt>
                <dd>{{ s.totalAmount | inr }}</dd>
              </div>
              <div class="total">
                <dt>Total amount</dt>
                <dd>{{ s.totalAmount | inr }}</dd>
              </div>
            </dl>

            @if (s.available) {
              <p><span class="badge badge-ok">{{ plural(s.availableRooms, 'room') }} left for these dates</span></p>

              <form (ngSubmit)="submit()" novalidate>
                <div class="field">
                  <label for="guestName">Full name</label>
                  <input
                    id="guestName"
                    name="guestName"
                    type="text"
                    autocomplete="name"
                    [class.invalid]="fieldErrors()['guestName']"
                    [attr.aria-invalid]="fieldErrors()['guestName'] ? true : null"
                    [ngModel]="guestName()"
                    (ngModelChange)="guestName.set($event)"
                  />
                  @if (fieldErrors()['guestName']) {
                    <p class="field-error">{{ fieldErrors()['guestName'] }}</p>
                  }
                </div>

                <div class="field">
                  <label for="guestEmail">Email address</label>
                  <input
                    id="guestEmail"
                    name="guestEmail"
                    type="email"
                    autocomplete="email"
                    [class.invalid]="fieldErrors()['guestEmail']"
                    [attr.aria-invalid]="fieldErrors()['guestEmail'] ? true : null"
                    [ngModel]="guestEmail()"
                    (ngModelChange)="guestEmail.set($event)"
                  />
                  @if (fieldErrors()['guestEmail']) {
                    <p class="field-error">{{ fieldErrors()['guestEmail'] }}</p>
                  }
                </div>

                @if (formError()) {
                  <div class="alert alert-error" role="alert">{{ formError() }}</div>
                }

                <button class="btn btn-primary btn-block" type="submit" [disabled]="submitting()">
                  {{ submitting() ? 'Booking...' : 'Confirm booking for ' + (s.totalAmount | inr) }}
                </button>
                <p class="small muted">No payment is taken in this demo. You will get a booking reference on the next page.</p>
              </form>
            } @else {
              <div class="alert alert-warn" role="status">
                <strong>Sold out</strong> for these dates.
              </div>

              <div class="suggestions">
                <h3>Nearby dates that are free</h3>
                @if (loadingAlternatives()) {
                  <p class="muted">Looking for the closest free dates...</p>
                } @else if (alternatives().length > 0) {
                  <p class="muted small">Same length of stay ({{ plural(s.nights, 'night') }}). Pick one to see it here.</p>
                  <ul class="suggestion-list">
                    @for (option of alternatives(); track option.checkIn) {
                      <li>
                        <button type="button" class="suggestion" (click)="pickAlternative(option)">
                          <strong>{{ option.checkIn | date: 'EEE, d MMM' }} to {{ option.checkOut | date: 'EEE, d MMM' }}</strong>
                          <span>{{ option.totalAmount | inr }} &middot; {{ plural(option.availableRooms, 'room') }} left</span>
                        </button>
                      </li>
                    }
                  </ul>
                } @else {
                  <p class="muted">
                    No free {{ plural(s.nights, 'night') }} stays within two weeks of your dates.
                    Try a shorter stay or another hotel.
                  </p>
                }
              </div>
            }
          } @else {
            <h2>Check availability</h2>
            <p class="muted">Choose your check-in and check-out dates to see the total price and book a room.</p>
          }
        </aside>
      </div>
    }
  `,
})
export class HotelDetailPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly today = todayIso();

  readonly hotel = signal<Hotel | null>(null);
  readonly loading = signal(true);
  readonly loadError = signal<string | null>(null);

  readonly checkIn = signal('');
  readonly checkOut = signal('');
  readonly dateError = signal<string | null>(null);

  readonly alternatives = signal<DateOption[]>([]);
  readonly loadingAlternatives = signal(false);

  readonly guestName = signal('');
  readonly guestEmail = signal('');
  readonly submitting = signal(false);
  readonly formError = signal<string | null>(null);
  readonly fieldErrors = signal<Record<string, string>>({});

  readonly stay = computed(() => this.hotel()?.stay ?? null);
  readonly minCheckOut = computed(() => addDaysIso(this.checkIn() || this.today, 1));
  readonly backParams = computed(() => ({
    checkIn: this.stay()?.checkIn ?? null,
    checkOut: this.stay()?.checkOut ?? null,
  }));

  protected readonly hue = hueFor;
  protected readonly plural = plural;

  private hotelId = 0;

  ngOnInit(): void {
    this.hotelId = Number(this.route.snapshot.paramMap.get('id'));
    const params = this.route.snapshot.queryParamMap;
    this.checkIn.set(params.get('checkIn') ?? '');
    this.checkOut.set(params.get('checkOut') ?? '');
    this.load();
  }

  /** Reloads the hotel (and its quote for the chosen dates). */
  private load(quiet = false): void {
    if (!quiet) {
      this.loading.set(true);
    }
    this.loadError.set(null);
    this.alternatives.set([]);

    // Bad dates in the address bar are ignored rather than crashing the page.
    const problem = validateDates(this.checkIn(), this.checkOut());
    this.dateError.set(problem);
    const useDates = problem === null;

    this.api
      .hotel(this.hotelId, useDates ? this.checkIn() : '', useDates ? this.checkOut() : '')
      .subscribe({
        next: (hotel) => {
          this.hotel.set(hotel);
          this.loading.set(false);
          if (hotel.stay && !hotel.stay.available) {
            this.loadAlternatives();
          }
        },
        error: (err: AppError) => {
          this.loadError.set(err.message);
          this.loading.set(false);
        },
      });
  }

  /** The surprise feature: ask the API for the nearest dates that are free. */
  private loadAlternatives(): void {
    this.loadingAlternatives.set(true);
    this.api.alternatives(this.hotelId, this.checkIn(), this.checkOut()).subscribe({
      next: (options) => {
        this.alternatives.set(options);
        this.loadingAlternatives.set(false);
      },
      error: () => {
        this.alternatives.set([]);
        this.loadingAlternatives.set(false);
      },
    });
  }

  updateDates(): void {
    const problem = validateDates(this.checkIn(), this.checkOut());
    if (problem) {
      this.dateError.set(problem);
      return;
    }
    this.router.navigate([], {
      queryParams: { checkIn: this.checkIn() || null, checkOut: this.checkOut() || null },
      replaceUrl: true,
    });
    this.formError.set(null);
    this.fieldErrors.set({});
    this.load(true);
  }

  pickAlternative(option: DateOption): void {
    this.checkIn.set(option.checkIn);
    this.checkOut.set(option.checkOut);
    this.updateDates();
  }

  submit(): void {
    const hotel = this.hotel();
    const stay = this.stay();
    if (!hotel || !stay || !stay.available) {
      return;
    }

    this.formError.set(null);
    const errors: Record<string, string> = {};
    if (!this.guestName().trim()) {
      errors['guestName'] = 'Name is required.';
    }
    if (!EMAIL_PATTERN.test(this.guestEmail().trim())) {
      errors['guestEmail'] = 'Please enter a valid email address.';
    }
    this.fieldErrors.set(errors);
    if (Object.keys(errors).length > 0) {
      return;
    }

    this.submitting.set(true);
    this.api
      .createBooking({
        hotelId: hotel.id,
        guestName: this.guestName().trim(),
        guestEmail: this.guestEmail().trim(),
        checkIn: stay.checkIn,
        checkOut: stay.checkOut,
      })
      .subscribe({
        next: (booking) => {
          this.router.navigate(['/confirmation', booking.reference], {
            queryParams: { email: booking.guestEmail },
          });
        },
        error: (err: AppError) => {
          this.submitting.set(false);
          const fromServer: Record<string, string> = {};
          for (const issue of err.fieldErrors) {
            fromServer[issue.field] = issue.message;
          }
          this.fieldErrors.set(fromServer);
          this.formError.set(err.message);
          if (err.status === 409) {
            // Someone else took the room first: show the new availability and alternatives.
            this.load(true);
          }
        },
      });
  }
}
