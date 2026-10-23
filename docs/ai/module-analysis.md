# KI-Modulanalyse

Stand: erste Code-Inventur auf Branch `develop`.

Ziel: Tatsächliches Engine-Verhalten, beabsichtigte Spielregeln und spätere KI-Bewertung getrennt dokumentieren. Ein in der Engine vorhandenes Verhalten ist nicht automatisch die beabsichtigte Regel.

## 1. Zentrale Quellen

- `src/main/java/de/phlup/circuitchaos/common/enums/ModuleType.java`: Modulnamen, Standardaktivierung, Kennzeichnung als Waffe, Anfangsmunition.
- `src/main/java/de/phlup/circuitchaos/common/model/Module.java`: Laufzeitstatus eines Moduls (Munition, Aktivierung pro Phase, Richtung, Schutzstatus).
- `src/main/java/de/phlup/circuitchaos/common/model/Robot.java`: vorhandene Module und Abfrage `hasModule`/`getModule`.
- `src/main/java/de/phlup/circuitchaos/server/player/Player.java`: Aktivierung, Schaden, Schutzmodule, Pit-Stop.
- `src/main/java/de/phlup/circuitchaos/server/game/Shooter.java`: Zielermittlung und Auswahl der robotermontierten Waffe.
- `src/main/java/de/phlup/circuitchaos/server/game/Game.java`: tatsächliche Auflösung von Waffen, Bewegung und freigesetzten Geräten.
- `src/main/java/de/phlup/circuitchaos/server/player/ComputerMoveInfo.java`: aktuelle Simulation und Heuristik der KI.
- `src/main/java/de/phlup/circuitchaos/server/player/ComputerPlayer.java`: Programmauswahl und Bewertung.

## 2. Bereits festgestellte Grundregeln

- Ein Roboter hat nicht automatisch einen Laser. Eine Waffe ist nur verfügbar, wenn das entsprechende Modul vorhanden und in der betrachteten Phase aktiv ist.
- Die Engine behandelt Aktivierung pro Phase (`activeInPhase[0..4]`). `Player.uses(type, phase)` prüft Modulbesitz und dieses Aktivierungsfeld.
- Die Waffe wird vor der Programmausführung für die jeweilige Phase ausgewählt; während der Ausführung kann die Auswahl nicht mehr geändert werden. Die Reihenfolge der Prüfungen in `Shooter` ist deshalb keine strategische Auswahl zwischen mehreren Waffen: Ein `Shooter` sollte nur die für diese Phase ausgewählte Waffe erhalten bzw. eindeutig daraus ableiten. Die Schnittstelle sollte diese Vorentscheidung möglichst explizit machen.
- Pit-Stop: In Phase 4 ruft die Engine `Player.arriveAtPitStop()` auf. Das vergibt ein neues Modul und repariert einen Schadenspunkt, falls Schaden vorhanden ist. Das ist ein eigenständiger taktischer Vorteil und darf nicht entfernt werden, nur weil ein anderes Spielziel gewählt ist.
- Für `Goal.KILL_ALL` bedeutet Tod Ausscheiden: `Player.hasLost()` prüft `dieCounter > 0`; `takeDamage` erhöht den Schaden und entfernt den Roboter bei 10 Schaden.
- `ComputerMoveInfo` simuliert derzeit primär den eigenen Roboter. Die Gegnersuche `probableTargets` ist eine geometrische Näherung (Richtung und Entfernung bis 4 Felder), keine Simulation tatsächlicher Schüsse. Sie prüft aktuell nicht, ob eine passende Waffe vorhanden/aktiv ist und ignoriert Waffentyp, Schutz, Hindernisse und gegnerische Bewegungsvorhersagen. Daher ist sie keine belastbare Trefferwahrscheinlichkeit.

## 3. Waffen und Strahlmodule – Codeverhalten

