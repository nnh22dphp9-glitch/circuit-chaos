# KI-Entscheidungen und offene Fragen

Diese Datei hält bestätigte Spielregeln, Architekturentscheidungen und noch offene Fragen für die KI fest.

## Bestätigte Rahmenbedingungen

- Die KI bleibt Java-basiert und lokal im Spiel; keine kostenpflichtigen Dienste oder KI-Bibliotheken sind erforderlich.
- Die Planung betrachtet die fünf Programmregister und soll später die Programme aller Roboter berücksichtigen.
- Modulbewertung basiert auf tatsächlich vorhandenen/aktiven Modulen, Munition und dem jeweiligen Phasenzeitpunkt.
- Pit-Stop bleibt auch bei `KILL_ALL` relevant: Ein Schadenspunkt wird repariert (falls vorhanden) und ein neues Modul vergeben.
- `probableTargets` in `ComputerMoveInfo` ist nur eine grobe geometrische Heuristik und keine tatsächliche Trefferprognose.
- Es werden keine virtuellen Roboter angenommen.
- Aggressives Verhalten bleibt als Strategieoption erhalten; es darf nicht dazu führen, dass das eigentliche Spielziel ignoriert.
- Die Waffenwahl erfolgt vor der Programmausführung für jede Phase. Während der Ausführung kann die KI ihre Wahl nicht nachträglich ändern. Besitzt ein Roboter eine Waffe, muss er eine Waffe auswählen; die KI darf nicht absichtlich „keine Waffe“ wählen.
- `Shooter` sollte die für die Phase bereits ausgewählte Waffe eindeutig erhalten bzw. daraus ableiten. Die interne Reihenfolge von Waffenprüfungen ist keine strategische Wahl, wenn pro `Shooter` nur eine Waffe verfügbar ist. Die Schnittstelle kann expliziter gestaltet werden, falls sie die Vorentscheidung nicht klar erkennen lässt.
- Spieler kennen gegnerische Programme und nicht standardmäßig bekannte Module nicht. Die KI darf geheime gegnerische Informationen nicht für Entscheidungen aus Spielersicht verwenden; sichtbare Aktionen und standardmäßig bekannte Module dürfen berücksichtigt werden.
- Der High-Power-Laser kann den ersten Roboter durchdringen und einen dahinterliegenden Roboter treffen.
- Die Regeln zu Reichweite, Sichtlinie, Höhe und blockierenden Objekten gelten für alle Beams, nicht nur für den High-Power-Laser. Roboter blockieren grundsätzlich Strahlen; die Durchdringung des ersten Roboters ist die Ausnahme für den High-Power-Laser. Solide Wände und Repulsor-Felder blockieren. One-Way-Wände blockieren Rot-nach-Grün, aber nicht Grün-nach-Rot. Höhere Levels blockieren; niedrigere Levels blockieren nicht und dort stehende Roboter werden nicht getroffen.
- `RAMMING_GEARS` ist eine permanente Verbesserung und wird bei einem Zusammenstoß nicht deaktiviert. Sie verursacht bei Kollisionen zusätzlichen Schaden. Eine Deaktivierung im Code wäre ein Fehler.
- `GYROSCOPIC_STABILIZER` verhindert die automatische Drehung sowohl auf Gears als auch auf Conveyor Belts.
- Ein Power-Down wird während der Programmierung angekündigt. Der Roboter setzt eine Runde aus und verliert seine bisherigen Schadenspunkte, bleibt aber auf dem Spielfeld und kann erneut Schaden erhalten. `POWER_DOWN_SHIELD` schützt in diesem Zustand zumindest vor Laser-Strahl-Schaden; es ist keine allgemeine Unverwundbarkeit.
- Die frühere fehlerhafte Einschränkung in `Player.takeDamage`, durch die Schaden nur mit `source == null` verarbeitet wurde, wurde laut Spieleigner behoben.

## Offene Fragen

1. **Shooter-Schnittstelle:** Sollte die ausgewählte Waffe explizit als Parameter übergeben werden, damit klar ist, dass die Auswahl bereits vor Programmausführung erfolgt? Das ist eine mögliche Klarstellung der Schnittstelle, kein bestätigtes Regelproblem.
2. **Beam-Regressionsfälle:** Die bestätigten Regeln sollten durch gezielte Tests für Höhenunterschiede, One-Way-Wände in beiden Richtungen, Roboter als Hindernisse und den zweiten Treffer des High-Power-Lasers abgesichert werden.

## Nächster Arbeitsschritt

Die dokumentierten Regeln als Grundlage für gezielte Regressionstests der Engine verwenden. Erst danach die KI so erweitern, dass sie die zentralen Engine-Regeln wiederverwendet, statt Waffen- und Beam-Verhalten separat nachzubilden.