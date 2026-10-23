# Bewegungsengine: Öl, Flug und Teleporter

Stand: `develop`, geprüft am 2026-10-23. Diese Notiz dokumentiert den Abgleich von `Game`, `Teleporter` und `ComputerMoveInfo`. Sie beschreibt den vorhandenen Code und die bestätigten Regeln; sie ändert keine Spielmechanik.

## 1. Bestätigte Regel: Wasser, Öl und Hovercraft

- Wasser reduziert die Bewegungsstärke am Boden um eins.
- Öl reduziert die Bewegungsstärke am Boden ebenfalls um eins.
- Zusätzlich rutscht ein am Boden bewegter Roboter über ein Ölfeld weiter, bis er ein Feld erreicht, auf dem er nicht mehr rutschen muss, oder die Bewegung blockiert wird.
- Eine Wand oder ein anderer Roboter kann das Rutschen blockieren. Die Rutschbewegung darf dabei keinen Roboter wie bei einer normalen Schiebebewegung vor sich herschieben.
- Führt der Rutschweg in einen Abgrund, fällt der Roboter hinein.
- Während einer Hovercraft-Flugbewegung sind Wasser und Öl wirkungslos: Das gilt sowohl beim Abheben als auch während des Flugs.
- Landet der Roboter am Ende einer Hovercraft-Bewegung auf Öl, löst diese Landung kein Öl-Rutschen aus.

## 2. Was die Engine tatsächlich macht

### Bewegungsstärke und Abheben

In `Game.robotsMove()` wird zunächst die Bewegungsstärke bestimmt. Wenn das Modul `HOVERCRAFT` in der Phase verwendet wird, setzt die Engine vor der Behandlung von Wasser und Öl:

```java
robot.setFlying(movement != 0);
```

Danach wird `Teleporter.teleport(...)` aufgerufen. Falls keine Teleportation stattfindet, folgt `applyWaterAndOilSlick(...)`. Diese Methode reduziert die Bewegungsstärke nur dann, wenn `!ce.isFlying()` gilt und das Ausgangsfeld Wasser oder Öl ist.

**Befund:** Die Engine ignoriert Wasser und Öl beim Abheben korrekt, weil der Flugzustand bereits vor der Reduktion gesetzt wurde.

### Bewegung und Rutschen auf Öl

`Game.move(...)` ist keine reine Berechnung eines Zielorts. Die Methode verändert den echten Spielzustand und verarbeitet unter anderem:

- Klebstoff, der Bewegungsstärke verbrauchen kann;
- Wandtypen, Einbahnwände, Rampen und Höhenwechsel;
- Konflikte mit Robotern und rekursive Schiebebewegungen;
- Ramming-Armor-Schaden;
- Minen und Proximity-Minen bei relevanten Positionswechseln am Boden;
- Portale, die Position und Höhe ändern können;
- Abgründe und aktive Falltüren, die Tod/Fallbehandlung auslösen;
- weitere Bewegungsschritte bei mehr als einem Feld;
- Öl-Rutschen nach einer Positionsänderung.

Das Öl-Rutschen am Ende von `move(...)` ist an `!ce.isFlying()` gebunden. Es ruft für jedes Rutschfeld erneut `move(...)` mit Stärke `0`, Bewegungsmenge `1` und `notBlockedByMovableRobot = false` auf. Damit wird die Bewegung bei einem Konflikt nicht als normale Schiebekette fortgesetzt. Wenn die Position sich nicht mehr ändert, endet die Rutschschleife. Wird dabei ein Abgrund erreicht, verarbeitet `move(...)` den Fall.

### Landung nach einer Hovercraft-Bewegung

Nach `singleRobotMove(...)` ruft `robotsMove()` für einen weiterhin fliegenden Roboter `land(...)` auf. `land(...)` behandelt zuerst mögliche Roboterkonflikte, solange der Roboter noch als fliegend markiert ist, und setzt erst danach `flying = false`.

Das Öl-Rutschen wird bereits am Ende des vorherigen `move(...)` geprüft. Zu diesem Zeitpunkt war der Roboter noch fliegend, daher wurde die Rutschschleife übersprungen. `land(...)` enthält selbst keine Öl-Rutschlogik.

**Befund:** Der aktuelle Ablauf entspricht der beschriebenen Regel, dass eine Landung auf Öl kein Rutschen auslöst.

## 3. Abweichungen in `ComputerMoveInfo`

Die vorhandene Simulation weicht an mehreren Stellen ab:

1. **Öl reduziert auch im Flug die Bewegung.** Die Bedingung lautet sinngemäß `(Wasser && !flying) || Öl`. Der Ölteil ist nicht an `!flying` gebunden. Das ist ein bestätigter Simulationsfehler.
2. **Öl-Rutschen wird während des Flugs simuliert.** Die Schleifen über Öl prüfen nicht zuverlässig den Flugzustand. Ein simuliertes Hovercraft kann dadurch nach seiner Flugbewegung weitergeschoben werden, obwohl die Engine das nicht tut.
3. **Flugzustand ist nicht Teil des unabhängigen Kandidatenzustands.** `simulateCourse()` liest `player.getRobot().isFlying()` vom echten Roboter. Es leitet den Zustand nicht für jeden simulierten Programmplatz aus der gewählten Bewegung und dem verwendeten Hovercraft-Modul ab. Damit kann die Simulation auch beim Abheben oder in späteren Phasen den falschen Zustand verwenden.
4. **Rutschblockaden sind nicht gleichwertig.** Die Simulation bewegt im Wesentlichen nur ihre eigenen `x/y`-Koordinaten und prüft Wände; sie löst keine vollständigen Konflikte mit anderen Robotern auf. Rutschen kann deshalb an Robotern oder anderen dynamischen Hindernissen falsch bewertet werden.
5. **Tödliche Folgen stoppen die Simulation nicht zuverlässig.** `checkForAbyss()` setzt `willDie`, während die Simulation weitere Schritte berechnen kann. Das kann auch bei einem Rutschweg in einen Abgrund zu einem falschen Endzustand führen.

