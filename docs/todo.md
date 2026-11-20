
# TODO-Liste

- **neue, mehr Roboter zulassen:** in robots/normal und robots/flying liegen bereits 72 neuen Roboter-Varianten - diese sind in das Spiel zu integrieren
- **Default-Module auswählbar machen:** die `default-modules`-Auswahl in der application.yml sollte nur als Default gelten, die eigentlichen Start-Module sollen bei der Start-Game-Auswahl ausgewählt werden können
- **Minen-Darstellung fehlerhaft:** Explodiert eine Mine auf einem Conveyor-Belt, so wird dieses in einem separaten Schritt angezeigt: 1) Roboter/Mine bewegen sich aufeinander zu, Explosion ist zu sehen, andere Roboter bleiben scheinbar stehen, 2) Conveyor Belts bewegen die anderen Roboter, der Roboter, der die Explosion ausgelöst hat, bleibt scheinbar stehen.
- **Stapel-Schieben:** Am Anfang des Spiels und wenn mehrere Roboter kaputt gehen, sind sie "aufeinander gestapelt". Fährt man in diesen Stapel rein, werden aber zurzeit nicht alle Roboter in diesem Stapel verschoben.
- **Landung bei Hovercraft:** Landet ein Roboter mit einem Hovercraft auf einem Feld, dass von einem anderen Roboter blockiert ist, soll er ein Feld weiter einen "Landeversuch" machen. Zurzeit scheint er den anderen Roboter zu verschieben.
- **Öl auf Förderbändern:** Öl soll mit dem Förderband genau ein Feld weiterbewegt werden und nicht anschließend auf weitere Ölfelder weiterrutschen. Aktuell verschwindet Öl auf einem Förderband stattdessen (Log: "Oil died").
