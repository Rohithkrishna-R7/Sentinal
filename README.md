# Sentinel — Real-Time Security Monitor

A Java 21 Swing desktop application that monitors a chosen directory for suspicious
file-system activity and watches running processes for known-bad name patterns.
Built as a KTU Semester 3 CSE Object-Oriented Programming project.

---

## Table of Contents

1. [Overview](#1-overview)
2. [Features](#2-features)
3. [Requirements](#3-requirements)
4. [Folder Structure](#4-folder-structure)
5. [Build Instructions](#5-build-instructions)
6. [Run Instructions](#6-run-instructions)
7. [Detection Rules](#7-detection-rules)
8. [Overall Risk Score](#8-overall-risk-score)
9. [Triggering the Demo Pattern (Rule B)](#9-triggering-the-demo-pattern-rule-b)
10. [CSV Log Format](#10-csv-log-format)
11. [Classroom Demonstration (11 Steps)](#11-classroom-demonstration-11-steps)
12. [Known Limitations](#12-known-limitations)

---

## 1. Overview

Sentinel watches a single directory using Java's built-in `WatchService` API and
polls all running processes using `ProcessHandle.allProcesses()`. When it detects
suspicious activity it assigns a risk score, shows the threat in a coloured table,
and appends every event and threat to a CSV log.

No external libraries are used. No files or processes are ever modified, deleted,
quarantined, or terminated.

---

## 2. Features

- **Real-time file monitoring** — detects files created, modified, or deleted in the
  watched directory using `WatchService`.
- **Process monitoring** — detects newly-started processes every 2 seconds.
- **Three detection rules** (see §7 below).
- **Risk scoring** — every threat gets a numeric score (0–100) and a severity band
  (LOW / MEDIUM / HIGH / CRITICAL).
- **Dark-themed dashboard** — status indicator, stat cards, events table, colour-coded
  threats table, and status bar.
- **CSV log** — every event and threat is appended to `data/events.csv` in RFC 4180
  format (UTF-8, handles commas and quotes inside fields).
- **Session risk score** — tracks the highest single-threat score seen since monitoring
  started.

---

## 3. Requirements

| Requirement | Value |
|-------------|-------|
| JDK | 21 or later (tested on JDK 27) |
| OS | Windows (WatchService and ProcessHandle behaviour are Windows-specific) |
| Build tool | None — plain `javac` |
| External libraries | None |

---

## 4. Folder Structure

```
sentinel/
├── src/
│   └── sentinel/
│       ├── Main.java                  Entry point; applies dark theme
│       ├── model/
│       │   ├── AppEvent.java          Value object for one detected event
│       │   ├── ThreatAlert.java       Value object for one threat alert
│       │   └── Severity.java          Enum: LOW / MEDIUM / HIGH / CRITICAL
│       ├── monitor/
│       │   ├── FileMonitor.java       WatchService wrapper
│       │   └── ProcessMonitor.java    ProcessHandle poller
│       ├── core/
│       │   ├── ThreatDetector.java    All detection rules + session risk score
│       │   ├── EventStorage.java      CSV read/write
│       │   └── SentinelService.java   Orchestration layer + Listener interface
│       └── ui/
│           ├── Dashboard.java         Main JFrame
│           ├── EventTableModel.java   AbstractTableModel for events table
│           └── ThreatTableModel.java  AbstractTableModel for threats table
├── data/
│   └── events.csv     Created automatically on first run
├── out/               Compiled .class files (created by build command)
├── README.md
└── .gitignore
```

---

## 5. Build Instructions

Open PowerShell in the project root (`sentinel/`) and run:

```powershell
# Make sure the output directory exists
New-Item -ItemType Directory -Force -Path out | Out-Null

# Collect all .java files and compile them
$files = Get-ChildItem -Path src -Recurse -Filter *.java | ForEach-Object { $_.FullName }
javac --release 21 -d out $files
```

You should see no output if the build is successful (exit code 0).

---

## 6. Run Instructions

From the same project root, after building:

```powershell
java -cp out sentinel.Main
```

The dashboard will open. To start monitoring:
1. Click **Browse…** and select any folder (e.g. your Downloads folder).
2. Click **▶ Start**.
3. Watch the Events and Threats tables populate as you create, modify, or delete
   files in the chosen directory.
4. Click **■ Stop** to stop. The CSV log is preserved between runs.

---

## 7. Detection Rules

All constants are defined at the top of `ThreatDetector.java` and can be edited
without touching any other file.

### Rule A — Executable-type file created (score: 40, MEDIUM)

**Trigger:** A `FILE_CREATED` event for a file whose name ends with one of:

`.exe` `.bat` `.cmd` `.ps1` `.msi` `.scr` `.vbs` `.jar`

**Alert message:** `Executable-type file created: <filename> – requires review`

> This is a heuristic. Many legitimate files have these extensions. The alert
> message deliberately says "requires review", not "malware".

---

### Rule B — Suspicious process name (score: 70, HIGH)

**Trigger:** A `PROCESS_STARTED` event whose process name or full path
contains any string in `SUSPICIOUS_PATTERNS` (case-insensitive substring match).

**Default patterns:** `["sentinel-demo"]`

**Alert message:** `Suspicious process detected matching pattern '<pattern>': <name>`

See §9 for how to safely trigger this in a classroom.

---

### Rule C — Rapid file-change burst (score: 60, HIGH)

**Trigger:** 5 or more `FILE_MODIFIED` or `FILE_DELETED` events within a 10-second
sliding window.

**Alert message:** `Rapid file change burst: <N> events in 10s – possible ransomware activity`

**Cooldown:** After one alert fires, further bursts are suppressed for 10 seconds
so a single busy-save event does not flood the threats table.

---

## 8. Overall Risk Score

The **Risk Score** stat card shows the **highest individual threat score** seen
during the current monitoring session.

- It is reset to **0** when you click **Start**.
- It is **not** a cumulative sum — it represents the single most severe threat
  observed in the session.
- Example: if Rule A (40) fires once and Rule C (60) fires once, the Risk Score
  will be **60**.

This is documented in `ThreatDetector.java` as `sessionRiskScore`.

---

## 9. Triggering the Demo Pattern (Rule B)

Rule B's default suspicious pattern is `"sentinel-demo"`. To trigger it safely
in a classroom without any real malware:

1. Find any harmless `.exe` already on your machine (for example,
   `C:\Windows\System32\notepad.exe`).
2. **Copy** it (do **not** move or rename the original) to your Desktop:
   ```powershell
   Copy-Item "$env:SystemRoot\System32\notepad.exe" "$env:USERPROFILE\Desktop\sentinel-demo.exe"
   ```
3. In Sentinel, set the monitored directory to your Desktop and click **Start**.
4. Double-click `sentinel-demo.exe` on the Desktop to launch it.
5. Sentinel will detect `sentinel-demo.exe` as a new process matching the pattern
   and raise a HIGH alert.
6. Close the notepad window. Delete `sentinel-demo.exe` when done.

> **Nothing harmful happens.** You are only running a copy of Notepad.
> Sentinel will never terminate, modify, or delete any file or process.

---

## 10. CSV Log Format

Log file location: `data/events.csv` (relative to the project root).

The file uses UTF-8 encoding, RFC 4180 quoting (fields containing commas, double-quotes,
or newlines are wrapped in double-quotes with embedded quotes doubled), and
one record per line. A header row is written when the file is created.

| Column | Name | Values |
|--------|------|--------|
| 1 | `timestamp` | ISO-8601 instant (e.g. `2026-10-18T17:22:08Z`) |
| 2 | `recordType` | `EVENT` or `THREAT` |
| 3 | `eventType` | `FILE_CREATED`, `FILE_MODIFIED`, `FILE_DELETED`, `PROCESS_STARTED` |
| 4 | `description` | Human-readable sentence |
| 5 | `source` | File path or process command path |
| 6 | `riskScore` | Integer 0–100 for THREATs; empty for EVENTs |
| 7 | `severity` | `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` for THREATs; empty for EVENTs |

The reader skips any malformed line and logs a warning to `stderr` instead of crashing.

---

## 11. Classroom Demonstration (11 Steps)

Follow these steps during the viva or a class demo. Each step shows one feature.

1. **Build** — open PowerShell in the project root, run the compile command (§5).
   Show that exit code is 0 and no errors appear.

2. **Start the app** — run `java -cp out sentinel.Main`. Point out the dark theme,
   the title "🛡 Sentinel", and the red/pink "Stopped" indicator dot.

3. **Browse for a folder** — click **Browse…**, pick your Desktop (or any writable
   folder). The path appears in the text field.

4. **Start monitoring** — click **▶ Start**. The dot turns green and shows "Running".
   Start and Stop buttons toggle correctly.

5. **File event** — create a new text file inside the watched folder (right-click →
   New → Text Document). Watch the **Events** table get a `FILE_CREATED` row within
   ~1 second.

6. **Rule A** — rename the text file to `test.exe` (or copy any file to `test.exe`).
   A `FILE_CREATED` row appears in Events **and** a MEDIUM threat row (score 40)
   appears in Threats with an amber background.

7. **Rule C** — quickly save/modify 6 or more files in the watched folder. You can
   do this by opening the folder in Notepad and saving repeatedly, or by running:
   ```powershell
   1..6 | ForEach-Object { "data$_" | Out-File -FilePath "$watchedFolder\burst$_.tmp" }
   ```
   A HIGH threat (score 60) should appear in Threats with a red background.

8. **Risk Score stat card** — point out that the Risk Score card now shows 60 (the
   highest score seen this session). It does not sum scores; it shows the maximum.

9. **Rule B (demo pattern)** — follow the steps in §9 above. Show the HIGH alert
   (score 70) naming the matching pattern `sentinel-demo`.

10. **CSV log** — open `data/events.csv` in Notepad or Excel. Show the header row,
    EVENT rows, and THREAT rows including the risk score and severity columns.

11. **Stop and exit** — click **■ Stop** (dot turns red, status "Stopped").
    Close the window with the ✕ button. The app exits cleanly; no errors in the
    console.

---

## 12. Known Limitations

| Limitation | Details |
|------------|---------|
| **Single directory only** | Only the directly selected folder is watched. Sub-directories are not monitored. |
| **Windows only** | `WatchService` event kinds and `ProcessHandle.allProcesses()` behaviour are tested on Windows only. |
| **New process ≠ automatic threat** | Only processes whose names match the configured `SUSPICIOUS_PATTERNS` trigger Rule B. Most new processes are benign. |
| **Rule C is a heuristic** | Backup software, IDEs auto-saving, and build tools can all trigger the burst rule. Treat it as a prompt for investigation, not a confirmation of attack. |
| **CSV is append-only** | There is no built-in log rotation. The file grows indefinitely. Delete or archive it manually. |
| **Events table is capped at 200 rows** | Older rows are removed to keep memory bounded. Check the CSV for the full history. |
| **WatchService may miss events** | If the OS event queue overflows, a `OVERFLOW` event is generated and some file events may be lost. An error is shown in the status bar. |
| **Protected processes are hidden** | Many Windows system processes do not expose their command path to unprivileged users. They appear with `"unknown"` as the source. |
| **No persistence of session risk score** | The risk score resets to 0 on every Start click. |
