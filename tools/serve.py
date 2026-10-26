"""Server statis untuk preview launcher.

Dipakai `npm start` (port 4173). Semua respons dikirim dengan header no-store
supaya browser tidak pernah menampilkan `scene.png` / `index.html` versi lama
setelah preview dibangun ulang.
"""
import functools
import http.server
import os
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
WEB = ROOT / "web"
PORT = int(os.environ.get("PORT", "4173"))


class NoCacheHandler(http.server.SimpleHTTPRequestHandler):
    def end_headers(self):
        self.send_header("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0")
        self.send_header("Pragma", "no-cache")
        self.send_header("Expires", "0")
        super().end_headers()

    def log_message(self, *args):  # ringkas: jangan banjiri log
        pass


def main() -> None:
    handler = functools.partial(NoCacheHandler, directory=str(WEB))
    with http.server.ThreadingHTTPServer(("0.0.0.0", PORT), handler) as httpd:
        print(f"preview {WEB} -> http://0.0.0.0:{PORT}", flush=True)
        httpd.serve_forever()


if __name__ == "__main__":
    main()
