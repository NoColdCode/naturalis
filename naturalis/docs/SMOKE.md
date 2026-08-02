# Multi-loader smoke checklist

After each major change batch: compile all five modules, then smoke `runClient` on each.

## Compile gate

From `naturalis/`:

```powershell
.\gradlew :neoforge-1.21.1:compileJava :neoforge-1.21.8:compileJava :forge-1.20.1:compileJava :fabric-1.21.1:compileJava :fabric-1.21.8:compileJava
```

Pass: all five tasks succeed.

## Client smoke (interactive)

Launch one at a time; reach the title / vanilla menu, then stop the process (~30–60s if healthy):

```powershell
.\gradlew :neoforge-1.21.1:runClient
.\gradlew :neoforge-1.21.8:runClient
.\gradlew :forge-1.20.1:runClient
.\gradlew :fabric-1.21.1:runClient
.\gradlew :fabric-1.21.8:runClient
```

### Pass criteria

- Client reaches title screen (or vanilla main menu) without a fatal crash.
- `latest.log` must **not** contain:
  - `Fatal`
  - `Failed to load registries`
  - mixin prepare / apply crashes that abort boot
  - biome codec errors for `"carvers"` (expect `"carvers": {}`, not `[]`)

### Log locations (typical)

| Module | Log under module `run/` |
|--------|-------------------------|
| neoforge-1.21.1 | `neoforge-1.21.1/run/logs/latest.log` |
| neoforge-1.21.8 | `neoforge-1.21.8/run/logs/latest.log` |
| forge-1.20.1 | `forge-1.20.1/run/logs/latest.log` |
| fabric-1.21.1 | `fabric-1.21.1/run/logs/latest.log` |
| fabric-1.21.8 | `fabric-1.21.8/run/logs/latest.log` |

Quick fail grep (PowerShell):

```powershell
Select-String -Path '<module>/run/logs/latest.log' -Pattern 'Fatal|Failed to load registries|Mixin apply failed|Invalid map entry'
```

## Notes

- Forge 1.20.1 keeps existing features green; Survival-as Create World UI is **not** required on Forge.
- Do not commit `run/`, unpacked `libs`, or scratch `.class` trees under `tocraft/`.
- Fabric / NeoForge versions should stay aligned with root `mod_version` in `gradle.properties` (and Fabric `fabric.mod.json`).
