# Circuit Chaos – Spielanleitung (Regelentwurf)

> Dieses Dokument sammelt die bisher festgelegten Spielregeln und das Spielverhalten in einer Form, die später als Anleitung für Spieler dienen kann. Es beschreibt **Regeln und Spielabläufe, keine technische Implementierung**.
>
> Wo eine Regel noch nicht abschließend geklärt oder vollständig geprüft ist, steht das ausdrücklich im Abschnitt **Offene Punkte**. Diese Stellen sollten vor einer Veröffentlichung als verbindliche Anleitung entschieden werden.

## 1. Spielidee und Ziel

In **Circuit Chaos** programmieren die Spieler ihre Roboter mit Bewegungskarten. Anschließend führen die Roboter ihre Programme Schritt für Schritt aus. Dabei müssen sie den Kurs, andere Roboter und die eingebauten Gefahren berücksichtigen.

Je nach Spielziel gewinnt man, indem man das Ziel beziehungsweise die Kontrollpunkte erreicht oder alle gegnerischen Roboter ausschaltet.

### Spielziele

- **Finish:** Die Kontrollpunkte müssen in der vorgegebenen Reihenfolge erreicht werden. Wer das Finish zuerst erreicht, gewinnt.
- **Kill All:** Ziel ist es, alle anderen Roboter auszuschalten. In diesem Modus werden Kontrollpunkte nicht als normale Rennreihenfolge verwendet; die Startpositionen können über mehrere Startfelder verteilt werden.

## 2. Programmieren und Ausführen

### Programmregister und Karten

Ein Roboter besitzt fünf Programmregister. Für die Register werden Bewegungskarten mit Prioritäten programmiert. Die Karten bestimmen, welche Bewegung beziehungsweise Drehung der Roboter in der jeweiligen Phase ausführt.

Bekannte Karten:

- **Move 1, Move 2, Move 3, Move 4:** Vorwärtsbewegung um die angegebene Anzahl Felder.
- **Back-Up / Back-Up 2:** Rückwärtsbewegung um ein beziehungsweise zwei Felder.
- **Rotate Left / Rotate Right:** Drehung um 90 Grad nach links beziehungsweise rechts.
- **U-Turn:** Drehung um 180 Grad.

Die Programme werden anhand ihrer Prioritäten abgearbeitet; eine höhere Priorität wird zuerst ausgeführt. Bewegungen können durch Hindernisse, andere Roboter oder Kurseffekte beeinflusst werden.

### Ablauf einer Phase

Der grobe Ablauf ist:

1. Die Programme werden für die Phase aufgedeckt.
2. Die programmierten Roboterbewegungen werden ausgeführt.
3. Anschließend wirken die Kurselemente, beispielsweise Förderbänder, Pushers und Zahnräder.
4. Danach werden Laser und andere Strahlen beziehungsweise Waffen abgewickelt.
5. Es wird geprüft, ob Kontrollpunkte erreicht wurden und ob ein Spielziel erfüllt ist.

Dieser Ablauf wiederholt sich für die Programmphasen der Runde. Bestimmte Kurselemente wie Zahnräder und Förderbänder wirken in ihrer jeweiligen Phase unabhängig davon, ob ein Roboter sie bewusst ansteuert.

## 3. Bewegung, Wände und Höhenunterschiede

### Normale Bewegung

Ein Roboter bewegt sich in die Richtung, in die er ausgerichtet ist. Rückwärtskarten bewegen ihn in die entgegengesetzte Richtung, ohne dass er sich dafür drehen muss. Drehkarten ändern die Ausrichtung.

Wände und andere Hindernisse können eine Bewegung verhindern. Roboter können einander außerdem blockieren oder schieben.

### Höhenunterschiede, Abgründe und Rampen

