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
- Mehrere Waffen sind alternative Module. `Shooter` wählt bei Laserfeuer in dieser Reihenfolge: Main Laser, High Power Laser, Double Barrel Laser, Rear Laser. Das bedeutet: Wenn mehrere davon vorhanden und aktiv sind, wird in diesem Schritt nur die erste passende Waffe gewählt.
- Pit-Stop: In Phase 4 ruft die Engine `Player.arriveAtPitStop()` auf. Das vergibt ein neues Modul und repariert einen Schadenspunkt, falls Schaden vorhanden ist. Das ist ein eigenständiger taktischer Vorteil und darf nicht entfernt werden, nur weil ein anderes Spielziel gewählt ist.
- Für `Goal.KILL_ALL` bedeutet Tod Ausscheiden: `Player.hasLost()` prüft `dieCounter > 0`; `takeDamage` erhöht den Schaden und entfernt den Roboter bei 10 Schaden.
- `ComputerMoveInfo` simuliert derzeit primär den eigenen Roboter. Die Gegnersuche `probableTargets` ist eine geometrische Näherung (Richtung und Entfernung bis 4 Felder), keine Simulation tatsächlicher Schüsse. Sie prüft aktuell nicht, ob eine passende Waffe vorhanden/aktiv ist und ignoriert Waffentyp, Schutz, Hindernisse und gegnerische Bewegungsvorhersagen. Daher ist sie keine belastbare Trefferwahrscheinlichkeit.

## 3. Waffen und Strahlmodule – Codeverhalten

| Modul | Tatsächliches Engine-Verhalten | KI-relevante Bedingungen / offene Punkte |
|---|---|---|
| `MAIN_LASER` | Trifft das erste gültige Ziel in Roboterrichtung und verursacht einen Schadenspunkt, sofern das Ziel nicht geschützt ist. | Modul muss vorhanden und aktiv sein; Sichtlinie, Höhe, Wände und Schutz zählen. |
| `DOUBLE_BARREL_LASER` | Wie Main Laser, verursacht beim getroffenen Ziel zwei Schadensaufrufe. | Schutz kann die Schadenswirkung beeinflussen; Details der Schadensreduktion berücksichtigen. |
| `HIGH_POWER_LASER` | Ermittelt ein Ziel im normalen Strahl und ein weiteres Ziel mit High-Power-Abfrage. Kann zwei Roboter treffen. | Reichweite/Sichtlinie und Höhenregeln in `Game.getLaserTarget` genau abbilden. |
| `REAR_LASER` | Schießt entgegen der Roboterrichtung. | KI muss hinter dem Roboter nach Zielen suchen; die derzeitige `probableTargets`-Heuristik schaut nur nach vorn. |
| `PRESSURE_BEAM` | Trifft das erste gültige Ziel vor dem Roboter und schiebt es ein Feld in Schussrichtung. | Push kann Kollisionen, Fallen, Abgründe und Kettenbewegungen auslösen. |
| `TRACTOR_BEAM` | Zieht das erste gültige Ziel ein Feld entgegen der Schussrichtung zum Schützen. | Wie Push: Folgewirkungen der Bewegung sind entscheidend. |
| `EXCHANGE_BEAM` | Tauscht Position und Level des Schützen und des getroffenen Roboters. | Kann sowohl den eigenen Weg verbessern als auch den eigenen Roboter gefährden; danach Position/Level neu bewerten. |
| `SPIN_LEFT_BEAM` | Dreht den getroffenen Roboter nach links. | Verändert dessen spätere Bewegungsrichtung und potenziell dessen Schussrichtung. |
| `SPIN_RIGHT_BEAM` | Dreht den getroffenen Roboter nach rechts. | Wie Spin Left in Gegenrichtung. |
| `RAMMING_ARMOR` | Sonderbehandlung bei Bewegungskonflikten: wird beim Zusammenstoß deaktiviert; der kollidierende Roboter kann Schaden erleiden. | Muss in der Kollisions-/Bewegungssimulation berücksichtigt werden; nicht wie eine Fernkampfwaffe bewerten. |

### Gemeinsame Details der Strahlwaffen

- `Shooter` ruft für Strahlen `Game.getLaserTarget(...)` auf. Die Zielermittlung berücksichtigt unter anderem Kursgrenzen, Level und bestimmte Wandtypen.
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
- `GYROSCOPIC_STABILIZER`: verhindert die automatische Drehung auf Zahnrädern.

### Schutz / Reparatur

- `SHIELD`: blockiert einen passenden gerichteten Treffer, verbraucht dabei seine Aktivierung für die Phase.
- `POWER_DOWN_SHIELD`: blockiert gerichtete Treffer über noch verfügbare Schutzseiten; Aktivierung hängt vom Power-Down-Zustand ab.
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


## 7. Wichtige Auffälligkeit: Schadensquelle wird offenbar herausgefiltert

Bei der Detailprüfung ist eine mögliche Inkonsistenz aufgefallen:

- `Game.robotMountedBeamsAndLasersShoot` ruft für Laserziele `playerOfTargetRobot.takeDamage(phase, player, dir, true)` auf. Dabei ist `source` der angreifende Spieler.
- `Player.takeDamage(Integer phase, Player source, Direction direction, boolean considerShields)` führt die Schadensbehandlung aktuell aber nur innerhalb von `if (robot.isOnCourse() && source == null)` aus.
- Auch der Ramming-Armor-Pfad ruft `takeDamage(phase, player, null, false)` mit einer nicht-null Quelle auf.

