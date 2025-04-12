# StandCore

A comprehensive staff management plugin for Minecraft servers, featuring ranks, permissions, staff tools, and punishment systems.

## Features

### Rank Management
- Hierarchical rank system with inheritance
- Temporary and permanent rank grants
- Grant history tracking
- Custom prefixes and permissions per rank

### Staff Mode
- Toggle staff mode with `/staff`
- Vanish functionality with `/vanish` or `/v`
- Staff tools:
  - Teleport Tool (Right-click to teleport)
  - Inspection Tool (View player/block info)
  - Freeze Tool (Right-click players to freeze)
  - Vanish Tool (Quick toggle)
- Inventory inspection with `/invsee`

### Admin Chat
- Private staff communication
- Toggle mode or direct messages
- Commands: `/adminchat` or `/ac`
- Direct message syntax: `/ac !player message`

### Punishment System
- Temporary and permanent bans
- Mute system with duration support
- Kick command with reason
- Warning system with auto-punishments
- Full punishment history

## Commands

### Staff Commands
- `/staff` - Toggle staff mode
- `/vanish` (alias: `/v`) - Toggle visibility
- `/freeze <player>` - Freeze a player
- `/unfreeze <player>` - Unfreeze a player
- `/invsee <player> [armor]` - View player inventory
- `/adminchat` (alias: `/ac`) - Staff chat

### Grant Commands
- `/grant <player>` - Open grant GUI
- `/grantshistory <player>` (aliases: `/grantshist`, `/gh`) - View grant history

### Punishment Commands
- `/ban <player> [duration] <reason>` - Ban a player
- `/unban <player>` - Unban a player
- `/mute <player> [duration] <reason>` - Mute a player
- `/unmute <player>` - Unmute a player
- `/kick <player> <reason>` - Kick a player
- `/warn <player> <reason>` - Warn a player

## Permissions

### Staff Permissions
- `standcore.staff` - Access to staff mode
- `standcore.staff.vanish` - Vanish ability
- `standcore.staff.vanish.see` - See vanished players
- `standcore.staff.freeze` - Freeze players
- `standcore.staff.invsee` - View inventories
- `standcore.adminchat` - Access admin chat

### Grant Permissions
- `standcore.grant` - Grant ranks
- `standcore.grant.self` - Grant to self
- `standcore.grant.override` - Override rank restrictions
- `standcore.grantshistory` - View grant history
- `standcore.grantshistory.book` - View history in book format

### Punishment Permissions
- `standcore.ban` - Ban players
- `standcore.ban.override` - Override rank restrictions
- `standcore.unban` - Unban players
- `standcore.mute` - Mute players
- `standcore.mute.override` - Override rank restrictions
- `standcore.unmute` - Unmute players
- `standcore.kick` - Kick players
- `standcore.warn` - Warn players
- `standcore.warn.override` - Override rank restrictions

## Configuration

The plugin uses three main configuration files:

### config.yml
- Database settings
- Staff mode settings
- Grant settings
- Punishment settings
- GUI customization

### ranks.yml
- Rank definitions
- Permission inheritance
- Prefix settings
- GUI icons

### messages.yml
- All plugin messages
- Chat formats
- Punishment screens

## PlaceholderAPI Integration

Available placeholders:
- `%standcore_rank%` - Player's rank name
- `%standcore_prefix%` - Player's rank prefix
- `%standcore_grant_remaining%` - Time until rank expires
- `%standcore_staff_mode%` - Staff mode status
- `%standcore_vanished%` - Vanish status
- `%standcore_frozen%` - Freeze status
- `%standcore_muted%` - Mute status
- `%standcore_mute_remaining%` - Time until mute expires
- `%standcore_mute_reason%` - Mute reason
- `%standcore_grants_count%` - Total grants received
- `%standcore_sanctions_count%` - Total punishments received
- `%standcore_warns_count%` - Total warnings received
- `%standcore_rank_weight%` - Rank weight
- `%standcore_rank_display%` - Full rank display (prefix + name)

## Support

For support, please create an issue on our GitHub repository or contact us through our Discord server.
