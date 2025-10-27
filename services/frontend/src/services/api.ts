import axios, { AxiosError } from 'axios';
import type { PackingRequest, PackingResponse, SessionResponse, ApiError } from '../types';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

// Session management
class SessionManager {
  private static SESSION_TOKEN_KEY = 'session_token';
  private static SESSION_ID_KEY = 'session_id';

  static getToken(): string | null {
    return localStorage.getItem(this.SESSION_TOKEN_KEY);
  }

  static setSession(token: string, sessionId: string): void {
    localStorage.setItem(this.SESSION_TOKEN_KEY, token);
    localStorage.setItem(this.SESSION_ID_KEY, sessionId);
  }

  static clearSession(): void {
    localStorage.removeItem(this.SESSION_TOKEN_KEY);
    localStorage.removeItem(this.SESSION_ID_KEY);
  }

  static hasValidSession(): boolean {
    return this.getToken() !== null;
  }
}

// API Client
class ApiClient {
  private axiosInstance = axios.create({
    baseURL: API_BASE_URL,
    headers: {
      'Content-Type': 'application/json',
    },
  });

  constructor() {
    // Add session token to all requests
    this.axiosInstance.interceptors.request.use((config) => {
      const token = SessionManager.getToken();
      if (token) {
        config.headers['X-Session-Token'] = token;
      }
      return config;
    });

    // Handle session errors by creating new session
    this.axiosInstance.interceptors.response.use(
      (response) => response,
      async (error: AxiosError) => {
        const errorMessage = (error.response?.data as any)?.message || '';
        const isSessionError =
          error.response?.status === 401 ||
          error.response?.status === 404 ||
          errorMessage.includes('Session not found') ||
          errorMessage.includes('session');

        if (isSessionError) {
          console.log('Session error detected, creating new session...');
          SessionManager.clearSession();
          // Try to create new session and retry
          try {
            await this.createSession();
            // Retry original request
            if (error.config) {
              return this.axiosInstance.request(error.config);
            }
          } catch (sessionError) {
            console.error('Failed to create new session:', sessionError);
            throw this.handleError(sessionError);
          }
        }
        throw this.handleError(error);
      }
    );
  }

  private handleError(error: unknown): ApiError {
    if (axios.isAxiosError(error)) {
      const message = error.response?.data?.message || error.message || 'An unexpected error occurred';
      return {
        message,
        status: error.response?.status,
      };
    }
    return {
      message: 'An unexpected error occurred',
    };
  }

  // Session endpoints
  async createSession(): Promise<SessionResponse> {
    const response = await this.axiosInstance.post<SessionResponse>('/api/sessions');
    SessionManager.setSession(response.data.sessionToken, response.data.sessionId);
    return response.data;
  }

  async ensureSession(): Promise<void> {
    if (!SessionManager.hasValidSession()) {
      await this.createSession();
    }
  }

  // Packing list endpoints
  async generatePackingList(request: PackingRequest): Promise<PackingResponse> {
    await this.ensureSession();
    const response = await this.axiosInstance.post<PackingResponse>('/api/packing/generate', request);
    return response.data;
  }

  async getPackingList(id: string): Promise<PackingResponse> {
    await this.ensureSession();
    const response = await this.axiosInstance.get<PackingResponse>(`/api/packing/${id}`);
    return response.data;
  }

  async getRecentLists(limit: number = 10): Promise<PackingResponse[]> {
    await this.ensureSession();
    const response = await this.axiosInstance.get<PackingResponse[]>('/api/packing/session/recent', {
      params: { limit },
    });
    return response.data;
  }

  async searchByDestination(destination: string): Promise<PackingResponse[]> {
    await this.ensureSession();
    const response = await this.axiosInstance.get<PackingResponse[]>('/api/packing/search', {
      params: { destination },
    });
    return response.data;
  }

  // Health check
  async healthCheck(): Promise<boolean> {
    try {
      const response = await this.axiosInstance.get('/api/packing/health');
      return response.data.status === 'UP';
    } catch {
      return false;
    }
  }
}

// Export singleton instance
export const apiClient = new ApiClient();
export { SessionManager };
