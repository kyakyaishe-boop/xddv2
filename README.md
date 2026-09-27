# TierTagger 26.2

Fabric 26.2 client mod that reads a remotely hosted tier database. Press Right Shift to open the tier list.

## GitHub build

Upload this repository to GitHub, then open Actions -> Build TierTagger 26.2 -> Run workflow. The workflow uses Java 25, Gradle 9.5.1, and Fabric Loom 1.17.

The production JAR is uploaded as the `TierTagger-26.2` artifact.

## Tier data

Change `src/client/resources/tiertagger.json` to point at the published `data/tiers.json` URL. The Minecraft client periodically downloads that JSON and caches it locally.

The Discord synchronization service is intentionally separate from the Minecraft JAR so no Discord bot token is embedded in the mod.
