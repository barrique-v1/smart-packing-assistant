// Enums
export type TravelType = 'BUSINESS' | 'VACATION' | 'BACKPACKING';
export type Season = 'SPRING' | 'SUMMER' | 'FALL' | 'WINTER';

// Request types
export interface PackingRequest {
  destination: string;
  durationDays: number;
  travelType: TravelType;
  season: Season;
}

// Response types
export interface PackingItem {
  item: string;
  quantity: number;
  reason: string;
}

export interface PackingCategories {
  clothing: PackingItem[];
  tech: PackingItem[];
  hygiene: PackingItem[];
  documents: PackingItem[];
  other: PackingItem[];
}

export interface WeatherInfo {
  tempMin: number;
  tempMax: number;
  conditions: string;
}

export interface PackingResponse {
  id: string;
  destination: string;
  categories: PackingCategories;
  weatherInfo: WeatherInfo | null;
  cultureTips: string[];
  specialNotes: string | null;
}

// Session types
export interface SessionResponse {
  sessionToken: string;
  sessionId: string;
}

// API Error types
export interface ApiError {
  message: string;
  status?: number;
}
