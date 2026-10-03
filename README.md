# circuit-chaos
Copyright © 1999 - 2026 Philipp Diederich

You can start this game simply with
<code>%JAVA_HOME%\bin\java -jar circuit-chaos-<Version>.jar</code>
(after compilation, of course).

If SSL is required for the communication, und spring profile ssl and define the ssk properties below.
<code>%JAVA_HOME%\bin\java -jar circuit-chaos-<Version>.jar -Dspring.profiles.active=ssl</code>

On a server deployment, you can suppress the gui by starting the server headless.
This maybe helpful, if clients are behind firewalls and must use the pull-mechanism provided-
<code>%JAVA_HOME%\bin\java -jar circuit-chaos-<Version>.jar -Dspring.profiles.active=headless</code>

HAVE FUN!
