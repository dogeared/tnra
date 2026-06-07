# Landing content format

The public pages render from the Markdown files in this directory:

| File           | Page        | Route       |
|----------------|-------------|-------------|
| `landing.md`   | Home        | `/`         |
| `pricing.md`   | Pricing     | `/pricing`  |
| `about-us.md`  | About Us    | `/about-us` |

Most of a page is plain Markdown (headings, paragraphs, lists, links, blockquotes,
`code`, `---` rules — rendered with the site theme). Structured or interactive
pieces use **custom blocks**: a fence line `:::<type>` opens a block and a bare
`:::` closes it. Everything outside a block is plain Markdown.

Parser: `LandingContentParser`. Renderer: `LandingContentRenderer`.

## Block types

### `:::cards <variant>` — a grid of cards

`<variant>` is one of `squares`, `features`, or `pricing` (controls styling).
Markdown before the first card is the section title/intro. Each card starts with
`:: <heading>`; the lines under it are the card body (Markdown).

- **`squares`** — equal square cards (The TNRA Way cadence).
- **`features`** — feature cards. A leading emoji in the heading becomes the card
  icon, e.g. `:: 📝 Structured weekly posts`.
- **`pricing`** — pricing tiers. Add `{featured}` to a heading to highlight it,
  e.g. `:: Group {featured}`. The body is normal Markdown: a `**price**` (rendered
  large), a `-` list of features, and a `[Request Access](/?to=request-access)` link
  (rendered as a button).

```
:::cards squares
## The TNRA Way

Intro paragraph.

### Cadence
:: Daily
What daily looks like.
:: Weekly
What weekly looks like.
:::
```

### `:::hero` — the home-page hero

Markdown headline (`#`), a sub-paragraph, and a link CTA.

```
:::hero
# Accountability for groups that mean it.

One-sentence pitch.

[Request Access for Your Group](#request-access)
:::
```

### `:::cta` — a standalone primary button

A single Markdown link, rendered centered as a primary button.

```
:::cta
[Request Access](#request-access)
:::
```

### `:::form` — the live Request Access form

Injects the interactive form (fields, validation, submit). Every label and message
is a `key: value` line; any you omit falls back to a sensible default:

```
:::form
title: Request access
intro: TNRA is invite-only. Tell us about your group and we'll be in touch.
group-label: Group name
name-label: Your name
email-label: Your email
size-label: Estimated group size
description-label: Tell us about your group
description-placeholder: What brings your group together? What are you hoping TNRA will help with?
submit: Send Request
sent: Sent!
success: Thanks! We'll be in touch at {email}.
:::
```

`{email}` in `success` is replaced with the submitted address. "Required" validation
messages are derived from the labels (e.g. `group-label` → "Group name is required").
The fields themselves and validation logic live in `LandingView`.

## Section anchors

Any directive block (`:::cards`, `:::hero`, `:::cta`) can declare an anchor id with a
`#id` token in its args. The id is set on the section element so it can be a scroll target:

```
:::cards squares #the-tnra-way
```

The home page (`LandingView`) scrolls to a section when arrived at with `?to=<id>`. The
Request Access form's id is `request-access`.

## Links and scrolling

- `[…](#request-access)` — same-page smooth scroll (browser-native) to an element id on the
  current page; use on the home page (e.g. the form, `request-access`).
- `[…](/?to=request-access)` or `[…](/?to=the-tnra-way)` — from another page; opens the home
  page and `LandingView` scrolls that section into view (server-side, no fragment-timing issue).

## Notes

- Content is trusted (repo-owned), so rendered HTML is not sanitized.
- Changing copy here needs only a redeploy — no code change.
