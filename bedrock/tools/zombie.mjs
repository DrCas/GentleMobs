// Keep the unmodified upstream definition separate so version upgrades and the
// exact scope of our vanilla override can be reviewed and tested.
export function gentleZombie(baseline) {
  const result = structuredClone(baseline);
  const entity = result["minecraft:entity"];
  if (entity.description.identifier !== "minecraft:zombie") throw new Error("Unexpected vanilla entity");
  entity.description.properties["gentlemobs:mode"] = {
    type: "enum", values: ["passive", "neutral", "vanilla"], default: "passive", client_sync: false
  };
  entity.description.properties["gentlemobs:engaged"] = {
    type: "bool", default: false, client_sync: false
  };
  const allowedTarget = {
    any_of: [
      { test: "is_family", subject: "other", operator: "not", value: "player" },
      { test: "enum_property", subject: "self", domain: "gentlemobs:mode", value: "vanilla" },
      { all_of: [
        { test: "enum_property", subject: "self", domain: "gentlemobs:mode", value: "neutral" },
        { test: "bool_property", subject: "self", domain: "gentlemobs:engaged", value: true }
      ] }
    ]
  };
  for (const name of ["minecraft:behavior.nearest_attackable_target", "minecraft:behavior.hurt_by_target"]) {
    for (const target of entity.components[name].entity_types) {
      target.filters = { all_of: [target.filters, structuredClone(allowedTarget)] };
      target.reevaluate_description = true;
    }
  }
  entity.component_groups["gentlemobs:fleeing"] = {
    "minecraft:behavior.avoid_mob_type": {
      priority: 0, ignore_visibility: true, remove_target: true,
      max_dist: 16, max_flee: 12, avoid_target_xz: 12,
      entity_types: [{
        filters: { test: "is_family", subject: "other", value: "player" },
        max_dist: 16, walk_speed_multiplier: 1.3, sprint_speed_multiplier: 1.3
      }]
    }
  };
  const calm = {
    remove: { component_groups: ["gentlemobs:fleeing"] },
    set_property: { "gentlemobs:engaged": false },
    reset_target: {}
  };
  for (const mode of ["PASSIVE", "NEUTRAL", "VANILLA"]) {
    entity.events[`gentlemobs:${mode.toLowerCase()}`] = {
      ...structuredClone(calm),
      set_property: { "gentlemobs:engaged": false, "gentlemobs:mode": mode.toLowerCase() }
    };
  }
  entity.events["gentlemobs:calm"] = calm;
  entity.events["gentlemobs:flee"] = {
    filters: { test: "enum_property", domain: "gentlemobs:mode", value: "passive" },
    add: { component_groups: ["gentlemobs:fleeing"] }, reset_target: {}
  };
  entity.events["gentlemobs:engage"] = {
    filters: { test: "enum_property", domain: "gentlemobs:mode", value: "neutral" },
    set_property: { "gentlemobs:engaged": true }
  };
  return result;
}
