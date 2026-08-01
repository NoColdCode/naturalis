# Commands

This page lists player-facing commands for Naturalis.

## Resonance commands

- Status:
  - `/morph resonance`
  - `/morph resonance status`
- Bond current morph:
  - `/morph resonance bond`
- Bond specific morph:
  - `/morph resonance bond <namespace:entity_id>`
- Clear bond:
  - `/morph resonance clearbond`
- Activate:
  - `/morph resonance activate`
- Deactivate:
  - `/morph resonance deactivate`
- Trigger burst:
  - `/morph resonance burst`

### Common Resonance command messages

- `No bonded morph set`: set a bond first
- `Resonance is not active`: activate before burst
- `Burst requires your bonded morph form`: transform into the bonded form first
- `Burst is on cooldown`: wait until cooldown ends

## Knowledge commands

- Open knowledge UI:
  - `/morph knowledge`
- Show current morph knowledge stats:
  - `/morph knowledge stats`

## Admin and test commands

Some commands require operator permissions (for balancing, testing, and events):

- `/morph acquire <namespace:entity_id>`
- `/morph brewed <namespace:entity_id> [seconds]`
- `/morph knowledge addxp <amount>`
- `/morph knowledge setxp <amount>`
- `/morph knowledge setlevel <level>`

If a command fails, see `05-troubleshooting.md`.
