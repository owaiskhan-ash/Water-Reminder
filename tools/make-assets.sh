#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# Generates every image asset the Android app needs from the master WR logo.
# Requires: ImageMagick 6 (`convert`).
#
#   ./tools/make-assets.sh
# ---------------------------------------------------------------------------
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SRC="$ROOT/assets/logo/wr-logo-option-a.png"
RES="$ROOT/android/app/src/main/res"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

[ -f "$SRC" ] || { echo "Missing master logo: $SRC"; exit 1; }
mkdir -p "$RES"/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi} \
         "$RES"/drawable-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi} \
         "$RES/drawable"

# --- 1. Cut the logo out of its white studio background --------------------
convert "$SRC" -fuzz 10% -transparent white -trim +repage "$TMP/cut.png"

# --- 2. Legacy launcher icons (square) + round variants --------------------
while read -r density size; do
  convert "$TMP/cut.png" -resize "${size}x${size}" "$RES/mipmap-$density/ic_launcher.png"

  # Circular crop for ic_launcher_round
  radius=$(( size / 2 ))
  convert "$TMP/cut.png" -resize "${size}x${size}" \
    \( -size "${size}x${size}" xc:none -fill white \
       -draw "circle $radius,$radius $radius,0" \) \
    -alpha set -compose DstIn -composite \
    "$RES/mipmap-$density/ic_launcher_round.png"
done <<'EOF'
mdpi 48
hdpi 72
xhdpi 96
xxhdpi 144
xxxhdpi 192
EOF

# --- 3. Adaptive icon foreground (108dp canvas, logo inside the 66dp safe zone)
while read -r density canvas logo; do
  convert -size "${canvas}x${canvas}" xc:none \
    "$TMP/cut.png" -resize "${logo}x${logo}" -gravity center -composite \
    "$RES/mipmap-$density/ic_launcher_foreground.png"
done <<'EOF'
mdpi 108 66
hdpi 162 99
xhdpi 216 132
xxhdpi 324 198
xxxhdpi 432 264
EOF

# --- 4. Felt / sweatshirt fleece UI texture --------------------------------
# Fine fibre grain (high frequency) + soft mottling (low frequency), kept at
# very low contrast so it reads as material, not noise.
#
# One copy per density bucket so the grain stays crisp instead of being
# upscaled on high density screens.
make_felt() {
  local size="$1" out="$2"
  convert -size "${size}x${size}" xc:'#2b2b2f' \
    \( -size "${size}x${size}" xc:gray50 +noise Gaussian -attenuate 0.85 -blur 0x0.45 -level 22%,78% \) \
    \( -size "${size}x${size}" plasma:fractal -blur 0x1.6 -level 35%,65% \) \
    -compose Overlay -composite \
    \( -size "${size}x${size}" xc:gray50 +noise Gaussian -attenuate 0.4 -blur 0x0.3 -level 30%,70% \) \
    -compose Overlay -composite \
    -colorspace sRGB -depth 8 \
    -colors 64 \
    "PNG8:$out"
}

make_felt 128 "$RES/drawable-mdpi/felt_texture.png"
make_felt 192 "$RES/drawable-hdpi/felt_texture.png"
make_felt 256 "$RES/drawable-xhdpi/felt_texture.png"
make_felt 384 "$RES/drawable-xxhdpi/felt_texture.png"
make_felt 512 "$RES/drawable-xxxhdpi/felt_texture.png"

echo "Assets written to $RES"
ls -la "$RES/drawable-xxxhdpi"
