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

/** @covi 그룹 없는 보조 함수 */
export function archiveUser(): void {}

/** @covi 같은 이름의 첫 번째 함수 */
function duplicateUser(): void {}

/** @covi 같은 이름의 두 번째 함수 */
function duplicateUser(): void {}

/**
 * 관리자 등록
 * @covi-root
 * @covi-group Admin
 */
export function registerAdmin(): void {}

/** @covi-group User API/User */
export function undocumentedHelper(): void {}