Damit ist die Wasser-/Öl-Abweichung nicht nur eine unklare Stelle: **Die aktuelle `ComputerMoveInfo` bildet die bestätigte Flug- und Ölregel nicht korrekt ab.**

## 4. `Teleporter.teleport()`: tatsächlicher Ablauf

`Game.robotsMove()` ruft `Teleporter.teleport(robot, movement, this, phase)` vor der normalen Bewegungsreduktion für Wasser/Öl und vor `singleRobotMove()` auf.

Die Teleporter-Klasse:

1. Sucht auf der aktuellen Position nach einem Objekt vom Typ `TELEPORTER`.
2. Aktiviert die Teleportation nur bei Bewegungsstärke ungleich null und wenn das bewegte Element nicht fliegt.
3. Berechnet aus der Bewegungsstärke eine Teleportdistanz: Bei negativer Bewegung wird `1 - movement`, bei positiver Bewegung `2 + movement` verwendet.
4. Bestimmt das Zielfeld anhand der aktuellen Blickrichtung.
5. Teleportiert nur, wenn das Zielfeld keine Objekte und Roboter enthält.
6. Setzt dann die Position über `game.setPosition(...)`, meldet die Änderung und gibt `hasBeenTeleported = true` zurück.

Wenn das Zielfeld belegt ist, wird nicht teleportiert; der ursprüngliche Bewegungswert bleibt erhalten und die normale Bewegung wird fortgesetzt. Wenn die Teleportation erfolgreich ist, beendet `robotsMove()` die Bearbeitung dieses Programm-Schritts an dieser Stelle: Es folgt für diesen Schritt keine normale Bewegung, keine Wasser-/Öl-Reduktion und kein `land(...)` aus diesem Zweig.

Wichtig: `Teleporter.teleport()` verwendet nicht den rekursiven `Game.move()`-Ablauf. Der Teleporter prüft selbst die Belegung des Zielorts, aber keine Bewegung durch die Zwischenfelder. Er löst auch keine Mine durch den normalen Minenabschnitt von `Game.move()` aus. Das entspricht der bestätigten Circuit-Chaos-Regel, dass Teleportieren keine Minen auslöst. Teleportieren in ein freies Feld im Abgrund ist als Spielverhalten erlaubt; der Teleporter selbst verweigert das Ziel nicht aufgrund des Bodentyps.

### Abweichung in `ComputerMoveInfo`

Die Simulation dupliziert die Teleporterformel lokal und verschiebt `x/y` direkt. Sie bildet dabei nicht den vollständigen Ablauf und die Entscheidung von `Teleporter.teleport()` ab. Insbesondere müssen Flugzustand, belegtes Zielfeld und der Unterschied zwischen erfolgreicher Teleportation (Programm-Schritt beendet) und normaler Bewegung konsistent modelliert werden. Auch die aktuell von der Simulation gelesenen Live-Positionen anderer Roboter sind keine sichere Grundlage für eine Kandidatensimulation.

## 5. Konsequenz für die Architektur

Wir sollten **nicht** `Game.move()` direkt für jeden KI-Kandidaten auf dem echten `Game`-Objekt aufrufen. Die Methode mutiert den Spielzustand, löst Schaden und Minen aus, verändert Objekte und kann Roboter töten. Ein Aufruf für einen bloß hypothetischen Programmvorschlag würde dadurch den echten Spielzustand beschädigen.

Stattdessen sollten wir die Regeln nicht unabhängig ein zweites Mal nachbauen. Ein sinnvoller Weg ist:

1. Einen eigenständigen Simulationszustand definieren, der mindestens Position, Blickrichtung, Höhe, Flugzustand, relevante Module, Spielfeldobjekte und die Positionen/Zustände aller Roboter enthält.
2. Die Bewegungsregeln schrittweise so aufteilen, dass reine Entscheidungen (z. B. welche Wand-/Rampenübergänge zulässig sind, wie Wasser/Öl die Bewegungsstärke beeinflussen, wie das Teleporterziel berechnet wird) von Seiteneffekten (Schaden verbuchen, Benachrichtigungen senden, echte Objekte ändern) getrennt sind.
3. Dieselben reinen Regeln sowohl in der echten Engine als auch in der Simulation verwenden, soweit das ohne Verhaltensänderung möglich ist. Die echte Engine führt anschließend die resultierenden Aktionen aus; die Simulation wendet sie auf eine Kopie ihres Zustands an.
4. Durch Tests sicherstellen, dass Engine und Simulation für kontrollierte Szenarien dieselben Positionen, Flugzustände, Blockaden und Fallfolgen ergeben.

Das ist ein Architekturvorschlag, keine Aufforderung zu einer sofortigen Umstrukturierung. Der nächste sinnvolle Schritt ist, zunächst die Bewegungs- und Teleporterregeln als kleine, testbare Entscheidungen zu isolieren und danach die gemeinsame Zustandsrepräsentation zu entwerfen. Bis dahin sollte `ComputerMoveInfo` als heuristische Näherung und nicht als regelgetreuer Simulator behandelt werden.
