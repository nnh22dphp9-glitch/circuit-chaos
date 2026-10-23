# KI-Inventur: Bewegung, Kollisionen und Schieben

Stand: `develop`, geprüft am 2026-10-22. Dies ist die erste Mechanik-Matrix zur tatsächlichen Engine gegenüber `ComputerMoveInfo`. Sie beschreibt den Ist-Zustand, nicht die gewünschte endgültige KI-Architektur.

Die bestehende Computerlogik ist Legacy-Code und bildet nicht zuverlässig die heutigen Circuit-Chaos-Regeln ab. Für Regeln und Ablauf ist `Game` maßgeblich; `ComputerMoveInfo` ist lediglich die bisherige Heuristik-/Simulationsimplementierung.

## Bewertungslegende

- **Abgedeckt (vereinfacht):** Es gibt eine entsprechende Simulation, die aber nicht notwendigerweise alle Engine-Fälle abbildet.
- **Fehlt:** Die Mechanik wird nicht oder nicht ausreichend im Kandidatenzustand simuliert.
- **Zu verifizieren:** Aus der ersten Code-Lektüre lässt sich keine sichere Gleichheit ableiten; ein gezielter Test oder genauerer Abgleich ist nötig.

## Mechanik-Matrix

| Mechanik | Tatsächliche Engine | Aktuelle ComputerMoveInfo-Simulation | Befund |
|---|---|---|---|
| Programm-Schritt | `Game.robotsMove()` ruft für den Roboter den Programm-Schritt der aktuellen Phase auf; `singleRobotMove()` führt Bewegung und danach die Drehung aus. | Iteriert über fünf Programmplätze; dreht bei ROTATE_LEFT/RIGHT/U_TURN, sonst bewegt sie anhand von `ProgramType.getMovement()`. | Abgedeckt (vereinfacht). Reihenfolge und Randfälle sind noch zu testen. |
| Bremsen | `determineMovementAmount()` reduziert jede von null verschiedene Bewegungsstärke um eins, wenn `BRAKES` in dieser Phase verwendet wird. | Führt dieselbe Art Reduktion durch. | Abgedeckt; Test für Vorwärts- und Rückwärtsbewegung fehlt. |
| Wasser und Öl unter dem Roboter vor der Bewegung | `robotsMove()` setzt beim Hovercraft vor der Bewegungsreduktion `flying = (movement != 0)`. `applyWaterAndOilSlick()` reduziert danach nur, wenn `!ce.isFlying()` und das Ausgangsfeld Wasser oder Öl ist. Damit sind Wasser und Öl beim Abheben wirkungslos. | `ComputerMoveInfo` prüft Wasser nur bei `!flying`, Öl aber unabhängig vom Flugzustand. Außerdem liest es den Flugzustand vom echten Roboter statt ihn pro simuliertem Kandidaten/Phase abzuleiten. | **Bestätigter Simulationsfehler:** Öl reduziert in der Simulation auch während des Flugs die Bewegung. Die Engine ignoriert Wasser und Öl beim Abheben und während der Flugbewegung. Der Flugzustand der Simulation ist zusätzlich veraltet/gekoppelt an den Live-Roboter. |
| Wände und Einbahnwände | `move(...)` prüft die Wandregeln an Quell- und Zielfeld; Rampen, Kanten und Einbahnwände haben kontextabhängige Regeln. | `notBlocked()` prüft Wandtypen für die nächste Kachel, aber mit einer eigenen, kompakteren Regelmenge. | Abgedeckt (vereinfacht), konkrete Wand-/Rampen-Kombinationen vergleichen. |
| Andere Roboter als Hindernisse | `move(...)` ermittelt Konflikte mit anderen Robotern und kann den Bewegungsablauf rekursiv auflösen; der Aufruf der Roboterbewegung erlaubt ausdrücklich Konflikte mit beweglichen Robotern. | Kein eigenständiger Zustand für Positionen/Bewegungen anderer Roboter; `notBlocked()` prüft nur Wände. | Fehlt – zentrale Lücke. |
| Schieben und Kettenreaktionen | Die Engine löst Konflikte im Bewegungsablauf auf; Schieben kann weitere Roboter verschieben, blockiert werden oder Schaden verursachen. Ramming Armor wird im echten Bewegungsablauf phasenbezogen berücksichtigt. | Keine Simulation der gegnerischen Positionen, Schiebeketten, Schiebeschäden oder der Ramming Armor. | Fehlt – zentrale Lücke. |
| Höhen und Rampen | `Game.move()` und die Landungs-/Fallbehandlung berücksichtigen Level, Rampen und Höhenwechsel; ein Fall kann Schaden verursachen. | `ComputerMoveInfo` aktualisiert im Wesentlichen `x`, `y` und das aktuelle `Floor`; kein vollständiger eigener Level-Zustand oder Fall-/Schadensablauf. | Fehlt/vereinfacht; kann gefährliche Routen falsch bewerten. |
| Teleporter | Vor normaler Bewegung ruft `robotsMove()` `Teleporter.teleport(...)` auf. Das Ergebnis kann die Bewegung vollständig ersetzen. Zielbelegung und weitere Teleporterregeln werden in dieser zentralen Logik entschieden. | Sucht ein `TELEPORTER`-Objekt auf der aktuellen Position und berechnet eine Bewegungsdistanz per lokaler Formel; bildet den Rückgabewert von `Teleporter.teleport()` nicht nach. | Abweichung wahrscheinlich; zentrale Teleporterlogik muss als Referenz herangezogen werden. |
| Portale | Bei der normalen Bewegung prüft `Game.move()` Portalübergänge und setzt Position/Level am Ziel; Teleportieren/Portale können die weitere Bewegung verändern. | Prüft nach einzelnen simulierten Bewegungsschritten auf Portalobjekte und setzt `x/y` direkt auf die Zielposition. | Abgedeckt (vereinfacht); Kollisions-, Level- und Minenfolgen fehlen bzw. sind nicht gleichwertig nachgewiesen. |
| Minen während der Bewegung | Die Engine prüft bei relevanten Positionswechseln aktive Minen und Proximity-Minen und löst sie aus. | Keine Minenauslösung während der simulierten Bewegung. | Fehlt. |
| Abgrund und aktive Falltür | Die Engine behandelt Abgrund/Falltür während des tatsächlichen Bewegungsablaufs; der Roboter kann sterben und aus dem weiteren Ablauf entfernt werden. | `checkForAbyss()` setzt nur `willDie`; weitere Bewegungsschritte werden trotzdem simuliert. Falltüren werden anhand `activeInPhase[phase]` geprüft. | Teilweise abgedeckt; nach einem tödlichen Ereignis sollte die Simulation nicht so weiterrechnen, als bewege sich der Roboter normal weiter. |
| Flug/Hovercraft | `robotsMove()` setzt den Flugzustand abhängig von Bewegung und `HOVERCRAFT`; Flug beeinflusst Hindernisse, Wasser/Öl und Landung. | Liest `robot.isFlying()` vom echten Roboter und verwendet diesen Wert über die gesamte Simulation; aktualisiert ihn nicht anhand des simulierten Programm-Schritts. | Fehlt/abweichend; der Zustand wird nicht pro Kandidat und Phase simuliert. |
| Förderbänder nach Roboterbewegung | `stepCourseElementsMove()` bewegt zuerst Express-Förderbänder und danach normale Förderbänder. Die Phase normaler Förderbänder bewegt laut Engine ebenfalls Objekte auf Express-Bändern, sofern sie dort noch liegen. | Simuliert Express- und normale Förderbänder jeweils direkt am eigenen Roboter und nacheinander im lokalen Ablauf. Keine gemeinsame Auflösung aller Objekte oder Konfliktmarkierungen. | Fehlt/abweichend: keine global konsistente Förderbandauflösung mit anderen Robotern und Objekten. Wichtig: Express-Bänder können auch im Schritt der normalen Förderbänder erneut bewegt werden. |
| Pusher und Zahnräder | Nach den Förderbändern werden Pusher abgewickelt, danach Zahnräder. Pusher nutzen den echten Konflikt-/Bewegungsablauf; Zahnräder drehen nicht fliegende Objekte, wobei der Gyroscopic Stabilizer Roboter schützt. | Bewegt den simulierten Roboter einmal von einem aktiven Pusher weg und dreht ihn auf Zahnrädern; keine globale Objekt-/Roboterkonfliktauflösung. Die Stabilizer-Regel ist hier nicht erkennbar berücksichtigt. | Fehlt/abweichend; insbesondere Stabilizer und Pusher-Konflikte müssen ergänzt werden. |
| Ende einer Bewegung / Öl verlassen / Landung | Am Ende von `Game.move()` wird nur dann über Öl gerutscht, wenn sich die Position geändert hat und `!ce.isFlying()`. `land()` wird nach der Hovercraft-Bewegung aufgerufen und setzt `flying` erst nach der Konfliktbehandlung auf `false`; die vorangegangene Flugbewegung hat daher kein Öl-Rutschen ausgelöst. | Die Simulationsschleifen rutschen über Öl unabhängig davon, ob der simulierte Roboter fliegt; außerdem berücksichtigen sie keine dynamischen Blockaden zuverlässig. | **Bestätigter Simulationsfehler:** Öl-Rutschen darf während des Flugs und bei der Landung am Ende einer Hovercraft-Bewegung nicht ausgelöst werden. Am Boden muss Öl-Rutschen Blockaden berücksichtigen und kann in einen Abgrund führen. Details siehe [Bewegungsengine: Öl, Flug und Teleporter](movement-engine-analysis.md). |

