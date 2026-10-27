# Circuit Chaos – Spielanleitung (Regelentwurf)

> Dieses Dokument sammelt die bisher festgelegten Spielregeln und das Spielverhalten in einer Form, die später als Anleitung für Spieler dienen kann. Es beschreibt **Regeln und Spielabläufe, keine technische Implementierung**.
>
> Wo eine Regel noch nicht abschließend geklärt oder vollständig geprüft ist, steht das ausdrücklich im Abschnitt **Offene Punkte**. Diese Stellen sollten vor einer Veröffentlichung als verbindliche Anleitung entschieden werden.

## 1. Spielidee und Ziel

In **Circuit Chaos** programmieren die Spieler ihre Roboter mit Bewegungskarten. Anschließend führen die Roboter ihre Programme Schritt für Schritt aus. Dabei müssen sie den Kurs, andere Roboter und die eingebauten Gefahren berücksichtigen.

Je nach Spielziel gewinnt man, indem man das Ziel beziehungsweise die Kontrollpunkte erreicht oder alle gegnerischen Roboter ausschaltet.

### Spielziele

- **Finish:** Die Kontrollpunkte müssen in der vorgegebenen Reihenfolge erreicht werden. Wer das Ziel (Finish) zuerst erreicht, gewinnt.
- **Kill All:** Ziel ist es, alle anderen Roboter auszuschalten. In diesem Modus werden alle Kontrollpunkte zu Startpositionen, auf die die Roboter gleichmäßig verteilt werden.

## 2. Programmieren und Ausführen

### Programmregister und Karten

Ein Roboter hat fünf Programmregister, eines für jede Phase. Alle fünf Register müssen belegt werden. Die Spieler dürfen ihre verfügbaren Programmschritte frei auf diese fünf Phasen verteilen.

Bekannte Karten:

- **Move 1, Move 2, Move 3, Move 4:** Vorwärtsbewegung um die angegebene Anzahl Felder.
- **Back-Up / Back-Up 2:** Rückwärtsbewegung um ein beziehungsweise zwei Felder.
- **Rotate Left / Rotate Right:** Drehung um 90 Grad nach links beziehungsweise rechts.
- **U-Turn:** Drehung um 180 Grad.

Die Anzahl der zugeteilten Programmschritte beträgt grundsätzlich **neun minus den erlittenen Schaden**, zuzüglich des Werts des Moduls **Extra Program**, falls der Roboter es besitzt. Das Extra-Program-Modul beginnt mit einem Wert (*amount*) von 1. Jedes weitere Extra-Program-Modul, das der Roboter zufällig an einem Pit Stop erhält, erhöht diesen Wert um 1.

Nicht auf die fünf Register verteilte Programmschritte verfallen am Ende der Runde. Nur blockierte Programmschritte werden in der nächsten Runde wiederverwendet.

Die Programme werden anhand ihrer Kartenprioritäten abgearbeitet; eine höhere Priorität wird zuerst ausgeführt. Bewegungen können durch Hindernisse, andere Roboter oder Kurseffekte beeinflusst werden.

### Blockierte Programmschritte und Zerstörung

Hat ein Roboter mehr als vier Schadenspunkte, werden Programmregister von hinten nach vorne blockiert: zuerst Phase 5, danach Phase 4 und so weiter. Ein blockierter Programmschritt bleibt im Register und kann nicht durch eine andere Karte ersetzt werden. Die blockierten Programmschritte werden wiederverwendet; dadurch bleibt das Programm auch dann vollständig, wenn dem Roboter nur noch vier oder weniger neue Programmschritte zugeteilt werden.

Das Extra-Program-Modul verändert weder die Schadensgrenze für das Blockieren von Registern noch die Schadensgrenze für die Zerstörung.

Bei **zehn oder mehr Schadenspunkten** wird der Roboter zerstört. Die Regeln für seine Rückkehr hängen vom Spielmodus ab: Im Finish-Modus erscheint er beim nächsten Zug an seiner Archivposition wieder, normalerweise mit zwei Schadenspunkten. Mit **Superior Archive Copy** kehrt er ohne Schaden zurück. Im Kill-All-Modus scheidet er dauerhaft aus.

