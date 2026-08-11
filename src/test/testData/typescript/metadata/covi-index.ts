/**
 * 사용자 API 가입
 * @covi-root
 * @covi-group User API/User
 */
export async function signup(email: string): Promise<void> {
  return undefined;
}

/**
 * @covi 사용자를 생성한다
 * @covi-group User API/User
 */
export const createUser = (email: string): string => email;

/** @covi 이메일을 검증한다 */
export function validateEmail(email: string): boolean {
  return email.includes("@");
}

/**
 * 관리자 등록
 * @covi-root
 * @covi-group Admin
 */
export function registerAdmin(): void {}

/** @covi-group User API/User */
export function undocumentedHelper(): void {}
