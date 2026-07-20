cd /root/.qwenpaw/workspaces/LvFang/media/BetterPlayerHUD/src/main/java/com/yourname/betterplayerhud
for f in *.java; do
  grep -n "@SubscribeEvent" "$f" | while IFS=: read -r line rest; do
    sed -n "${line},+4p" "$f"
    echo "--- $f:$line"
  done
done
