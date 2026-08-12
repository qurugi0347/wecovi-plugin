import { excludedTarget } from "./excluded.test";

/** @covi documented target */
export function documentedTarget(): void {}

export function plainTarget(): void {}

export function constructTarget(value: string): void {}

export function root(): void {
  documentedTarget();
  plainTarget();
  new constructTarget("value");
  excludedTarget();
  Array.isArray([]);
  unknownTarget();
}
