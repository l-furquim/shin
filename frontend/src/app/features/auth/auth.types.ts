export interface AuthResponse {
  token: string;
  expiresIn: number;
}

export interface AuthRequest {
  email: string;
  password: string;
  deviceId: string | null;
}

export interface RegisterRequest {
  email: string;
  password: string;
  displayName: string;
  showAdultContent: boolean;
}

export interface RegisterResponse {
  id: string;
  displayName: string;
  email: string;
  showAdultContent: boolean;
  locale: string;
  avatar: string;
  banner: string;
  updatedAt: Date;
  createdAt: Date;
}