### Power-Down und Programmkarten

Ein Power-Down setzt den Roboter für die folgende Runde aus. In dieser Runde kann er Schaden abbauen und anschließend wieder neun oder mehr Programmschritte erhalten. Der Power-Down hat darüber hinaus keinen eigenen Einfluss auf die Anzahl der zugeteilten Programmschritte.

### Geheimhaltung und gleichzeitige Programmierung

Alle Spieler programmieren gleichzeitig mit den ihnen zugeteilten Programmkarten und ihren verfügbaren Modulen. Sie kennen weder die noch nicht ausgeführten Programmschritte noch die Module der anderen Spieler, solange diese nicht eingesetzt wurden oder keinen sichtbaren Effekt ausgelöst haben. Aus früheren Runden und sichtbaren Auswirkungen können sie Rückschlüsse ziehen. Manche Module, etwa **Layered Armor**, können aktiv sein, ohne einen sichtbaren Hinweis darauf zu geben.

### Ablauf einer Runde

Nach der Programmierung aller Spieler wird das Programm in **fünf Phasen** abgearbeitet. Die folgenden Schritte werden in jeder dieser Phasen in der angegebenen Reihenfolge ausgeführt:

1. **Programmschritt aufdecken und Reihenfolge festlegen:** Der Programmschritt der Phase wird sichtbar gemacht. Die Roboter werden nach Kartenpriorität sortiert, höchste Priorität zuerst. Randomizer können den aktuellen Programmschritt eines Roboters durch einen zufällig ausgewählten anderen Programmschritt ersetzen. Die drei Turn-Karten haben keine Priorität mehr; sie werden am Ende und untereinander in zufälliger Reihenfolge ausgeführt.
2. **Falltüren öffnen:** Falltüren, die in dieser Phase aktiv sind, öffnen sich. Roboter können dadurch bereits an dieser Stelle in den Abgrund stürzen und zerstört werden.
3. **Programmschritte ausführen:** Die Roboter führen ihren jeweiligen Programmschritt in der festgelegten Prioritätsreihenfolge aus. Zu Beginn des Programmschritts setzt ein Roboter gegebenenfalls durch aktivierte Module Geräte ab, beispielsweise Minen oder Brücken. Neu abgelegte Minen sind zunächst inaktiv. In dieser Phase bewegt sich der Roboter gemäß seinem Programmschritt.
4. **Express-Förderbänder bewegen:** Express-Förderbänder bewegen die Roboter und andere darauf befindliche Dinge; je nach Förderband kann dabei auch die Ausrichtung geändert werden.
5. **Förderbänder bewegen:** Nun bewegen sich sowohl normale als auch Express-Förderbänder und transportieren die darauf befindlichen Roboter und Dinge; dabei kann sich ebenfalls die Ausrichtung ändern.
6. **Pusher:** Aktive Pusher drücken Roboter gegebenenfalls ein Feld in die vorgegebene Richtung.
7. **Zahnräder:** Zahnräder drehen Roboter und andere Dinge auf ihnen.
8. **Strahlen und Laser feuern:** Zuerst feuern die Waffen der Roboter, danach die kursmontierten Strahlen und Laser. Die Reihenfolge der Schützen richtet sich nach derselben Prioritätsreihenfolge wie bei den Programmschritten.
9. **Kontrollpunkte und Minen prüfen:** Es wird geprüft, ob Roboter Kontrollpunkte oder das Finish erreicht haben. Dabei aktivieren sich Minen und explodieren unmittelbar, wenn ein Roboter auf einer Mine steht. Annäherungsminen können auch auslösen, wenn ein Roboter auf einem benachbarten Feld steht. Erreicht ein Roboter das Finish, wird er als Gewinner festgelegt. Mehrere gleichzeitige Gewinner werden derzeit nicht geprüft; das ist ein offener Punkt für eine mögliche Unterstützung mehrerer Finish-Felder.
10. **Kill-All-Sieg prüfen:** Im Kill-All-Modus wird der Gewinner anhand der Kill-All-Regeln neu bestimmt; dadurch kann der zuvor festgelegte Finish-Gewinner überschrieben oder auf keinen Gewinner gesetzt werden.

