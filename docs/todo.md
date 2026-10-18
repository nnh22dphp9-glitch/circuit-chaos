
# KI für Circuit Chaos

- [ ] **Code-Inventur abschließen:** ComputerPlayer, ComputerMoveInfo und die tatsächliche Spielausführung systematisch gegenüberstellen.
- [ ] **Simulationszustand kopierbar machen:** Einen Zustand erzeugen, in dem Bewegungen und Effekte ausprobiert werden können, ohne den echten Spielzustand zu verändern.
- [ ] **Simulation gegen die echte Engine validieren:** Prüfen, ob beide bei denselben Programmen zu denselben Ergebnissen kommen.
- [ ] **Bewertungsfunktion entwickeln:** Checkpoints und Ziel, Route, Ausrichtung, Gefahren, Schaden, Gegner und Pit Stops berücksichtigen.
- [ ] **Waffenauswahl des ComputerPlayers:** Die Waffe muss – wie beim NetworkPlayer – zusammen mit dem Programm vor dessen Ausführung ausgewählt werden. Wenn der ComputerPlayer Waffen nicht oder nicht sinnvoll auswählt, fehlt ihm ein Teil der Spielstrategie.
- [ ] **Fünf Programmplätze gemeinsam optimieren:** Nicht nur den nächsten Zug betrachten, sondern sinnvolle Kombinationen der fünf Karten bewerten.
- [ ] **Strategie für KILL_ALL:** Die KI sollte Gegner gezielt ausschalten und gleichzeitig selbst überleben, ohne dabei das eigentliche Ziel aus den Augen zu verlieren. Das ist eine andere Bewertungsstrategie als das normale Rennen und sollte explizit berücksichtigt werden.
- [ ] **Gegner simulieren:** Auch die Roboter der anderen Spieler und deren mögliche Programme berücksichtigen, soweit deren geheime Informationen nicht bekannt sind.
- [ ] **Module systematisch integrieren:** Die tatsächlichen Effekte und Aktivierungsbedingungen aus dem Code abbilden.
- [ ] **Mehrere Runden vorausplanen:** Beispielsweise mit einer Beam Search über zwei bis drei Runden.
- [ ] Regressionstests, Laufzeitmessung und Entscheidungslogging ergänzen.

Dabei bleibt die Vorgabe bestehen: keine zusätzliche KI-Bibliothek und keine laufenden Lizenzkosten. Wir wollen das mit Java und den vorhandenen Spielmechaniken umsetzen.

# Konkrete Spielmechanik-Tests
Diese Punkte würde ich zusätzlich aufnehmen, weil wir die Mechanik zuletzt genauer besprochen haben:
- [ ] **Ramming Armor:** Ein Roboter schiebt einen anderen zwei bis vier Felder weit. Der geschobene Roboter darf durch diesen Schiebevorgang nur einen Schadenspunkt durch den Ramm-Effekt erhalten.
- [ ] **Ramming Armor in späteren Phasen:** Sicherstellen, dass der Effekt in der nächsten Phase wieder verfügbar ist. Im Code-Review war kein offensichtlicher Reaktivierungsfehler erkennbar; der Test steht noch aus.
- [ ] **Power-Down:** Prüfen, dass der Roboter sein aktuelles Programm noch vollständig ausführt und erst in der darauffolgenden Runde aussetzt und seine bisherigen Schadenspunkte verliert.
- [ ] **Power-Down Shield:** Prüfen, dass der Schild während des Power-Downs den vorgesehenen Schaden durch Laserstrahlen verhindert.
- [ ] **Teleporter und Minen:** Die beabsichtigte Regel absichern, dass ein Teleporter nicht auf eine mit einer Mine belegte Position teleportiert; die Prüfung benachbarter Felder bleibt davon unberührt.
- [ ] **Laser-Zielermittlung:** Insbesondere den High Power Laser, Roboter als Hindernisse, Höhenunterschiede, Einbahnwände und Repulsoren testen.
Das sind zunächst Testaufgaben, keine Behauptung, dass die genannten Funktionen aktuell fehlerhaft sind.

# Weitere TODOs

- [ ] **Minen-Darstellung fehlerhaft:** Explodiert eine Mine auf einem Conveyor-Belt, so erzeugt sie einen zusätzlichen Conveyor-Belt-Animationsschritt (ohne dass sich wirklich was ändert), und der Explosions-Sound wird nicht abgespielt.
- [ ] **Board-Design:** Mehrere Start-Fehler
- [ ] **Board-Design:** Mehrere Finish- und CP-Felder
- [ ] **Schalter-Course-Element:** kann Falltüren öffnen/schließen oder Repulsors (de-)aktivieren
