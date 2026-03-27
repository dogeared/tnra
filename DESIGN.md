# Design System — TNRA

## Product Context
- **What this is:** A structured accountability group app where members track daily call chains, weekly posts (intro, kryptonite, commitments, personal/family/work best & worst, stats), monthly meetings, and yearly retreats.
- **Who it's for:** Adults in recovery groups, faith communities, and professional accountability circles. People who chose structure and commitment.
- **Space/industry:** Accountability & recovery tools. Peers: Covenant Eyes, I Am Sober, GoalsWon, Fortify, Relay.
- **Project type:** Web app (Vaadin Flow) — dashboard + structured content editor.

## Aesthetic Direction
- **Direction:** Organic/Grounded — warm, substantial, serious without being heavy.
- **Decoration level:** Intentional — subtle warm texture on surfaces, thin dividers, understated depth. Just enough to feel crafted, not decorated.
- **Mood:** Like a quality leather journal or a well-run meeting room. The visual weight says "this matters" without saying "this is corporate." Grounded, earned, natural.
- **Reference sites:** Covenant Eyes (covenanteyes.com), I Am Sober (iamsober.com), GoalsWon (goalswon.com)

## Typography
- **Display/Hero:** System font stack, weight 700 (-apple-system, BlinkMacSystemFont, Segoe UI, system-ui) — same family as body, differentiated by size and weight. One font everywhere, zero flash, total visual unity.
- **Body:** System font stack (-apple-system, BlinkMacSystemFont, Segoe UI, system-ui) — native platform feel, zero flash, instant render. San Francisco on Mac, Segoe UI on Windows.
- **UI/Labels:** System font stack (weight 500-600) — same as body for consistency.
- **Data/Tables:** System font stack with tabular-nums — same as body for consistency, use `font-variant-numeric: tabular-nums` for aligned numbers.
- **Code:** System monospace (ui-monospace, SF Mono, Menlo, Consolas)
- **Loading:** All system fonts — zero external font loading, zero network requests, zero flash.
- **Scale:**
  - Hero: 3.2rem (51.2px)
  - H1: 2rem (32px)
  - H2: 1.6rem (25.6px)
  - H3: 1.4rem (22.4px)
  - Body: 1rem (16px)
  - Small: 0.9rem (14.4px)
  - Caption: 0.8rem (12.8px)
  - Label: 0.75rem (12px) — uppercase, letter-spacing 0.04-0.08em

## Color

### Light Mode
- **Approach:** Restrained — 1 accent + warm neutrals. Color is rare and meaningful.
- **Primary:** `#2D6A4F` — forest green, growth/rootedness. Used for CTAs, active states, links, success.
- **Primary hover:** `#245740`
- **Primary light:** `rgba(45, 106, 79, 0.10)` — hover backgrounds, active sidebar items.
- **Accent:** `#B8860B` — dark goldenrod, warm/earned. Used sparingly for milestones, achievements, badges.
- **Accent hover:** `#9A7209`
- **Accent light:** `rgba(184, 134, 11, 0.10)`
- **Background:** `#FDFBF7` — warm white, paper-like.
- **Surface:** `#FFFFFF` — cards, modals, elevated content.
- **Surface dim:** `#F5F0EB` — warm linen, sidebar backgrounds, inset areas.
- **Border:** `#E0D6CC` — warm gray dividers.
- **Border subtle:** `#EBE4DB` — card borders, light separators.
- **Text primary:** `#2C2418` — warm near-black.
- **Text secondary:** `#6B5D4F` — body text, descriptions.
- **Text muted:** `#9B8E80` — labels, placeholders, timestamps.
- **Semantic:** success `#2D6A4F`, warning `#D4880F`, error `#C53030`, info `#2B6CB0`

