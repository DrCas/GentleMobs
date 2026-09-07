import { Entity } from "@minecraft/server";
import { movementFor } from "./policy";

/** Native navigation handles walkers/swimmers; flight receives bounded steering. */
export function steerFlight(entity: Entity, attacker: Entity) {
  if (movementFor(entity.typeId) !== "fly" || !attacker.isValid || entity.dimension.id !== attacker.dimension.id) return;
  const from = attacker.location, at = entity.location;
  const dx = at.x - from.x, dz = at.z - from.z;
  const length = Math.hypot(dx, dz);
  if (length >= 12) return;
  const velocity = entity.getVelocity();
  if (Math.hypot(velocity.x, velocity.y, velocity.z) > 0.4) return;
  entity.applyImpulse({ x: length > 0.01 ? dx / length * 0.08 : 0.08,
    y: at.y < from.y + 4 ? 0.025 : 0, z: length > 0.01 ? dz / length * 0.08 : 0 });
}

/** Shulkers cannot walk. Try a nearby clear space above a known full support block. */
export function retreatShulker(entity: Entity, attacker: Entity) {
  if (movementFor(entity.typeId) !== "teleport") return;
  const support = new Set(["minecraft:end_stone", "minecraft:purpur_block", "minecraft:purpur_pillar", "minecraft:stone", "minecraft:grass_block", "minecraft:dirt", "minecraft:deepslate", "minecraft:netherrack"]);
  const at = entity.location, from = attacker.location;
  const original = Math.hypot(at.x - from.x, at.z - from.z);
  for (const distance of [10, 8, 6, 4]) for (let angle = 0; angle < 8; angle++) {
    const x = Math.floor(at.x + Math.cos(angle * Math.PI / 4) * distance);
    const z = Math.floor(at.z + Math.sin(angle * Math.PI / 4) * distance);
    if (Math.hypot(x + 0.5 - from.x, z + 0.5 - from.z) < original + 2) continue;
    for (const dy of [0, 1, -1, 2, -2]) {
      const y = Math.floor(at.y) + dy;
      const below = entity.dimension.getBlock({ x, y: y - 1, z });
      if (!below || !support.has(below.typeId)) continue;
      if (!entity.dimension.getBlock({ x, y, z })?.isAir || !entity.dimension.getBlock({ x, y: y + 1, z })?.isAir) continue;
      if (entity.tryTeleport({ x: x + 0.5, y, z: z + 0.5 }, { checkForBlocks: true })) return;
    }
  }
}