- Ein Feld mit einer **LEDGE**-Kante liegt auf der niedrigeren Seite eines Höhenunterschieds.
- Eine Bewegung von einem niedrigeren auf ein höheres Feld ist ohne passende Rampe nicht möglich.
- Eine Bewegung bergab auf ein niedrigeres Feld ist grundsätzlich möglich und verursacht **einen Schadenspunkt**.
- Eine Überquerung über eine Rampe verursacht durch den Höhenunterschied keinen zusätzlichen Schaden.
- Die Rampenrichtungen **RAMP_UP** und **RAMP_DOWN** unterscheiden sich in ihrer Darstellung beziehungsweise Ausrichtung; das Überfahren einer passenden Rampe kostet keine zusätzliche Bewegung und verursacht keinen Höhenschaden.
- Auch wenn eine LEDGE-Kante durch eine andere Wandart überlagert ist, bleibt der Höhenunterschied für die Bewegung relevant.
- Ein Sturz in einen Abgrund oder durch eine aktive Falltür ist tödlich.

### Einbahnwände und Abstoßfelder

Es gibt Einbahnwände mit grüner und roter Kennzeichnung sowie Abstoßfelder. Die genaue Wirkung eines Abstoßfelds und die vollständigen Sonderfälle der Einbahnwände müssen vor der endgültigen Anleitung noch einmal gegen die Spielregeln geprüft werden. Einbahnwände werden im Kurs als zusammengehörige Paare angelegt.

### Fliegen und Hovercraft

Das Hovercraft-Modul ermöglicht Flugbewegungen. Während des Flugs gelten besondere Bewegungsregeln: Wände und Höhenunterschiede blockieren die Flugbewegung nicht wie eine normale Fahrt. Nach Ende der Flugbewegung landet der Roboter. Liegt das Landefeld tiefer als die aktuelle Flughöhe, verliert er pro Höhenstufe zwei Schadenspunkte; eine Landung in einem Abgrund oder auf einer aktiven Falltür ist tödlich.

## 4. Roboter schieben und Rammen

Roboter können andere Roboter schieben. Schieben kann mehrere Felder umfassen und wird durch Hindernisse und die Kursgrenzen begrenzt.

### Ramming Armor

- Die Ramming Armor schützt beziehungsweise unterstützt beim Rammen.
- Ihre Schutzwirkung ist während der jeweiligen Phase nur vorübergehend deaktiviert und steht in der nächsten Phase wieder zur Verfügung.
- Wird ein Roboter über zwei bis vier Felder geschoben, erhält der geschobene Roboter insgesamt höchstens **einen** Ramming-Schadenspunkt, nicht einen pro Feld.

Die genaue Abwicklung komplexer Kollisionen und mehrerer gleichzeitig schiebender Roboter ist noch als offener Prüfpunkt vermerkt.

## 5. Kurselemente und Gefahren

### Förderbänder

- Normale Förderbänder transportieren darauf stehende Roboter und Objekte in Pfeilrichtung.
- Express-Förderbänder können zusätzliche Bewegungsschritte ausführen.
- Drehende Förderbänder ändern beim Transport gegebenenfalls auch die Ausrichtung des Roboters.
- Förderbänder können hintereinander wirken, wenn ein transportierter Roboter auf einem weiteren Förderband landet.
- Wasserfelder mit Förderbanddarstellung werden bei der normalen Förderbandbewegung gesondert behandelt.
- Höhenunterschiede, Wände und Rampen beeinflussen, ob der Transport möglich ist. Ein Abstieg ohne Rampe verursacht einen Schadenspunkt; ein nicht erlaubter Aufstieg wird blockiert.

### Zahnräder

Zahnräder drehen Roboter, die darauf stehen. Es gibt Drehungen im Uhrzeigersinn und gegen den Uhrzeigersinn. Die Zahnräder arbeiten dauerhaft, sofern sie nicht durch eine besondere Spielregel oder ein Kurselement beeinflusst werden.

Der **Gyroscopic Stabilizer** verhindert, dass der Roboter durch Zahnräder oder Förderbänder gedreht beziehungsweise in seiner Ausrichtung verändert wird.

### Pusher

Aktive Pusher stoßen Roboter in ihre vorgegebene Richtung. Ein Pusher fährt hydraulisch aus und zieht sich unmittelbar danach wieder zurück. Für die erzwungene Bewegung gelten die relevanten Wand- und Höhenregeln.

### Öl

Öl kann dazu führen, dass ein Roboter nach einer Bewegung weiter rutscht. Die Rutschbewegung folgt der Bewegungsrichtung. Wände und unüberwindbare Höhenunterschiede stoppen die Rutschbewegung; ein Abstieg ohne Rampe verursacht einen Schadenspunkt. Portale können während der Rutschbewegung weiterhin wirksam werden.

