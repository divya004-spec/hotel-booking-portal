import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <a class="skip-link" href="#main">Skip to content</a>

    <header class="site-header">
      <div class="container header-inner">
        <a routerLink="/" class="brand">
          <span class="brand-mark" aria-hidden="true">HB</span>
          <span>Hotel Booking Portal</span>
        </a>
        <nav aria-label="Main">
          <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">Hotels</a>
          <a routerLink="/manage" routerLinkActive="active">Manage booking</a>
        </nav>
      </div>
    </header>

    <main id="main" class="container">
      <router-outlet />
    </main>

    <footer class="site-footer">
      <div class="container">Interview assignment project. Sample data only; no real payments are taken.</div>
    </footer>
  `,
})
export class App {}