## Wichtigste Lücken für die nächste Entwicklungsentscheidung

1. **Kein gemeinsamer Zustand für alle Roboter und beweglichen Objekte.** Daher können wir weder Kollisionen noch Schiebeketten, blockierte Förderbänder oder Gegnerbewegungen realistisch bewerten.
2. **Die Simulation liest Live-Zustand aus dem echten Spiel.** `ComputerMoveInfo` hält Referenzen auf `Game`, `Course` und `Player`; sein `x/y` ist nur eine partielle Kopie. Werte wie Flugzustand, Module, andere Roboterpositionen, Minen, Level und Spielfeldveränderungen stammen nicht aus einem unabhängigen Kandidatenzustand.
3. **Tödliche Folgen sind nur ein Flag.** `willDie` markiert den Tod, stoppt aber die Simulation nicht zuverlässig an diesem Punkt.
4. **Spezialbewegungen sind dupliziert.** Teleporter, Portale, Öl, Förderbänder, Pusher und Zahnräder werden lokal nachgebildet statt durch eine gemeinsame, überprüfbare Simulationslogik abgebildet.
5. **Die zeitliche Reihenfolge muss exakt bleiben.** Programmbewegungen inklusive Drehung → Express-Förderbänder → normale Förderbänder (die auch Express-Förderbänder bewegen können) → Pusher → Zahnräder → Strahlen/Laser → Checkpoints/Minen.

