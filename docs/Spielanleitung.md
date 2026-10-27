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

Öl kann entweder bereits auf dem Kurs liegen oder von einem Roboter mit einem Oil Dispenser abgelassen werden. Anders als Kleber verbraucht sich Öl nicht: Einmal auf dem Kurs, bleibt es dort dauerhaft – außer es liegt auf einem Förderband und wird damit auf ein Abyss-Feld geschoben.

Betritt ein Roboter ein Ölfeld, rutscht er in Bewegungsrichtung weiter, bis er ein Feld ohne Öl erreicht oder seine Bewegung anderweitig blockiert wird. Das Rutschen verbraucht keine zusätzlichen Programmschritte. Beispiel: Führt ein Roboter einen Move-2-Schritt aus und betritt dabei im ersten Schritt ein Ölfeld, kann er insgesamt drei Felder zurücklegen: ein Feld durch den Move-Schritt und zwei weitere durch das Rutschen.

Bleibt ein Roboter tatsächlich auf einem Ölfeld stehen, benötigt er einen zusätzlichen Bewegungsschritt zum Anfahren; seine Räder drehen zunächst durch. Ein Hovercraft kann sich ohne diesen Anfahrschritt bewegen.

Noch zu prüfen ist, was passiert, wenn sich mehrere Öl-Felder auf einem Förderband aufstapeln, bevor sie in den Abyss geschoben werden. Vermutlich ändert sich dadurch nichts am Effekt, dies ist aber nicht bestätigt.

### Wasser

Wasser reduziert die Bewegung eines Roboters um einen Bewegungsschritt. Bei Move-1 und Back-Up-1 bleibt der Roboter im Wasser daher stehen. Hovercraft sind von diesem Effekt nicht betroffen.

Förderbänder funktionieren unter Wasser wie gewohnt und werden durch die Verlangsamung nicht eingeschränkt.

Noch zu testen ist die Kombination von Wasser und **BRAKES**: Es ist zu prüfen, ob ein Move-2-Programmschritt bei zusätzlicher Verwendung von BRAKES ebenfalls zum Stillstand führt.

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

Alle Portale funktionieren nach demselben Prinzip. Die Farben dienen der optischen Unterscheidung, insbesondere im Editor: Dort markiert ein Fadenkreuz in derselben Farbe das Zielportal beziehungsweise Zielfeld. Im eigentlichen Spiel ist das Ziel nicht sichtbar. Spieler können es daher nur aus früheren Spielen kennen oder im Editor nachsehen; die Computer-KI kennt das Ziel.

Portale können frei auf ein Ziel verweisen. Das Zielfeld muss nicht selbst ein Portal enthalten. Es können auch mehrere Portale derselben Farbe angelegt werden, etwa mehrere Portalpaare. Eine Einschränkung auf ein einzelnes gleichfarbiges Paar wie im Brettspiel besteht nicht; die Kursdesigner können die Portalverbindungen frei festlegen.

Ein Roboter wird beim Betreten eines Portalfelds sofort zum Ziel versetzt, sofern dort kein anderer Roboter steht. Ist das Zielfeld durch einen Roboter blockiert, findet keine Versetzung statt. Ein Roboter, der auf dem Portalfeld steht oder darauf landet, wird ebenfalls nicht teleportiert. Pro Spielphase kann ein Roboter nur einmal durch ein Portal versetzt werden; eine Portalversetzung löst keine weitere Versetzung aus. So werden Kettenreaktionen und Endlosschleifen verhindert.

Das Zielportal beziehungsweise Zielfeld wird unabhängig von dortigen Gefahren betrachtet. Ein Kursdesigner kann beispielsweise ein Portal auf einen Abgrund zeigen lassen. Der Roboter wird dorthin versetzt und stürzt dann in den Abgrund.

### Minen und Annäherungsminen

**Mine Layer** und **Proximity Mine Layer** legen Minen auf dem Feld ab, auf dem der Roboter steht. Beide Minentypen verursachen denselben Schaden und haben denselben Schadensradius. Die Proximity Mine kann zusätzlich durch einen Roboter auf einem angrenzenden Feld ausgelöst werden.

Der Schaden nimmt mit der Entfernung vom Explosionszentrum ab:

|  |  |  |  |  |
|---:|---:|---:|---:|---:|
| 0 | 0 | 1 | 0 | 0 |
| 0 | 1 | 2 | 1 | 0 |
| 1 | 2 | **4** | 2 | 1 |
| 0 | 1 | 2 | 1 | 0 |
| 0 | 0 | 1 | 0 | 0 |

Die Mine verursacht auf ihrem eigenen Feld 4 Schadenspunkte, auf den orthogonal angrenzenden Feldern 2 und auf den diagonal angrenzenden Feldern 1. Außerhalb dieses 5×5-Bereichs entsteht kein Schaden.

Eine Mine wird ausgelöst, wenn ein Roboter auf ihr Feld tritt beziehungsweise auf ihr landet. Eine Proximity Mine wird außerdem ausgelöst, wenn ein Roboter auf einem unmittelbar angrenzenden Feld steht. Neu abgelegte Minen sind zunächst **inaktiv** und werden erst nach dem ROBOTS_MOVE-Schritt aktiviert. Dreht sich ein Roboter während seines Programmschritts lediglich auf einer Mine, kann er diese anschließend selbst auslösen.

Minen können auch den Roboter treffen, der sie gelegt hat. Wird ein Roboter durch eine Bewegung, einen Pusher oder einen anderen Effekt auf eine Mine geschoben oder landet dort, kann er sie auslösen. Wenn mehrere Minen gleichzeitig explodieren, werden die verursachten Schadenspunkte addiert.

Noch zu prüfen ist, ob Wände die Auslösung beziehungsweise den Schaden einer Proximity Mine blockieren. Ebenfalls zu prüfen ist, ob Minen durch eine andere Minenexplosion zur Detonation gebracht werden, sobald sie selbst mindestens einen Schadenspunkt abbekommen (Kettenreaktion).

### Kleber

Ein Kleber hält einen Roboter fest und besitzt anfangs **3 Kleber-Punkte**. Solange der Roboter festklebt, wird jeder vorgesehene Bewegungsschritt stattdessen dazu verwendet, einen Kleber-Punkt abzubauen. Der Roboter muss also insgesamt mindestens drei Bewegungsschritte aufwenden, um sich zu lösen.

Dabei ist unerheblich, ob die Schritte in einem Zug (z. B. mit Move-3) oder über mehrere Programmschritte erfolgen. Auch Rückwärtsbewegung baut Kleber-Punkte ab. Ebenso spielt es keine Rolle, ob der Kleber durch einen Glue Dispenser auf den Kurs gebracht wurde oder bereits Teil des Kurses war.

Ein Hovercraft kann einfach vom Kleber wegfliegen; in diesem Fall greift der Kleber nicht.

Noch zu prüfen ist, wie Kleber beim Verschieben eines Roboters reagiert. Außerdem ist noch nicht geklärt, ob neu ausgelegte Kleber- und Öl-Devices – wie Minen – zunächst inaktiv sind.

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
- Kombination von Wasser und BRAKES (insbesondere Move-2 mit BRAKES).\n- Verhalten, wenn sich mehrere Öl-Felder auf einem Förderband aufstapeln.
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