### Dark Mode
- **Strategy:** Warm dark surfaces (brown-blacks, not blue-blacks). Reduce primary saturation ~15%, lighten for contrast.
- **Primary:** `#4DA47A`
- **Primary hover:** `#5FB88C`
- **Accent:** `#D4A01D`
- **Background:** `#1A1610`
- **Surface:** `#231E17`
- **Surface raised:** `#2C261E`
- **Surface dim:** `#141110`
- **Border:** `#3D352B`
- **Border subtle:** `#2C261E`
- **Text primary:** `#EDE6DC`
- **Text secondary:** `#A89A8A`
- **Text muted:** `#7A6E60`

## Shadows
- **Small:** `0 1px 3px rgba(44, 36, 24, 0.06), 0 1px 2px rgba(44, 36, 24, 0.04)` — cards, buttons.
- **Medium:** `0 4px 12px rgba(44, 36, 24, 0.07), 0 1px 4px rgba(44, 36, 24, 0.04)` — hover states, dropdowns.
- **Large:** `0 12px 40px rgba(44, 36, 24, 0.08), 0 4px 12px rgba(44, 36, 24, 0.04)` — modals, overlays.
- **Dark mode:** increase opacity to 0.3-0.4 range with pure black base.

## Spacing
- **Base unit:** 8px
- **Density:** Comfortable — enough breathing room for weekly posts without wasting screen real estate on the dashboard.
- **Scale:** 2xs(2px) xs(4px) sm(8px) md(16px) lg(24px) xl(32px) 2xl(48px) 3xl(64px)

## Layout
- **Approach:** Grid-disciplined — strict columns, predictable alignment. TNRA is data-heavy (weekly posts, stats, call chains); structure keeps dense content scannable.
- **Grid:** 12 columns. Mobile: 4col, Tablet: 8col, Desktop: 12col.
- **Max content width:** 1100px
- **Border radius:**
  - sm: 6px — inputs, buttons, small elements
  - md: 10px — cards, dropdowns
  - lg: 14px — modals, large containers
  - full: 9999px — avatars, pills, badges

## Motion
- **Approach:** Minimal-functional — only transitions that aid comprehension (expanding a post, switching tabs, focus states). No bounce, no play. The content is serious; the interface stays out of the way.
- **Easing:** enter(ease-out) exit(ease-in) move(ease-in-out)
- **Duration:** micro(50-100ms) short(150-200ms) medium(200-300ms)
- **Default transition:** `0.2s cubic-bezier(0.4, 0, 0.2, 1)`

## Design Risks (deliberate departures from category norms)
1. **All system sans-serif** — one font family everywhere, headings differentiated by size (2.5rem) and weight (700). Total visual consistency.
2. **Forest green instead of blue** — every competitor defaults to trust-blue or wellness-teal. Green says growth/rootedness and is distinctive in the space.
3. **Warm paper-like backgrounds** — competitors use cool whites and blue-grays. Warm linen tones make the app feel more like a physical journal and less like a SaaS dashboard.

## Decisions Log
| Date | Decision | Rationale |
|------|----------|-----------|
| 2026-03-27 | Initial design system created | Created by /design-consultation based on competitive research (Covenant Eyes, I Am Sober, GoalsWon) and first-principles insight: TNRA users chose structure, design should feel grounded/substantial like a team workspace not a wellness tracker |
| 2026-03-27 | Forest green primary (#2D6A4F) | Distinctive in accountability space dominated by blues/teals; signals growth and rootedness |
| 2026-03-27 | Instrument Serif for display | Editorial weight differentiates from geometric sans-serif defaults in the category |
| 2026-03-27 | Warm linen backgrounds (#FDFBF7, #F5F0EB) | Physical journal feel vs. cold SaaS dashboard |
| 2026-03-27 | All system sans-serif typography | One font family everywhere — headings use weight 700/600 for hierarchy instead of a separate serif. Zero flash, total consistency. |
| 2026-03-27 | Nav size bump (tabs 1rem, navbar 64px) | Desktop comfort — original 14.4px/56px felt mobile-sized |
