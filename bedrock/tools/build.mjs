import { readFile, mkdir, writeFile, cp, readdir } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import { resolve, join, relative } from "node:path";
import { build } from "esbuild";
import { parse } from "jsonc-parser";
import { zipSync } from "fflate";
import { gentleZombie } from "./zombie.mjs";

const root = fileURLToPath(new URL("../", import.meta.url));
const output = resolve(root, "dist/GentleMobs_BP");
await mkdir(join(output, "entities"), { recursive: true });
await cp(join(root, "pack"), output, { recursive: true });
const errors = [];
const baseline = parse(await readFile(join(root, "vendor/zombie.jsonc"), "utf8"), errors);
if (errors.length) throw new Error(`Invalid upstream JSON: ${JSON.stringify(errors)}`);
await writeFile(join(output, "entities/zombie.json"), JSON.stringify(gentleZombie(baseline), null, 2) + "\n");
await cp(join(root, "vendor/NOTICE.md"), join(output, "VANILLA-NOTICE.md"));
await build({
  entryPoints: [join(root, "src/main.ts")], outfile: join(output, "scripts/main.js"),
  bundle: true, format: "esm", platform: "neutral", target: "es2022", external: ["@minecraft/server"]
});
const files = {};
async function collect(dir) {
  for (const entry of await readdir(dir, { withFileTypes: true })) {
    const path = join(dir, entry.name);
    if (entry.isDirectory()) await collect(path);
    else files[relative(output, path).replaceAll("\\", "/")] = new Uint8Array(await readFile(path));
  }
}
await collect(output);
const artifact = join(root, "dist/GentleMobs-Bedrock-0.1.0-prototype.mcpack");
await writeFile(artifact, zipSync(files));
console.log(`Built ${artifact}`);