### Rundenende

Nach den fünf Programmphasen werden die Effekte am Rundenende abgewickelt. Zerstörte Roboter werden wieder auf das Spielfeld gesetzt, und der Schaden sowie der Power-Down werden entsprechend den jeweiligen Regeln verarbeitet. Danach erhalten die Spieler neue Programmkarten und die nächste Runde beginnt.

Die fünf Programmphasen wiederholen sich also nicht nur als einfache Roboterbewegungen: In jeder Phase wirken in festgelegter Reihenfolge auch Falltüren, Förderbänder, Pusher, Zahnräder, Waffen und Minen.

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

### Kleber

Kleber kann die Bewegung beeinflussen. Seine genauen Wirkungen und Wechselwirkungen mit anderen Kurselementen müssen für die endgültige Anleitung noch vollständig beschrieben werden.

### Randomizer

Steht ein Roboter auf einem Randomizer, wird sein nächster Programmschritt durch einen zufällig ausgewählten anderen Programmschritt ersetzt.

### Laser und Strahlen

Laser und Beams schießen grundsätzlich in die Richtung, in die der Roboter ausgerichtet ist. Die Ausnahme ist der **Rear Laser**, der nach hinten schießt. Kursmontierte Laser und Beams schießen in die Richtung, in die sie auf dem Kurs zeigen.

Der erste Trefferpunkt ist bei einer Robotermodul-Waffe das nächste Feld vor dem Roboter, bei einer kursmontierten Waffe dagegen das Feld, auf dem die Waffe selbst montiert ist. Der Schuss läuft anschließend bis zum Spielfeldrand.

Ein Schuss wird aufgehalten von:
- einem anderen Roboter, der getroffen wird (Ausnahme: **High Power Laser**),
- soliden Wänden,
- Repulsors,
- Einbahnwänden in Rot-Grün-Richtung.

Ob Einbahnwände in Grün-Rot-Richtung Schüsse durchlassen, muss noch geprüft werden. Höhenunterschiede werden beim Schießen absichtlich ignoriert: Eine LEDGE hält einen Schuss nicht auf. Ein Roboter auf einer anderen Höhe kann jedoch getroffen werden und den Schuss aufhalten.

Die Waffen haben folgende Wirkungen:

| Waffe | Wirkung |
|---|---|
| **Main Laser** / **Single Course-Mounted Laser** | Verursacht bei einem Treffer 1 Schadenspunkt. |
| **Double Barrel Laser** / **Double Course-Mounted Laser** | Verursacht bei einem Treffer 2 Schadenspunkte. |
| **Triple Course-Mounted Laser** | Verursacht bei einem Treffer 3 Schadenspunkte. |
| **High Power Laser** | Verursacht beim ersten getroffenen Ziel 1 Schadenspunkt. Der Schuss geht durch dieses Ziel hindurch und kann ein zweites Ziel treffen, das ebenfalls 1 Schadenspunkt erhält. |
| **Rear Laser** | Schießt nach hinten und verursacht bei einem Treffer 1 Schadenspunkt, wie der Main Laser. |
| **Exchange Beam** | Tauscht die Position des schießenden Roboters mit der Position des Ziels. Verursacht keinen Schaden. |
| **Pressure Beam** / **Course-Mounted Pressure Beam** | Drückt das Ziel ein Feld weiter. Verursacht keinen Schaden. |
| **Tractor Beam** / **Course-Mounted Tractor Beam** | Zieht das Ziel ein Feld zum Schützen heran. Verursacht keinen Schaden. |
| **Spin Left Beam** / **Course-Mounted Spin-Left Beam** | Dreht das Ziel um 90 Grad gegen den Uhrzeigersinn. Verursacht keinen Schaden. |
| **Spin Right Beam** / **Course-Mounted Spin-Right Beam** | Dreht das Ziel um 90 Grad im Uhrzeigersinn. Verursacht keinen Schaden. |

