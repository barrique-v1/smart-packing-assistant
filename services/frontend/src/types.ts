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
  name: string;
  quantity: number;
  category: string;
}

export interface WeatherInfo {
  tempMin: number;
  tempMax: number;
  conditions: string;
}

export interface PackingResponse {
  id: string;
  destination: string;
  durationDays: number;
  travelType: TravelType;
  season: Season;
  items: PackingItem[];
  weatherInfo: WeatherInfo;
  cultureTips: string[];
  createdAt: string;
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
