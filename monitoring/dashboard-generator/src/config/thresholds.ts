import {
  ThresholdsConfigBuilder,
  ThresholdsMode,
  type Threshold,
} from "@grafana/grafana-foundation-sdk/dashboard";

export const COLORS = {
  green: "green",
  yellow: "yellow",
  red: "red",
  blue: "blue",
  text: "text",
} as const;

function absoluteThresholds(steps: Threshold[]): ThresholdsConfigBuilder {
  return new ThresholdsConfigBuilder().mode(ThresholdsMode.Absolute).steps(steps);
}

/** Base green, then yellow at `yellowAt`, red at `redAt`. */
export function percentThresholds(yellowAt: number, redAt: number): ThresholdsConfigBuilder {
  return absoluteThresholds([
    { value: null, color: COLORS.green },
    { value: yellowAt, color: COLORS.yellow },
    { value: redAt, color: COLORS.red },
  ]);
}

export const platformHealthThresholds = (): ThresholdsConfigBuilder =>
  absoluteThresholds([
    { value: null, color: COLORS.green },
    { value: 1, color: COLORS.yellow },
    { value: 2, color: COLORS.red },
  ]);

export const criticalAlertsThresholds = (): ThresholdsConfigBuilder =>
  absoluteThresholds([
    { value: null, color: COLORS.green },
    { value: 1, color: COLORS.red },
  ]);

export const warningAlertsThresholds = (): ThresholdsConfigBuilder =>
  absoluteThresholds([
    { value: null, color: COLORS.green },
    { value: 1, color: COLORS.yellow },
  ]);

export const cpuThresholds = (): ThresholdsConfigBuilder => percentThresholds(70, 90);
export const memoryThresholds = (): ThresholdsConfigBuilder => percentThresholds(75, 90);
export const filesystemThresholds = (): ThresholdsConfigBuilder => percentThresholds(80, 90);