| Modul | Tatsächliches Engine-Verhalten | KI-relevante Bedingungen / offene Punkte |
|---|---|---|
| `MAIN_LASER` | Trifft das erste gültige Ziel in Roboterrichtung und verursacht einen Schadenspunkt, sofern das Ziel nicht geschützt ist. | Modul muss vorhanden und aktiv sein; Sichtlinie, Höhe, Wände und Schutz zählen. |
| `DOUBLE_BARREL_LASER` | Wie Main Laser, verursacht beim getroffenen Ziel zwei Schadensaufrufe. | Schutz kann die Schadenswirkung beeinflussen; Details der Schadensreduktion berücksichtigen. |
| `HIGH_POWER_LASER` | Trifft einen Roboter und durchdringt ihn, um einen dahinterliegenden Roboter ebenfalls treffen zu können. | Wie bei allen Beams müssen Reichweite, Sichtlinie, Höhenunterschiede und blockierende Objekte korrekt berücksichtigt werden; zusätzlich gilt die Durchdringung des ersten Roboters. |
| `REAR_LASER` | Schießt entgegen der Roboterrichtung. | KI muss hinter dem Roboter nach Zielen suchen; die derzeitige `probableTargets`-Heuristik schaut nur nach vorn. |
| `PRESSURE_BEAM` | Trifft das erste gültige Ziel vor dem Roboter und schiebt es ein Feld in Schussrichtung. | Push kann Kollisionen, Fallen, Abgründe und Kettenbewegungen auslösen. |
| `TRACTOR_BEAM` | Zieht das erste gültige Ziel ein Feld entgegen der Schussrichtung zum Schützen. | Wie Push: Folgewirkungen der Bewegung sind entscheidend. |
| `EXCHANGE_BEAM` | Tauscht Position und Level des Schützen und des getroffenen Roboters. | Kann sowohl den eigenen Weg verbessern als auch den eigenen Roboter gefährden; danach Position/Level neu bewerten. |
| `SPIN_LEFT_BEAM` | Dreht den getroffenen Roboter nach links. | Verändert dessen spätere Bewegungsrichtung und potenziell dessen Schussrichtung. |
| `SPIN_RIGHT_BEAM` | Dreht den getroffenen Roboter nach rechts. | Wie Spin Left in Gegenrichtung. |
| `RAMMING_ARMOR` (vom Spieleigner als Ramming Gears bezeichnet) | Dauerhafte Verbesserung, deren Schadenseffekt pro Phase höchstens einmal pro schiebendem Roboter ausgelöst werden soll. Bei einer Bewegung mit Stärke 2–4 darf derselbe Roboter nicht für jedes geschobene Feld erneut Schaden erhalten. | Die Deaktivierung von `activeInPhase[phase]` nach dem ersten Zusammenstoß ist beabsichtigt und gilt nur für diese Phase. Die phasenbezogene Logik in `stepRobotsMove(...)`, `robotsMove(...)` und `reactivateRammingArmor(...)` dient dazu, den Effekt für die nächste Phase wieder verfügbar zu machen. Kein Fehler allein aufgrund dieser Deaktivierung; bei Änderungen muss die Reaktivierung in späteren Phasen erhalten bleiben. |

### Gemeinsame Details der Strahlwaffen

- Die Zielermittlung für Beams muss für alle Strahlmodule Reichweite, Sichtlinie, Level/Höhe und blockierende Objekte nach denselben Grundregeln behandeln. Die besonderen Wirkungen einzelner Beams (z. B. Schieben, Ziehen, Drehen, Tauschen oder Durchdringen) kommen zusätzlich dazu.
- Robotermontierte Strahlen werden in fester Reihenfolge aufgelöst: Tractor, Pressure, Spin Left, Spin Right, Exchange, Laser. Danach folgen Kurs-Strahlen.
- Schutz wird bei Laserschaden über `Player.takeDamage(..., considerShields=true)` berücksichtigt. Das umfasst Power-Down Shield, Shield und Layered Armor, sofern die Bedingungen passen.
- Die tatsächliche Engine arbeitet mit dem Zustand zum Zeitpunkt der jeweiligen Auflösungsphase. Eine korrekte KI muss später die Phasenreihenfolge und Änderungen durch frühere Strahlen berücksichtigen.
- Noch zu prüfen: exakte High-Power-Laser-Regeln bei Höhen-/Wandübergängen und die Frage, ob die derzeitige Reihenfolge mehrerer gleichzeitig aktiver Laser bewusst so gewählt ist.

## 4. Andere Module – erste Klassifikation

### Bewegung / Orientierung / Programme

