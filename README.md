# szbsb
Diese App ermöglicht über einen BSB-Zugang die Süddeutsche Zeitung zu lesen: \
https://www.bsb-muenchen.de/recherche-und-service/anmelden-ausleihen-bestellen/anmelden/

*Ohne BSB-Konto ist diese App nicht sinnvoll nutzbar.*

Diese App wird mit Android-Studio gebaut und ist in Kotlin und Python geschrieben. Die Verbindung zwischen beiden Teilen wird durch Chaquopy hergestellt. Der Python-Teil implementiert die Authentifizierung gegenüber dem BSB-Server, den Zugriff auf die Navigationsseiten und lädt die PDFs herunter. Der Kotlin-Teil implementiert eine Benutzeroberfläche um die Konfiguration zu pflegen, den Download zu starten, einen PDF-Viewer samt Navigation, sowie eine Verwaltung für die PDFs. Der PDF-Viewer basiert auf Pdfium und ergänzt dessen wertvolle Features um eine eigene Gestensteuerung.
