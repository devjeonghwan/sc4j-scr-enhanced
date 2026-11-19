# Branding

**English** | [한국어](README.ko.md)

The launcher art of the SC:R Enhanced mod: its icon, its banner and a wide banner.

- [Files](#files)
- [Exporting PNG](#exporting-png)

## Files

| File | What it is |
|---|---|
| `icon.svg` | launcher icon, source (512 x 512) |
| `banner.svg` | launcher banner, source (1740 x 256) |
| `banner-wide.svg` | wide banner, source (950 x 150) |
| `banner-wide-950x150.png`, `banner-wide-1900x300.png` | wide banner, 950 x 150 and 1900 x 300 pixels |

Copies in the build:

| Copy | Exported from |
|---|---|
| `overlay/scr-client/src/main/resources/meta/icon.png` (512 x 512) | `icon.svg` |
| `overlay/scr-client/src/main/resources/meta/banner.png` (1740 x 256) | `banner.svg` |

## Exporting PNG

The SVGs use masks and filters: render them with a browser engine, not ImageMagick's built-in SVG renderer.

1. For a size `W x H`, write a page next to the SVG that shows it at that size:

   ```html
   <!doctype html><html><body style="margin:0;background:transparent"><img src="<image>.svg" width="W" height="H" style="display:block"></body></html>
   ```

2. Take a transparent screenshot of it with a Chromium-based browser (Chrome, Chromium or Edge) in headless mode:

   ```text
   <browser> --headless=new --disable-gpu --hide-scrollbars --allow-file-access-from-files --default-background-color=00000000 --window-size=W,H --screenshot=<out>.png <page>.html
   ```
