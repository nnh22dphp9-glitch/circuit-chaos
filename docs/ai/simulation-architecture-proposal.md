# Entwurf: Gemeinsame Bewegungsregeln und unabhängiger KI-Simulationszustand

Stand: develop, 2026-10-23. Dies ist ein Architekturentwurf, keine Änderung der Spielmechanik und noch keine Implementierung.

## Ziel und Leitplanken

Die KI muss viele mögliche Programme bewerten können, ohne dabei den echten Spielzustand zu verändern. Gleichzeitig darf sie Bewegungsregeln nicht unabhängig von der Engine ein zweites Mal implementieren.

1. **Simulation ohne Seiteneffekte:** Ein hypothetischer Zug verändert weder das echte Game, Course, Robot noch echte Module, Minen, Schadenswerte oder Benachrichtigungen.
2. **Gemeinsame Regeln:** Entscheidungen über Bewegungsstärke, Wandübergänge, Flug, Öl, Teleporter und Konflikte sollen – wo sinnvoll – von Engine und Simulation gemeinsam verwendet werden.
3. **Eigenständiger Kandidatenzustand:** Jede Kandidatenbewertung arbeitet auf einem unabhängigen Zustand. Zwei Kandidaten dürfen sich nicht gegenseitig beeinflussen.
4. **Schrittweise Migration:** Bestehendes Spielverhalten bleibt unverändert, bis gezielte Tests belegen, dass eine Extraktion äquivalent ist.

## 1. Grobe Architektur

Vorgeschlagene Verantwortlichkeiten (Namen sind Arbeitstitel):

- **Game / echte Spielausführung:** liest den echten Zustand, ruft gemeinsame Bewegungsregeln auf und wendet Ergebnisse mit Seiteneffekten an (Positionen ändern, Schaden/Minen ausführen, Benachrichtigungen senden).
- **ComputerPlayer:** lässt Kandidatenprogramme auf einem unabhängigen Simulationszustand bewerten.
- **SimulationState:** hält die eigene Kopie des für die Prognose relevanten Zustands.
- **MovementRules:** enthält gemeinsame, möglichst reine fachliche Berechnungen.
- **SimulationExecutor:** wendet Bewegungsentscheidungen auf den Simulationszustand an, nicht auf echte Spielobjekte.
- **SimulationSnapshotFactory:** erstellt den Kandidatenzustand aus dem aktuellen echten Spielzustand.

Engine und Simulation müssen nicht dieselben Ausführungsobjekte benutzen. Sie sollen dieselben fachlichen Regeln verwenden, soweit diese als nebenwirkungsfreie Berechnung ausdrückbar sind.

## 2. Kandidatenzustand

Der Simulationszustand muss den für die Prognose relevanten Zustand explizit enthalten. Die Simulation darf nicht während der Ausführung laufend Werte vom echten Robot, Player oder Course abfragen.

### Erste sinnvolle Zustandsversion

**Je Roboter**
- stabile ID zur Zuordnung;
- Position, Blickrichtung und Level;
- Flugzustand;
- auf dem Spielfeld / ausgeschieden / zerstört, soweit für die Prognose erforderlich;
- Schaden und blockierte Register, soweit sie Entscheidungen und Überleben beeinflussen;
- aktive Module je Phase, inklusive Bremsen und Hovercraft;
- relevante Modulzustände wie Ramming Armor und Schilde, sobald deren Regeln simuliert werden.

**Spielfeld**
- Geometrie: Koordinaten, Wände, Höhen, Bodenart, Wasser, Rampen und aktive Falltüren;
- bewegliche Objekte mit Typ, Position, Zielposition und regelrelevantem Zustand;
- Minen/Proximity-Minen und Aktivität, wenn diese Folgen simuliert werden;
- Förderbänder, Pusher, Zahnräder und deren phasenbezogene Zustände, sobald diese Simulationsstufe erreicht wird.

**Phasenzustand**
- aktuelle Phase;
- Reihenfolge/Priorität der Roboterbewegung, soweit bekannt;
- bereits erfolgte Aktionen der aktuellen Phase;
- Status wie alive, falling oder teleportedThisProgramStep, damit Ereignisse die restliche Bewegung korrekt beenden.

