import { Entity, EntityComponentTypes, Player, system, world } from "@minecraft/server";
import { changeConfig, Config, effectiveMode, hitResponse, parseConfig, ZOMBIE } from "./policy";

const CONFIG_KEY = "gentlemobs:config";
let config: Config;
let ready = false;
// Only temporary combat state belongs in memory. Configuration persists in the world.
const active = new Map<string, { entity: Entity; expires: number }>();

function report(error: unknown) {
  console.warn(`[GentleMobs] ${error instanceof Error ? error.message : String(error)}`);
}

function initialize(entity: Entity) {
  if (!ready || !entity.isValid || entity.typeId !== ZOMBIE) return;
  active.delete(entity.id);
  entity.triggerEvent(`gentlemobs:${effectiveMode(config, entity.typeId)!.toLowerCase()}`);
}

function forEachZombie(action: (entity: Entity) => void) {
  for (const dimension of ["overworld", "nether", "the_end"]) {
    for (const entity of world.getDimension(dimension).getEntities({ type: ZOMBIE })) {
      if (entity.isValid) action(entity);
    }
  }
}

world.afterEvents.worldLoad.subscribe(() => {
  try {
    config = parseConfig(world.getDynamicProperty(CONFIG_KEY));
    ready = true;
    forEachZombie(initialize);
    console.warn(`[GentleMobs] Zombie prototype loaded. Global mode: ${config.mode}.`);
  } catch (error) { report(error); }
});

world.afterEvents.entitySpawn.subscribe(({ entity }) => {
  try { initialize(entity); } catch (error) { report(error); }
});
world.afterEvents.entityLoad.subscribe(({ entity }) => {
  try { initialize(entity); } catch (error) { report(error); }
});
world.afterEvents.entityRemove.subscribe(({ removedEntityId }) => active.delete(removedEntityId));

world.afterEvents.entityHurt.subscribe(({ hurtEntity, damageSource }) => {
  if (!ready || hurtEntity.typeId !== ZOMBIE || !hurtEntity.isValid) return;
  try {
    const projectile = damageSource.damagingProjectile;
    const attacker = damageSource.damagingEntity ??
      projectile?.getComponent(EntityComponentTypes.Projectile)?.owner;
    if (attacker?.typeId !== "minecraft:player") return;
    const response = hitResponse(effectiveMode(config, hurtEntity.typeId)!, system.currentTick);
    if (!response) return;
    hurtEntity.triggerEvent(response.event);
    active.set(hurtEntity.id, { entity: hurtEntity, expires: response.expires });
  } catch (error) { report(error); }
});

system.runInterval(() => {
  for (const [id, state] of active) {
    if (!state.entity.isValid) { active.delete(id); continue; }
    if (system.currentTick < state.expires) continue;
    active.delete(id);
    try { state.entity.triggerEvent("gentlemobs:calm"); } catch (error) { report(error); }
  }
}, 1);

system.afterEvents.scriptEventReceive.subscribe(event => {
  if (!["gentlemobs:mode", "gentlemobs:override", "gentlemobs:status"].includes(event.id)) return;
  const reply = (message: string) => {
    if (event.sourceEntity instanceof Player) event.sourceEntity.sendMessage(`[GentleMobs] ${message}`);
    else console.warn(`[GentleMobs] ${message}`);
  };
  try {
    if (!ready) throw new Error("Initialization failed or has not finished; check the content log.");
    if (event.id !== "gentlemobs:status") {
      const next = changeConfig(config, event.id, event.message);
      world.setDynamicProperty(CONFIG_KEY, JSON.stringify(next));
      config = next;
      active.clear();
      forEachZombie(initialize);
    }
    reply(`Global: ${config.mode}; zombie override: ${config.overrides[ZOMBIE] ?? "none"}; effective: ${effectiveMode(config, ZOMBIE)}.`);
  } catch (error) { reply(error instanceof Error ? error.message : String(error)); }
});
