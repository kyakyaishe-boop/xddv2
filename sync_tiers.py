import json
import os
import re
import sys
from pathlib import Path

import requests

API = "https://discord.com/api/v10"
TOKEN = os.environ["DISCORD_BOT_TOKEN"]
CHANNEL_ID = os.environ["DISCORD_CHANNEL_ID"]
OUT = Path(os.environ.get("TIERS_FILE", "data/tiers.json"))

headers = {"Authorization": f"Bot {TOKEN}"}


def get_messages(after=None):
    params = {"limit": 100}
    if after:
        params["after"] = after
    r = requests.get(f"{API}/channels/{CHANNEL_ID}/messages", headers=headers, params=params, timeout=30)
    r.raise_for_status()
    return r.json()


def text_from_embed(embed):
    parts = []
    title = embed.get("title")
    if title:
        parts.append(title)
    for field in embed.get("fields", []):
        name = field.get("name", "")
        value = field.get("value", "")
        parts.append(f"{name}\n{value}")
    return "\n".join(parts)


def clean(s):
    return re.sub(r"<@!?(\d+)>", r"@\1", s or "").strip()


def parse_message(message):
    # TierList normally puts the result in an embed.
    for embed in message.get("embeds", []):
        fields = {f.get("name", "").strip().lower(): clean(f.get("value", "")) for f in embed.get("fields", [])}
        player = fields.get("player")
        ign = fields.get("ingame name")
        gamemode = fields.get("gamemode")
        new_rank = fields.get("new rank")
        tester = fields.get("tester", "")
        tested_at = fields.get("tested at", "")
        if not new_rank or not (player or ign) or not gamemode:
            continue

        def strip_backticks(v):
            return (v or "").replace("`", "").strip()

        player_value = strip_backticks(ign or player).lower()
        rank = strip_backticks(new_rank).upper()
        mode = strip_backticks(gamemode).upper()
        tester_value = strip_backticks(tester).replace("@", "")
        return {
            "player": player_value,
            "gamemode": mode,
            "tier": rank,
            "tester": tester_value,
            "testedAt": strip_backticks(tested_at),
            "updatedAt": int(message.get("timestamp", "").replace("-", "").replace(":", "").replace("T", "").replace("Z", "")[:14] or 0),
            "_message_id": message.get("id", "0"),
        }
    return None


def main():
    if not TOKEN or not CHANNEL_ID:
        raise SystemExit("Missing DISCORD_BOT_TOKEN or DISCORD_CHANNEL_ID")

    messages = get_messages()
    parsed = []
    for message in messages:
        item = parse_message(message)
        if item:
            parsed.append(item)

    # Keep the latest known result for each player + gamemode.
    db = {}
    if OUT.exists():
        try:
            old = json.loads(OUT.read_text())
            for player, entries in old.items():
                for entry in entries:
                    db[(player.lower(), entry.get("gamemode", "").upper())] = entry
        except Exception:
            pass

    for item in parsed:
        key = (item["player"].lower(), item["gamemode"].upper())
        item.pop("_message_id", None)
        db[key] = item

    output = {}
    for (player, _mode), entry in sorted(db.items()):
        output.setdefault(player, []).append(entry)

    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(output, indent=2, ensure_ascii=False) + "\n")
    print(f"Processed {len(parsed)} tier result(s); database now has {len(output)} player(s).")


if __name__ == "__main__":
    main()
