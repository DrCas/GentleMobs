export const TARGET_GOALS = [
  "minecraft:behavior.nearest_attackable_target",
  "minecraft:behavior.nearest_prioritized_attackable_target",
  "minecraft:behavior.hurt_by_target"
];

export function allowedTarget(subject = "other") {
  return { any_of: [
    { test: "is_family", subject, operator: "not", value: "player" },
    { test: "enum_property", subject: "self", domain: "gentlemobs:mode", value: "vanilla" },
    { all_of: [
      { test: "enum_property", subject: "self", domain: "gentlemobs:mode", value: "neutral" },
      { test: "bool_property", subject: "self", domain: "gentlemobs:engaged", value: true }
    ] }
  ] };
}
function guarded(original, subject = "other") {
  return original ? { all_of: [original, allowedTarget(subject)] } : allowedTarget(subject);
}

export function gentleMob(baseline, movement) {
  const result = structuredClone(baseline);
  const e = result["minecraft:entity"];
  const id = e.description.identifier;
  e.component_groups ??= {};
  e.events ??= {};
  e.description.properties ??= {};
  Object.assign(e.description.properties, {
    "gentlemobs:mode": { type: "enum", values: ["passive", "neutral", "vanilla"], default: id === "minecraft:creaking" ? "vanilla" : "passive", client_sync: false },
    "gentlemobs:engaged": { type: "bool", default: false, client_sync: false },
    "gentlemobs:fleeing": { type: "bool", default: false, client_sync: false }
  });
  const containers = [e.components, ...Object.values(e.component_groups)];
  for (const c of containers) {
    for (const name of TARGET_GOALS) {
      const goal = c[name];
      if (!goal) continue;
      // Missing entity_types means any attacker; single objects also occur.
      const targets = goal.entity_types === undefined ? [{}] : Array.isArray(goal.entity_types) ? goal.entity_types : [goal.entity_types];
      for (const target of targets) {
        target.filters = guarded(target.filters);
        target.reevaluate_description = true;
      }
      goal.entity_types = targets;
    }
    if (c["minecraft:area_attack"]) c["minecraft:area_attack"].entity_filter = guarded(c["minecraft:area_attack"].entity_filter);
    if (c["minecraft:angry"]) c["minecraft:angry"].filters = guarded(c["minecraft:angry"].filters);
    if (c["minecraft:anger_level"]) c["minecraft:anger_level"].nuisance_filter = guarded(c["minecraft:anger_level"].nuisance_filter);
    const looked = c["minecraft:looked_at"];
    if (looked && looked.set_target !== "never") looked.filters = guarded(looked.filters);
    const roar = c["minecraft:behavior.knockback_roar"];
    if (roar) for (const key of ["damage_filters", "knockback_filters"]) roar[key] = guarded(roar[key]);
  }
  // Catch targets assigned by native anger broadcasts or a summoner as well as
  // normal target goals. Append to each sensor variant so transformations and
  // difficulty/equipment component changes cannot remove this guard.
  e.components["minecraft:environment_sensor"] ??= { triggers: [] };
  for (const c of containers) {
    const sensor = c["minecraft:environment_sensor"];
    if (!sensor) continue;
    const triggers = sensor.triggers === undefined ? [] : Array.isArray(sensor.triggers) ? sensor.triggers : [sensor.triggers];
    triggers.push({ filters: { all_of: [
      { test: "has_target", value: true },
      { test: "is_family", subject: "target", value: "player" },
      { none_of: [allowedTarget("target")] }
    ] }, event: "gentlemobs:drop_player_target" });
    sensor.triggers = triggers;
  }
  // Gate a permanent entry instead of replacing then removing native avoidance.
  if (movement === "walk" || movement === "swim") {
    const name = "minecraft:behavior.avoid_mob_type";
    const avoid = e.components[name] ??= { priority: 0, ignore_visibility: true, max_dist: 16, max_flee: 12, avoid_target_xz: 12 };
    const targets = avoid.entity_types === undefined ? [] : Array.isArray(avoid.entity_types) ? avoid.entity_types : [avoid.entity_types];
    targets.push({ filters: { all_of: [
      { test: "is_family", subject: "other", value: "player" },
      { test: "bool_property", subject: "self", domain: "gentlemobs:fleeing", value: true }
    ] }, max_dist: 16, walk_speed_multiplier: 1.3, sprint_speed_multiplier: 1.3 });
    avoid.entity_types = targets;
  }
  // Staring/anger/daylight mobs need a route to retaliate after the first hit.
  // This distinct goal never replaces their native nearest-target component.
  const needsRetaliation = ["enderman", "zombie_pigman", "spider", "cave_spider", "warden", "creaking"].some(name => id === `minecraft:${name}`);
  if (needsRetaliation) {
    e.components["minecraft:behavior.nearest_prioritized_attackable_target"] = {
      priority: 1, must_see: true,
      entity_types: [{ priority: 0, max_dist: 32, filters: { all_of: [
        { test: "is_family", subject: "other", value: "player" },
        { test: "enum_property", domain: "gentlemobs:mode", value: "neutral" },
        { test: "bool_property", domain: "gentlemobs:engaged", value: true }
      ] } }]
    };
  }
  const reset = {
    set_property: { "gentlemobs:engaged": false, "gentlemobs:fleeing": false },
    reset_target: {}
  };
  if (id === "minecraft:creeper") {
    reset.remove = { component_groups: ["minecraft:exploding", "minecraft:charged_exploding"] };
    e.events["minecraft:start_exploding"].filters = guarded(e.events["minecraft:start_exploding"].filters, "target");
  }
  e.events["gentlemobs:drop_player_target"] = { reset_target: {} };
  if (reset.remove) e.events["gentlemobs:drop_player_target"].remove = structuredClone(reset.remove);
  for (const mode of ["passive", "neutral", "vanilla"]) {
    e.events[`gentlemobs:${mode}`] = {
      ...structuredClone(reset), set_property: { ...reset.set_property, "gentlemobs:mode": mode }
    };
  }
  e.events["gentlemobs:calm"] = structuredClone(reset);
  e.events["gentlemobs:flee"] = {
    ...structuredClone(reset),
    filters: { test: "enum_property", domain: "gentlemobs:mode", value: "passive" },
    set_property: { "gentlemobs:engaged": false, "gentlemobs:fleeing": true }
  };
  e.events["gentlemobs:engage"] = {
    filters: { test: "enum_property", domain: "gentlemobs:mode", value: "neutral" },
    set_property: { "gentlemobs:engaged": true, "gentlemobs:fleeing": false }
  };
  if (id === "minecraft:creaking") {
    // Release a hostile frozen state, preserving crumbling/death transitions.
    e.events["gentlemobs:release_creaking"] = {
      filters: { any_of: ["hostile_observed", "hostile_unobserved"].map(value => ({ test: "enum_property", domain: "minecraft:creaking_state", value })) },
      trigger: "minecraft:become_neutral"
    };
    for (const name of ["passive", "neutral", "calm", "flee"]) {
      e.events[`gentlemobs:${name}`].sequence = [{ trigger: "gentlemobs:release_creaking" }];
    }
  }
  return result;
}
