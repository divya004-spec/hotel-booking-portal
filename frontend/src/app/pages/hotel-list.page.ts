import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { ApiService } from '../api.service';
import { addDaysIso, hueFor, nightsBetween, plural, todayIso, validateDates } from '../date-utils';
import { InrPipe } from '../inr.pipe';
import { AppError, Hotel } from '../models';

@Component({
  selector: 'app-hotel-list',
  imports: [FormsModule, RouterLink, InrPipe],
  template: `
    <section class="hero">
      <h1>Find your stay</h1>
      <p class="muted">Search by city and pick your dates to see live availability and the total price.</p>

      <form class="search-form" (ngSubmit)="search()" novalidate>
        <div class="field">
          <label for="location">Where to?</label>
          <input
            id="location"
            name="location"
            type="text"
            list="location-options"
            placeholder="City, for example Goa"
            autocomplete="off"
            [ngModel]="location()"
            (ngModelChange)="location.set($event)"
          />
          <datalist id="location-options">
            @for (name of locations(); track name) {
              <option [value]="name"></option>
            }
          </datalist>
        </div>

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

        <button class="btn btn-primary" type="submit">Search</button>
      </form>

      @if (nights() > 0) {
        <p class="hint">{{ plural(nights(), 'night') }} selected</p>
      }
    </section>

    <section aria-live="polite">
      @if (error()) {
        <div class="alert alert-error" role="alert">{{ error() }}</div>
      }

      @if (loading()) {
        <p class="muted">Loading hotels...</p>
      } @else if (!error()) {
        @if (hotels().length === 0) {
          <div class="empty">
            <h2>No hotels found</h2>
            <p class="muted">Try another city, or clear the search box to see every hotel.</p>
          </div>
        } @else {
          <p class="muted result-count">{{ plural(hotels().length, 'hotel') }} found</p>
          <div class="card-grid">
            @for (hotel of hotels(); track hotel.id) {
              <article class="hotel-card">
                <div class="banner" [style.--hue]="hue(hotel.id)">
                  <span>{{ hotel.location }}</span>
                </div>

                <div class="card-body">
                  <h2>{{ hotel.name }}</h2>
                  <p class="muted">{{ hotel.description }}</p>
                  <ul class="chips" aria-label="Amenities">
                    @for (amenity of hotel.amenities; track amenity) {
                      <li>{{ amenity }}</li>
                    }
                  </ul>
                </div>

                <div class="card-footer">
                  <div class="price">
                    @if (hotel.stay) {
                      <strong>{{ hotel.stay.totalAmount | inr }}</strong>
                      <span class="muted"> for {{ plural(hotel.stay.nights, 'night') }}</span>
                      <span class="muted small block">{{ hotel.pricePerNight | inr }} per night</span>
                    } @else {
                      <strong>{{ hotel.pricePerNight | inr }}</strong>
                      <span class="muted"> per night</span>
                    }
                  </div>

                  @if (hotel.stay) {
                    @if (hotel.stay.available) {
                      <span class="badge badge-ok">{{ plural(hotel.stay.availableRooms, 'room') }} left</span>
                    } @else {
                      <span class="badge badge-danger">Sold out for these dates</span>
                    }
                  }

                  <a
                    class="btn btn-primary"
                    [routerLink]="['/hotels', hotel.id]"
                    [queryParams]="linkParams()"
                  >
                    {{ hotel.stay && !hotel.stay.available ? 'See nearby dates' : 'View and book' }}
                  </a>
                </div>
              </article>
            }
          </div>
        }
      }
    </section>
  `,
})
export class HotelListPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly today = todayIso();

  // What the visitor is typing.
  readonly location = signal('');
  readonly checkIn = signal('');
  readonly checkOut = signal('');

  // What the results on screen were searched with.
  private readonly applied = signal({ checkIn: '', checkOut: '' });

  readonly hotels = signal<Hotel[]>([]);
  readonly locations = signal<string[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  readonly nights = computed(() => nightsBetween(this.checkIn(), this.checkOut()));
  readonly minCheckOut = computed(() => addDaysIso(this.checkIn() || this.today, 1));
  readonly linkParams = computed(() => ({
    checkIn: this.applied().checkIn || null,
    checkOut: this.applied().checkOut || null,
  }));

  protected readonly hue = hueFor;
  protected readonly plural = plural;

  ngOnInit(): void {
    const params = this.route.snapshot.queryParamMap;
    this.location.set(params.get('location') ?? '');
    this.checkIn.set(params.get('checkIn') ?? '');
    this.checkOut.set(params.get('checkOut') ?? '');

    this.api.locations().subscribe({
      next: (names) => this.locations.set(names),
      error: () => this.locations.set([]), // suggestions are optional
    });

    this.load();
  }

  search(): void {
    // Keep the search in the address bar so refresh and the back button work.
    this.router.navigate([], {
      queryParams: {
        location: this.location().trim() || null,
        checkIn: this.checkIn() || null,
        checkOut: this.checkOut() || null,
      },
      replaceUrl: true,
    });
    this.load();
  }

  private load(): void {
    const problem = validateDates(this.checkIn(), this.checkOut());
    if (problem) {
      this.error.set(problem);
      this.hotels.set([]);
      this.loading.set(false);
      return;
    }

    this.loading.set(true);
    this.error.set(null);
    this.api.hotels(this.location(), this.checkIn(), this.checkOut()).subscribe({
      next: (hotels) => {
        this.hotels.set(hotels);
        this.applied.set({ checkIn: this.checkIn(), checkOut: this.checkOut() });
        this.loading.set(false);
      },
      error: (err: AppError) => {
        this.error.set(err.message);
        this.hotels.set([]);
        this.loading.set(false);
      },
    });
  }
}