Die statische Spielfeldgeometrie muss nicht für jeden Kandidaten neu kopiert werden, wenn sie garantiert unveränderlich ist. Bewegliche Objekte und veränderliche Zustände müssen dagegen isoliert sein. Eine sichere erste Version darf eine tiefe Kopie verwenden; Optimierung durch immutable Board-Daten oder Copy-on-Write kommt erst nach Korrektheitstests.

**Nicht als Simulationszustand verwenden:**
- mutable Robot-Objekte aus dem echten Spiel;
- Player-Methoden, die über gameId auf das echte Game zugreifen;
- Game.move() auf dem echten Kurs;
- echte CourseObject-Instanzen, deren Position oder Aktivität verändert werden könnte.

## 3. Gemeinsame Regeln und getrennte Seiteneffekte

Die Trennlinie sollte nicht „Engine-Code versus KI-Code“ lauten, sondern **Regelentscheidung versus Ausführung eines Ereignisses**.

| Regelbereich | Gemeinsame, möglichst reine Berechnung | Ausführung mit Seiteneffekten |
|---|---|---|
| Programmbewegung | Programmtyp + Bremsen + relevante Module → Bewegungsstärke | Programm-/Registerzustand fortschreiben |
| Hovercraft | Modulaktivität + Bewegungsstärke → Flugzustand für diesen Schritt | Flugstatus des echten Roboters setzen |
| Wasser/Öl | Flugstatus + Ausgangsfeld + Bewegungsstärke → reduzierte Bewegungsstärke | Bewegung mit Ergebnis ausführen |
| Wand/Rampe/Einbahnwand | Zustand + Richtung → Übergang erlaubt, blockiert oder Spezialübergang | Position/Level ändern, Benachrichtigung |
| Konflikt/Schieben | Kandidatenzustand + Bewegungsabsicht → Konflikt-/Schiebeentscheidung | Positionen ändern, Ramming Armor und Schaden anwenden |
| Öl-Rutschen | Flugstatus + Kachel + Blockadeprüfung → nächster Schritt/Stop | Position ändern und gegebenenfalls Fall auslösen |
| Teleporter | Bewegung, Flugstatus, Richtung, Zielbelegung → nicht aktiv / blockiert / teleportiert + Ziel | Position setzen und Ereignis melden |
| Tod/Fall | Spielfeldzustand + Position + Flugstatus → Fall-/Todesereignis | Roboter entfernen, Schaden/Die-Counter/Benachrichtigung ändern |

Eine reine Funktion darf keine Game- oder Player-Methoden aufrufen, keine echten Modelle mutieren, keine Benachrichtigungen auslösen und keinen Zufallszustand verändern. Zufällige Ergebnisse müssen als Eingabe explizit sein oder über einen kontrollierten Zufallsdienst kommen.

## 4. Vorgeschlagene Bausteine

### A. MovementRules – gemeinsame fachliche Entscheidungen

Zunächst kleine Funktionen statt einer neuen großen Engine:

- Bewegungsstärke aus Programmtyp und Bremsen berechnen;
- Flugzustand für einen Programmschritt bestimmen, unter Berücksichtigung von Hovercraft und Bewegungsstärke;
- Wasser/Öl-Reduktion berechnen, nur am Boden;
- Teleporterabsicht und Ziel berechnen;
- Wand-/Rampenübergang anhand beider Felder und Bewegungsrichtung klassifizieren.

Die Funktionen sollten kleine Eingabe- und Ergebnisobjekte nutzen, nicht das gesamte Game. Nicht jede bestehende Methode muss sofort extrahiert werden. Insbesondere Game.move() ist zu groß und zu nebenwirkungsreich, um sie unverändert zwischen Engine und Simulation zu teilen.

### B. SimulationState – Zustand eines Kandidaten

Enthält den unabhängigen Zustand aller relevanten Roboter und beweglichen Objekte plus Zugriff auf Spielfeldgeometrie. Änderungen erfolgen nur innerhalb dieses Zustands.

