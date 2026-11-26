import { Routes } from '@angular/router';

import { HotelSearchPage } from './pages/hotel-search';
import { HotelDetailPage } from './pages/hotel-detail';
import { BookingConfirmationPage } from './pages/booking-confirmation';
import { ManageBookingPage } from './pages/manage-booking';

export const routes: Routes = [
  { path: '', component: HotelSearchPage, title: 'Find a hotel' },
  { path: 'hotels/:id', component: HotelDetailPage, title: 'Hotel details' },
  { path: 'confirmation', component: BookingConfirmationPage, title: 'Booking confirmed' },
  { path: 'manage', component: ManageBookingPage, title: 'Manage booking' },
  { path: '**', redirectTo: '' },
];
