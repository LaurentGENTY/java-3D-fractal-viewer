#!/usr/bin/env bash
# Regenerates every showcase media file in docs/media from the scripted demo.
set -euo pipefail

cd "$(dirname "$0")/.."

for tool in mvn ffmpeg cwebp; do
  command -v "$tool" >/dev/null 2>&1 || { echo "error: '$tool' not found in PATH" >&2; exit 1; }
done

FRAMES=target/demo-frames
OUT=docs/media
# SKIP_DEMO=1 re-encodes the frames already in target/demo-frames.
SKIP_DEMO=${SKIP_DEMO:-0}
# Fractal detail compresses badly: GIFs shrink until they fit under GIF_MAX_BYTES.
GIF_WIDTHS=(640 480 360)
GIF_FPS=12
GIF_MAX_BYTES=$((5 * 1024 * 1024))
MP4_MAX_WIDTH=960
STILL_WIDTH=1200
CLIPS=(sphere-spin deep-zoom julia-morph tour)
# 2D-only montage for the portfolio card, in playback order.
PORTFOLIO_CLIPS=(deep-zoom burning-ship-zoom julia-morph newton-zoom levy-growth square-growth)
# Portfolio cards are 16:10: crop the 2:1 flat frames to their center.
PORTFOLIO_FILTER="crop=1600:1000,scale=1024:640:flags=lanczos,setsar=1"

if [ "$SKIP_DEMO" != "1" ]; then
  # Stale frames from a previous, longer run would leak into the videos.
  rm -rf "$FRAMES"
  mvn -q -Pdemo javafx:run
fi

mkdir -p "$OUT"
for clip in "${CLIPS[@]}"; do
  echo "Encoding $clip"
  # Flat frames are 2000x1000: cap the video width; -2 keeps an even height for yuv420p.
  ffmpeg -y -loglevel error -framerate 30 -i "$FRAMES/$clip/frame-%04d.png" \
    -vf "scale='min(${MP4_MAX_WIDTH},iw)':-2:flags=lanczos" \
    -c:v libx264 -pix_fmt yuv420p -crf 26 -movflags +faststart "$OUT/$clip.mp4"
  for width in "${GIF_WIDTHS[@]}"; do
    ffmpeg -y -loglevel error -framerate 30 -i "$FRAMES/$clip/frame-%04d.png" \
      -vf "fps=${GIF_FPS},scale=${width}:-1:flags=lanczos,split[a][b];[a]palettegen=stats_mode=diff[p];[b][p]paletteuse=dither=bayer:bayer_scale=5:diff_mode=rectangle" \
      "$OUT/$clip.gif"
    size=$(wc -c < "$OUT/$clip.gif" | tr -d ' ')
    [ "$size" -le "$GIF_MAX_BYTES" ] && break
    echo "  $clip.gif is $((size / 1024)) KiB at ${width}px, retrying smaller"
  done
  if [ "$size" -gt "$GIF_MAX_BYTES" ]; then
    echo "warning: $OUT/$clip.gif is still $((size / 1024)) KiB (target < 5 MiB)" >&2
  fi
done

for still in "$FRAMES"/stills/*.png; do
  ffmpeg -y -loglevel error -i "$still" -vf "scale='min(${STILL_WIDTH},iw)':-2:flags=lanczos" "$OUT/$(basename "$still")"
done
echo "Encoding portfolio montage"
mkdir -p "$OUT/portfolio"
inputs=()
filter=""
streams=""
for k in "${!PORTFOLIO_CLIPS[@]}"; do
  inputs+=(-framerate 30 -i "$FRAMES/${PORTFOLIO_CLIPS[$k]}/frame-%04d.png")
  filter+="[$k:v]${PORTFOLIO_FILTER}[v$k];"
  streams+="[v$k]"
done
filter+="${streams}concat=n=${#PORTFOLIO_CLIPS[@]}:v=1:a=0[out]"
# Two-pass VP9 at a fixed bitrate: constant quality gave 26 MB on fractal detail.
# 650 kb/s keeps the ~37 s montage around 3 MB, like the other portfolio videos.
vp9=(-c:v libvpx-vp9 -pix_fmt yuv420p -b:v 650k -row-mt 1 -an -passlogfile "$FRAMES/vp9")
ffmpeg -y -loglevel error "${inputs[@]}" -filter_complex "$filter" -map "[out]" "${vp9[@]}" -pass 1 -f webm /dev/null
ffmpeg -y -loglevel error "${inputs[@]}" -filter_complex "$filter" -map "[out]" "${vp9[@]}" -pass 2 "$OUT/portfolio/fractals.webm"
# Poster: the last frame of the Burning Ship zoom, the most striking still.
poster=$(ls "$FRAMES/burning-ship-zoom" | tail -n 1)
# This ffmpeg build has no WebP encoder: crop to PNG, then convert with cwebp.
ffmpeg -y -loglevel error -i "$FRAMES/burning-ship-zoom/$poster" -vf "$PORTFOLIO_FILTER" "$FRAMES/poster.png"
cwebp -quiet -q 80 "$FRAMES/poster.png" -o "$OUT/portfolio/fractals.webp"

ls -lh "$OUT" "$OUT/portfolio"
