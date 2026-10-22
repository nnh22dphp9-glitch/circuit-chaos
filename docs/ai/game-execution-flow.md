# Tatsächlicher Spielablauf als Grundlage für die Computer-KI

Stand: `develop`, geprüft am 2026-10-22.

Zweck dieses Dokuments ist, den tatsächlichen Ablauf der Engine zu beschreiben, bevor `ComputerPlayer` und `ComputerMoveInfo` überarbeitet werden. Die bestehende Computerlogik stammt aus einer älteren Regelbasis und wurde nicht vollständig an Circuit Chaos angepasst. Unterschiede zwischen ihr und der Engine sind deshalb zunächst erwartbare Lücken der KI-Simulation und nicht automatisch Fehler der eigentlichen Spielmechanik.

## Zentrale Implementierungsstellen

- [Game.java](../../src/main/java/de/phlup/circuitchaos/server/game/Game.java): Spiel-/Rundenablauf, Phasen, Bewegung, Effekte, Waffen, Schaden, Checkpoints und Zerstörung.
- [Shooter.java](../../src/main/java/de/phlup/circuitchaos/server/game/Shooter.java): Ermittelt pro Phase für einen Roboter die aktive Waffe und ihre Ziele.
- [Player.java](../../src/main/java/de/phlup/circuitchaos/server/player/Player.java): Spielerzustand und Initialisierung einer Runde.
- [NetworkPlayer.java](../../src/main/java/de/phlup/circuitchaos/server/player/NetworkPlayer.java): Übergibt den Programmierzustand an den Netzwerkspieler und übernimmt dessen Antwort.
- [ComputerPlayer.java](../../src/main/java/de/phlup/circuitchaos/server/player/ComputerPlayer.java): Wählt aktuell ein Programm anhand der bestehenden Heuristik.
- [ComputerMoveInfo.java](../../src/main/java/de/phlup/circuitchaos/server/player/ComputerMoveInfo.java): Bestehende, vereinfachte Bewegungssimulation und Bewertung.

## 1. Ablauf einer Runde

Der äußere Spielablauf in `Game.play(...)` ist:

1. Spielziel setzen und Spieler/Roboter vorbereiten.
2. Solange kein Gewinner feststeht, noch Spieler vorhanden sind und das Spiel nicht abgebrochen wurde:
   1. Rundenbeginn anzeigen.
   2. `deal()`: Programm-Karten für jeden nicht ausgeschalteten Roboter bereitstellen. Die Anzahl hängt unter anderem vom Schaden und vom `EXTRA_PROGRAM`-Modul ab.
   3. `interactWithAllPlayers(...)`: Netzwerkspieler zur Programmierung auffordern und ComputerPlayer ihre Programme auswählen lassen. Auf Netzwerkantworten wird anschließend gewartet und die Antwort übernommen.
   4. `loopThroughPhases()`: die fünf Programmphasen ausführen.
   5. `endOfTurnCourseEffects(...)`: unter anderem Flugzustände beenden, zerstörte Roboter behandeln und ausgeschiedene Spieler entfernen.
   6. Rundenende anzeigen.

Die Programmierung – inklusive der Auswahl von Programm und Modulen/Waffen durch den Spieler – erfolgt vor der Ausführung. Während die fünf Phasen abgearbeitet werden, kann ein Spieler sein Programm nicht spontan an neue Ziele oder Kollisionen anpassen.

## 2. Ablauf jeder der fünf Phasen

`Game.loopThroughPhases()` durchläuft die Phasen `0` bis `4`. Für jede Phase gilt diese Reihenfolge:

1. **Phasenbeginn:** `START_OF_TURN_COURSE_EFFECTS` wird gemeldet.
2. **Programme vorbereiten und aufdecken:** `stepRevealProgrammes(phase)`
   - Randomizer auf dem Spielfeld können den Programm-Schritt der betroffenen Roboter für diese Phase neu erzeugen.
   - Roboter werden nach ihrer Priorität sortiert.
   - Die Programm-Schritte werden aufgedeckt.
3. **Roboter bewegen:** `stepRobotsMove(phase, robots)`
   - Aktive Falltüren werden vor der Bewegung abgewickelt.
   - Die Ramming Armor wird für die Bewegungsauflösung phasenweise deaktiviert/vorbereitet.
   - Die Roboter bewegen sich in Prioritätsreihenfolge.
   - Vor der Bewegung werden entsprechende Geräte freigesetzt/aktiviert.
   - Die Bewegungsstärke wird bestimmt, einschließlich des Bremsen-Moduls.
   - Hovercraft-/Flugzustand, Teleporter sowie Wasser und Öl beeinflussen die Bewegung.
   - Die eigentliche Bewegung wird ausgeführt; anschließend wird der Programm-Schritt gegebenenfalls als Drehung angewendet.
   - Nach der Bewegungsauflösung wird die phasenbezogene Ramming-Armor-Behandlung abgeschlossen.
4. **Spielfeldelemente bewegen:** `stepCourseElementsMove(phase)`, in dieser Reihenfolge:
   1. Express-Förderbänder (1 Feld, ggf. mit Drehung)
   2. normale und Express-Förderbänder (1 (weiteres) Feld, ggf. mit Drehung)
   3. Pusher
   4. Zahnräder (Drehung)