Nach dem gelesenen Code würden diese Aufrufe daher keine Schadenspunkte verursachen. Das könnte ein Fehler sein, oder die Bedeutung von `source` ist an anderer Stelle anders gedacht als der Methodenname vermuten lässt. **Vor jeder KI-Bewertung muss der Spieleigner diese Stelle prüfen und das beabsichtigte Verhalten bestätigen.** Ich habe den Spielcode nicht geändert.

Das ist besonders wichtig, weil wir sonst die KI auf eine angenommene Waffenwirkung trainieren, die von der Engine möglicherweise gar nicht ausgeführt wird.


## 8. Ergänzungen nach Regelabgleich mit dem Spieleigner

### Bestätigte Regeln

- Pro Phase ist höchstens eine robotermontierte Waffe aktiv. Der NetworkPlayer wählt die Waffe pro Phase vor der Programmausführung; diese Auswahl kann während der laufenden Programmausführung nicht mehr geändert werden. Die Computer-KI muss ihre Waffenwahl ebenfalls vorab je Phase festlegen.
- Wenn der Roboter mindestens eine Waffe besitzt, darf er nicht absichtlich „keine Waffe“ wählen. Besitzt er beispielsweise nur den Exchange Beam, muss er ihn verwenden, auch wenn ein Treffer taktisch nachteilig wäre. Die KI darf die Waffe nicht nach Kenntnis des tatsächlichen Ziels nachträglich wechseln.
- Spieler kennen weder die Programme noch die Module anderer Spieler. Sichtbar sind nur Module, die ein Spieler tatsächlich aktiv einsetzt, sowie die Module, die andere Spieler standardmäßig zu Spielbeginn besitzen. Die KI darf daher nicht auf geheime gegnerische Module oder Programme zugreifen, wenn sie eine Entscheidung aus Spielersicht bewertet.
- High Power Laser: Der Strahl trifft einen Roboter und geht durch diesen hindurch, um einen dahinterliegenden Roboter ebenfalls zu treffen.
- Gewünschte Strahlblockierung: Roboter blockieren Strahlen, außer beim zweiten Treffer eines High Power Lasers; solide Wände, Repulsor-Felder, One-Way-Wände in Rot-nach-Grün-Richtung sowie ein höheres Level blockieren. Ein niedrigeres Level blockiert nicht; Roboter auf darunterliegenden Levels sollen nicht getroffen werden.
- Die Prüfung von `source == null` in `Player.takeDamage` ist nach Rückmeldung des Spieleigners vermutlich ein unbeabsichtigter Fehler aus einer früheren Refaktorisierung.

### Kritischer Codebefund bei `Game.getLaserTarget`

Die aktuelle Methode scheint die bestätigten Regeln für robotermontierte Waffen nicht vollständig umzusetzen:

1. Im Schleifenblock, der Roboter auf der aktuellen Position untersucht, wird ein Ziel nur gesetzt, wenn `isCourseMounted` wahr ist. Bei robotermontierten Waffen ist dieser Parameter falsch. Dadurch werden dort keine Roboter als Ziel ausgewählt.
2. Die Wandprüfung blockiert `ONE_WAY_RED` auf der Quellseite, lässt `ONE_WAY_GREEN` dort aber passieren. Die Rückseite der Zielwand wird auf `SOLID` und `REPULSOR_FIELD` geprüft, nicht sichtbar auf beide One-Way-Farben. Das ist nicht ohne Weiteres mit der bestätigten Regel „Rot nach Grün blockiert, Grün nach Rot nicht“ gleichzusetzen.
3. `level` wird beim rekursiven Durchlaufen unverändert weitergegeben. Die aktuelle Methode findet bei robotermontierten Waffen kein Ziel anhand eines Levelvergleichs, weil die Zielerkennung dort nicht greift. Die gewünschte Regel, dass höhere Levels blockieren und niedrigere Levels weder blockieren noch getroffen werden, ist daher noch nicht sauber abgebildet.
4. Die Sonderbehandlung für High Power Laser greift bei einer blockierenden Wand, indem die Rekursion mit `highPowerLaser=false` fortgesetzt wird. Das allein beweist noch nicht, dass der Laser bei einem ersten Roboter korrekt hindurchgeht und einen zweiten trifft. Zielermittlung und Strahlblockierung sollten anhand der bestätigten Regel separat geprüft werden.
5. Die Schadensquelle wird bei `Player.takeDamage` aktuell nur akzeptiert, wenn `source == null`. Der Spieleigner bestätigt, dass dies vermutlich ein Refaktorisierungsfehler ist.

Diese Punkte sind Befunde zur vorhandenen Implementierung, keine bereits vorgenommenen Korrekturen. Vor dem Fix sollten die Regeln in einer zentralen Beschreibung festgehalten und anschließend die Engine-Zielermittlung korrigiert werden. Die KI sollte danach dieselbe Zielermittlung verwenden, statt eine zweite, abweichende Laserlogik zu implementieren.
