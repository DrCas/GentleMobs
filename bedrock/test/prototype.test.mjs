import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { build, transform } from "esbuild";
import { parse } from "jsonc-parser";
import { runInNewContext } from "node:vm";
import { gentleZombie } from "../tools/zombie.mjs";

const source = await readFile(new URL("../src/policy.ts", import.meta.url), "utf8");
const { code } = await transform(source, { loader: "ts", format: "esm" });
const policy = await import(`data:text/javascript;base64,${Buffer.from(code).toString("base64")}`);
const vanilla = parse(await readFile(new URL("../vendor/zombie.jsonc", import.meta.url), "utf8"));
const entity = gentleZombie(vanilla)["minecraft:entity"];

test("config defaults, overrides and unsupported mobs", () => {
  const config = policy.parseConfig(undefined);
  assert.equal(policy.effectiveMode(config, "minecraft:zombie"), "PASSIVE");
  for (const id of ["minecraft:skeleton", "minecraft:drowned", "cobblemon:pokemon", "other:zombie"]) {
    assert.equal(policy.effectiveMode(config, id), undefined);
  }
  const changed = policy.changeConfig(config, "gentlemobs:override", "minecraft:zombie neutral");
  assert.equal(policy.effectiveMode(changed, "minecraft:zombie"), "NEUTRAL");
  assert.deepEqual(policy.parseConfig(JSON.stringify(changed)), changed);
  assert.deepEqual(policy.changeConfig(changed, "gentlemobs:override", "zombie clear"), config);
  assert.throws(() => policy.changeConfig(config, "gentlemobs:override", "other:zombie VANILLA"));
  assert.throws(() => policy.changeConfig(config, "gentlemobs:mode", "PASSIVE extra"));
  assert.throws(() => policy.parseConfig('{"mode":"oops","overrides":{}}'));
  assert.equal(config.mode, "PASSIVE");
});

test("vanilla spawning, loot, transformations and all unrelated behavior remain intact", () => {
  const restored = structuredClone(entity);
  for (const section of ["component_groups", "events"]) {
    for (const key of Object.keys(restored[section])) if (key.startsWith("gentlemobs:")) delete restored[section][key];
  }
  for (const key of Object.keys(restored.description.properties)) {
    if (key.startsWith("gentlemobs:")) delete restored.description.properties[key];
  }
  for (const name of ["minecraft:behavior.nearest_attackable_target", "minecraft:behavior.hurt_by_target"]) {
    for (const target of restored.components[name].entity_types) {
      target.filters = target.filters.all_of[0];
      delete target.reevaluate_description;
    }
  }
  assert.deepEqual(restored, vanilla["minecraft:entity"]);
  assert.equal(entity.component_groups["gentlemobs:fleeing"]["minecraft:timer"], undefined);
});

function matches(filter, family, mode, engaged) {
  if (filter.any_of) return filter.any_of.some(f => matches(f, family, mode, engaged));
  if (filter.all_of) return filter.all_of.every(f => matches(f, family, mode, engaged));
  let result;
  if (filter.test === "is_family") result = family === filter.value;
  else if (filter.test === "enum_property") result = mode === filter.value;
  else if (filter.test === "bool_property") result = engaged === filter.value;
  else if (filter.test === "in_water") result = false === filter.value;
  else throw new Error(`Unhandled filter ${filter.test}`);
  return filter.operator === "not" ? !result : result;
}

test("player targeting permission and the vanilla Breeze exception", () => {
  const nearest = entity.components["minecraft:behavior.nearest_attackable_target"].entity_types[0].filters;
  const hurt = entity.components["minecraft:behavior.hurt_by_target"].entity_types[0].filters;
  for (const filter of [nearest, hurt]) {
    assert.equal(matches(filter, "player", "passive", false), false);
    assert.equal(matches(filter, "player", "passive", true), false);
    assert.equal(matches(filter, "player", "neutral", false), false);
    assert.equal(matches(filter, "player", "neutral", true), true);
    assert.equal(matches(filter, "player", "vanilla", false), true);
    assert.equal(matches(filter, "irongolem", "passive", false), true);
  }
  assert.equal(matches(hurt, "breeze", "vanilla", false), false);
});

const runtime = await build({
  entryPoints: [new URL("../src/main.ts", import.meta.url).pathname.replace(/^\/(\w:)/, "$1")],
  bundle: true, write: false, format: "iife", platform: "neutral",
  plugins: [{ name: "test-server", setup(builder) {
    builder.onResolve({ filter: /^@minecraft\/server$/ }, () => ({ path: "server", namespace: "test" }));
    builder.onLoad({ filter: /.*/, namespace: "test" }, () => ({ contents:
      "export const { Entity, Player, EntityComponentTypes, system, world } = globalThis.server;" }));
  } }]
});