### C. SimulationExecutor – Regeln auf Kandidatenzustand anwenden

Führt eine Bewegungsabsicht auf SimulationState aus. Er verwendet MovementRules, löst Konflikte im simulierten Zustand auf und gibt Ereignisse wie Moved, Blocked, Teleported, Fell oder TookDamage zurück.

Zunächst deckt er nur Roboter-Programmschritte ab. Förderbänder, Pusher, Zahnräder, Laser und Checkpoints werden später in der echten Phasenreihenfolge ergänzt.

### D. SimulationSnapshotFactory – echten Zustand sicher abbilden

Erzeugt zu Beginn der Kandidatenbewertung einen Snapshot aus Game/Course/Robot-Daten. Das ist der einzige Teil, der den echten Zustand liest. Die Simulation liest danach nur noch den Snapshot.

## 5. Teleporter als erstes Extraktionsbeispiel

Die vorhandene Teleporter.teleport()-Methode kombiniert Regelentscheidung und echte Ausführung:

- Regelentscheidung: Teleporter am aktuellen Feld? Bewegung ungleich null? Nicht fliegend? Ziel anhand Richtung und Bewegungsstärke berechnen? Ziel frei?
- Ausführung: game.setPosition(...), Benachrichtigung und Markierung des Programmschritts als teleportiert.

Diese Trennung ist klein genug, um sie früh zu testen. Die bestehende Methode sollte zunächst durch Tests abgesichert werden. Danach kann eine reine Berechnung wie resolveTeleporterIntent(...) eingeführt werden, die Engine und Simulation gemeinsam verwenden. Die echte Engine führt das Ergebnis weiterhin über ihre vorhandenen Mechanismen aus; die Simulation verändert stattdessen ihren Snapshot.

Zu erhalten sind diese Regeln:
- Flug verhindert die Teleportation.
- Ein belegtes Ziel verhindert die Teleportation; normale Bewegung läuft dann weiter.
- Ein freies Ziel beendet den normalen Programm-Bewegungsschritt.
- Der Teleporter prüft nicht die Zwischenfelder.
- Teleportieren löst nicht den normalen Minenablauf aus.
- Ein freies Zielfeld im Abgrund wird nicht allein wegen des Bodentyps abgelehnt.

## 6. Wasser/Öl/Hovercraft als erstes Regelmodul

Dieses Modul eignet sich als erster risikoarmer Extraktionsschritt.

Für einen Programmschritt:
1. Bewegungsstärke inklusive Bremsen berechnen.
2. Wenn Hovercraft aktiv ist, Flugzustand anhand movement != 0 setzen, bevor Wasser/Öl geprüft werden.
3. Teleporterregel vor der Wasser-/Öl-Reduktion anwenden, wie es die Engine derzeit macht.
4. Nur bei Nichtflug Wasser oder Öl unter dem Ausgangsroboter berücksichtigen und die Bewegungsstärke um höchstens eins Richtung null reduzieren.
5. Bewegung ausführen.
6. Öl-Rutschen nur nach tatsächlicher Bodenbewegung anwenden. Beim Fliegen und bei der anschließenden Hovercraft-Landung nicht rutschen.
7. Öl-Rutschen mit Blockaden, Wänden und Abgründen über den gemeinsamen Bewegungs-/Übergangsmechanismus auflösen, nicht durch ungeschützte x/y-Schleifen.

Die Reihenfolge ist an den Ist-Zustand der Engine angelehnt. Eine Änderung des Spielverhaltens wäre eine separate, ausdrücklich zu entscheidende Änderung.

## 7. Teststrategie: erst Gleichheit, dann Ausbau

Für jede extrahierte Regel sollten Tests sowohl die Regelentscheidung als auch die Wirkung auf echte Engine und Simulation abdecken.

### Erste Tests (MVP)

