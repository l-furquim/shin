export interface AuthResponse {
  token: string;
  deviceId: string;
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

export interface CreatorSignUpRequest {
  displayName: string;
  email: string;
  password: string;
  showAdultContent: boolean;
  username: string;
  description: string;
}

export interface CreatorSignUpResponse {
  id: string;
  displayName: string;
  email: string;
  username: string;
  channelUrl: string;
  avatar: string;
  banner: string;
  languageTag: string;
  showAdultContent: boolean;
  createdAt: string;
}

export interface UserAuthProfile {
  id: string;
  email: string;
  encryptedPassword: string;
}

export interface MeResponse {
  id: string;
  displayName: string;
  username: string;
  email: string;
  showAdultContent: boolean;
  locale: string;
  description: string;
  channelUrl: string;
  avatar: string;
  banner: string;
  subscribersCount: number;
  languageTag: string;
  createdAt: string;
  lastUpdate: string;
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
