# MeowClient

A kawaii-themed Minecraft 1.8.9 PVP utility client based on MCP (Minecraft Coder Pack).

- **Version:** R3
- **Minecraft:** 1.8.9
- **Java:** 8
- **34 Modules** across 8 categories

---

## Features

- **Two ClickGUI styles** — Dropdown (classic panel) & CSGO (windowed)
- **Modern animation system** — eased hover / expand / slide-in transitions
- **Notification system** — animated slide-in with type icons
- **Enhanced event system** — Pre/POST motion, packet send/receive, move, strafe
- **Three bind modes** — Toggle / Hold / Smart
- **Dynamic module tags** — auto-display current mode in ArrayList
- **Value groups** — nested settings for cleaner ClickGUI

---

## Modules

| Category | Modules |
|----------|---------|
| **Combat** | AutoClicker, KillAura, ClickSound, NoClickDelay |
| **Movement** | Sprint, Speed, NoSlow, NoJumpDelay |
| **Player** | Cape, Animations, Derp, SkinDerp, Twerk |
| **Render** | ESP, NameTag, FullBright, Zoom, Tracers, Breadcrumbs, JumpEffect, HitParticles, DamageParticles |
| **World** | AutoTool, Eagle, FastPlace, TimeChanger |
| **Misc** | AutoGG |
| **HUD** | ArrayList, InfoHUD, TargetHUD, ArmorHUD, Tab, Logo |
| **Draw** | ClickGUI |

---

## Getting Started

### Prerequisites

- JDK 8 (Liberica Full JDK 1.8 recommended)
- IntelliJ IDEA
- Minecraft 1.8.9

### Setup

1. Clone the repository
2. Open in IntelliJ IDEA, set SDK to JDK 8
3. Add all JARs in `lib/` to module classpath
4. Mark `src/` as source, `resources/` as resource, `test/` as test root
5. Run `test/Start.java`

### Controls

| Key | Action |
|-----|--------|
| RShift | Open ClickGUI |
| Arrow keys | Navigate TabMod |
| C (hold) | Zoom |
| V (hold) | Sprint |
| X (hold) | Eagle |

---

## Architecture

```
src/cn/sux1ng/client/
├── MeowClient.java          # Client bootstrap
├── mod/                     # Module system
│   └── mods/                # 34 modules (8 categories)
├── events/                  # Event system (annotation-based dispatch)
├── value/                   # Config values (Boolean/Number/Mode/Color/Text/Group)
├── gui/clickgui/            # Dropdown-style ClickGUI
├── gui/csgo/                # CSGO-style windowed GUI
├── ui/notification/         # Animated notification system
├── config/                  # JSON config persistence
├── command/                 # Chat commands
└── util/
    ├── animation/           # Easing functions & Animation class
    ├── DrawUtil.java        # Rendering utilities
    └── ColorUtil.java       # Color manipulation
```

---

## Credits

Developed by **sux1ng** — a learning/educational project.

Reference clients:
- [Reversal v3.2.2](https://github.com) — 1.8.9 PVP client
- [LiquidBounce NextGen](https://github.com/CCBlueX/LiquidBounce) — 1.21 Fabric client
