---
name: Forest Ledger
colors:
  surface: '#0b1326'
  surface-dim: '#0b1326'
  surface-bright: '#31394d'
  surface-container-lowest: '#060e20'
  surface-container-low: '#131b2e'
  surface-container: '#171f33'
  surface-container-high: '#222a3d'
  surface-container-highest: '#2d3449'
  on-surface: '#dae2fd'
  on-surface-variant: '#bdcabe'
  inverse-surface: '#dae2fd'
  inverse-on-surface: '#283044'
  outline: '#889489'
  outline-variant: '#3e4941'
  surface-tint: '#77da9f'
  primary: '#77da9f'
  on-primary: '#00391f'
  primary-container: '#198754'
  on-primary-container: '#ffffff'
  inverse-primary: '#006d41'
  secondary: '#b7c8e1'
  on-secondary: '#213145'
  secondary-container: '#3a4a5f'
  on-secondary-container: '#a9bad3'
  tertiary: '#7bd0ff'
  on-tertiary: '#00354a'
  tertiary-container: '#007fac'
  on-tertiary-container: '#ffffff'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#93f7ba'
  primary-fixed-dim: '#77da9f'
  on-primary-fixed: '#002110'
  on-primary-fixed-variant: '#00522f'
  secondary-fixed: '#d3e4fe'
  secondary-fixed-dim: '#b7c8e1'
  on-secondary-fixed: '#0b1c30'
  on-secondary-fixed-variant: '#38485d'
  tertiary-fixed: '#c4e7ff'
  tertiary-fixed-dim: '#7bd0ff'
  on-tertiary-fixed: '#001e2c'
  on-tertiary-fixed-variant: '#004c69'
  background: '#0b1326'
  on-background: '#dae2fd'
  surface-variant: '#2d3449'
  deep-forest: '#0F5132'
  forest-accent: '#20C997'
  leak-critical: '#DC3545'
  leak-warning: '#FD7E14'
  cash-badge: '#10B981'
  paylater-badge: '#F59E0B'
  surface-base: '#0B1120'
  surface-card: '#1E293B'
  surface-border: '#334155'
typography:
  display-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  display-md:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 36px
    letterSpacing: -0.01em
  headline-lg:
    fontFamily: Inter
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: 0em
  headline-md:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
    letterSpacing: 0em
  headline-sm:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 22px
    letterSpacing: 0.005em
  title-md:
    fontFamily: Inter
    fontSize: 15px
    fontWeight: '500'
    lineHeight: 20px
    letterSpacing: 0.01em
  body-lg:
    fontFamily: Inter
    fontSize: 15px
    fontWeight: '400'
    lineHeight: 22px
    letterSpacing: 0.01em
  body-md:
    fontFamily: Inter
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 18px
    letterSpacing: 0.015em
  body-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
    letterSpacing: 0.02em
  label-lg:
    fontFamily: Inter
    fontSize: 13px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.02em
  label-md:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '600'
    lineHeight: 14px
    letterSpacing: 0.04em
  label-sm:
    fontFamily: Inter
    fontSize: 10px
    fontWeight: '700'
    lineHeight: 12px
    letterSpacing: 0.06em
  numeric-currency:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 28px
    letterSpacing: -0.03em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  none: 0px
  xxs: 2px
  xs: 4px
  sm: 8px
  md: 12px
  lg: 16px
  xl: 20px
  xxl: 24px
  gutter: 16px
  margin-screen: 16px
---

## Brand & Style
The design system embodies a disciplined, privacy-first financial utility designed specifically for modern Android environments. It pairs the architectural rigor of Material Design 3 with the reassurance of offline-first sovereign data control. The target audience values analytical clarity, deterministic budgeting, and actionable safeguards against micro-leakages and predatory credit mechanisms like PayLater.

The design movement is **Modern Utilitarian**: precise, high-density, and free of decorative fluff. Visual hierarchy is established through meticulous typographic contrast, tonal surface mapping, and functional color coding. The emotional tone evokes absolute confidence, quiet precision, and total fiscal agency.

## Colors
The color architecture is calibrated for dark mode native Android UI to optimize OLED battery life and reduce cognitive strain during nightly ledger reviews. 

- **Primary (`#198754` / `#0F5132`)**: Serves as the bedrock of positive financial health, verified balances, and confirmed states. Deep forest tones provide organic gravitas rather than playful neon tones.
- **Secondary (`#64748B`) & Neutral (`#0F172A`)**: Slate and charcoal tones form structural cards, divider borders, and muted ledger data.
- **Status & Warning Tokens**:
  - `leak-critical` (`#DC3545`) flags severe financial vulnerabilities, such as `PAYLATER_OVERUSE` (>20% threshold) and high-frequency `IMPULSE_BUY` surges (>=2.0x).
  - `leak-warning` (`#FD7E14`) highlights moderate risks, such as recurring `ZOMBIE_SUB` charges and micro-spending habits (`MICRO_EXPENSE` < Rp25.000).
  - `cash-badge` (`#10B981`) and `paylater-badge` (`#F59E0B`) differentiate liquid balance spending from liabilities, eliminating mental overhead and double-counting during transfers.

