# KI-Entscheidungen und Architekturhinweise

Die abgestimmte Spielanleitung ist die maßgebliche Quelle für Spielregeln. Diese Notiz hält technische Entscheidungen und KI-relevante Konsequenzen fest; bereits in der Spielanleitung geklärte Regeln werden nicht erneut als offene Fragen behandelt.

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
- Der programmierende Roboter kennt seine eigene Kartenhand (`potentialProgramme`) vollständig und muss ausschließlich diese Karten verwenden; jede Karte darf höchstens einmal eingesetzt werden. Die Programmkarten anderer Roboter bleiben unbekannt. Die KI darf dagegen alle Module aller Roboter aus dem Simulationszustand verwenden, auch wenn diese Module für menschliche Gegner nicht sichtbar wären.
- Der High-Power-Laser kann den ersten Roboter durchdringen und einen dahinterliegenden Roboter treffen.
- Die Regeln zu Reichweite, Sichtlinie, Höhe und blockierenden Objekten gelten für alle Beams, nicht nur für den High-Power-Laser. Roboter blockieren grundsätzlich Strahlen; die Durchdringung des ersten Roboters ist die Ausnahme für den High-Power-Laser. Solide Wände und Repulsor-Felder blockieren. One-Way-Wände blockieren Rot-nach-Grün, aber nicht Grün-nach-Rot. Höhere Levels blockieren; niedrigere Levels blockieren nicht und dort stehende Roboter werden nicht getroffen.
- Das Modul heißt im aktuellen Code `RAMMING_ARMOR` (vom Spieleigner als Ramming Gears bezeichnet). Es ist eine dauerhafte Verbesserung, deren Schadenseffekt innerhalb einer Phase nach dem ersten Zusammenstoß nicht erneut ausgelöst werden soll. Wenn ein Roboter einen anderen über mehrere Felder schiebt, darf dadurch nur ein Schadenspunkt entstehen. Die Deaktivierung von `activeInPhase[phase]` im Kollisionspfad ist daher beabsichtigt; wichtig ist, dass das Modul in einer späteren Phase wieder aktiviert werden kann. Das ist kein Fehler allein aufgrund der Deaktivierung.
- `GYROSCOPIC_STABILIZER` verhindert die automatische Drehung sowohl auf Gears als auch auf Conveyor Belts.
- Ein Power-Down wird während der Programmierung angekündigt, aber das aktuelle Programm wird zunächst noch ausgeführt. Erst in der darauffolgenden Runde setzt der Roboter aus und verliert seine bisherigen Schadenspunkte. Er bleibt auf dem Spielfeld und kann erneut Schaden erhalten. `POWER_DOWN_SHIELD` schützt in diesem Zustand zumindest vor Laser-Strahl-Schaden; es ist keine allgemeine Unverwundbarkeit.
- Die frühere fehlerhafte Einschränkung in `Player.takeDamage`, durch die Schaden nur mit `source == null` verarbeitet wurde, wurde laut Spieleigner behoben.

## Technische Überlegungen

- Die Shooter-Schnittstelle kann bei Gelegenheit expliziter machen, dass die Waffenauswahl bereits vor Programmausführung feststeht. Das ist eine mögliche Schnittstellenklarstellung, keine offene Regelfrage.
- KI-Logik soll zentrale Engine-Regeln wiederverwenden, statt Waffen- und Beam-Verhalten unabhängig nachzubilden.