### Wasser

Wasser beeinflusst die Bewegung und kann Bewegungen verlangsamen. Die genaue Kombination aus Wasser, Öl und einzelnen Kurselementen sollte vor der Veröffentlichung noch einmal abschließend festgehalten werden.

### Falltüren und Abgründe

- Ein Abgrund ist tödlich.
- Eine aktive Falltür lässt den Roboter in den Abgrund fallen und ist ebenfalls tödlich.
- Schalter können Kurselemente wie Falltüren oder Wände beeinflussen; die konkrete Wirkung hängt von der jeweiligen Kurskonfiguration ab.

### Teleporter

- Ein Teleporter versetzt einen Roboter entlang seiner aktuellen Ausrichtung.
- Die Entfernung des Teleports hängt von der ursprünglichen Bewegungsrichtung beziehungsweise dem Bewegungsbefehl ab.
- Teleportieren löst **keine Mine** auf dem Zielfeld aus.
- Das Teleportieren in ein Feld ohne Boden beziehungsweise in den Abgrund ist erlaubt. Ein Roboter kann dadurch absichtlich in den Abgrund geschickt werden.
- Mobile Teleporter können im Spiel vorkommen. Deshalb lässt sich nicht pauschal davon ausgehen, dass es auf einem Kurs immer nur eine feste Teleporter-Konfiguration gibt.

### Portale

Portale verbinden Positionen miteinander und können einen Roboter beim Betreten eines Portalfelds an eine andere Position versetzen. Die verschiedenen Portaltypen sind farblich gekennzeichnet. Sie sind von den Teleportern zu unterscheiden.

### Minen und Annäherungsminen

Minen verursachen bei ihrer Auslösung Schaden. Annäherungsminen reagieren auf die Nähe eines Roboters. Roboter können Minen mit entsprechenden Modulen legen. Die genaue Auslösebedingung und der Schaden der einzelnen Minentypen müssen in der endgültigen Anleitung noch vollständig präzisiert werden.

### Kleber und weitere Felder

Kleber kann die Bewegung beeinflussen. Weitere bekannte Kurselemente beziehungsweise Objekte sind unter anderem Randomizer, Gruben, Laser, Strahlungsfelder, Crusher und Drohnen. Ihre vollständigen Spielerregeln sind in diesem Entwurf noch nicht ausreichend beschrieben und werden deshalb nicht als abschließend dokumentiert ausgegeben.

## 6. Schaden, Zerstörung und Reparatur

Roboter sammeln Schaden durch Gefahren und Angriffe. Ein Roboter, der zerstört wird, scheidet aus dem Spiel aus. In **Kill All** ist das Ausschalten der gegnerischen Roboter das Spielziel.

### Power-Down

Bei einem Power-Down wird das bereits programmierte Programm noch vollständig ausgeführt. Das Aussetzen und das Zurücksetzen beziehungsweise der Verlust der bisherigen Schadenspunkte wirken sich erst in der folgenden Runde aus.

### Power-Down Shield

Der Power-Down Shield ist ein Modul, das im Zusammenhang mit dem Power-Down Schutz bieten soll. Welche Schadensarten er im Detail verhindert, muss noch abschließend bestätigt werden.

### Pit Stop

Der Pit Stop ist eine Reparaturstation. Die früher getrennten Funktionen für einfache und doppelte Reparatur wurden zu einem gemeinsamen Pit-Stop-Konzept zusammengeführt. Die genaue Reparaturmenge und die vollständigen Bedingungen sollten vor Veröffentlichung noch einmal verbindlich festgehalten werden.

## 7. Module und Waffen

Module verbessern Roboter oder geben ihnen zusätzliche Fähigkeiten. Waffen sind nicht grundsätzlich bei jedem Roboter aktiv; sie können im Spiel aufgenommen werden. Es kann jeweils nur **eine Waffe gleichzeitig aktiv** sein.

Die Waffe wird für eine Phase zusammen mit dem Programm ausgewählt. Sobald die Programmausführung begonnen hat, kann die Waffe für diese Ausführung nicht mehr gewechselt werden.

