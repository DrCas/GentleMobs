import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { parse } from "jsonc-parser";
import { gentleMob, TARGET_GOALS } from "../tools/mobs.mjs";

const mobs = JSON.parse(await readFile(new URL("../mobs.json", import.meta.url), "utf8"));
const definitions = {};
for (const [name, movement] of Object.entries(mobs)) {
  const errors = [];
  const baseline = parse(await readFile(new URL(`../vendor/${name}.jsonc`, import.meta.url), "utf8"), errors);
  assert.deepEqual(errors, [], name);
  const original = baseline["minecraft:entity"];
  const generated = gentleMob(baseline, movement)["minecraft:entity"];
  definitions[name] = generated;
  test(`${name}: retains unrelated vanilla components, events and lifecycle`, () => {
    const restored = structuredClone(generated);
    for (const key of Object.keys(restored.description.properties)) if (key.startsWith("gentlemobs:")) delete restored.description.properties[key];
    if (!original.description.properties) delete restored.description.properties;
    for (const key of Object.keys(restored.events)) if (key.startsWith("gentlemobs:")) delete restored.events[key];
    if (name === "creeper") delete restored.events["minecraft:start_exploding"].filters;
    for (const [group, c] of [[null, restored.components], ...Object.entries(restored.component_groups)]) {
      const old = group === null ? original.components : original.component_groups[group];
      for (const key of Object.keys(c)) if (!(key in old)) delete c[key];
      for (const key of TARGET_GOALS) if (old[key]) {
        // Verify all non-filter parameters too; account for vanilla single objects.
        const entries = c[key].entity_types;
        const oldEntries = old[key].entity_types === undefined ? [{}] : Array.isArray(old[key].entity_types) ? old[key].entity_types : [old[key].entity_types];
        assert.equal(entries.length, oldEntries.length);
        entries.forEach((entry, i) => {
          assert.ok(entry.filters);
          if (oldEntries[i].filters) entry.filters = entry.filters.all_of[0];
          else delete entry.filters;
          if (oldEntries[i].reevaluate_description === undefined) delete entry.reevaluate_description;
          else entry.reevaluate_description = oldEntries[i].reevaluate_description;
          assert.deepEqual(entry, oldEntries[i]);
        });
        if (old[key].entity_types === undefined) delete c[key].entity_types;
        else if (!Array.isArray(old[key].entity_types)) c[key].entity_types = entries[0];
      }
      const fieldChanges = {
        "minecraft:area_attack": ["entity_filter"], "minecraft:angry": ["filters"],
        "minecraft:anger_level": ["nuisance_filter"],
        "minecraft:behavior.knockback_roar": ["damage_filters", "knockback_filters"]
      };
      if (old["minecraft:looked_at"]?.set_target !== "never") fieldChanges["minecraft:looked_at"] = ["filters"];
      for (const [key, fields] of Object.entries(fieldChanges)) if (old[key]) for (const field of fields) {
        if (old[key][field]) c[key][field] = c[key][field].all_of[0];
        else delete c[key][field];
      }
      if (old["minecraft:environment_sensor"]) {
        const sensor = c["minecraft:environment_sensor"];
        const added = sensor.triggers.pop();
        assert.equal(added.event, "gentlemobs:drop_player_target");
        const before = old["minecraft:environment_sensor"].triggers;
        if (before === undefined) delete sensor.triggers;
        else if (!Array.isArray(before)) sensor.triggers = sensor.triggers[0];
      }
      if (group === null && old["minecraft:behavior.avoid_mob_type"] && ["walk", "swim"].includes(movement)) {
        const avoid = c["minecraft:behavior.avoid_mob_type"];
        avoid.entity_types.pop();
        const before = old["minecraft:behavior.avoid_mob_type"].entity_types;
        if (before === undefined) delete avoid.entity_types;
        else if (!Array.isArray(before)) avoid.entity_types = avoid.entity_types[0];
      }
    }
    if (!original.component_groups) delete restored.component_groups;
    if (original.events === null) restored.events = null;
    else if (original.events === undefined) delete restored.events;
    assert.deepEqual(restored, original);
  });
}

function accepts(filter, mode, engaged, family = "player") {
  if (filter.all_of || filter.AND) return (filter.all_of ?? filter.AND).every(f => accepts(f, mode, engaged, family));
  if (filter.any_of || filter.OR) return (filter.any_of ?? filter.OR).some(f => accepts(f, mode, engaged, family));
  if (filter.none_of) return !filter.none_of.some(f => accepts(f, mode, engaged, family));
  let answer;
  switch (filter.test) {
    case "is_family": answer = filter.value === family; break;
    case "enum_property": answer = filter.value === mode; break;
    case "bool_property": answer = filter.value === engaged; break;
    default: return true; // Original world/equipment predicates are tested by Minecraft.
  }
  return ["not", "!=", 1].includes(filter.operator) ? !answer : answer;
}

test("all native target paths and contact attacks reject calm player targets", () => {
  for (const [name, e] of Object.entries(definitions)) {
    for (const c of [e.components, ...Object.values(e.component_groups)]) {
      for (const key of TARGET_GOALS) for (const entry of c[key]?.entity_types ?? []) {
        assert.equal(accepts(entry.filters, "passive", false), false, `${name} ${key} passive`);
        assert.equal(accepts(entry.filters, "neutral", false), false, `${name} ${key} neutral`);
      }
      const area = c["minecraft:area_attack"];
      if (area) {
        assert.equal(accepts(area.entity_filter, "passive", false), false);
        assert.equal(accepts(area.entity_filter, "neutral", true), true);
        assert.equal(accepts(area.entity_filter, "vanilla", false), true);
      }
    }
  }
});

test("creeper calming defuses both automatic fuses while preserving forced ignition", () => {
  const e = definitions.creeper;
  for (const event of ["gentlemobs:passive", "gentlemobs:neutral", "gentlemobs:calm", "gentlemobs:flee", "gentlemobs:drop_player_target"]) {
    assert.deepEqual(e.events[event].remove.component_groups, ["minecraft:exploding", "minecraft:charged_exploding"]);
  }
  assert.equal(accepts(e.events["minecraft:start_exploding"].filters, "passive", false), false);
  assert.equal(accepts(e.events["minecraft:start_exploding"].filters, "neutral", true), true);
  assert.equal(e.events["minecraft:start_exploding_forced"].filters, undefined);
});

test("staring, suspicion, anger and ravager roar respect player protection", () => {
  const filters = [definitions.enderman.components["minecraft:looked_at"].filters,
    definitions.warden.components["minecraft:anger_level"].nuisance_filter,
    definitions.piglin.component_groups.angry["minecraft:angry"].filters,
    definitions.ravager.component_groups.roaring["minecraft:behavior.knockback_roar"].damage_filters,
    definitions.creaking.component_groups["minecraft:neutral"]["minecraft:looked_at"].filters];
  for (const filter of filters) {
    assert.equal(accepts(filter, "passive", false), false);
    assert.equal(accepts(filter, "neutral", false), false);
  }
});