- `HOVERCRAFT`: bei Bewegung wird der Roboter fliegend gesetzt, wenn die Bewegung ungleich null ist; anschließend versucht die Engine zu landen. Abgrund-/Fallregeln unterscheiden zwischen Flug und Boden.
- `BRAKES`: reduziert eine positive oder negative Bewegung um ein Feld in Richtung null.
- `OVERDRIVE`, `REVERSE_DRIVE`: werden bei der Erzeugung der verfügbaren Programm-Karten berücksichtigt.
- `EXTRA_PROGRAM`: zusätzliche verfügbare Programm-Karten entsprechend der Munition.
- `GYROSCOPIC_STABILIZER`: verhindert die automatische Drehung sowohl auf Zahnrädern (*Gears*) als auch auf Förderbändern (*Conveyor Belts*).

### Schutz / Reparatur

- `SHIELD`: blockiert einen passenden gerichteten Treffer, verbraucht dabei seine Aktivierung für die Phase.
- `POWER_DOWN_SHIELD`: schützt einen heruntergefahrenen Roboter zumindest vor Laser-Strahl-Schaden. Ein Power-Down wird während der Programmierung angekündigt, aber das aktuelle Programm wird zunächst noch vollständig ausgeführt. Erst in der darauffolgenden Runde setzt der Roboter aus und verliert seine bisherigen Schadenspunkte. Er bleibt auf dem Spielfeld und kann erneut Schaden erhalten. Der Schild bedeutet daher keine allgemeine Unverwundbarkeit.
- `LAYERED_ARMOR`: verbraucht Munition, um Schaden bei einem entsprechend geschützten Treffer zu verhindern.
- Pit-Stop: heilt einen Schadenspunkt und vergibt ein neues Modul. Die zufällige Modulauswahl und der Nutzen des erhaltenen Moduls sind für eine spätere Strategie zu bewerten.
- `SUPERIOR_ARCHIVE_COPY`: wird bei der Wiederherstellung am Archiv berücksichtigt; die Engine setzt den Schaden dabei auf 0 statt auf 2.

### Geräte / Fallen / Kursveränderung

- `MOBILE_TELEPORTER`, `MOBILE_RANDOMIZER`, `GLUE_DISPENSER`, `OIL_DISPENSER`, `MINE_LAYER`, `PROXIMITY_MINE_LAYER`: werden bei aktiver Phase und ausreichender Munition auf offenem Boden freigesetzt; die Munition wird reduziert.
- `BRIDGE_PROJECTOR`: prüft das Feld vor dem Roboter. Bei Abgrund oder Falltür wird Munition verbraucht und das Feld zu offenem Boden.
- `MECHANICAL_ARM`: kann beim Archiv-/Checkpoint-Schritt ein benachbartes Ziel bzw. einen Pit-Stop für die Archivposition berücksichtigen. Die genaue Auswahlreihenfolge und Koordinaten sind noch separat gegen die beabsichtigte Regel zu prüfen.

## 5. Auffälligkeiten für die KI

1. `ComputerMoveInfo.probableTargets` zählt geometrische Ziele ohne Waffenausstattung oder aktive Phase. Das kann einen Roboter ohne Waffe fälschlich für einen Angriff belohnen.
2. Die Heuristik bildet weder die unterschiedlichen Waffeneffekte noch tatsächliche Zielermittlung, Deckung, Schutz, Level oder Push-/Pull-Folgen ab.
3. Die Simulation muss Modulbesitz, Phase, Munition und Status aus dem aktuellen Zustand verwenden. Es darf kein eingebauter Standardlaser angenommen werden.
4. Der Pit-Stop ist ein eigenständiger positiver Faktor: +1 Reparatur (falls beschädigt) und ein neues Modul. Er bleibt auch bei `KILL_ALL` relevant.
5. Die Simulation ist noch keine gemeinsame Rundensimulation aller Roboter. Für verlässliche Vorhersagen müssen gegnerische Programme und deren Interaktionen später explizit einbezogen werden.

## 6. Nächste Schritte

1. Diese erste Inventur mit dem Spieleigner abgleichen, besonders Waffenauswahl bei mehreren installierten Modulen und High-Power-Laser.
2. Danach Geräte/Fallen einzeln prüfen (Auslösezeitpunkt, Aktivierungsdauer, Munition, Auswirkungen auf andere Roboter).
3. Anschließend defensive und Bewegungsmodule mit der Simulation vergleichen.
4. Erst nach dem Regelabgleich die KI-Bewertung ändern. Keine Änderungen an Spielregeln allein aufgrund dieser Dokumentation.