## Typography
Typography is tuned for numeric legibility, quick glanceability, and strict layout discipline on 390px mobile screens. Inter is chosen for its neutral personality and open apertures at small scale.

All currency numbers and balances must use tabular lining (`font-variant-numeric: tabular-nums`) to preserve alignment across multi-row ledger cards. `label-sm` is uppercase with wider letter spacing (`0.06em`) for rapid identification of category badges (`FIXED`, `VARIABLE_ESSENTIAL`, `DISCRETIONARY`) and account flags (`CASH` vs `PAYLATER`).

## Layout & Spacing
The layout adheres strictly to an Android portrait target (390dp width, scalable across 360dp–412dp standard screens). 

A baseline 4dp sub-grid supports an 8dp structural layout rhythm. Horizontal screen margins are fixed at `16px` (`spacing.margin-screen`), maximizing horizontal data density without edge crowding. Transaction rows, batch reconciliation lists, and ledger cards maintain `12px` internal padding for vertical economy, ensuring that at least 5 to 6 distinct ledger events fit above the bottom navigation bar fold.

## Elevation & Depth
Elevation is achieved using Material 3 dark tonal layering rather than drop shadows. As elevation increases, surface luminosity lightens systematically:

- **Level 0 (Background)**: `surface-base` (`#0B1120`) – root canvas.
- **Level 1 (Default Cards & Items)**: `surface-card` (`#1E293B`) with a hairline border (`1px solid #334155`).
- **Level 2 (Active/Interactive State, Modals)**: Fill `#27354A` with zero drop shadow to prevent muddy gradients.
- **Level 3 (Reconciliation Banners & Warning Alerts)**: Colored low-opacity fills (`#DC3545` or `#FD7E14` at 12% alpha) wrapped in a crisp 1px stroke at 40% alpha, creating an urgent, clear visual hierarchy.

## Shapes
Shapes balance Material 3 soft geometric contours with the structured precision of financial ledgers:

- **Surfaces & Cards**: `12px` border radius (`rounded-lg` equivalent) provides clear containment without wasting corner space.
- **Interactive Badges & Status Chips**: `6px` radius for a compact, structural token appearance.
- **Action Buttons & Inputs**: `8px` to `12px` radius. Fully circular pills (`9999px`) are reserved solely for floating micro-actions and primary quick-capture buttons.

## Components

### Status & Leak Warning Badges
- **Leak Warning Chips**: Rendered with an alert icon, uppercase `label-sm` text, and a saturated border. 
  - `PAYLATER_OVERUSE` & `IMPULSE_BUY`: Background `#DC35451A`, text `#DC3545`, stroke `#DC354566`.
  - `MICRO_EXPENSE` & `ZOMBIE_SUB`: Background `#FD7E141A`, text `#FD7E14`, stroke `#FD7E1466`.
- **Account Type Tags**:
  - `CASH`: Outlined chip with `cash-badge` (`#10B981`) text and border.
  - `PAYLATER`: Subdued amber chip (`#F59E0B`) with an explicit obligation indicator.

### Transaction Ledger Cards
- Standard state consists of `surface-card` background, `12px` padding, split across two vertical lines:
  - Top row: Category identifier (`label-md`), transaction merchant/note (`headline-sm`), and signed amount (`numeric-currency` styling).
  - Bottom row: Account pill (`CASH` vs `PAYLATER`), relative timestamp, and status indicator (`CONFIRMED` in deep green or `PENDING` in amber).

### Draft Ingestion Banner (Offline Parsing)
- High-priority banner placed above the primary account balance when `unparsed_notifications` or incoming SMS/Notification drafts exist.
- Employs a dual-action layout: "Review (N)" in bold emerald and "Dismiss" in neutral slate, designed for swift single-tap verification.

### Buttons & Inputs
- **Filled Primary Button**: Forest green (`#198754`), high-contrast white text (`Inter` 600), height 48dp for reliable tap targets.
- **Outlined / Secondary**: Slate border (`#334155`), background transparent, text `#F8FAFC`.
- **Amount Input Field**: Large numerical presentation (`32px` Inter tabular-nums), embedded currency prefix, zero bottom underline wobble, fixed numeric keyboard focus.