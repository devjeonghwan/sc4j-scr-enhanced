# 브랜딩

[English](README.md) | **한국어**

SC:R Enhanced 모드의 런처 그림: 아이콘, 배너, 넓은 배너입니다.

- [파일](#파일)
- [PNG 내보내기](#png-내보내기)

## 파일

| 파일 | 내용 |
|---|---|
| `icon.svg` | 런처 아이콘, 원본(512 x 512) |
| `banner.svg` | 런처 배너, 원본(1740 x 256) |
| `banner-wide.svg` | 넓은 배너, 원본(950 x 150) |
| `banner-wide-950x150.png`, `banner-wide-1900x300.png` | 넓은 배너, 950 x 150과 1900 x 300픽셀 |

빌드에 있는 복사본:

| 복사본 | 원본 |
|---|---|
| `overlay/scr-client/src/main/resources/meta/icon.png`(512 x 512) | `icon.svg` |
| `overlay/scr-client/src/main/resources/meta/banner.png`(1740 x 256) | `banner.svg` |

## PNG 내보내기

SVG는 마스크와 필터를 씁니다. ImageMagick의 내장 SVG 렌더러가 아니라 브라우저 엔진으로 렌더합니다.

1. 크기 `W x H`마다, SVG 옆에 그 크기로 보여 주는 페이지를 씁니다.

   ```html
   <!doctype html><html><body style="margin:0;background:transparent"><img src="<image>.svg" width="W" height="H" style="display:block"></body></html>
   ```

2. Chromium 기반 브라우저(Chrome, Chromium, Edge)를 헤드리스 모드로 실행해 투명 스크린숏을 찍습니다.

   ```text
   <browser> --headless=new --disable-gpu --hide-scrollbars --allow-file-access-from-files --default-background-color=00000000 --window-size=W,H --screenshot=<out>.png <page>.html
   ```