- [ ] Bremsen: vorwärts/rückwärts, Bewegungsstärke 0 und 1.
- [ ] Wasser am Boden: vorwärts/rückwärts reduziert um eins.
- [ ] Öl am Boden: Bewegungsreduktion und anschließendes Rutschen.
- [ ] Hovercraft-Abheben auf Wasser und Öl: keine Reduktion.
- [ ] Hovercraft-Flug über Öl: kein Rutschen während der Flugbewegung.
- [ ] Hovercraft-Landung auf Öl: kein nachträgliches Rutschen.
- [ ] Öl-Rutschen: freier Weg, Wandblockade, Roboterblockade, Abgrund.
- [ ] Teleporter: Flug, Bewegungsstärke 0, positive und negative Bewegung, freies Ziel, belegtes Ziel, Ziel im Abgrund.
- [ ] Kandidatenisolation: Simulation verändert weder echten Roboter noch Kursobjekte, Module, Schaden oder Positionen.
- [ ] Kandidatenreproduzierbarkeit: dieselben Eingaben ergeben denselben simulierten Zustand.

Wo möglich, soll ein Test denselben kontrollierten Ausgangszustand auf echte Engine und Simulation anwenden und die beobachtbaren Ergebnisse vergleichen. Für echte Engine-Tests müssen externe Benachrichtigungen und globale Spielregistrierung kontrolliert eingerichtet werden; Produktionsregeln dürfen nicht nur für Tests dupliziert werden.

## 8. Empfohlene Reihenfolge der Umsetzung

1. Tests für vorhandenes Teleporter- und Wasser/Öl/Hovercraft-Verhalten ergänzen, ohne die Implementierung umzuschreiben.
2. Kleine reine Bewegungsberechnungen extrahieren und die echte Engine darauf umstellen; nach jedem Schritt bestehende Tests ausführen.
3. SimulationState und SimulationSnapshotFactory einführen, zunächst mit Position, Richtung, Level, Flugzustand, relevanten Modulen, Boden-/Wandzugriff und allen Roboterpositionen.
4. Bewegung und Teleporter für einen Kandidatenzustand simulieren; Kandidatenisolation und Vergleichstests ergänzen.
5. Wand-/Rampenübergänge, Konflikte und Schiebeketten nach und nach in die gemeinsame Regelbasis aufnehmen.
6. Erst danach Förderbänder, Pusher, Zahnräder, Laser, Minen und Checkpoints als vollständigen Phasenablauf modellieren.
7. Zum Schluss die Bewertung in ComputerMoveInfo von der konkreten Bewegungsimplementierung entkoppeln. Eine bestehende Heuristik kann vorübergehend weiter genutzt werden, aber nicht als Regelquelle.

## 9. Risiken und bewusste Nicht-Ziele

- **Game.move() nicht direkt auf echten Objekten für Kandidaten aufrufen:** zu viele Seiteneffekte.
- **Keine vollständige Kopie des gesamten Game-Modells im ersten Schritt:** unnötig großer Umbau und schwer zu testen.
- **Keine Änderung der Spielregeln als Nebenprodukt der Extraktion:** die bestehende Engine ist Referenz, außer eine bewusste Regelkorrektur wird separat beschlossen.
- **Keine Optimierung vor Korrektheit:** erst tiefe Kopie und klare Datenflüsse, später gegebenenfalls immutable Board-Daten/Copy-on-Write.
- **Keine Annahme, dass unbekannte gegnerische Programme bekannt wären:** Simulationen müssen mit bekannten Zuständen und später gegebenenfalls Szenarien für unbekannte Programme arbeiten.

## Fazit

Empfohlen ist eine **kleine gemeinsame Regelbasis plus unabhängiger Kandidatenzustand**, keine zweite Spiel-Engine und kein Aufruf der echten Engine auf mutierbaren Live-Objekten. Teleporter und Wasser/Öl/Hovercraft sind gute erste Extraktionsschritte. Konfliktauflösung und Schiebeketten folgen erst, wenn Snapshot und Teststruktur stehen.

Die nächsten Codeänderungen sollten nach den Tests erfolgen und jeweils klein genug sein, um nachzuweisen, dass die echte Engine ihr bisheriges Verhalten beibehält.
