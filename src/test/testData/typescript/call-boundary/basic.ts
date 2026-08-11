/** @covi documented target */
export function documentedTarget(): void {}

export function plainTarget(): void {}

export function root(): void {
  documentedTarget();
  plainTarget();
  Array.isArray([]);
  unknownTarget();
}
