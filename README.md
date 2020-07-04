# Kurzbeschreibung
Diese App ermöglicht über einen BSB-Zugang die Süddeutsche Zeitung zu lesen. 
*Ohne BSB-Konto ist diese App nicht sinnvoll nutzbar.* 
Stand Juli 2020 ist das Konto kostenlos, Sie müssen persönlich in der BSB erscheinen und einen Wohnsitz in München nachweisen, für Details siehe https://www.bsb-muenchen.de/recherche-und-service/anmelden-ausleihen-bestellen/anmelden/ 

# Download / Installation
Laden Sie durch einen Klick auf https://github.com/snfiware/szbsb/releases/download/v1.2.1/szbsb-v1.2.1-Private-arm7-release.apk
das Installationspaket herunter. Bestätigen Sie die Sicherheitsabfrage und ermöglichen die Installation aus dieser Quelle. Nach der Installation starten Sie bitte die App und lesen in der Hilfe die Lizenzbestimmungen, in Kürze: Diese App vereinfacht den Zugriff auf die von der BSB bereitgestellten Dienste, automatisierte und systematische Downloads sind gemäß den BSB-Bestimmungen verboten. Bitte halten Sie sich daran.

# Architektur
Diese App wird mit Android-Studio gebaut und ist in Kotlin und Python geschrieben. Die Verbindung zwischen beiden Teilen wird durch Chaquopy hergestellt. Der Python-Teil implementiert die Authentifizierung gegenüber dem BSB-Server, den Zugriff auf die Navigationsseiten und lädt die PDFs herunter. Der Kotlin-Teil implementiert eine Benutzeroberfläche um die Konfiguration zu pflegen, den Download zu starten, einen PDF-Viewer samt Navigation, sowie eine Verwaltung für die PDFs. Der PDF-Viewer basiert auf Pdfium und ergänzt dessen wertvolle Features um eine eigene Gestensteuerung.

# Build from Source
https://github.com/snfiware/szbsb/wiki
