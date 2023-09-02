# Respawn Complex

Respawn to the closest respawn point

## Feature
- Multiple respawn points and respawn to the closest one
- Optional require activation before respawn
  - Two activation method, interact and moving.
- Optional points sync with blocks with `#respawn_complex:respawn_point` tag.
  - Sync means auto create/remove point when place/break by player
  - Respawn anchor, waystones(from the two), beacon is `#respawn_complex:respawn_point` by default
  - **Will disable respawn anchor and bed function**
  - Respawn anchor only works in level that can work
  - Notice: the generated blocks won't be treated as point before anyone interact it. So, it's recommend to using activation and set method to interacting


## Commands

- `/spawn` Teleport to the closest spawn point
- `/spawn list` List all spawn point block position
- `/spawn set` Set current position as spawn point
- `/spawn remove` Remove the closest point in 4 blocks

## Permissions

Works with [luckperms](luckperms.net/)
All admin permission will need op level 3 without luckperms
- `respawncomplex.command.spawn`
- `respawncomplex.admin.command.set`
- `respawncomplex.admin.command.remove`
- `respawncomplex.admin.command.list"`
