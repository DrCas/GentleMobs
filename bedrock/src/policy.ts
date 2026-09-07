export const MODES = ["PASSIVE", "NEUTRAL", "VANILLA"] as const;
export type Mode = typeof MODES[number];
export const ZOMBIE = "minecraft:zombie";
export const FLEE_TICKS = 60;
export const NEUTRAL_TICKS = 600;

export interface Config {
  mode: Mode;
  overrides: Partial<Record<typeof ZOMBIE, Mode>>;
}

export function parseMode(value: unknown): Mode {
  if (typeof value === "string") {
    const mode = value.toUpperCase();
    if (MODES.includes(mode as Mode)) return mode as Mode;
  }
  throw new Error("Mode must be PASSIVE, NEUTRAL or VANILLA.");
}

export function parseConfig(raw: unknown): Config {
  if (raw === undefined) return { mode: "PASSIVE", overrides: {} };
  if (typeof raw !== "string") throw new Error("Saved configuration is not text.");
  const value = JSON.parse(raw);
  if (!value || typeof value !== "object" || !value.overrides ||
      typeof value.overrides !== "object" || Array.isArray(value.overrides)) {
    throw new Error("Invalid saved configuration.");
  }
  const overrides: Config["overrides"] = {};
  for (const [id, mode] of Object.entries(value.overrides)) {
    if (id !== ZOMBIE) throw new Error(`Unsupported mob: ${id}`);
    overrides[id] = parseMode(mode);
  }
  return { mode: parseMode(value.mode), overrides };
}

export function effectiveMode(config: Config, type: string): Mode | undefined {
  return type === ZOMBIE ? config.overrides[ZOMBIE] ?? config.mode : undefined;
}

export function changeConfig(config: Config, command: string, message: string): Config {
  const parts = message.trim().split(/\s+/);
  if (command === "gentlemobs:mode" && parts.length === 1) {
    return { ...config, mode: parseMode(parts[0]) };
  }
  if (command === "gentlemobs:override" && parts.length === 2) {
    const id = parts[0].toLowerCase();
    if (id !== ZOMBIE && id !== "zombie") throw new Error("This prototype supports minecraft:zombie only.");
    const overrides = { ...config.overrides };
    if (parts[1].toUpperCase() === "CLEAR") delete overrides[ZOMBIE];
    else overrides[ZOMBIE] = parseMode(parts[1]);
    return { ...config, overrides };
  }
  throw new Error("Use /scriptevent gentlemobs:mode PASSIVE or gentlemobs:override minecraft:zombie NEUTRAL|CLEAR.");
}

export function hitResponse(mode: Mode, tick: number) {
  if (mode === "PASSIVE") return { event: "gentlemobs:flee", expires: tick + FLEE_TICKS };
  if (mode === "NEUTRAL") return { event: "gentlemobs:engage", expires: tick + NEUTRAL_TICKS };
  return undefined;
}