Die kursmontierten Waffen sind fest auf dem Kurs angebracht und feuern im Ablauf einer Phase nach den Waffen der Roboter. Einen kursmontierten Exchange Beam gibt es nicht, da dessen Ausgangspunkt dafür bewegt werden müsste.

## 6. Schaden, Zerstörung und Reparatur

Roboter sammeln Schaden durch Gefahren und Angriffe. Was bei einer Zerstörung passiert, hängt vom Spielmodus ab:

- **Finish:** Der Roboter scheidet nur für den Rest der laufenden Runde aus. Zu Beginn der nächsten Runde kehrt er an seine Archivposition zurück – dem zuletzt erreichten Kontrollpunkt oder Pit Stop – und bleibt im Spiel. Bei der Rückkehr hat er normalerweise zwei Schadenspunkte. Das Modul **Superior Archive Copy** kann diesen Schaden auf null reduzieren.
- **Kill All:** Ein zerstörter Roboter scheidet dauerhaft aus dem Spiel aus.

### Power-Down

Bei einem Power-Down wird das bereits programmierte Programm noch vollständig ausgeführt. Das Aussetzen und das Zurücksetzen beziehungsweise der Verlust der bisherigen Schadenspunkte wirken sich erst in der folgenden Runde aus.

### Power-Down Shield

Der Power-Down Shield ist ein Modul, das im Zusammenhang mit dem Power-Down Schutz bieten soll. Welche Schadensarten er im Detail verhindert, muss noch abschließend bestätigt werden.

### Pit Stop

Ein Pit Stop repariert maximal **einen Schadenspunkt** und teilt dem Roboter zusätzlich ein zufällig ausgewähltes neues Modul aus.

## 7. Module und Waffen

Module sind entweder von Beginn an vorhanden oder werden nach Erreichen eines Pit Stops am Ende der Runde (nach Phase 5) vergeben. Je nach Modul ist es direkt aktiv oder wird während der Programmierung berücksichtigt.

Einige Module besitzen **Ammunition** (auch *amount* genannt). Diese Module können mehrfach erworben werden; bei einem weiteren Erwerb erhöht sich die Ammunition jeweils um den Standardwert (*default amount*) des Modultyps. Module ohne Ammunition sind entweder vorhanden oder nicht vorhanden. Ein solches Modul kann am Pit Stop nicht erneut vergeben werden.

### Schusswaffen

Die Schusswaffen werden im Rahmen der Programmierung ausgewählt. Es kann jeweils nur eine Waffe gleichzeitig aktiv sein. Die folgenden Waffen haben keine Ammunition:

- **Main Laser**
- **Double Barrel Laser**
- **High Power Laser**
- **Rear Laser**
- **Exchange Beam**
- **Pressure Beam**
- **Spin Left Beam**
- **Spin Right Beam**
- **Tractor Beam**

Der **High Power Laser** kann durch den zuerst getroffenen Roboter hindurchschießen und einen weiteren Roboter dahinter treffen.

### Bewegungswaffe: Ramming Armor

Die **Ramming Armor** fügt in jeder Phase dem ersten Roboter, mit dem ihr Träger kollidiert beziehungsweise den er anfährt, einen Schadenspunkt zu. Das gilt auch dann, wenn der angefahrene Roboter wegen einer Wand oder eines anderen Hindernisses nicht verschoben werden kann. Die Ramming Armor besitzt keine Ammunition.

### Dispenser

Dispenser hinterlassen auf dem Feld, auf dem der Roboter steht, ein Device beziehungsweise Objekt. Der **Bridge Projector** platziert seine Brücke stattdessen auf dem Feld vor dem Roboter. Dispenser besitzen Ammunition, deren Wert zwischen 1 und 3 liegen kann.

Zu den Dispensern gehören:

- **Bridge Projector**
- **Glue Dispenser**
- **Oil Dispenser**
- **Mine Layer**
- **Proximity Mine Layer**
- **Mobile Teleporter**
- **Mobile Randomizer**