5. **Strahlen und Laser auflösen:** `stepResolveBeamsAndLaserFire(phase)`
   - Roboterwaffen werden in dieser Reihenfolge abgewickelt: Tractor Beam, Pressure Beam, Spin Left Beam, Spin Right Beam, Exchange Beam, Laser.
   - Danach folgen die entsprechenden kursmontierten Effekte: Tractor Beam, Pressure Beam, Laser.
   - Für Roboterwaffen werden Schützen, Waffen und Ziele über `Shooter` ermittelt. Die Zielpositionen werden vor der jeweiligen Abwicklung festgehalten, sodass die Abwicklung einer Waffengruppe nicht einfach mit einer später neu berechneten Zielposition gleichzusetzen ist.
6. **Checkpoints und berührte Objekte:** `setTouchCheckpoints(phase)`
   - Checkpoints aktualisieren den nächsten Checkpoint und Archivposition/-höhe.
   - Pit-Stop-Ankunft wird in Phase 5 der Runde (Index `4`) berücksichtigt.
   - Minen und Proximity-Minen können auslösen.
7. **KILL_ALL-Auswertung:** Verlorene Spieler werden entfernt und geprüft, ob noch ein Sieger übrig ist.

Die Phasenschleife endet vorzeitig, wenn ein Gewinner feststeht, keine Spieler mehr übrig sind oder das Spiel abgebrochen wurde.

## 3. Ende der Runde, Zerstörung und Power-Down

Am Rundenende werden unter anderem folgende Dinge behandelt:

- Ein noch fliegender Roboter wird gelandet.
- Die Möglichkeit zur freien Richtungswahl wird zurückgesetzt.
- Bei mindestens 10 Schadenspunkten wird ein Roboter im Zielmodus `FINISH` grundsätzlich zerstört und – sofern er nicht ausgeschieden ist – an seine Archivposition zurückgesetzt. Dabei werden Schaden, blockierte Register und Archivmodule berücksichtigt.
- Ein angekündigter Power-Down wird erst nach Ausführung des aktuellen Programms wirksam: Der Roboter wird für die nächste Runde als ausgeschaltet markiert, sein bisheriger Schaden wird zurückgesetzt und blockierte Register werden freigegeben. Er bleibt dabei auf dem Spielfeld und kann in der laufenden Runde noch Schaden erhalten.
- Im Zielmodus `KILL_ALL` werden ausgeschiedene Spieler entfernt und der letzte verbleibende Spieler gewinnt.

Die konkreten Bedingungen und Reihenfolgen in `handleDestroyedRobots(...)`, `resetPoweredDownForDestroyedRobot(...)` und `endOfTurnCourseEffects(...)` müssen beim späteren Modellieren unverändert berücksichtigt oder bewusst als Regeländerung dokumentiert werden.

## 4. Wichtige Konsequenzen für die neue KI

Die KI sollte nicht einfach die bisherige `ComputerMoveInfo.simulateCourse()`-Logik weiter vergrößern, ohne die Abfolge der Engine als Referenz zu verwenden.

- **Eine Bewegung ist nicht das Ende einer Phase.** Nach der eigenen Programmbewegung können Förderbänder, Pusher, Zahnräder, Strahlen/Laser, Minen und Checkpoints den Zustand weiter verändern.
- **Reihenfolge ist relevant.** Roboter bewegen sich nach Programm-Priorität. Roboterbewegungen finden vor Spielfeldelementbewegung und vor der Waffenauflösung statt.
- **Andere Roboter sind Teil des Zustands.** Sie können Bewegungen blockieren, geschoben werden, Waffen abfangen oder Ziele sein. Die gegnerischen Programme sind nicht bekannt; die KI kann daher nur plausible Szenarien bewerten, keine tatsächlichen geheimen Programme voraussetzen.
- **Waffenentscheidungen sind vor der Ausführung festgelegt.** Die KI muss eine erlaubte Waffenauswahl vor Programmablauf berücksichtigen und kann Ziele nicht während der Phasen neu auswählen.
- **Module und Effekte haben unterschiedliche Zeitpunkte.** Einige wirken bei der Bewegungsbestimmung, andere beim Gerätefreisetzen, bei automatischen Spielfeldeffekten, bei Treffern oder am Rundenende.
- **Der Zustand ist veränderlich.** Die bestehende Simulation referenziert das echte `Game`, `Course` und `Player`-Objekt. Eine künftige Suche über viele Kandidaten braucht eine sichere, unabhängige Zustandsrepräsentation oder eine andere garantiert nebenwirkungsfreie Simulationsstrategie.
- **Die Bewertung muss zielabhängig sein.** `FINISH` und `KILL_ALL` verfolgen unterschiedliche Ziele. Überleben, Fortschritt, Checkpoints, Schaden, Pit Stop, Module und offensive Möglichkeiten müssen je nach Spielmodus unterschiedlich gewichtet werden.

## 5. Was diese Inventur noch nicht leistet

Dieses Dokument ist eine Übersicht des Kontrollflusses, keine vollständige Spezifikation jeder Regel. Insbesondere müssen für die spätere KI-Arbeit noch die konkreten Details von `move(...)`, `getLaserTarget(...)`, `Player.takeDamage(...)`, den einzelnen Modulen und den dynamischen Spielfeldelementen gezielt abgeglichen werden. Ebenso ist noch nicht durch Tests nachgewiesen, dass die bestehende `ComputerMoveInfo`-Simulation in den von ihr abgebildeten Fällen mit der Engine übereinstimmt.

Der nächste sinnvolle Schritt ist deshalb eine **Mechanik-für-Mechanik-Matrix**: tatsächlicher Engine-Ablauf, derzeitige Abdeckung in `ComputerMoveInfo`, Abweichung/Unsicherheit und benötigter Test. Erst auf dieser Grundlage sollten wir die Simulationsarchitektur festlegen.