function harness(saved) {
  function signal() {
    const handlers = [];
    return { subscribe(fn) { handlers.push(fn); }, emit(value = {}) { for (const fn of handlers) fn(value); } };
  }
  class Entity {
    constructor(typeId = "minecraft:zombie") { this.typeId = typeId; this.id = String(++Entity.next); }
    static next = 0;
    isValid = true;
    events = [];
    triggerEvent(event) { this.events.push(event); }
  }
  class Player extends Entity { constructor() { super("minecraft:player"); } sendMessage() {} }
  const entities = [];
  const properties = new Map(saved === undefined ? [] : [["gentlemobs:config", saved]]);
  const world = {
    afterEvents: Object.fromEntries(["worldLoad", "entitySpawn", "entityLoad", "entityRemove", "entityHurt"].map(n => [n, signal()])),
    getDynamicProperty: key => properties.get(key), setDynamicProperty: (key, value) => properties.set(key, value),
    getDimension: name => ({ getEntities: ({ type }) => name === "overworld" ? entities.filter(e => e.typeId === type) : [] })
  };
  let interval;
  const system = { currentTick: 0, afterEvents: { scriptEventReceive: signal() }, runInterval(fn) { interval = fn; } };
  runInNewContext(runtime.outputFiles[0].text, { server: { Entity, Player, world, system, EntityComponentTypes: { Projectile: "minecraft:projectile" } }, console: { warn() {} } });
  const zombie = new Entity(); entities.push(zombie);
  world.afterEvents.worldLoad.emit();
  return { zombie, entities, properties, world, system, Entity,
    hit(entity = zombie, attacker = new Player()) { world.afterEvents.entityHurt.emit({ hurtEntity: entity, damageSource: { damagingEntity: attacker } }); },
    tick(value) { system.currentTick = value; interval(); },
    command(id, message) { system.afterEvents.scriptEventReceive.emit({ id: `gentlemobs:${id}`, message }); }
  };
}

test("runtime passive hits flee for 60 ticks, repeated hits extend the timer", () => {
  const h = harness();
  h.hit(); h.tick(59);
  assert.equal(h.zombie.events.at(-1), "gentlemobs:flee");
  h.hit(); h.tick(60);
  assert.equal(h.zombie.events.at(-1), "gentlemobs:flee");
  h.tick(119);
  assert.equal(h.zombie.events.at(-1), "gentlemobs:calm");
});

test("runtime neutral expires, vanilla does not intercept combat, config persists", () => {
  const h = harness();
  h.command("mode", "NEUTRAL"); h.hit(); h.tick(599);
  assert.equal(h.zombie.events.at(-1), "gentlemobs:engage");
  h.tick(600); assert.equal(h.zombie.events.at(-1), "gentlemobs:calm");
  h.command("override", "zombie VANILLA");
  const count = h.zombie.events.length; h.hit(); h.tick(1500);
  assert.equal(h.zombie.events.length, count);
  const reloaded = harness(h.properties.get("gentlemobs:config"));
  assert.equal(reloaded.zombie.events.at(-1), "gentlemobs:vanilla");
});

test("runtime excludes other entities and non-player damage; mode changes clear timers", () => {
  const h = harness();
  const other = new h.Entity("other:zombie");
  h.world.afterEvents.entitySpawn.emit({ entity: other }); h.hit(other);
  assert.deepEqual(other.events, []);
  const count = h.zombie.events.length;
  h.hit(h.zombie, new h.Entity("minecraft:skeleton"));
  assert.equal(h.zombie.events.length, count);
  h.hit(); h.command("mode", "VANILLA"); h.tick(1000);
  assert.equal(h.zombie.events.at(-1), "gentlemobs:vanilla");
});

test("runtime load resets temporary combat; removed entities are forgotten", () => {
  const h = harness(); h.hit();
  h.world.afterEvents.entityRemove.emit({ removedEntityId: h.zombie.id });
  const count = h.zombie.events.length; h.tick(100);
  assert.equal(h.zombie.events.length, count);
  h.world.afterEvents.entityLoad.emit({ entity: h.zombie });
  assert.equal(h.zombie.events.at(-1), "gentlemobs:passive");
});

test("runtime credits projectiles to their player owner", () => {
  const h = harness();
  h.world.afterEvents.entityHurt.emit({ hurtEntity: h.zombie,
    damageSource: { damagingProjectile: { getComponent: () => ({ owner: { typeId: "minecraft:player" } }) } } });
  assert.equal(h.zombie.events.at(-1), "gentlemobs:flee");
});