### Bekannte Module

Die folgenden Namen sind im Spiel vorgesehen; nicht alle Effekte sind in diesem Dokument bereits vollständig beschrieben:

- **Hovercraft:** ermöglicht Flugbewegungen.
- **Gyroscopic Stabilizer:** verhindert Drehungen durch Zahnräder und Förderbänder.
- **Ramming Armor:** beeinflusst das Rammen und dessen Schadenswirkung.
- **Power-Down Shield:** schützt im Zusammenhang mit einem Power-Down; genaue Schutzwirkung noch zu bestätigen.
- **Brakes:** beeinflussen die Bewegung durch Abbremsen.
- **Reverse Drive:** unterstützt Rückwärtsbewegungen.
- **Overdrive:** verändert die Bewegungsmöglichkeiten.
- **Layered Armor:** dient der Schadensabwehr.
- **Shield:** dient dem Schutz vor Schaden.
- **Extra Program:** erweitert die Programmiermöglichkeiten.
- **Superior Archive Copy:** verbessert die Archivkopie.
- **Main Laser, Double Barrel Laser, High Power Laser, Rear Laser:** unterschiedliche Laserwaffen.
- **Exchange Beam, Pressure Beam, Spin Left Beam, Spin Right Beam, Tractor Beam:** weitere Strahlenwaffen.
- **Mine Layer, Proximity Mine Layer, Oil Dispenser, Glue Dispenser:** legen beziehungsweise verteilen Objekte auf dem Kurs.
- **Mobile Teleporter, Mobile Randomizer, Bridge Projector:** verändern den Kurs beziehungsweise dessen Effekte.

### High Power Laser

Der High Power Laser kann durch den zuerst getroffenen Roboter hindurchschießen und einen weiteren Roboter dahinter treffen.

Die genauen Reichweiten, Schadenswerte, Munitionsregeln und Wechselwirkungen aller Waffen und Module müssen für eine vollständige Anleitung noch ergänzt werden.

## 8. Kontrollpunkte und Sieg

Im normalen Rennmodus müssen Kontrollpunkte in der vorgegebenen Reihenfolge erreicht werden. Wer das Finish zuerst erreicht, gewinnt. Der Pit Stop kann im Verlauf des Rennens zur Reparatur genutzt werden.

Im Modus **Kill All** gewinnt man, wenn alle anderen Roboter ausgeschaltet wurden. Für diesen Modus sind mehrere Startpositionen vorgesehen, damit Roboter nicht zwingend alle auf demselben Startfeld beginnen müssen.

## 9. Vor Veröffentlichung noch zu klären

Die folgenden Punkte sind bewusst nicht als abschließend geklärte Regeln formuliert:

- Vollständige Kollisions- und Schieberegeln, insbesondere mehrere Roboter und gleichzeitige Bewegungen.
- Exakte Effekte und Schadenswerte für Minen, Annäherungsminen, Laser, Strahlenwaffen, Strahlung, Crusher und Drohnen.
- Vollständige Wasser-, Öl- und Kleberregeln sowie ihre Wechselwirkungen.
- Genaue Funktion der Abstoßfelder und sämtliche Einbahnwand-Sonderfälle.
- Exakte Wirkung der Schalter auf Wände, Falltüren und andere dynamische Kurselemente.
- Genaue Reparaturmenge und Bedingungen am Pit Stop.
- Exakte Schutzwirkung von Power-Down Shield, Shield und Layered Armor.
- Munitionsregeln und vollständige Waffenwerte.
- Behandlung mehrerer oder überlappender Teleporter, insbesondere in Kombination mit mobilen Teleportern.
- Exakte Regeln für die Bewertung und das Spielende in allen Sonderfällen von Kill All.
- Ob und wie einzelne Module in jeder Phase aktivierbar sind und welche Wirkungen dauerhaft gelten.

---

**Redaktioneller Hinweis:** Dies ist eine konsolidierte Arbeitsfassung aus bisher besprochenen Regeln. Vor der Veröffentlichung sollte jeder offene Punkt entschieden und der Text anschließend mit dem tatsächlich spielbaren Verhalten abgeglichen werden.
