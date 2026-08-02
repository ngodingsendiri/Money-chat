// Money Chat — preview build.
// Copies the static preview layer into dist/ so the Freebuff hosting builder
// can serve it as static output. Must exit (no server) so deploys stay simple.
import { cp, mkdir, rm } from "node:fs/promises";
import { join } from "node:path";
import { fileURLToPath } from "node:url";

const PREVIEW = fileURLToPath(new URL(".", import.meta.url));
const DIST = join(PREVIEW, "..", "dist");

await rm(DIST, { recursive: true, force: true });
await mkdir(DIST, { recursive: true });

await cp(PREVIEW, DIST, {
  recursive: true,
  filter: (src) =>
    !src.endsWith("server.mjs") &&
    !src.endsWith("build.mjs") &&
    !src.endsWith(".DS_Store"),
});

console.log("Money Chat preview build complete: static output in dist/");
