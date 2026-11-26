export interface StayQuote {
  checkIn: string;
  checkOut: string;
  nights: number;
  availableRooms: number;
  available: boolean;
  totalAmount: number;
}

export interface Hotel {
  id: number;
  name: string;
  location: string;
  description: string;
  amenities: string[];
  pricePerNight: number;
  stay: StayQuote | null;
}

/** An alternative stay (same number of nights) that still has rooms. */
export interface DateOption {
  checkIn: string;
  checkOut: string;
  nights: number;
  availableRooms: number;
  totalAmount: number;
}

export interface BookingRequest {
  hotelId: number;
  guestName: string;
  guestEmail: string;
  checkIn: string;
  checkOut: string;
}

export interface Booking {
  reference: string;
  status: 'CONFIRMED' | 'CANCELLED';
  hotelId: number;
  hotelName: string;
  hotelLocation: string;
  guestName: string;
  guestEmail: string;
  checkIn: string;
  checkOut: string;
  nights: number;
  pricePerNight: number;
  totalAmount: number;
  createdAt: string;
  cancelledAt: string | null;
}

export interface FieldIssue {
  field: string;
  message: string;
}

/** The one error shape the UI works with, built from the API's error JSON. */
export interface AppError {
  status: number;
  code: string;
  message: string;
  fieldErrors: FieldIssue[];
}