### Bewegungsverändernde Module

Diese Module besitzen keine Ammunition:

- **Hovercraft:** ermöglicht Flugbewegungen.
- **Extra Program:** erhöht die Anzahl der zugeteilten Programmschritte. Details stehen im Abschnitt zur Programmierung.
- **Overdrive:** ermöglicht, dass die Programmkarte **Move 4** ausgeteilt wird.
- **Reverse Drive:** ermöglicht, dass die Programmkarte **Back-Up 2** ausgeteilt wird.
- **Brakes:** erlaubt, einen Programmschritt um eine Bewegungseinheit zu verkürzen, bis hin zum vollständigen Anhalten.
- **Gyroscopic Stabilizer:** verhindert, dass der Roboter durch Zahnräder oder Förderbänder gedreht wird.

### Schutzmodule

- **Layered Armor:** passiver Schutz mit Ammunition. Der Standardwert beträgt 4; die Armor kann damit vier Schadenspunkte aufnehmen.
- **Shield:** schützt vor einem direkten Laserschuss. Die frühere Spielversion erlaubte, den Schutz pro Phase in eine Richtung auszurichten (vorne, hinten oder seitlich). Ob diese Richtungswahl noch Teil der aktuellen Implementierung ist, muss geprüft werden.
- **Power-Down Shield:** schützt den Roboter während des Power-Down-Modus vor Schaden. Er schützt nicht davor, durch Schieben in einen Abgrund gestoßen zu werden.

### Mechanical Arm

Der **Mechanical Arm** ermöglicht es, einen Checkpoint auch von einem benachbarten Feld aus zu erreichen. Er besitzt keine Ammunition.

### Weitere Module

- **Superior Archive Copy:** reduziert den Schaden bei der Rückkehr an die Archivposition auf null.

## 8. Kontrollpunkte und Sieg

Im normalen Rennmodus müssen Kontrollpunkte in der vorgegebenen Reihenfolge erreicht werden. Wer das Finish zuerst erreicht, gewinnt. Der Pit Stop kann im Verlauf des Rennens zur Reparatur genutzt werden.

Im Modus **Kill All** gewinnt man, wenn alle anderen Roboter ausgeschaltet wurden. In diesem Modus werden alle Kontrollpunkte zu Startpositionen, auf die die Roboter gleichmäßig verteilt werden. **Auch im normalen Finish-Modus können mehrere Startpositionen verwendet werden.**

## 9. Vor Veröffentlichung noch zu klären

Die folgenden Punkte sind bewusst nicht als abschließend geklärte Regeln formuliert:

- Vollständige Kollisions- und Schieberegeln, insbesondere mehrere Roboter und gleichzeitige Bewegungen.
- Ob Einbahnwände in Grün-Rot-Richtung Schüsse durchlassen.
- Vollständige Wasser-, Öl- und Kleberregeln sowie ihre Wechselwirkungen.
- Genaue Funktion der Abstoßfelder und sämtliche Einbahnwand-Sonderfälle.
- Exakte Wirkung der Schalter auf Wände, Falltüren und andere dynamische Kurselemente.
- Exakte Reparatur- und Modulausgabe-Details des Pit Stops.
- Exakte Schutzwirkung von Power-Down Shield, Shield und Layered Armor.
- Munitionsregeln und vollständige Waffenwerte.
- Behandlung mehrerer oder überlappender Teleporter, insbesondere in Kombination mit mobilen Teleportern.
- Exakte Regeln für die Bewertung und das Spielende in allen Sonderfällen von Kill All.
- Ob und wie einzelne Module in jeder Phase aktivierbar sind und welche Wirkungen dauerhaft gelten.

---

**Redaktioneller Hinweis:** Dies ist eine konsolidierte Arbeitsfassung aus bisher besprochenen Regeln. Vor der Veröffentlichung sollte jeder offene Punkt entschieden und der Text anschließend mit dem tatsächlich spielbaren Verhalten abgeglichen werden.
