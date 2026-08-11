export function loadDto(): string {
  return "dto";
}

export function inner(): string {
  return "inner";
}

export function outer(value: string): void {}

export class User {
  constructor(value: string) {}
}

export function save(user: User): string {
  return "saved";
}

export function syncFlow(): string {
  const dto = loadDto();
  const user = new User(dto);
  return save(user);
}

export async function asyncFlow(): Promise<void> {
  await save(new User(loadDto()));
  await ready;
}

export function nestedFlow(): void {
  outer(inner());
  new User(loadDto());
  if (ready) save(new User("conditional"));
  ["callback"].map(value => save(new User(value)));
}

declare const ready: Promise<void>;
