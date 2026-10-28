"""Drive a connected Android device through the booking flow and save screenshots.

Run after selecting a valid date range in the app. Uses only ADB and the system
UI hierarchy; no application test-only hooks are present in the release build.
"""

from pathlib import Path
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

SERIAL = sys.argv[1] if len(sys.argv) > 1 else "emulator-5554"
ANDROID_SDK = (os.environ.get("ANDROID_HOME") or r"C:\Android\sdk").strip()
ADB = str(Path(ANDROID_SDK) / "platform-tools" / "adb.exe")
ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "screenshots"
OUT.mkdir(exist_ok=True)


def adb(*args: str) -> bytes:
    return subprocess.check_output([ADB, "-s", SERIAL, *args], stderr=subprocess.STDOUT)


def nodes():
    adb("shell", "uiautomator", "dump", "/sdcard/luma-ui.xml")
    xml = adb("exec-out", "cat", "/sdcard/luma-ui.xml")
    return list(ET.fromstring(xml).iter("node"))


def find(identifier: str, by_text: bool = False, scroll: bool = True):
    for attempt in range(7 if scroll else 1):
        for node in nodes():
            value = node.get("text", "") if by_text else node.get("resource-id", "")
            if value == identifier or (not by_text and value.endswith(":id/" + identifier)):
                bounds = list(map(int, re.findall(r"\d+", node.get("bounds", ""))))
                if len(bounds) == 4 and bounds[3] > bounds[1]:
                    return node, ((bounds[0] + bounds[2]) // 2, (bounds[1] + bounds[3]) // 2)
        if attempt < 6 and scroll:
            adb("shell", "input", "swipe", "520", "2030", "520", "530", "320")
            time.sleep(0.3)
    raise AssertionError(f"UI element not visible: {identifier}")


def tap(identifier: str, by_text: bool = False, scroll: bool = True):
    node, (x, y) = find(identifier, by_text, scroll)
    adb("shell", "input", "tap", str(x), str(y))
    time.sleep(0.4)
    return node


def write(identifier: str, value: str):
    tap(identifier)
    adb("shell", "input", "text", value)
    time.sleep(0.3)
    adb("shell", "input", "keyevent", "4")
    time.sleep(0.4)


def shot(name: str):
    remote = f"/sdcard/{name}.png"
    adb("shell", "screencap", "-p", remote)
    adb("pull", remote, str(OUT / f"{name}.png"))
    print(f"saved {name}.png")


if "--resume-permission" not in sys.argv:
    assert find("check_in")[0].get("text", "").strip(), "Choose dates before running"
    assert find("check_out")[0].get("text", "").strip(), "Choose dates before running"
    write("name", "Ivanov%sIvan")
    write("passport", "1234567890")
    adb("shell", "am", "force-stop", "ru.lumastay.java")
    adb("shell", "monkey", "-p", "ru.lumastay.java", "1")
    time.sleep(3)
    assert "Ivanov" in find("name")[0].get("text", ""), "Name was not restored"
    assert "1234567890" in find("passport")[0].get("text", ""), "Passport was not restored"
    shot("draft-restored")
    write("phone", "79991234567")
    write("email", "guest@example.com")
    write("guests", "2")
    room = tap("room")
    left, top, right, bottom = map(int, re.findall(r"\d+", room.get("bounds", "")))
    # The exposed dropdown is a popup window and is absent from UIAutomator's
    # main-window hierarchy. Choose the second visible row by its field offset.
    adb("shell", "input", "tap", str(left + 240), str(bottom + 255))
    time.sleep(0.4)
    submit, _ = find("submit_button")
    assert submit.get("enabled") == "true", "Valid form did not enable submit"
    shot("valid-form")
    if "--skip-permission" not in sys.argv:
        tap("notifications_button")
        shot("permission-explanation")
        tap("Продолжить", by_text=True)
        find("com.android.permissioncontroller:id/permission_deny_button", scroll=False)
        time.sleep(0.5)
        shot("permission-request")
if "--skip-permission" not in sys.argv:
    tap("com.android.permissioncontroller:id/permission_deny_button", scroll=False)
    time.sleep(2)
tap("submit_button")
time.sleep(2)
shot("confirmation-dialog")
tap("Подтвердить", by_text=True)
shot("booking-complete")
tap("В календарь", by_text=True)
time.sleep(1)
print("Device smoke flow completed.")
