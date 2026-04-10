export type ReactionType = 'like' | 'dislike';

export interface ReactionResponse {
  likesCount: number;
  deslikesCount: number;
  interactedAt?: string;
}
