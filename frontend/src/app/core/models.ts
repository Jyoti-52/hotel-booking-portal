export interface Hotel {
  id: number;
  name: string;
  location: string;
  description: string;
  amenities: string[];
  pricePerNight: number;
  totalRooms: number;
  // present only when the request included dates
  checkIn?: string;
  checkOut?: string;
  nights?: number;
  totalAmount?: number;
  availableRooms?: number;
  available?: boolean;
  rating?: number | null;
  reviewCount?: number | null;
  imageUrl?: string | null;
}

export interface AlternativeStay {
  checkIn: string;
  checkOut: string;
  nights: number;
  shiftDays: number;
  availableRooms: number;
  totalAmount: number;
}

export interface CreateBookingRequest {
  hotelId: number;
  checkIn: string;
  checkOut: string;
  guestName: string;
  guestEmail: string;
}

export interface Booking {
  reference: string;
  status: 'CONFIRMED' | 'CANCELLED';
  hotelId: number;
  hotelName: string;
  hotelLocation: string;
  checkIn: string;
  checkOut: string;
  nights: number;
  pricePerNight: number;
  totalAmount: number;
  guestName: string;
  guestEmail: string;
  createdAt: string;
  cancelledAt?: string;
  cancellable: boolean;
}

export interface ApiError {
  status: number;
  code: string;
  message: string;
  fieldErrors?: { field: string; message: string }[];
}
