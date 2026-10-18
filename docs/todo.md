
# Empfohlene Reihenfolge der weiteren Arbeit

## Schadensbehandlung korrigieren
Alle Aufrufstellen prüfen und sicherstellen, dass sowohl verursachter Schaden als auch Umweltschaden korrekt funktionieren. Dabei Schutzmodule und Ausscheiden bei KILL_ALL berücksichtigen.

## Strahl-Zielermittlung korrigieren
Roboterziele, One-Way-Wände, Level und Repulsor-Felder nach deinen Regeln behandeln.

## High Power Laser gesondert prüfen
Ersten Treffer und Durchschuss zum zweiten Ziel sauber trennen.

## Waffenauswahl für den ComputerPlayer ergänzen
Eine feste Auswahl pro Phase, die vor der Programmausführung getroffen wird.

## Erst danach die KI-Angriffsstrategie entwickeln
Dabei dieselbe Zielermittlung wie die Spiel-Engine verwenden, statt eine zweite, abweichende Simulation zu bauen.

# Weitere TODOs

## Minen-Darstellung fehlerhaft
- Explodiert eine Mine auf einem Conveyor-Belt, so erzeugt sie einen zusätzlichen Conveyor-Belt-Animationsschritt (ohne dass sich wirklich was ändert), und der Explosions-Sound wird nicht abgespielt.

## Test Ramming-Armor
- Ein Roboter schiebt einen anderen über vier Felder: genau ein Ramming-Schadenspunkt.
- Derselbe Roboter verursacht in der nächsten Phase erneut einen Zusammenstoß: der Ramming-Effekt funktioniert wieder.

## Board-Design: Mehrere Start-Fehler

## Board-Design: Mehrere Finish- und CP-Felder

## Schalter-Course-Element
- Kann Faltüren öffnen/schließen
- Kann Repulsors (de-)aktivieren

