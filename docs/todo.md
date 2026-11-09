# KI für Circuit Chaos

- [X] **Code-Inventur abschließen:** ComputerPlayer, ComputerMoveInfo und die tatsächliche Spielausführung systematisch gegenüberstellen.
- [X] **Simulationszustand kopierbar machen:** Einen Zustand erzeugen, in dem Bewegungen und Effekte ausprobiert werden können, ohne den echten Spielzustand zu verändern.
- [X] **Simulation gegen die echte Engine validieren:** Prüfen, ob beide bei denselben Programmen zu denselben Ergebnissen kommen.
- [ ] **Bewertungsfunktion entwickeln:** Checkpoints und Ziel, Route, Ausrichtung, Gefahren, Schaden, Gegner und Pit Stops berücksichtigen.
- [ ] **Waffenauswahl des ComputerPlayers:** Die Waffe muss – wie beim NetworkPlayer – zusammen mit dem Programm vor dessen Ausführung ausgewählt werden. Wenn der ComputerPlayer Waffen nicht oder nicht sinnvoll auswählt, fehlt ihm ein Teil der Spielstrategie.
- [ ] **Fünf Programmplätze gemeinsam optimieren:** Nicht nur den nächsten Zug betrachten, sondern sinnvolle Kombinationen der fünf Karten bewerten.
- [ ] **Strategie für KILL_ALL:** Die KI sollte Gegner gezielt ausschalten und gleichzeitig selbst überleben, ohne dabei das eigentliche Ziel aus den Augen zu verlieren. Das ist eine andere Bewertungsstrategie als das normale Rennen und sollte explizit berücksichtigt werden.
- [ ] **Gegner simulieren:** Auch die Roboter der anderen Spieler und deren mögliche Programme berücksichtigen, soweit deren geheime Informationen nicht bekannt sind.
- [ ] **Module systematisch integrieren:** Die tatsächlichen Effekte und Aktivierungsbedingungen aus dem Code abbilden.
- [ ] **Mehrere Runden vorausplanen:** Beispielsweise mit einer Beam Search über zwei bis drei Runden.

# Weitere TODOs

- [ ] **neue, mehr Roboter zulassen:** in robots/normal und robots/flying liegen bereits 72 neuen Roboter-Varianten - diese sind in das Spiel zu integrieren
- [ ] **Default-Module auswählbar machen:** die `default-modules`-Auswahl in der application.yml sollte nur als Default gelten, die eigentlichen Start-Module sollen bei der Start-Game-Auswahl ausgewählt werden können
- [ ] **Repulsor-Bewegungen** gegen Endlosschleifen absichern, insbesondere bei zwei gegenüberliegenden Repulsors.
- [ ] **Minen-Darstellung fehlerhaft:** Explodiert eine Mine auf einem Conveyor-Belt, so wird dieses in einem separaten Schritt angezeigt: 1) Roboter/Mine bewegen sich aufeinander zu, Explosion ist zu sehen, andere Roboter bleiben scheinbar stehen, 2) Conveyor Belts bewegen die anderen Roboter, der Roboter, der die Explosion ausgelöst hat, bleibt scheinbar stehen.
- [ ] **Stapel-Schieben:** Am Anfang des Spiels und wenn mehrere Roboter kaputt gehen, sind sie "aufeinander gestapelt". Fährt man in diesen Stapel rein, werden aber zurzeit nicht alle Roboter in diesem Stapel verschoben.
- [ ] **Landung bei Hovercraft:** Landet ein Roboter mit einem Hovercraft auf einem Feld, dass von einem anderen Roboter blockiert ist, soll er ein Feld weiter einen "Landeversuch" machen. Zurzeit scheint er den anderen Roboter zu verschieben.
- [ ] **Schalter-Course-Element:** kann Falltüren öffnen/schließen oder Repulsors (de-)aktivieren
- [ ] **Öl auf Förderbändern:** Öl soll mit dem Förderband genau ein Feld weiterbewegt werden und nicht anschließend auf weitere Ölfelder weiterrutschen. Aktuell verschwindet Öl auf einem Förderband stattdessen (Log: "Oil died").
