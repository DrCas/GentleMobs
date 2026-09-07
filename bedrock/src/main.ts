import { Entity, EntityComponentTypes, Player, system, world } from "@minecraft/server";
import { changeConfig, Config, effectiveMode, hitResponse, isSupported, mobId, parseConfig, SUPPORTED } from "./policy";
import { retreatShulker, steerFlight } from "./flee";

const CONFIG_KEY = "gentlemobs:config";
let config: Config;
let ready = false;
// Only temporary combat state belongs in memory. Configuration persists in the world.
const active = new Map<string, { entity: Entity; expires: number; attacker: Entity; fleeing: boolean }>();

function report(error: unknown) {
  console.warn(`[GentleMobs] ${error instanceof Error ? error.message : String(error)}`);
}

function initialize(entity: Entity) {
  if (!ready || !entity.isValid || !isSupported(entity.typeId)) return;
  active.delete(entity.id);
  entity.triggerEvent(`gentlemobs:${effectiveMode(config, entity.typeId)!.toLowerCase()}`);
}

function forEachMob(action: (entity: Entity) => void) {
  for (const dimension of ["overworld", "nether", "the_end"]) {
    for (const type of SUPPORTED) for (const entity of world.getDimension(dimension).getEntities({ type })) {
      if (entity.isValid) { try { action(entity); } catch (error) { report(error); } }
    }
  }
}

world.afterEvents.worldLoad.subscribe(() => {
  try {
    config = parseConfig(world.getDynamicProperty(CONFIG_KEY));
    ready = true;
    forEachMob(initialize);
    console.warn(`[GentleMobs] Mob preview loaded (${SUPPORTED.length} entity types). Global mode: ${config.mode}.`);
  } catch (error) { report(error); }
});

world.afterEvents.entitySpawn.subscribe(({ entity }) => {
  try { initialize(entity); } catch (error) { report(error); }
});
world.afterEvents.entityLoad.subscribe(({ entity }) => {
  try { initialize(entity); } catch (error) { report(error); }
});
world.afterEvents.entityRemove.subscribe(({ removedEntityId }) => active.delete(removedEntityId));

function attacked(hurtEntity: Entity, attacker?: Entity) {
  if (!ready || !isSupported(hurtEntity.typeId) || !hurtEntity.isValid || attacker?.typeId !== "minecraft:player") return;
  try {
    const response = hitResponse(effectiveMode(config, hurtEntity.typeId)!, system.currentTick);
    if (!response) return;
    hurtEntity.triggerEvent(response.event);
    const fleeing = response.event === "gentlemobs:flee";
    active.set(hurtEntity.id, { entity: hurtEntity, expires: response.expires, attacker, fleeing });
    if (fleeing) retreatShulker(hurtEntity, attacker);
  } catch (error) { report(error); }
}
world.afterEvents.entityHurt.subscribe(({ hurtEntity, damageSource }) => {
  if (!isSupported(hurtEntity.typeId)) return;
  try {
    const direct = damageSource.damagingEntity;
    const attacker = direct?.typeId === "minecraft:player" ? direct :
      damageSource.damagingProjectile?.getComponent(EntityComponentTypes.Projectile)?.owner ?? direct;
    attacked(hurtEntity, attacker);
  } catch (error) { report(error); }
});
// Heart-bound Creakings can register a melee hit without taking health damage.
world.afterEvents.entityHitEntity.subscribe(({ hitEntity, damagingEntity }) => {
  if (hitEntity.typeId === "minecraft:creaking") attacked(hitEntity, damagingEntity);
});

system.runInterval(() => {
  for (const [id, state] of active) {
    if (!state.entity.isValid) { active.delete(id); continue; }
    if (system.currentTick < state.expires) {
      if (state.fleeing) { try { steerFlight(state.entity, state.attacker); } catch (error) { report(error); } }
      continue;
    }
    active.delete(id);
    try { state.entity.triggerEvent("gentlemobs:calm"); } catch (error) { report(error); }
  }
}, 1);

system.afterEvents.scriptEventReceive.subscribe(event => {
  if (!["gentlemobs:mode", "gentlemobs:override", "gentlemobs:status", "gentlemobs:list"].includes(event.id)) return;
  const reply = (message: string) => {
    if (event.sourceEntity instanceof Player) event.sourceEntity.sendMessage(`[GentleMobs] ${message}`);
    else console.warn(`[GentleMobs] ${message}`);
  };
  try {
    if (!ready) throw new Error("Initialization failed or has not finished; check the content log.");
    if (event.id === "gentlemobs:list") { reply(SUPPORTED.join(", ")); return; }
    if (event.id !== "gentlemobs:status") {
      const next = changeConfig(config, event.id, event.message);
      world.setDynamicProperty(CONFIG_KEY, JSON.stringify(next));
      config = next;
      active.clear();
      forEachMob(initialize);
    }
    if (event.id === "gentlemobs:status" && event.message.trim()) {
      const id = mobId(event.message.trim());
      reply(`${id}: override ${config.overrides[id] ?? "none"}; effective ${effectiveMode(config, id)}.`);
    } else reply(`Global: ${config.mode}; overrides: ${Object.entries(config.overrides).map(([id, mode]) => `${id}=${mode}`).join(", ") || "none"}. Creaking defaults to VANILLA.`);
  } catch (error) { reply(error instanceof Error ? error.message : String(error)); }
});
