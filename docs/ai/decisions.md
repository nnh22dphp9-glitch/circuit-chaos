# KI-Entscheidungen und offene Fragen

Diese Datei hält bestätigte Spielregeln, Architekturentscheidungen und noch offene Fragen für die KI fest.

## Bestätigte Rahmenbedingungen

- KI bleibt Java-basiert und lokal im Spiel; keine kostenpflichtigen Dienste oder KI-Bibliotheken erforderlich.
- Die Planung betrachtet die fünf Programmregister und soll später die Programme aller Roboter berücksichtigen.
- Modulbewertung basiert auf tatsächlich vorhandenen/aktiven Modulen, Munition und dem jeweiligen Phasenzeitpunkt.
- Pit-Stop bleibt auch bei `KILL_ALL` relevant: ein Schadenspunkt wird repariert und ein neues Modul vergeben.
- `probableTargets` in `ComputerMoveInfo` ist nur eine grobe geometrische Heuristik und keine tatsächliche Trefferprognose.
- Es werden keine virtuellen Roboter angenommen.
- Aggressives Verhalten bleibt als Strategieoption erhalten; es darf nicht dazu führen, dass das eigentliche Spielziel ignoriert wird.

## Offene Fragen – bitte mit Spieleigner abgleichen

1. **Schadensbehandlung mit Quelle:** `Player.takeDamage(phase, source, direction, considerShields)` verarbeitet Schaden nur bei `source == null`, während Laser- und Ramming-Armor-Aufrufe eine nicht-null Quelle übergeben. Ist das beabsichtigt oder ein Fehler?
2. **Mehrere aktive Laser:** `Shooter` prüft Main Laser, High Power Laser, Double Barrel Laser und Rear Laser in dieser Reihenfolge und nimmt nur die erste passende Waffe. Soll pro Phase nur eine Laserwaffe feuern?
3. **High Power Laser:** Die genaue Regel, welche zwei Ziele bei unterschiedlichen Höhen und Hindernissen getroffen werden können, sollte anhand der gewünschten Spielregel bestätigt werden.

## Nächster Arbeitsschritt

Zuerst die offenen Fragen zur Waffenwirkung klären. Danach die Geräte und Fallen (Auslösezeitpunkt, Munition, Wirkungsdauer und Auswirkungen auf andere Roboter) inventarisieren. Noch keine Spiel- oder KI-Logik ändern, bevor diese Regeln geklärt sind.
