import { cp, mkdir, readFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import { join, resolve } from "node:path";

const root = fileURLToPath(new URL("../", import.meta.url));
const source = join(root, "dist/GentleMobs_BP");
// Pass a com.mojang directory explicitly for older Windows installs or servers.
const game = process.argv[2] ?? join(process.env.APPDATA ?? "", "Minecraft Bedrock/Users/Shared/games/com.mojang");
if (!process.argv[2] && !process.env.APPDATA) throw new Error("Pass your com.mojang directory.");
const destination = resolve(game, "development_behavior_packs/GentleMobs_BP");
const manifest = JSON.parse(await readFile(join(source, "manifest.json"), "utf8"));
try {
  const installed = JSON.parse(await readFile(join(destination, "manifest.json"), "utf8"));
  if (installed.header.uuid !== manifest.header.uuid) throw new Error("Destination contains a different pack; refusing to replace it.");
} catch (error) { if (error.code !== "ENOENT") throw error; }
await mkdir(destination, { recursive: true });
await cp(source, destination, { recursive: true });
console.log(`Deployed ${destination}\nActivate GentleMobs - Zombie Prototype in a NEW test world's Behavior Packs. Reopen the world after changes.`);