## 7. Erledigte Auffälligkeit: Schadensquelle

Die frühere Auffälligkeit in `Player.takeDamage`, bei der Schaden offenbar nur mit `source == null` verarbeitet wurde, wurde vom Spieleigner korrigiert. Sie ist daher kein offener Befund mehr. Bei späteren Änderungen sollte die Schadensbehandlung weiterhin für Angriffe mit einer nicht-null Quelle geprüft werden.


## 8. Ergänzungen nach Regelabgleich mit dem Spieleigner

### Bestätigte Regeln

- Pro Phase ist höchstens eine robotermontierte Waffe aktiv. Der NetworkPlayer wählt die Waffe pro Phase vor der Programmausführung; diese Auswahl kann während der laufenden Programmausführung nicht mehr geändert werden. Die Computer-KI muss ihre Waffenwahl ebenfalls vorab je Phase festlegen.
- Wenn der Roboter mindestens eine Waffe besitzt, darf er nicht absichtlich „keine Waffe“ wählen. Besitzt er beispielsweise nur den Exchange Beam, muss er ihn verwenden, auch wenn ein Treffer taktisch nachteilig wäre. Die KI darf die Waffe nicht nach Kenntnis des tatsächlichen Ziels nachträglich wechseln.
- Spieler kennen weder die Programme noch die Module anderer Spieler. Sichtbar sind nur Module, die ein Spieler tatsächlich aktiv einsetzt, sowie die Module, die andere Spieler standardmäßig zu Spielbeginn besitzen. Die KI darf daher nicht auf geheime gegnerische Module oder Programme zugreifen, wenn sie eine Entscheidung aus Spielersicht bewertet.
- High Power Laser: Der Strahl trifft einen Roboter und geht durch diesen hindurch, um einen dahinterliegenden Roboter ebenfalls zu treffen.
- Gewünschte Strahlblockierung: Roboter blockieren Strahlen, außer beim zweiten Treffer eines High Power Lasers; solide Wände, Repulsor-Felder, One-Way-Wände in Rot-nach-Grün-Richtung sowie ein höheres Level blockieren. Ein niedrigeres Level blockiert nicht; Roboter auf darunterliegenden Levels sollen nicht getroffen werden.
- Die frühere fehlerhafte Einschränkung auf `source == null` in `Player.takeDamage` wurde laut Spieleigner behoben; sie soll nicht mehr als aktueller Codebefund geführt werden.

### Regeln für die Strahl-Zielermittlung

Die folgenden vom Spieleigner bestätigten Regeln sind die maßgebliche Grundlage für Engine und KI:

- Alle Beams müssen Reichweite, Sichtlinie, Höhenunterschiede und blockierende Objekte korrekt berücksichtigen; das ist keine Besonderheit des High-Power-Lasers.
- Roboter blockieren Strahlen grundsätzlich. Ausnahme: Der High-Power-Laser kann den ersten Roboter durchdringen und einen dahinterliegenden Roboter treffen.
- Solide Wände und Repulsor-Felder blockieren Strahlen. One-Way-Wände blockieren in Rot-nach-Grün-Richtung, nicht in Grün-nach-Rot-Richtung.
- Ein höheres Level blockiert den Strahl. Ein niedrigeres Level blockiert ihn nicht; Roboter auf darunterliegenden Levels werden nicht getroffen.
- Zielermittlung und Strahlblockierung sollten von der Engine zentral bereitgestellt bzw. wiederverwendet werden, damit die KI keine abweichende zweite Beam-Logik entwickelt.

Diese Regeln sind fachliche Vorgaben. Nach Codeänderungen müssen sie anhand gezielter Spielsituationen regressionsgetestet werden.

### Ramming Armor: einmaliger Effekt pro Phase

Der Spieleigner hat klargestellt, dass die Deaktivierung während einer Phase beabsichtigt ist: Wenn ein Roboter einen anderen um mehrere Felder schiebt, darf der geschobene Roboter nicht für jedes Feld erneut Ramming-Schaden erhalten. Die Deaktivierung verhindert weitere Auslösungen in derselben Phase. Entscheidend ist, dass das Modul in einer späteren Phase wieder aktiviert werden kann. Die bestehende Deaktivierung in `Game.move(...)` ist daher nicht als Fehler zu behandeln, solange diese phasenbegrenzte Semantik und die spätere Reaktivierung korrekt funktionieren.