## Empfohlene Tests

Diese Tests sollten später gegen die echte Engine laufen und den resultierenden Zustand mit der Simulation vergleichen. Die Tests sind noch nicht implementiert.

- [ ] Vorwärtsbewegung und Rückwärtsbewegung mit 1–3 Schritten, jeweils ohne Hindernis.
- [ ] Drehung links/rechts und U-Turn nach einer Bewegung; Priorität/Programmreihenfolge berücksichtigen.
- [ ] Jede relevante Wand-/Einbahnwand-/Rampen-Kombination in beiden Bewegungsrichtungen.
- [ ] Roboter blockiert einen anderen; der Schiebende kann bzw. kann nicht weiterfahren.
- [ ] Schiebekette mit mehreren Robotern, einschließlich Rand, Wand und Abgrund.
- [ ] Ramming Armor: ein Schaden pro schiebendem Roboter und Phase, in späteren Phasen wieder verfügbar.
- [ ] Wasser, Öl und Flugzustand in Kombination mit Vorwärts- und Rückwärtsbewegung.
- [ ] Teleporter mit leerem Ziel, belegtem Ziel, Ziel im Abgrund und weiteren Teleportern gemäß den Circuit-Chaos-Regeln.
- [ ] Portalübergang mit Zielkonflikt und aktiver Mine/Proximity-Mine am Ziel bzw. in Nachbarfeldern.
- [ ] Abgrund/Falltür während einer mehrschrittigen Bewegung: Tod beendet die weitere normale Bewegung.
- [ ] Express-Förderband, das im Express-Schritt bewegt wird und danach noch auf einem Förderband liegt; prüfen, ob es im normalen Förderbandschritt erneut bewegt wird.
- [ ] Förderband-Konflikt mit Roboterkette; blockierte und bewegte Objekte müssen wie in der Engine aufgelöst werden.
- [ ] Pusher mit freiem Zielfeld, Wand und anderen Robotern.
- [ ] Zahnrad mit und ohne Gyroscopic Stabilizer; außerdem fliegender Roboter.

## Vorläufige Schlussfolgerung

Die vorhandene `ComputerMoveInfo`-Klasse ist als **Heuristik-Prototyp** weiterverwendbar, aber nicht als regelgetreuer Simulationskern. Die größte architektonische Lücke ist nicht ein einzelner fehlender Sonderfall, sondern das Fehlen eines unabhängigen, gemeinsamen Zustands und einer globalen Bewegungsauflösung.

Vor dem Implementieren einer neuen Simulation sollten wir als Nächstes die Details von `Game.move()` und `Teleporter.teleport()` vollständig inventarisieren und daraus festlegen, welche Teile der Engine gefahrlos wiederverwendet werden können und welche eine nebenwirkungsfreie Simulationsvariante brauchen.
