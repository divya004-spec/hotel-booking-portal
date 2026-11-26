import { Routes } from '@angular/router';

import { ConfirmationPage } from './pages/confirmation.page';
import { HotelDetailPage } from './pages/hotel-detail.page';
import { HotelListPage } from './pages/hotel-list.page';
import { ManageBookingPage } from './pages/manage-booking.page';

export const routes: Routes = [
  { path: '', component: HotelListPage, title: 'Find a hotel | Hotel Booking Portal' },
  { path: 'hotels/:id', component: HotelDetailPage, title: 'Hotel details | Hotel Booking Portal' },
  { path: 'confirmation/:reference', component: ConfirmationPage, title: 'Booking confirmed | Hotel Booking Portal' },
  { path: 'manage', component: ManageBookingPage, title: 'Manage booking | Hotel Booking Portal' },
  { path: '**', redirectTo: '' },
];
